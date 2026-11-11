# UI-R01 — 공통 디자인 시스템

기준일: 2026-11-11
상태: 검증 전 PR 단계
대상: FocusLock `com.haebi.focuslock`

## 근거

- 기획 원본: [Google Drive — 전체 화면 디자인 설계 v1.0](https://docs.google.com/document/d/160M4wHLgML5FHsKuORsU73akWD5ktMAAQp6nRbLW4W4/edit)
- 기획 이미지 10장: https://drive.google.com/drive/folders/1Y30NtM4eK-kQuQCa5hF8SPRX4bCSyrUa
- 기준선 UI-R00: [QA 기록](UI-R00.md), [실제 37장](https://drive.google.com/file/d/1VnXE-HwvKiLRaO2qFlUNrhYj9t9vxBOu/view)

## 이번 변경 범위

| 구성 요소 | 변경 | 유지할 것 |
| --- | --- | --- |
| Theme.kt | 공통 색상·간격·타이포그래피 토큰 정의 | 기존 라이트/다크 모드 선택, 시스템바 제어, 의미별 색상 |
| PageTitle / SectionTitle | 24sp 페이지 / 18sp 섹션 제목으로 정리 | 기존 제목·링크·동작 |
| FocusCard | 16dp 기본 내부 여백·12dp 요소 간격·12dp 반경 | 그림자 0, 기존 카드 구성 |
| Metric | 36sp 큰 수치와 tabular number, 13sp 보조 | 실제 숫자와 데이터 로직 |
| ProgressBar / BarChart | 분석/시간대별 막대는 무채색, 목표 진행·정원 성장 막대는 녹색, 경고는 주의색 | 그래프 데이터, 계산 및 기간 구조 |
| ActionButton / SettingRow | 최소 높이 52dp / 48dp | onClick, 비활성 상태 |
| PeriodSelector / AppUsageRow | 항목 최소 클릭 48dp·작은 텍스트 가독성 | 선택 상태·필터 기능 |
| 하단 5탭 | 24dp outlined 아이콘, 12sp 레이블 | 5개 탭, 테스트 태그, 선택/전환 로직 |
| 온보딩 공통 아이콘 | 일반 정보 안내에서 장식 녹색 제거, 검정·회색 적용 | 아이콘 종류·온보딩 순서 |

### 색상 원칙

라이트: `#F7F7F7 / #FFFFFF / #171717 / #666666 / #E4E4E4`.
다크: `#101010 / #1B1B1B / #F3F3F3 / #A8A8A8 / #363636`.
상태 전용: 녹색 `#357A57`, 주의 `#AA720E`, 오류/초과 `#B84343`. 정원에 한해 제한된 자연색을 허용합니다.

### 간격 / 글자

4 / 8 / 12 / 16 / 24 / 32dp, 아이콘 24dp, 컨트롤 최소 48dp, 주요 버튼 52dp.
페이지 제목 24sp, 섹션 18sp, 큰 수치 36sp, 본문 14~16sp, 작은 정보 12~13sp.
모든 화면의 세부 레이아웃 변경은 후속 UI-R02~R07에서 처리합니다.

## 변경하지 않는 부분

- 앱 ID, 버전 코드, 서명, AAB/APK, 권한 선언과 UsageStatsManager, Room, WorkManager.
- 사용량·목표·집중 데이터, 사용자가 저장한 설정, 정원 보상 로직.
- 실제 설치 앱 아이콘/분석 검색/기간 전환/요일별 목표 등 아직 없는 기능 추가.
- Google Play 아이콘·설명 이미지, 내부/비공개 테스트 트랙, 스토어 등록정보.
- 마스코트 일러스트, 3D 기기 목업, 과한 그림자·라운드·그라데이션.

## 검증 조건

1. PR 동일 SHA Android CI: 단위 테스트 + Lint + Debug APK 빌드.
2. Kotlin UI 토큰 테스트: 승인된 색상/간격/텍스트 대비(일반 텍스트 WCAG 4.5 이상).
3. API 36 Pixel 6 전체 37장 실제 캡처(기존 23 + 스크롤 14) 및 앱 권한·Room QA.
4. 라이트 홈·다크 홈·설정·분석과 온보딩의 변경 전/후 정렬·터치·스크롤을 실제 캡처로 비교.
5. 정확한 CI/검증 SHA에서만 병합. 앱 자동 재배포 없이 스토어 변경이 발생하지 않도록 `[skip ci]` 병합.

### 후속 검증으로 분리

API 29/35, 160% 폰트·폴더블/태블릿·물리 기기는 UI-R08에서 확대합니다. R01에서 전체 기기 완전 검증으로 표현하지 않습니다.

## 다음 작업

우선순위: `UI-R03 — 홈 대시보드` → `UI-R04 — 사용 분석` → `UI-R05 — 앱 상세·목표` → `UI-R02 — 온보딩` → `UI-R06 — 집중·정원` → `UI-R07 — 리포트·설정`.
전체 UI 검증 후 `UI-R09`에서 최신 실제 Android 캡처를 이용하여 Play 설명 이미지 8장을 다시 만듭니다.
