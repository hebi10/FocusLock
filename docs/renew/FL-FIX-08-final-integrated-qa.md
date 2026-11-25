# FL-FIX-08 — 최종 통합 QA 및 증빙

## 선행
- FL-FIX-01~07의 모든 변경 PR이 검증을 통과하고 main에 병합된 후 진행. #67의 최신 API29/35/36 기기 QA는 별도 확인.
- FL-FIX-08 증빙 추가 PR도 검증·병합 후 최신 main SHA를 다시 고정한다.
- Android CI, OPS-08 정책 검증 및 해당 SHA의 Android full-screen emulator QA 성공을 구분한다.

## 검증 범위
- 기존 실제 Android API36 스크린샷 37장과 분석 화면 증빙, 온보딩, 권한 허용/거부·복귀, 앱 상세와 목표, 실측 통계 및 결측 처리, 집중 기록·보상·재시작, 알림 진입, 로컬 데이터 내보내기 모델 및 삭제 보호.
- 신규 화면 5장: 목표 관리, 목표 이력, 데이터 관리, 목표 이력 삭제 확인, 집중 기록 및 보상 삭제 확인. 삭제 확인 화면 테스트는 취소하며 실제 사용자 데이터를 지우지 않는다.
- 실제 리포트 날짜 선택 및 월간 눈금 검증을 최종 API36 QA에도 포함. 실제 화면 2장 추가 검증.
- 기존 UI-R00의 고정 37장 검사 유지. 신규 7장은 별도 fl-fix08-evidence.json에 파일별 SHA-256·검증 커밋·실행 ID를 기록.
- API29/35 및 폴더블/태블릿/160% 글꼴의 선행 호환성 QA 증빙을 보존. 실제 Fold/태블릿 하드웨어의 측정, TalkBack, 잠금·제조사 절전 정책, Android 문서 선택기를 통한 실제 파일 저장·실패는 실기기 별도 확인.
- 기기별 속도·전력 개선 수치는 실측 없이는 확정하지 않는다.

## 실행 (QA만, Play 변경 없음)
- GitHub Actions > Android full-screen emulator QA (emulator-smoke.yml) > Run workflow, main 선택, expected_main_sha에 최신 main의 전체 SHA 입력.
- 연결된 API에 workflow_dispatch 쓰기 기능이 없으면 사용자가 위 화면에서 실행한다. CI 성공이나 PR 계측 성공을 최종 main QA 성공으로 간주하지 않는다.
- 성공한 실행의 focuslock-android-fullscreen-captures artifact에서 ui-r00-audit-report.json과 fl-fix08-evidence.json, 신규 캡처를 확인한다. 검사 통과와 시각 검토/실기기 검증은 별개다.
- 실패·누락·SHA 변경 시 보완하고 정확한 새 main에서 재검증. 장시간 실행 링크를 기록하고 불필요한 재실행을 피한다.

## FL-FIX-09 연결
- 기존 #56 스토어 preflight는 최신 기준과 정합화하되, 최종 QA 및 최신 실제 캡처에 맞춰 검증한다.
- 스토어 이미지 생성 체크는 업로드와 구분하며 공개 개인정보 정책 게시 및 Google Play 트랙 변경은 별도 승인.
- 최종 QA 이후 코드가 변경되면 기존 SHA의 성공을 재사용하지 않는다.
