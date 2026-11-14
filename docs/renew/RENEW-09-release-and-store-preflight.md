# RENEW-09 — 최종 Android 실화면·스토어 이미지·AAB 사전 검증

기준: 2026-11-14, Google Drive FocusLock RENEW-00/01, GitHub RENEW-MASTER #41. 이 문서는 Google Play 등록 승인이 아닙니다.

## 1. 기능 PR 병합 조건
- PR #51~55 최신 main 기준 Android CI, API29·35·36, API36 전체 화면 QA 성공 및 보안 검토 완료
- #53 리포트 화면과 #55 설정 화면 변경이 함께 적용됐는지 최종 검사
- 저장된 사용자 목표·요일·Room 세션·보상·정원/권한·다크/라이트/글꼴160% 회귀 검증
- 실기기 삼성/폴더블, TalkBack, 장기간 7/30일 Android 통계값 대조는 실제 기기 검증 전 미완료로 유지

## 2. 스토어 이미지 생산
- 모든 기능 병합 후 현재 main 동일 40자리 SHA에서 Android full-screen emulator QA 성공 및 37장 이상의 실제 캡처 확보
- 1350×2400 RGB PNG 8장, 1024×500 RGB 대표 그래픽 1장, SHA-256 매니페스트 source_commit·source_workflow_run 확인
- 순서: 01 오늘 홈, 02 사용 분석, 03 앱 상세, 04 선택형 목표, 05 집중, 06 결과, 07 정원, 08 사용 변화 리포트
- 실제 앱 내부를 편집하거나 가짜 앱 사용 시간·활동 수치를 삽입하지 않음
- 강제 앱 차단, 실시간 감시, 미구현 과거 목표 달성률 및 불확실한 30일 실행 횟수를 홍보하지 않음
- 화이트·블랙·중성 회색·평면 구성, 기존 아이콘 원본 보존
- 신규 캡처와 완성 이미지는 개인정보/계정/민감 내용 노출 여부를 사람이 최종 검수
- Drive의 기존 2026-11-10/11 시안은 보존하되 최종 리뉴얼 이미지 대체 자료가 아님

## 3. 별도 승인형 Play 작업
- STORE-01 FocusLock Korean Play images: check로 최종 main 실화면 8+1 생성, Play 변화 없음. 승인 후 upload 및 OPS-01/06 검증
- OPS-03 release-build: 최종 main SHA·버전코드·배치 승인에 따라 서명 AAB build-only, 업로드 없음
- OPS-04 play-internal-release: 동일 AAB SHA256·서명·applicationId·versionCode 검증 후 사용자 별도 승인으로 internal 게시
- OPS-05 play-closed-release: 완료된 동일 internal versionCode, 실제 Play 트랙 ID와 별도 승인 확인 후 closed 승격
- Play API 응답/실제 Console 게시/테스터 설치는 구분해 보고; 정식 출시는 별도 작업

## 4. 현재 판정
RENEW-04~08 전체 최종 병합과 실기기 QA가 완료되기 전에는 최종 이미지·AAB·Play 내부/비공개 트랙 게시를 완료로 표시하지 않습니다.
