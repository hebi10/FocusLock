# FocusLock 내부 테스트 배포 명세

## 목표와 기준

사용자가 승인한 TripCanvas 방식(검증된 main → 앱별 서명 APK/AAB → 최초 내부 초안 → 내부 업데이트)을 FocusLock에 적용합니다. 기준 main은 `9b8279ae8d80d752aeb1b7e3a423aae5a1ac33ee`, 패키지는 `com.haebi.focuslock`, 현재 버전명은 `0.3.0`입니다. 기존 Kotlin/Compose/Gradle 구성과 병합된 사용 통계 기능은 유지합니다.

## 구조

- `Play internal release`는 현재 main의 `Android CI/verify`와 `Android full-screen emulator QA/emulator`가 모두 성공한 정확한 SHA만 처리합니다. Android CI main push 완료가 자동 배포를 한 번 유발하며, 같은 SHA의 에뮬레이터 검증이 끝날 때까지 최대 50분 기다립니다. 실패한 검증은 즉시 차단하고, 수동 실행은 main 및 이미 완료된 두 검증으로 제한합니다.
- `check`는 인증·인벤토리만 확인합니다. `build`는 서명 산출물만 생성합니다. `initialize`는 인벤토리가 비어 있을 때 versionCode 1의 internal draft만 생성합니다. 일반 `deploy`는 모든 번들·APK·트랙(초안 포함)보다 큰 versionCode를 할당합니다.
- 자동 최초 업로드는 main 커밋 제목의 독립 토큰 `[initialize-play]`로 명시합니다. 일반 main push는 `deploy`입니다. `google-play` 환경은 main으로 제한하고 WIF를 사용합니다.
- 서명키는 앱별 기존 키만 사용합니다. 생성·대체·다른 앱 키 재사용은 없습니다. AAB/APK의 패키지·버전명·코드·인증서와 native ELF(32비트 4 KB, 64비트 16 KB) 및 APK ZIP 정렬을 실제 산출물에서 검사합니다.
- 업로드 직전 main 최신 상태와 두 CI의 최신 시도 성공을 다시 확인합니다. CI는 실행 ID 대신 검증된 시도 시작 시각으로 비교하므로 이전 실행의 새 재시도를 우회하지 않습니다. 패키지별 동시 실행을 직렬화합니다. 업로드 응답 SHA-256과 커밋 후 새 edit의 트랙을 읽어 확인합니다. 최초 업로드는 새 edit의 번들 해시도 확인합니다.

## 범위와 준비 상태

production/closed 승격 워크플로와 CLI는 제공하지 않습니다. Play 앱 생성, App Signing 확인, 계정·테스터·정책 설문·스토어 스크린샷·개인정보 URL 등록은 Console에서 확인해야 합니다. 저장소 코드만으로 등록·검토·설치 가능 상태를 성공으로 표시하지 않습니다.

자격 증명·환경·WIF·앱별 Play 권한은 별도 계정 작업에서 설정 완료 확인되었으며 이 PR에서 변경하지 않습니다. 실제 서명 빌드·OIDC/Play API 연결·최초 업로드·공개 정책 URL·설치 가능성은 별도 실행 검증 전입니다. 개인정보 및 한국어 소개는 현재 코드의 기기 내 사용 기록·권한 조회와 기기 내 집중 기록·보상 저장 및 목표 알림 제약을 그대로 설명합니다.

## 방식 선택

승인된 참조 구현을 앱 식별자와 현재 CI에 맞춰 이식합니다. 별도 배포 서비스나 새 의존성은 추가하지 않습니다. 계정 비밀값을 저장소에 넣는 방식은 사용하지 않습니다.

## OPS-04 — 2026-11-25 실제 배포 회귀 보완
- 승인된 OPS-09 실행 45563456799: 정확한 main 5fc28176fbc1937ec809a553260f4073922d6534, 최종 QA 45563477508 성공, OPS-03 45564709665 성공, 서명 AAB 빌드 45564726965 성공(0.3.0/5, focuslock-signed-release-5). OPS-04 45565326012 verify에서 `Invalid source run ID`, publish skipped. 원래 빌드 ID는 정상 전달되었으나 Android versionCode용 최대 10자리 정규식을 GitHub 실행 ID에 잘못 적용했다.
- OPS-04 실행 ID를 정규형 양의 signed int64로 검증하고, 반환된 API 실행 ID의 일치까지 요구한다. 정확한 main/SHA/서명/패키지/버전/해시/아티팩트/승인 게이트는 완화하지 않는다. 11자리·최댓값·잘못된 경로·범위·다른 소스/실행 ID/워크플로 회귀 테스트 추가.
- 실패 verify에서는 기존 인증서 pin 환경 값이 비어 있었고 기존 google-play 환경의 서명 빌드는 인증서 검증 성공했다. verify도 동일한 기존 환경에서 인증서 검증 단계에만 pin을 주입한다. verify는 contents/actions read만 보유하며 Play 인증/OIDC 요청을 하지 않는다. publish는 기존 인증 및 별도 승인/동일 AAB 재검증 유지.
- 기존 AAB는 미업로드, 과거 실행 증빙으로 보존. 이 수정 병합 시 main SHA가 바뀌므로 동일 SHA 정책에 따라 기존 AAB를 새 main의 배포 입력으로 사용하지 않는다. 새 main CI 및 전체 QA, 새 SHA로 서명한 미사용 versionCode 빌드 후 내부 테스트에만 업로드. Play 비공개/정식/스토어 변경 없음.
