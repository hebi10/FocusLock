# FL-FIX-09 — 2026-11-25 실제 캡처 검수 후속

- 원본: main 57f210aeb0399776d17796c6714919b90504e18c, Android full-screen emulator QA [45524459135](https://github.com/hebi10/FocusLock/actions/runs/45524459135). 28건 계측 성공, UI-R00 37장 및 추가 7장 해시 일치. ZIP 54개 PNG (보조 분석/반응형 캡처 포함). 원본 Drive 보존: https://drive.google.com/file/d/1WnuZO7_3pBuW26Z1RHRBOWhnGWIjudam/view
- 실제 PNG 전체 미리보기에서 local-data.png가 데이터 관리 화면 대신 기존 설정 화면을 보여 주고 goal-history.png는 목표 관리 상단 화면과 거의 동일하여 이력 본문 증빙에 불충분함을 확인. 두 삭제 확인창은 실제 해당 모달 표시. 기능 QA 통과와 캡처 시각 검수는 별개다. 자동 해시 성공만으로 공개 자료 승인하지 않는다.
- 홈/앱 목록에 실제 에뮬레이터의 com.haebi.focuslock.test와 시스템 앱 기록이 노출되고 관측 기간은 1일뿐이다. 가짜 사용 시간을 삽입하거나 실제 앱 화면을 편집하지 않는다. 공개용 이미지에서는 실제 사용자 승인 및 충분한 실제 기록/민감정보 검수가 필요하다.
- 후속: 대상 화면의 실제 가시 영역, 필수·금지 UI 노드와 일정 시간 안정화 확인; 목표 이력 본문까지 스크롤; 이미지와 XML 계측 증빙 함께 보존. API36 PR에서 신규 캡처 분기를 직접 실행한다. 기존 UI-R00 37장 검사와 최종 SHA 게이트 유지.
- 변경은 QA 코드/스크립트/문서뿐. 기존 데이터·제품 로직·Play/웹 변경 없음. 최신 PR QA 통과 후 병합하고 변경된 정확한 main SHA로 전체 QA 및 STORE-01 check를 실행한다. 실제 이미지 검수/Drive 갱신 후 승인 전까지 배포 금지.
