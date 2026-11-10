# FocusLock 002 — 실제 사용 기록·권한 API 및 에뮬레이터 QA

## 변경 범위

- `UsageStatsManager.queryUsageStats(INTERVAL_DAILY,...)`: 최근 60일 범위의 일별 OS 사용 시간에서 오늘·7일·30일 및 이전 기간 비교를 집계합니다. 공개 API가 제공하는 기록에 의존하므로 기기 제조사와 데이터 보관 기간에 따라 달라질 수 있습니다.
- `UsageStatsManager.queryEvents(...)`: 최근 7일에서 앱 ACTIVITY_RESUMED 전환 이벤트 수와 오늘 시간대별 전면 사용 시간을 확인합니다. OS 이벤트 미제공·보관 만료 시 ‘확인 불가’로 표기합니다. 앱 실행 횟수와 정확히 동일하지 않습니다.
- `AppOpsManager.OPSTR_GET_USAGE_STATS` 허용 여부를 확인하고 `ACTION_USAGE_ACCESS_SETTINGS`로 사용자를 연결합니다. 거부된 권한은 자동 승인하거나 우회하지 않습니다.
- `PackageManager.GET_PERMISSIONS`로 실제 조회 가능한 앱의 요청 권한과 일부 위험 권한의 허용 여부를 표시합니다. 패키지 가시성 제한 시 ‘Android에서 직접 확인’ 안내로 대체합니다.
- `QUERY_ALL_PACKAGES`, 접근성 서비스, 다른 앱 위 표시·자동 차단, 네트워크 전송 없음.
- 목표 시간과 제외·방해 앱 선택은 앱 내부 SharedPreferences에 저장됩니다. 자동 목표 경고·집중 보상 영구 보관은 포함하지 않습니다.

## 권한 및 앱 상태 검사

1. 미허용: 시스템 `GET_USAGE_STATS` app-op을 ignore 설정. 홈·사용 분석·리포트에서 실제 수치 대신 권한 설명·설정 CTA가 보여야 하며 집중 타이머는 작동합니다.
2. 허용: `GET_USAGE_STATS` allow, 다른 앱(예: Android 설정)을 전면 사용 후 FocusLock으로 복귀. 앱별 사용 시간과 조회 가능한 권한을 표시합니다.
3. 권한 취소: 앱 전면 복귀 시 권한 상태를 다시 검사하고, 임의의 0분 통계를 표시하지 않습니다.
4. 기록 없는 기간: 빈 상태가 표시되고 이전 기간 변화량이나 가상 데이터를 만들어내지 않습니다.
5. 권한 조회 불가: Android 패키지 가시성 정책으로 앱 정보를 가져올 수 없으면 실제 패키지 이름과 시스템 설정 이동 경로를 안내합니다.
6. 목표 설정: 사용 기록에서 선택한 실제 패키지별 목표 저장 및 재진입 시 값 유지.
7. 시간대 그래프: 일부 최근 이벤트가 없는 경우 측정치 누락 안내.
8. 개인정보: 오프라인 조회, 서버 업로드 없음.

## 자동화

- `Android CI` (`.github/workflows/android-ci.yml`): `testDebugUnitTest`, `lintDebug`, `assembleDebug`, APK artifact.
- `Android full-screen emulator QA` (`.github/workflows/emulator-smoke.yml`): API 36 Pixel 6 x86_64, 실기기 아닌 에뮬레이터 검증. `UiAutomator` 시나리오 2개로 온보딩·권한 미허용·집중·정원과 권한 허용·사용 분석·앱 상세·리포트·라이트·다크를 캡처합니다.
- `focuslock-android-fullscreen-captures` artifact에 화면 캡처 PNG 저장. 캡처를 실제 생성한 성공 실행에서만 QA 완료로 판단합니다.
- 단위 검사 성공을 화면 QA 성공으로 대체하지 않으며, 자동화 스크린샷과 삼성 실기기 검증은 별개입니다.

## 남은 구현 과제

- 실제 장기 집중 세션·보상 기록 로컬 DB 저장
- Android 백그라운드 제한을 고려한 목표 초과 알림
- 주간 리포트에서 실제 집중 세션 통계 반영
- 7일 이상 장기 데이터 정확성 및 제조사별 실기기 점검
- 폴더블, 글꼴 확대, 높은 대비 접근성 보정
- Play 권한 정책·데이터 보안 사전 검토 및 비공개 테스트
