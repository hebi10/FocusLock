# FocusLock RENEW-03 — 스마트폰 사용 분석 정확도 및 근거 표시

2026-11-12 / Google Drive RENEW-00 M01~M13·RENEW-01의 사용 분석 탭 기준

## 개발 범위
- Android `UsageStatsManager.queryUsageStats` 원본 `totalTimeInForeground`의 **밀리초를 먼저 합산하고 최종 분으로 단 한 번 반올림**한다. 기존 ‘앱별 반올림 후 합산’으로 생기던 총계/그래프 차이를 줄인다.
- 각 실제 OS 앱 집계의 `foregroundMillis`와 선택 구간의 `totalForegroundMillis`를 별도 보존한다. 분 단위 0으로 반올림되는 실제 사용 앱도 임의로 삭제하지 않는다.
- 앱별 사용량·카테고리 비중은 가능한 경우 원본 밀리초로 계산하고, 기존 테스트용/레거시 사용모델은 분 단위 fallback으로 호환한다.
- 관측된 날짜 수(`UsageCoverage`)를 사용 분석 7일·30일 차트 아래에 표시한다. ‘Android 기록이 없는 날짜’는 **실제 미사용인지, OS 미보관인지** 구분할 수 없다는 안내를 제공한다.
- 최근 7일 `UsageEvents` 기록이 조회되지 않는 앱은 ‘실행·전환 0회’가 아닌 ‘확인 불가(-1)’로 처리한다. 전체 조회 대상에 실행 횟수가 없는 기간에는 ‘실행 횟수’ 정렬을 제공하지 않고 ‘사용 시간’ 정렬로 명확히 대체한다.
- 기간, 검색, 카테고리 필터, 앱 상세, 기존 `UsageLoadState` 권한/오류 상태, Goal Alerts 및 Room 데이터 변경 금지.
- 디지털 활동의 정확한 콘텐츠, 실제 기기 배터리·RAM·CPU 소모, 강제 앱 차단, 사용자 중독 진단을 만들거나 표시하지 않는다.

## 단위 테스트
- UsageMetricsTest: 20초+20초 합산 시 1분, 음수 제외, 매우 긴 duration overflow 방어, 원본 ms 비중, 7일 관측일 2일.
- AnalysisModelsTest: 실행 이벤트 누락 기간의 정렬 fallback; 두 앱이 같은 1분으로 반올림돼도 원본 32초/58초 순서 보존.
- 기존 Unit/Room/리포트 계산 회귀 유지.

## 실제 Android 계측
- RenewUsageCoverageQaTest: **실제 OS 사용 통계만** 사용, 원본 밀리초 총합과 각 period 합계의 일치, 선택 구간 관측일 집계, unknown launch count 확인.
- FocusLockUsageStateAuditTest: raw ms / observed days metadata를 JSON 증빙에 기록.
- 기존 Android 전체 화면 37장·R04 검색/정렬/기간 차트·실제 권한 허용/거부·Room 중복 보상 회귀.
- API29/35/36 및 좁은 폰/160%/태블릿 크기 시뮬레이션은 기존 matrix workflow에서 재검증.

## 데이터 해석 주의
- 관측일수는 ‘그 날짜에 양수 foreground duration이 OS에서 반환되었다’는 뜻이지, 해당 구간 OS 사용 기록이 **완전하다**는 뜻이 아니다.
- 이용 기록 0분, 조회 실패, 권한 미허용, 관측기록 부족, 앱이 실제 30초 미만 사용해 분 단위 0으로 반올림된 상태를 혼동하지 않는다.
- 표시용 분 단위 반올림 때문에 앱별 표시값을 다시 더한 합과 OS 원본 밀리초를 직접 합산한 총 사용 시간에 1분 수준 차이가 있을 수 있다. 비중 계산은 가능한 경우 원본 duration 사용.
- 이벤트 기간은 기존 실제 Android 데이터 범위(최근 7일)만 사용한다. 30일 앱 실행 횟수를 만들어내지 않는다.

## 코드·배포 경계
- 수정 범위: 데이터 모델 `AppUsage.foregroundMillis` 기본 0, `UsageWindow.totalForegroundMillis` 기본 0, `UsageSnapshot.coverage` nullable 기본, 데이터 계산, 분석 차트·분류·정렬, 단위·Android QA.
- 앱 ID·서명·Room·SharedPreferences 목표/알림/정원 데이터·Google Play 비공개 테스트 v4 및 스토어 이미지 변경 없음.
- RENEW-02-B PR #39가 main 6315e88c에 안전 병합된 뒤 이 PR의 변경을 그 최신 main으로 재구성했다. 새 HEAD의 Android CI와 실제 전체 화면 QA가 모두 성공한 경우에만 병합한다.
- 최종 Google Play 내부·비공개 테스트 배포는 사용자 지시에 따라 RENEW-09까지 보류한다.

## 배포 기준 문서
- RENEW-00: https://docs.google.com/document/d/1GK45KZIk8Ef60A4m4XMkuBh06KYvjma9QDkCgevz8uw/edit
- RENEW-01: https://docs.google.com/document/d/1AZw7TzpmOrY6SIOIZw5gCRC3-k8xtdZjs6ziWU4mN9s/edit
- RENEW-MASTER: https://github.com/hebi10/FocusLock/issues/41
