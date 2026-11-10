# FocusLock — 2026-11-10 개발·디자인·배포 자료 정리

## 프로젝트
- 저장소: https://github.com/hebi10/FocusLock
- Android 패키지: com.haebi.focuslock
- 현 버전: 0.3.0 (versionCode 2)
- GitHub main: 9b8279ae8d80d752aeb1b7e3a423aae5a1ac33ee (004 아이콘 변경 이전 기준)

## 001 — UI와 Android CI
- 온보딩·홈·앱 사용 분석·앱 상세·목표 설정·집중·정원·리포트·설정 등 주요 화면 구현
- Kotlin/Jetpack Compose, 라이트·다크 테마, 기본 Android CI
- 병합 PR #1: https://github.com/hebi10/FocusLock/pull/1

## 002 — 실제 데이터 / 권한 / UI 검증
- UsageStatsManager 실제 사용 통계(오늘·7일·30일), 권한 거부/허용 UI, PackageManager 권한 안내
- API36 Pixel6 에뮬레이터 23화면 캡처
- 병합 PR #2: https://github.com/hebi10/FocusLock/pull/2
- 검증 캡처: https://drive.google.com/file/d/1eUBkGlMr0Vu4gGpL3LtLU7sOl9fiyx3G/view

## 003 — 목표 알림 및 영구 저장
- 목표 80%/100% 경고: WorkManager 15분 주기 확인(실시간 보장 없음)
- Room 집중 세션·보상 저장 및 재실행/재부팅 복구, 중복 보상 방지, 7일/30일 리포트
- 병합 PR #4: https://github.com/hebi10/FocusLock/pull/4
- 검증 캡처: https://drive.google.com/file/d/1O7daVSeyJ6wUoxOexXP9kqhFsDSJucDh/view
- 구현 명세: https://docs.google.com/document/d/1Yl9ilo7WE_5DSXO1xzthNriEkmk4iUaiZdVz18zTEVk/edit
- Debug APK(기존 빌드): https://drive.google.com/file/d/1Sk1IXA_ImaYCPOF-sK9CkdjV50UdA1vk/view
- main CI 성공: https://github.com/hebi10/FocusLock/actions/runs/42778162531

## 004 — 아이콘/스토어 이미지
- 최종 원본: https://drive.google.com/file/d/1yhov-7N7DKG3WZJFE3Og3aY_oiUmSZJ_/view
- Google Play 512×512 이미지: https://drive.google.com/file/d/13ccHVbyNolgrSpZWNvC0ZZ5SavAGYpTW/view
- Android 런처 아이콘과 Manifest 연결 변경. 이 변경은 별도 PR에서 CI 후 main에 병합
- 스토어 소개 이미지 및 스토어 콘솔 등록은 후속 별도 작업

## Google Play 및 남은 점검
- Play 내부 테스트 배포 자동화 PR #3은 아직 미병합: https://github.com/hebi10/FocusLock/pull/3
- PR #3은 초기 0.2.0 코드 기준으로 작성돼 현재 0.3.0/main과 재검토 필요
- 삼성 실기기·폴더블·장기 사용 통계·백그라운드 알림 지연 실증 QA 미완료
- 정식/비공개 테스트 트랙 출시 주장 금지, 실제 Console 등록 완료는 별도 확인
