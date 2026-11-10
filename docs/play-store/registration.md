# FocusLock 내부 테스트 등록

## 현재 상태

이 변경은 배포 코드를 준비합니다. Play 앱 등록, 서명키 준비, WIF·환경 설정, 최초 업로드, 테스터 설치, Pages 공개 접근은 이 PR에서 수행하거나 확인하지 않았습니다. 현재 `0.2.0`의 사용 기록·권한 조회는 기기 내 처리이며 집중 타이머·정원은 시연 한계가 남아 있습니다. 기능 근거는 `docs/USAGE_API_QA.md`와 현재 소스입니다.

## GitHub 설정

`google-play` 환경의 배포 허용 브랜치를 `main`만으로 제한합니다. 다음 값은 비밀 설정에 직접 입력하며 채팅·커밋·로그에 남기지 않습니다. FocusLock용으로 준비한 키와 인증서만 사용합니다. 이미 앱 서명키가 있다면 교체하지 않으며, 다른 앱 키를 복사하지 않습니다.

| 환경 Secret | 용도 |
| --- | --- |
| `ANDROID_RELEASE_KEYSTORE_BASE64` | FocusLock keystore의 base64 |
| `ANDROID_RELEASE_STORE_PASSWORD` | keystore 암호 |
| `ANDROID_RELEASE_KEY_ALIAS` | 키 별칭 |
| `ANDROID_RELEASE_KEY_PASSWORD` | 키 암호 |
| `ANDROID_RELEASE_CERT_SHA256` | 서명 인증서 SHA-256 |

| 환경 Variable | 계획된 값: 설정·검증 대기 |
| --- | --- |
| `PLAY_WORKLOAD_IDENTITY_PROVIDER` | `projects/973619009476/locations/global/workloadIdentityPools/focuslock-github/providers/github-main` |
| `PLAY_SERVICE_ACCOUNT` | `play-focuslock@hebi-app-release-80847404.iam.gserviceaccount.com` |

WIF 조건은 저장소 ID `1452185615`, 저장소 `hebi10/FocusLock`, 소유자 ID `80847404`, ref `refs/heads/main`, 환경 `google-play`로 제한합니다. Cloud 프로젝트는 `hebi-app-release-80847404`입니다. 서비스 계정에는 해당 앱 내부 테스트 배포에 필요한 Play Console 권한을 별도로 부여해야 합니다. JSON 서비스 계정 키 fallback은 제공하지 않습니다. 문서의 식별자는 계획값이며 실제 존재·권한·연결 성공을 보증하지 않습니다.

## 검증과 실행

`Android CI/verify`와 `Android full-screen emulator QA/emulator`가 모두 현재 main의 정확한 SHA에서 성공해야 합니다. PR 실행 성공만으로 대체하지 않습니다. 최신 실패·취소·건너뛴 job을 이전 성공으로 우회하지 않습니다. 최신 시도는 GitHub의 `run_started_at`으로 비교하고, 업로드 직전에도 두 CI를 대기 없이 다시 확인합니다. 두 워크플로 모두 main push에서 실행합니다. 자동 배포는 Android CI 완료로 한 번 시작하고 동일 SHA의 에뮬레이터 완료를 최대 50분 기다립니다. 완료되지 않거나 main이 변경되면 업로드 없이 실패합니다.

GitHub Actions의 `Play internal release`를 main에서 수동 실행할 수 있습니다.

| 모드 | 결과 |
| --- | --- |
| `check` | 정확한 SHA 검증 및 인증 후 인벤토리 조회. 업로드·commit 없음 |
| `build` | 검증된 앱별 서명 APK/AAB 생성. Play API 호출 없음 |
| `initialize` | 빈 번들·APK·모든 트랙 인벤토리 확인 후 코드 1 internal draft 최초 생성 |
| `deploy` | 등록된 바이너리 확인 후 모든 Play 코드보다 큰 코드를 할당하여 internal 업데이트 |

최초 자동 업로드를 명시하려면 main 병합 커밋 제목에 독립 토큰 `[initialize-play]`가 있어야 합니다. 일반 main push는 `deploy`이므로 최초 바이너리가 없으면 실패합니다. 자동 배포 기본 internal 상태는 `completed`이며 최초 업로드는 항상 `draft`입니다. 수동 실행에서는 internal `draft`와 검토 대기 옵션을 선택할 수 있습니다. production/closed 승격 기능은 없습니다.

`build`의 `version_code`는 테스트 산출물 코드입니다. 실제 배포·초기화는 인벤토리를 다시 조회하여 할당·충돌을 검사합니다. 서명 APK/AAB와 안전한 JSON 보고서는 Actions 아티팩트에 보관합니다. 패키지·버전·서명, 32비트 ELF 4 KB 이상·64비트 ELF 16 KB 이상, AAB page alignment, APK ZIP 정렬을 검사합니다. 16 KB 기기 실행과 제조사별 통계 정확성 확인은 별도입니다.

최초 내부 초안은 새 edit에서 번들 해시와 트랙을 다시 읽습니다. 일반 배포도 업로드 해시와 새 edit의 트랙 읽기가 성공해야 성공으로 보고합니다. commit 또는 readback이 실패·불명확하면 Console을 확인한 후 재시도합니다. 자동 재시도나 키 교체로 우회하지 않습니다. App Signing, 심사·게시 상태, 테스터 설치 가능성은 별도 확인합니다.

## 개인정보와 스토어 자료

`listing-ko.json`은 현재 기능의 한국어 소개 초안이며 자동 제출하지 않습니다. `docs/privacy-policy.html`은 개인정보 방침 초안입니다. GitHub Pages Source를 GitHub Actions로 설정한 뒤 `Publish public privacy policy`를 실행하여 아래 후보 URL의 공개 접근과 내용을 확인해야 합니다.

```text
https://hebi10.github.io/FocusLock/privacy-policy.html
```

실제로 연락 가능한 지원 이메일을 Console과 정책에 보완해야 합니다. 스크린샷, 아이콘·그래픽, 연령 등급, 대상 연령, 광고·앱 액세스·데이터 보안 설문은 실제 앱과 계정 기준으로 작성해야 합니다. 사용 정보 접근은 기기 내 처리가 구현되어 있습니다. 앱 차단·목표 초과 실제 알림·집중 및 보상의 장기 저장을 제공한다고 표시하지 않습니다.

Manifest는 `allowBackup=false`이며 별도 `dataExtractionRules`가 없습니다. 자동 백업 비활성화 설정은 확인했지만 제조사별 기기 이전·복원 동작을 일괄 보증하지 않습니다.

## 검증 이력

- 기준 main `38ffc650ff2abf348206634d7fe125b37bd6d09d`의 Android CI push 실행 `42771077677` 성공을 확인했습니다. 이 SHA의 에뮬레이터 main 실행은 확인되지 않았으며 PR 검증으로 대체하지 않습니다.
- 로컬 Python 가드 전체 suite 및 워크플로 정적 검사를 실행하며 결과와 PR SHA Android 검증은 PR 본문에 기록합니다.
- 로컬 Gradle은 `services.gradle.org` 접근 시 `Network is unreachable`로 중단되어 Android 산출물을 생성하지 못했습니다. 성공한 Actions 빌드와 앱별 서명 산출물 생성은 별도 증거가 필요합니다.
