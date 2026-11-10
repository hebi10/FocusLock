# 004 — FocusLock 미니멀 아이콘 적용

## 선택 시안
- 짙은 파란색 배경, 흰색 자물쇠·집중 표적, 작은 초록색 잎사귀
- 원본 이미지 1254×1254: https://drive.google.com/file/d/1yhov-7N7DKG3WZJFE3Og3aY_oiUmSZJ_/view
- Google Play 이미지 512×512: https://drive.google.com/file/d/13ccHVbyNolgrSpZWNvC0ZZ5SavAGYpTW/view
- Android 아이콘 전경은 실제 선택 시안의 픽셀 윤곽을 벡터화해 사용 (별도 디자인으로 교체하지 않음)

## Android 리소스
- `app/src/main/res/drawable/ic_launcher_background.xml`: 고정 짙은 파란색 배경
- `app/src/main/res/drawable/ic_launcher_foreground.xml`: 흰색 자물쇠, 표적 및 초록 잎사귀
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: 전경/배경/단색 적응형 런처
- `app/src/main/AndroidManifest.xml`: icon과 roundIcon을 모두 연결
- `assets/store/FocusLock_Play_Icon_512.png`: 512×512 32-bit PNG, 1 MiB 이하의 Google Play 등록용 파일

## QA / 제약
- 기능·패키지 ID·개인정보 처리·사용 통계·Room·WorkManager 기능 수정 없음
- Android CI, API36 에뮬레이터 테스트와 작은 크기·원형 마스크 시각 검수 필요
- 원본 아트워크의 모서리 흰색 영역은 Play 규격용 내보내기에서 파란색 사각형 배경으로 정리
- Google Play Console 스토어 이미지 입력 또는 테스트 트랙 릴리스 수행 여부는 별도 확인
