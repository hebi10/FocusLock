# FocusLock 내부 테스트 등록

## 현재 상태

이 변경은 배포 코드를 준비합니다. Play 앱·WIF·Secrets·Pages 설정은 2026년 11월 10일 별도 계정 작업에서 완료 확인되었으며, 이 PR은 자격 증명이나 계정 상태를 변경하지 않습니다. 실제 서명 release 산출물, 최초 업로드, App Signing 및 테스터 설치는 실행 증거로 별도 확인해야 합니다. 현재 `0.3.0`은 사용 기록·권한 조회, 집중 세션·보상 로컬 DB 저장과 사용 목표 알림을 제공합니다. 알림은 약 15분 주기 작업과 앱 재진입 확인에 의존하며 정확한 실시간 발송을 보장하지 않습니다. 기능 근거는 `docs/USAGE_API_QA.md`와 현재 소스입니다.

## GitHub 설정

`google-play` 환경의 배포 허용 브랜치를 `main`만으로 제한합니다. 다음 값은 비밀 설정에 직접 입력하며 채팅·커밋·로그에 남기지 않습니다. FocusLock용으로 준비한 키와 인증서만 사용합니다. 이미 앱 서명키가 있다면 교체하지 않으며, 다른 앱 키를 복사하지 않습니다.

| 환경 Secret | 용도 |
| --- | --- |
| `ANDROID_RELEASE_KEYSTORE_BASE64` | FocusLock keystore의 base64 |
| `ANDROID_RELEASE_STORE_PASSWORD` | keystore 암호 |
| `ANDROID_RELEASE_KEY_ALIAS` | 키 별칭 |
| `ANDROID_RELEASE_KEY_PASSWORD` | 키 암호 |
| `ANDROID_RELEASE_CERT_SHA256` | 서명 인증서 SHA-256 |

| 환경 Variable | 계정 설정에서 확인할 값 |
| --- | --- |
| `PLAY_WORKLOAD_IDENTITY_PROVIDER` | `projects/973619009476/locations/global/workloadIdentityPools/focuslock-github/providers/github-main` |
| `PLAY_SERVICE_ACCOUNT` | `play-focuslock@hebi-app-release-80847404.iam.gserviceaccount.com` |

WIF 조건은 저장소 ID `1452185615`, 저장소 `hebi10/FocusLock`, 소유자 ID `80847404`, ref `refs/heads/main`, 환경 `google-play`로 제한합니다. Cloud 프로젝트는 `hebi-app-release-80847404`입니다. 서비스 계정에는 해당 앱의 조회·테스트 출시·테스터 관리 권한을 별도 계정 작업에서 부여했으며 production 권한은 없습니다. JSON 서비스 계정 키 fallback은 제공하지 않습니다. 위 식별자의 생성·권한 설정은 별도 계정 작업에서 확인했습니다. 실제 GitHub OIDC 토큰 교환과 Play API 호출 성공은 이 PR에서 아직 실행하지 않았습니다.

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

실제로 연락 가능한 지원 이메일을 Console과 정책에 보완해야 합니다. 스크린샷, 아이콘·그래픽, 연령 등급, 대상 연령, 광고·앱 액세스·데이터 보안 설문은 실제 앱과 계정 기준으로 작성해야 합니다. 사용 정보 접근은 기기 내 처리가 구현되어 있습니다. 앱 강제 차단·정확한 실시간 알림·클라우드 동기화를 제공한다고 표시하지 않습니다. 집중 세션·보상은 Room DB에, 정원 배치·목표/알림 설정은 기기에 저장합니다.

Manifest는 `allowBackup=false`이며 별도 `dataExtractionRules`가 없습니다. 자동 백업 비활성화 설정은 확인했지만 제조사별 기기 이전·복원 동작을 일괄 보증하지 않습니다.

## 검증 이력

- 기준 main `38ffc650ff2abf348206634d7fe125b37bd6d09d`의 Android CI push 실행 `42771077677` 성공을 확인했습니다. 이 SHA의 에뮬레이터 main 실행은 확인되지 않았으며 PR 검증으로 대체하지 않습니다.
- 로컬 Python 가드 전체 suite 및 워크플로 정적 검사를 실행하며 결과와 PR SHA Android 검증은 PR 본문에 기록합니다.
- 로컬 Gradle은 `services.gradle.org` 접근 시 `Network is unreachable`로 중단되어 Android 산출물을 생성하지 못했습니다. 성공한 Actions 빌드와 앱별 서명 산출물 생성은 별도 증거가 필요합니다.

## 003 main 통합

`9b8279ae8d80d752aeb1b7e3a423aae5a1ac33ee`의 Room/KSP/WorkManager, 목표 알림, 집중 기록·보상 저장 및 확장 계측 테스트를 그대로 보존했습니다. Gradle versionCode 충돌은 동적 배포 코드와 upstream 기본값 2를 함께 유지하여 해결하고, artifact/release versionName 검증을 `0.3.0`에 맞췄습니다. 최초 빈 인벤토리 업로드의 코드 1 규칙은 그대로입니다.

## 계정 설정 확인 (2026-11-10)

- Play 앱 ID `4974641493304969346`, 패키지 `com.haebi.focuslock`, Play 개발자 계정 `5537932067090246638` 생성 확인.
- 전용 서비스 계정의 앱별 조회·테스트 출시·테스터 관리 권한 활성화 확인. production 권한은 부여하지 않았습니다.
- 전용 WIF provider와 서비스 계정 연결은 저장소 ID `1452185615`, 소유자 ID `80847404`, main 및 `repo:hebi10/FocusLock:environment:google-play` subject로 제한했습니다.
- `google-play` 환경의 main 브랜치 제한, 서명 Secrets 5개와 인증 변수 2개 저장, Pages Source의 Actions 설정 확인. 비공개 업로드 키 백업은 소유자 전용 접근으로 확인했습니다.
- 실제 서명 release 빌드·최초 draft 업로드·공개 정책 URL 접근·App Signing·테스터 설치 검증은 별도 실행 전입니다.
