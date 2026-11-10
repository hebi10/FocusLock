# FocusLock Play internal 테스트 배포 요청 — 2026-11-10

## 사전 확인

- GitHub Actions `#42782819755` **시도 2**에서 Google Cloud WIF 인증 성공.
- 해당 시도에서 서명된 FocusLock `0.3.0` AAB의 최초 등록 완료: Play `internal` `versionCode=1` `draft`.
- 업로드 해시, 초기 트랙 상태를 **새 Play edit**에서 재조회하여 일치 확인.
- 이 보고는 **초안 등록 성공**이며 테스터 설치 가능/게시 완료를 의미하지 않음.

## 다음 내부 테스트 업데이트

- 앱 대상 `com.haebi.focuslock` (다른 앱/트랙 변경 금지).
- 현재 검증된 `main` 소스에서 Android CI 및 에뮬레이터 QA **동일 커밋 SHA 전체 성공**이 전제 조건.
- 승인된 `internal` 트랙으로만 Play release `completed`를 요청; 코드는 현재 등록된 모든 번들·APK·트랙의 최대 버전보다 크게 할당.
- 기존 FocusLock 업로드 서명키 그대로 사용, 키 생성/교체 없음.
- API 해시 검증 및 커밋 후 **새 Play edit의 internal track readback**을 통과한 경우만 API 배포 성공으로 기록.
- Google Play Console의 심사·게시·테스터 설치 가능 상태는 API 결과와 별도로 재확인.
- `production`·`closed` 승격 금지.

## 스토어 아이콘

- 승인된 미니멀 아이콘 `assets/store/FocusLock_Play_Icon_512.png`.
- `[upload-play-icon]` 표시가 있는 `main` 병합 커밋은 해당 아이콘만 Play API에 등록하는 별도 1회 실행을 요청함.
- 이 아이콘 작업의 성공·실패를 확인한 뒤 새 일반 `main` 커밋으로 내부 테스트 업데이트를 실행.
- 아이콘 편집과 내부 테스트 릴리스를 동일 Play edit로 혼합하지 않음.

## 참고

- 최초 내부 초안 API 성공 실행: https://github.com/hebi10/FocusLock/actions/runs/42782819755
- 아이콘 자동 등록 PR: https://github.com/hebi10/FocusLock/pull/7
- WIF 복구·Play 점검 Issue: https://github.com/hebi10/FocusLock/issues/6
