# FocusLock — 001 전체기능 및 화면 설계

Android 네이티브 UI 프로토타입 (`0.1.0-ui`). **앱 전체 기능이 완성된 릴리스가 아닙니다.**

코드는 [hebi10/FocusLock](https://github.com/hebi10/FocusLock) GitHub 저장소에서 관리하며, 브랜치·PR·Android CI를 통해 검증합니다. Google Drive는 기획·디자인·검증 자료와 시안을 보관합니다.

## 구성

- Kotlin + Jetpack Compose + Material 3
- `app/src/main/java/com/haebi/focuslock/ui/` — 화면·내비게이션·디자인 시스템
- `app/src/main/java/com/haebi/focuslock/data/DemoData.kt` — 디자인 검증용 예시 통계
- `app/src/main/res/drawable-nodpi/garden_illustration.png` — 생성된 온보딩 시안에서 분리한 그림
- [Google Drive 디자인 시안](https://drive.google.com/drive/folders/1Y30NtM4eK-kQuQCa5hF8SPRX4bCSyrUa) — 화면 이미지 10개 (GitHub에는 용량 절감을 위해 중복 저장하지 않음)
- `docs/SCREEN_MAP.md` — 화면↔구현 매핑·디자인 분석
- `docs/QA.md` — 검증 결과와 후속 작업

## 포함된 화면

1. 온보딩 소개 / 권한 안내 / 목표 설정
2. 홈 (라이트·다크)
3. 사용 분석 (오늘 / 7일 / 30일, 앱별 목록·정렬·카테고리)
4. 앱 상세 (사용량·요청 권한 안내·기기 영향 안내·관리 옵션)
5. 사용 목표 설정 바텀 시트
6. 집중 설정 / 진행 / 결과
7. 정원 / 아이템 상세 바텀 시트
8. 리포트 (7일 / 30일)
9. 설정 / 권한 관리
10. 공통 권한·빈 데이터 안내 컴포넌트

## 빌드 환경

- JDK 17
- Android SDK 36 (compileSdk), targetSdk 36, minSdk 26
- Gradle 9.5.0, Android Gradle Plugin 9.3.1, Kotlin 2.4.20
- `gradle/libs.versions.toml` 버전은 사용자 PetWorld 프로젝트의 기준과 맞췄습니다.

기존 작업 환경에는 Android SDK가 없어 **로컬 Android APK 빌드·에뮬레이터 검증을 실행하지 못했습니다**. 빌드는 GitHub Actions [Android CI](.github/workflows/android-ci.yml)에서 실행하며 성공 시 디버그 APK 아티팩트를 업로드합니다. 수동 [에뮬레이터 스모크 테스트](.github/workflows/emulator-smoke.yml)에서는 첫 실행 화면을 캡처합니다.

Windows PowerShell에서는 Android SDK/JDK를 준비한 뒤 다음 명령을 사용합니다.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

생성된 APK는 `app/build/outputs/apk/debug/`에서 확인합니다.

## 화면 검토 방법

1. Android Studio 또는 Android SDK/Gradle 환경에서 앱을 빌드합니다.
2. 최초 실행 시 온보딩을 진행하거나 `둘러보기`로 홈에 진입합니다.
3. 홈 → 사용 분석 → 앱 상세 → 목표 설정을 확인합니다.
4. 하단 `집중` → 집중 모드를 선택하여 세션을 시작하고, 일시정지·종료 확인을 검증합니다.
5. 하단 `정원`, `설정` 및 `권한 관리` 화면을 확인합니다.
6. `설정 → 화면 → 다크` 선택 후 레이아웃/텍스트 대비를 확인합니다.
7. 태블릿/폴더블에서 사용 분석과 정원의 2열 레이아웃을 확인합니다.

## 작동 범위 및 제한

- 모든 화면 수치와 앱 목록은 **UI 시연용 샘플**입니다. 실제 설치된 앱 정보를 수집하지 않습니다.
- 사용 정보 접근 권한 설정으로 이동하고 현재 권한 허용 여부는 확인할 수 있습니다.
- Android 앱별 배터리·CPU·RAM 소비량은 일반 앱에서 정확히 제공받을 수 없습니다. 앱 상세에는 시스템 설정 진입과 패턴 기반 설명만 제공합니다.
- `GoalSheet`, 집중 타이머, 온보딩, 테마 전환 등은 UI 상호작용을 시연합니다. 앱 사용 통계·실제 알림·보상 영구 저장·자동 차단은 미구현입니다.
- 테스트 타이머는 화면 생명주기·기기 재부팅을 완전히 보장하지 않습니다. Room/WorkManager 연동 단계에서 보완해야 합니다.
- 사용 시간 원본 이벤트 조회를 구현하지 않아 접근성 서비스, 화면 오버레이, QUERY_ALL_PACKAGES 권한을 요청하지 않습니다.
- 서명키, 비밀값, Google Play 등록정보는 이 프로토타입에 포함하지 않습니다.

## 프로젝트 연계

- Drive: `12_FocusLock_스마트폰습관관리`
- 기획: `01_기획/001_FocusLock_전체기능_및_화면_기획서_v1.0`
- 디자인: `02_디자인/001_FocusLock_전체_화면_디자인_설계_v1.0`
- 후속: Android SDK 빌드/실기기 스크린샷 → 디자인 대조 → Room/UsageStatsManager 구현

코드와 Android CI는 GitHub에서 관리합니다. Google Play 등록·서명키 설정·자동 업로드는 기존 GalliPick의 검증 사례를 참조하되, FocusLock의 Play 앱이 별도 구성되고 출시 승인이 확인되기 전까지 수행하지 않습니다.
