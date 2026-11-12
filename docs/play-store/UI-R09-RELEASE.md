# UI-R09 — 최신 Google Play 이미지·내부/비공개 테스트 배포 절차

## 권한과 목적
대상 앱: `com.haebi.focuslock` · GitHub: `hebi10/FocusLock`.
**사용자가 명시적으로 승인한 한 번의 UI-R09 릴리스 절차**를 지원합니다.
외부 PC·로컬 Android Studio 불필요. GitHub Actions 및 기존 Google Play WIF/업로드 서명만 이용합니다.

GitHub Issue #14에 저장소 소유자 `hebi10` 계정이 아래 **정확한 댓글 한 줄**을 등록하면 `UI-R09 owner-approved test and store workflow commands`가 대상 workflow를 main에서 수동 실행합니다. 이외의 사용자/이슈/본문은 실행되지 않습니다.

| 댓글 | 실행 워크플로 | 안전 범위 |
| --- | --- | --- |
| `/ui-r09-capture` | `emulator-smoke.yml` | main의 실제 Android API36 37화면 QA, Play 변경 없음 |
| `/ui-r09-images-check` | `store-01-images.yml` `check` | 최신 main 캡처의 이미지 8장+대표 1장 렌더 및 해시 검증, Play 변경 없음 |
| `/ui-r09-images-upload` | `store-01-images.yml` `upload` | 새 검증 및 Play `ko-KR` 이미지 전용 업로드/재조회 |
| `/ui-r09-android-ci` | `android-ci.yml` | main의 단위 테스트·Lint·debug APK, Play 자동 게시 없음(`workflow_dispatch`) |
| `/ui-r09-internal` | `play-internal.yml` `deploy` | 정확한 main의 CI+캡처 통과 후 새 버전 할당/기존 서명 AAB 생성 및 내부 테스트 업로드 |
| `/ui-r09-closed` | `play-internal.yml` `closed` | 완료된 최신 내부 AAB 버전(최소 code4)을 비공개 alpha에 안전 승격, AAB 재업로드 없음 |

## 순서
1. 이 기능 PR의 CI 통과 후 `[skip ci]`로 병합. 승인된 현재 main SHA를 확인
2. `/ui-r09-capture` 실행, API36 QA가 **실제 성공**했는지 확인
3. `/ui-r09-images-check` 실행, 캡처 원본/실제 화면, PNG 8장(1350×2400 RGB), 대표 1장(1024×500 RGB), manifest SHA/순서/문구 시각 검수
4. `/ui-r09-images-upload` 실행하여 Play API 이미지 등록 및 fresh readback 확인. 아이콘·AAB·비공개/내부 트랙은 변경하지 않음
5. **주의:** 이미지 업로드 워크플로가 `assets/play-store/ko-KR/`을 `main`에 `[skip ci]` 커밋할 수 있어 기존 QA가 **새 main SHA에서는 무효**. 최신 main SHA에 대해 `/ui-r09-android-ci`와 `/ui-r09-capture`를 다시 각각 한 번 실행해 둘 다 Success인지 확인
6. `/ui-r09-internal`로 새 서명 AAB 생성 및 내부 테스트 업로드 (versionCode는 모든 기존 빌드/트랙보다 큰 값 자동 할당). Play API 신규 edit에서 등록 결과 확인
7. 내부 테스트가 완료된 것을 **Play API 결과로 확인한 뒤에만** `/ui-r09-closed`. 내부 테스트의 완료된 단일 AAB code4+만 alpha로 승격. 기존 닫힌 트랙이 중복/중단/더 높은 코드면 자동 거부
8. 비공개 Play API readback 후 Google Play Console 심사·노출/테스터 링크는 별도 확인

## 중단 기준
- GitHub Actions가 대기/실패이면 다음 댓글 명령 실행 금지
- 이미지 출처 SHA가 `main`과 다르거나 이전 PR 캡처이면 이미지 생성·업로드 금지
- `Play internal release`에서 AAB 무결성/업로드 키/16KB 검사 또는 API upload/readback 실패이면 비공개 승격 금지
- 비공개 트랙의 기존 버전, 릴리스 상태, 테스터·심사 요청이 예상과 다르면 API 커밋 전에 차단
- Google Play API의 `completed`는 Play Console 심사 완료나 테스터의 실제 설치 가능 상태를 보장하지 않음

## GitHub Actions 링크
- 원본 캡처: https://github.com/hebi10/FocusLock/actions/workflows/emulator-smoke.yml
- 이미지: https://github.com/hebi10/FocusLock/actions/workflows/store-01-images.yml
- Android CI: https://github.com/hebi10/FocusLock/actions/workflows/android-ci.yml
- Play 내부/비공개 테스트: https://github.com/hebi10/FocusLock/actions/workflows/play-internal.yml
- 소유자 댓글 명령: https://github.com/hebi10/FocusLock/issues/14

이 문서는 사용자 승인된 릴리스 순서를 기록한 것으로, 실제 실행·검증이 완료되지 않은 단계를 성공이라고 선언하지 않습니다.
