# UI-R04 — 사용 분석 리디자인

기준: [Drive D03 전체 화면 기획](https://docs.google.com/document/d/160M4wHLgML5FHsKuORsU73akWD5ktMAAQp6nRbLW4W4/edit), [UI-R00 실화면·데이터 기준선](UI-R00.md), [UI-R01 디자인 시스템](UI-R01.md).
대상: FocusLock `com.haebi.focuslock`. 현재 상태는 PR 검증 단계.

## 작업 범위

1. 전체 사용 시간·이전 기간 증감을 상단 요약 카드로 유지. 비교할 이전 기록이 없으면 수치를 만들어 내지 않고 명시적 안내.
2. 오늘(24시간), 최근 7일, 최근 30일 그래프를 실제 `UsageSnapshot`에서 조회. 바를 누르면 해당 시각/날짜와 분 단위를 표시하고 막대별 48dp 선택 영역·스크린리더 설명 제공. 24/30개 막대는 가로 스크롤로 축 가독성 유지.
3. `앱별 사용 시간` 섹션에서 앱명 검색, 카테고리 필터, `사용 시간/실행 횟수/이름` 정렬 지원. 검색 결과 없음과 Android 기간 데이터 없음은 구분. 이름 정렬은 한국어 `Collator` 사용.
4. Android `PackageManager.getApplicationIcon` 조회 가능한 경우 실제 앱 아이콘 사용. 패키지 정보가 보이지 않거나 설치 제거 등으로 조회 불가능한 경우 기존 문자 아이콘을 대체로 표시. `QUERY_ALL_PACKAGES` 권한은 추가하지 않음.
5. 콤팩트 화면: 기간→요약→그래프→앱 검색/목록→카테고리 비중 순. 검색어 입력 중에는 그래프를 임시로 숨겨 목록 결과와 빈 상태를 우선 노출하며 검색 해제 시 그래프를 복구. 700dp 이상: 좌측 그래프/카테고리, 우측 앱 목록 2열. 기본 UI-R01 블랙/중성 그레이, 기존 카드와 48dp 터치 기준 재사용.
6. 제외된 앱은 앱 목록에서 숨기지만 전체 사용 시간과 카테고리 통계의 분모에는 포함되는 기존 정책을 유지. 사용 시간 계산·UsageStatsManager·UsageRepository·알림/목표 DB 변경 금지.

## 데이터 무결성

- **실제 사용 시간**은 Android UsageStatsManager에서 가져옵니다. QA용 DemoData나 기획 이미지의 임의 숫자를 앱 실데이터로 주입하지 않습니다.
- 시간대 그래프는 이벤트가 제공되지 않는 경우 별도 안내하며 0분짜리 가짜 데이터로 대체하지 않습니다.
- 그래프 라벨 날짜는 컴포넌트가 실행된 현재 날짜가 아닌 `UsageWindow.startDate`에 고정하여 기간 경계·자정 상태에서도 대응이 일치합니다.
- 카테고리 비율은 `UsageWindow.totalMinutes` 대비 앱별 합계, 앱 비율도 동일 분모를 사용.
- 실행 횟수 음수(`-1`)는 확인 불가로 표시하고 횟수 내림차순의 뒤에 배치.
- 앱 아이콘은 조회 가능한 앱만 표시하고 미조회 시 기존 AppSymbol 사용. 각 앱의 배터리/CPU 값을 직접 측정한 것처럼 표시하지 않음.

## 검증 계획

1. Android CI: Kotlin `AnalysisModelsTest` (이름/횟수/시간 정렬, 검색/카테고리/제외, 날짜 라벨, 이벤트 결손), 전체 단위 테스트·Lint·Debug APK.
2. API36 실제 에뮬레이터: 기존 기준선 37장 유지 + 검색·이름 정렬·7일·30일 계측 및 별도 UI-R04 이미지 5장.
3. 데이터 없음/권한 거부/지난 7일·30일/카테고리/다크 상태 회귀 확인. 기록 부족(`limited_real_data`)은 디자인 테스트 성공으로 위장하지 않음.
4. API29/35, 160% 이상 글꼴, Galaxy 폴더블/태블릿/실기기 실측은 UI-R08 종합 QA에서 별도 검증.
5. 선행 PR #22(UI-R03)과 `HomeAnalysisScreens.kt`가 겹칩니다. UI-R04는 변경한 분석 섹션과 신규 파일에 국한하며 병합 순서에 따라 회귀 검증 필수.

## 변경 안전 범위

- 변경: `HomeAnalysisScreens.kt` 분석 화면만, 신규 `AnalysisModels.kt` / `AnalysisComponents.kt`, 신규 단위·계측 테스트, `run-emulator-qa.sh`, 본 문서.
- 유지: Android 앱 ID/서명/버전, Room/UsageStats 통계 산식, 사용자 설정, 홈 UI-R03 기능, 앱 상세 UI-R05, Play 이미지/테스트 트랙.
- 완료 판정: 동일 PR head SHA에서 Android CI + API36 전체 37장/추가 3장 성공 및 시각 QA 후에만 `main` 병합. Play 재배포는 별도 단계.
