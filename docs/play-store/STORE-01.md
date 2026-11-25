# STORE-01 — FocusLock 한국어 스토어 이미지

대상 com.haebi.focuslock / ko-KR. 실제 Android API36 화면을 사용한 휴대전화 RGB PNG 1350×2400 8장 및 대표 그래픽 RGB PNG 1024×500 1장. 기존 아이콘 유지.

- FL-FIX-01~08과 스토어 준비 코드까지 모두 병합한 최신 main SHA에서 Android full-screen emulator QA 성공이 필수다.
- 기존 UI-R00 37장과 FL-FIX-08 추가 화면 7장, ui-r00-audit-report.json, fl-fix08-evidence.json의 SHA-256 및 실행 ID를 확인한다.
- STORE-01 workflow의 generate 작업은 검증된 artifact만 다운로드하여 실제 화면 외부에 홍보 문구를 넣고 생성 PNG·원본 PNG·대표 그래픽 소스 해시를 manifest.json에 기록한다. 앱 UI를 편집하거나 데이터를 만들어 넣지 않는다.
- 순서: 홈, 사용 분석, 앱 상세, 전체/개별 목표 관리, 집중 진행, 집중 결과, 정원, 실제 날짜 선택 리포트. 데이터 관리·목표 이력·삭제 확인 캡처는 QA 및 Drive 자료에서 별도 관리한다.
- 기존 Drive 자료를 보존하며 실제 최신 캡처와 생성 이미지의 사람이 읽을 수 있는 문구·민감정보 노출 여부를 검토한다.
- PR에서는 소스 선택 및 renderer 테스트만 수행한다. main 병합 후 최신 main의 emulator-smoke.yml을 수동 실행하고 같은 SHA에서 성공한 후 STORE-01 mode=check를 수동 실행한다. 두 승인 boolean은 false로 두고 Play 변경을 실행하지 않는다.
- 업로드는 별도 승인한 workflow_dispatch main / upload, ops01_release_approved=true, ops06_store_approved=true, 확인 문구·정확한 main SHA·배치 ID·승인한 manifest SHA-256을 모두 요구한다. 기존 OPS-06 게이트, Google Play 인증 및 upload 단계는 변경하지 않는다. 과거 issue 명령이나 자동 커밋은 배포 승인이 아니다.
- AAB/APK, 내부/비공개/프로덕션 트랙, 등록정보 텍스트, 아이콘, 다른 언어 변경은 하지 않는다. 결과 API 커밋과 Console 심사·실제 노출은 구분한다.
