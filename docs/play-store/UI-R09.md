# UI-R09-FINAL — 실제 Android 스토어 이미지 검증·한국어 등록

기준일: 2026-11-12 · 앱 ID `com.haebi.focuslock` · 로캘 `ko-KR`

## 기준 상태
- UI-R08-FINAL: PR #29 병합, main `29a0b2cc80aa230809da95bf05b23060caed456c`.
- Issue #31 가독성 보완은 별도 PR #32에서 진행 중. **PR #32의 실제 CI·API36 QA 성공 및 안전 병합 이후** 최신 화면 캡처를 새로 수집한다.
- 이전 PR #30은 오래된 main에 기반한 Draft이므로 백업 `archive/ui-r09-old-pr30-20261112`을 보존하고 최신 main으로 재구성한다.
- UI-R08 PR #29의 API36 캡처 (run #43137344275)로 제작한 미리보기 8+1장은 **디자인 검토용**이다. Issue #31 반영 전이며 Play에 업로드하지 않는다.

## 사용자가 승인한 디자인
- 실제 Android API36 캡처만 사용, 화면 내부 수치·상태·스크린샷 편집 또는 가공 금지
- 화이트/중성 회색/블랙, 평면 구성. 3D, 장식성 그라데이션, 과장된 AI 이미지는 사용하지 않음
- 휴대전화 스크린샷: RGB PNG 1350×2400 **정확히 8장**
- 대표 그래픽: RGB PNG 1024×500 **1장**, SHA-256 manifest 포함
- 텍스트는 실제 앱 스크린샷 영역 바깥에만 배치
- 실제 구현되지 않은 자동 앱 차단·실시간 방해 앱 감지 등을 홍보하지 않음

## 작업 순서 및 승인 게이트

1. PR #32: Kotlin JUnit·Lint·빌드 및 API36 실제 화면 QA 완료 후 main 안전 병합.
2. PR #30: 최신 main으로 안전하게 동기화, 소스 SHA 가드·검증용 테스트만 PR에서 수행, 이미지 생성 작업은 PR에서 업로드를 실행하지 않음.
3. PR #30을 `[skip ci]`로 병합한 후 **GitHub Actions → Android full-screen emulator QA → Run workflow → branch main**을 수동 실행. 이 단계는 바이너리 업로드하지 않음.
4. 같은 최신 main SHA에서 성공한 API36 `emulator-smoke.yml` run이 있을 경우에만 **STORE-01 FocusLock Korean Play images → Run workflow → main / check**로 실제 캡처 다운로드 및 8+1장 제작.
5. artifact `focuslock-store-01-ko-KR`의 원본 SHA/run ID와 생성 이미지 PNG 8장/대표 1장, 해시·크기·순서·실화면 문구, 민감정보 노출을 검수한다. 문제 발생 시 새 캡처 후 재생성한다.
6. 검수 승인 후 **STORE-01 → Run workflow → main / upload** 실행 또는 소유자 명령 `/store-01-upload` (Issue #14) 사용. 업로드는 `ko-KR`의 `phoneScreenshots`·`featureGraphic`에 한정한다.
7. Play API 커밋 후 새 edit에서 8+1 이미지의 SHA·순서 재조회, 아이콘·테스트 트랙·AAB/APK·기타 언어가 그대로인지 확인한다. Play Console 심사/스토어 노출 및 테스터 설치는 별도 확인.

**주의:** `Android CI`의 main 수동 실행은 내부 테스트 자동 릴리스 워크플로를 작동시킬 수 있으므로 UI-R09 이미지 작업을 위해 실행하지 않는다. 필요한 것은 `Android full-screen emulator QA`의 수동 실행이다.

## 방어 검증
- `scripts/store/source_gate.py`가 실제 **현재 main SHA**를 GitHub ref에서 조회해 동일한 `head_sha`, main branch, 성공한 완료 상태, push/workflow_dispatch 이벤트만 수락한다. 과거 성공 기록과 PR 전용 캡처는 거절한다.
- 최소 37장 실제 1080×2400 스크린샷, 필수 소스 8개 및 원본·생성 PNG SHA 검증
- 생성부터 업로드 사이 main SHA 변경 시 업로드 중단
- 생성물 manifest의 `source_commit` 및 `source_workflow_run`을 재검증한 후에만 저장소 자산 반영
- 이미지 외 앱 코드/서명/Play release track 및 테스트 그룹 수정 금지

## 스토어 이미지 8장 구성
| 순서 | 파일명 | 화면 | 제목 |
| --- | --- | --- | --- |
| 01 | 01-home.png | 홈 | 스마트폰 사용 습관 |
| 02 | 02-analysis.png | 사용 분석 | 오늘부터 30일까지 |
| 03 | 03-app-detail.png | 앱별 상세 | 앱별 이용 기록 |
| 04 | 04-goals.png | 목표 설정 | 앱별 사용 목표 |
| 05 | 05-focus.png | 집중 진행 | 집중 시간을 기록 |
| 06 | 06-result.png | 집중 결과 | 집중 기록 저장 |
| 07 | 07-garden.png | 정원 | 집중하며 정원 성장 |
| 08 | 08-report.png | 리포트 | 집중 리포트 |

## 현재 결과 기록
- [x] UI-R08-FINAL main 병합 검증
- [x] PR #29 실제 캡처를 이용한 **비배포 미리보기** 8 PNG + 대표 1 PNG 생성, 각 PNG RGB/크기/SHA 및 ZIP 검사
- [x] PR #32 가독성 보완 코드 준비
- [x] PR #30 최신 main 기반 소스 선택·업로드 보호 코드 준비
- [ ] PR #32 CI·API36 QA 확인 및 병합
- [ ] PR #30 정적 CI 확인 및 병합
- [ ] 두 PR 병합 후 현재 main SHA와 동일한 emulator 캡처 성공
- [ ] 최종 8+1 생성 및 사람의 시각 검수
- [ ] Play 이미지 전용 업로드·새 edit 해시/순서 재조회
- [ ] Play Console 심사·실제 노출 확인

완료하지 않은 항목은 성공으로 표기하지 않는다. 해당 이외의 자동 리마인더/추가 자동 배포는 생성하지 않는다.
