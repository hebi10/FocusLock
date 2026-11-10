# 001 전체기능 및 화면 설계 — 이미지 분석 및 화면 매핑

## 시안 분석

- **전체 방향**: 모노톤 정보 UI에 정원에서만 제한적으로 자연색 사용. 카드/타이포/하단 5탭 패턴을 유지.
- **배경**: 생성 시안의 과한 그림자·모서리 곡률을 그대로 복사하지 않음. Surface 계층과 12dp 기본 라운드로 단순화.
- **홈**: 사용 시간 2시간 15분, 목표 진행률, 집중 시작, 상위 앱 3개, 정원 미리보기, 7일 변화의 우선순위 유지.
- **사용 분석**: 생성된 이미지의 카테고리/앱 사용량 값이 불일치하던 점을 수정. 앱별 58+36+31+10분=135분, 시간대별 24개 막대 합=135분. 주간 7개 막대 합=860분(14시간 20분). 예시 값은 명시적으로 라벨링.
- **앱 상세**: ‘제한한다’는 오해를 피하고 방해 앱 감시 대상·알림 중심으로 재서술. 권한 허용 여부·실제 배터리 점유율을 수집하지 않았으므로 사실처럼 표시하지 않음.
- **온보딩**: 사용 통계 특수 권한 설정 안내, 미허용 건너뛰기 제공. 권한 화면은 실제 설정으로 연결되지만 수집은 없음.
- **집중/정원/리포트/설정**: 생성된 원본 10장에 시안이 없는 영역이므로 동일 토큰/타이포/간격 패턴을 기반으로 코드 화면 설계.
- **색상**: `#F7F7F7/#FFFFFF/#171717` 라이트, `#101010/#1B1B1B/#F3F3F3` 다크. 녹색은 상태 및 게임 강조에 한정.
- **접근성/적응형**: 레이블 있는 하단 메뉴, 48dp 이상 터치 영역 목표, 700dp 이상에서 분석/정원 2열.

## 생성 이미지 10장 → 구현 컴포넌트

| 디자인 파일 | 화면 | 구현 파일 |
|---|---|---|
| 01_온보딩_소개.png | OnboardingIntro | OnboardingScreens.kt |
| 02_온보딩_권한.png | OnboardingPermission | OnboardingScreens.kt |
| 03_온보딩_목표.png | OnboardingGoal | OnboardingScreens.kt |
| 04_홈_라이트.png | HomeScreen | HomeAnalysisScreens.kt |
| 05_홈_다크.png | FocusLockTheme | Theme.kt |
| 06_사용분석_오늘.png | AnalysisScreen / BarChart | HomeAnalysisScreens.kt, Components.kt |
| 07_사용분석_7일.png | AnalysisScreen / BarChart | HomeAnalysisScreens.kt, Components.kt |
| 08_앱상세_사용기록.png | AppDetailScreen | AppDetailScreens.kt |
| 09_앱상세_권한영향.png | AppDetailScreen | AppDetailScreens.kt |
| 10_목표설정_시트.png | GoalSheet | AppDetailScreens.kt |

## 추가 구현 화면

| 화면 | 컴포넌트 | 구현 파일 |
|---|---|---|
| 집중 설정 | FocusSetupScreen | FocusGardenScreens.kt |
| 집중 진행 | FocusRunningScreen | FocusGardenScreens.kt |
| 집중 결과 | FocusResultScreen | FocusGardenScreens.kt |
| 정원 | GardenScreen | FocusGardenScreens.kt |
| 아이템 시트 | GardenItemSheet | FocusGardenScreens.kt |
| 주간/월간 리포트 | ReportScreen | ReportSettingsScreens.kt |
| 설정 | SettingsScreen | ReportSettingsScreens.kt |
| 권한 관리 | PermissionManagementScreen | ReportSettingsScreens.kt |

## 구현 산출물의 한계

- 기존 시안은 실제 Android 위젯 캡처가 아닌 생성 이미지. 레이아웃을 동일 픽셀로 복사하지 않고 컴포넌트 시스템으로 옮겼음.
- 실제 앱 사용 통계·패키지 권한·기기 배터리 사용량은 아직 연동되지 않았음.
- 모델이 생성한 고유 마스코트 원본 레이어가 없으므로 온보딩 그림 부분을 참조 시안에서 잘라 `drawable-nodpi/garden_illustration.png`로 사용함.
- 화면별 실기기 비교 캡처 및 갤럭시 폴더블 QA는 빌드 후 별도 검증이 필요함.
