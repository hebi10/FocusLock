# UI-R00 — FocusLock 기획/실제 화면·데이터 QA

기준일: 2026-11-11
대상 앱: com.haebi.focuslock
상태: QA 전용 PR 검증 중. Google Play / 앱 운영 코드는 변경하지 않습니다.

## 1. 증거의 분류

- 디자인 참조: Google Drive 2026-11-10 시안 PNG 10장. 실제 Android 위젯 캡처가 아닙니다.
- 실제 UI 기준: Android API36 Pixel 6, GitHub Actions #42945780305, commit c485fecff504f40ad7555cce58443a49f0d844e9의 원본 23장.
- 원본 10장의 Drive 파일 ID·SHA-256과 실제 23장의 SHA-256은 docs/qa/UI-R00-baseline.json에 고정했습니다.
- STORE-02-A의 AI 생성 휴대전화 목업, 디자인상의 2시간 15분 등 예제 수치는 실제 사용 기록으로 취급하지 않습니다.
- 기존 23장에서는 스크롤 아래 영역이 다수 누락되었습니다. 추가 14장으로 이 문제를 분리 검증합니다.

## 2. 기존 디자인 10장 — 화면별 대응

| 참조 | 실제 PNG | Kotlin 코드 | 문제 분류 |
|---|---|---|---|
| 01 온보딩 소개 | 01-onboarding-intro | OnboardingScreens | L: 정보 위계·간격 |
| 02 온보딩 권한 | 02-onboarding-permission | OnboardingScreens | L: 설명 밀도·버튼 |
| 03 온보딩 목표 | 03-onboarding-goal | OnboardingScreens | D/F: 앱 기록 부족·관리 앱 선택 단계 |
| 04 홈 라이트 | 14-home-granted | HomeAnalysisScreens | D/L: 목표 조건·정보 배치 |
| 05 홈 다크 | 22-dark-home | Theme + HomeAnalysisScreens | L: 대비·간격 |
| 06 분석 오늘 | 15-analysis-today | HomeAnalysisScreens + Components | D/F: 데이터 부족·막대 선택·검색 |
| 07 분석 7일 | 16-analysis-week | HomeAnalysisScreens | D/L: 일자별 데이터 부족·차트 |
| 08 앱 상세 기록 | 18-app-detail | AppDetailScreens | F: 목표 진행률·기간 전환 |
| 09 앱 상세 권한 | 18-app-detail + 33-app-detail-scroll | AppDetailScreens | S: 스크롤 아래 카드 미촬영 |
| 10 목표 시트 | 19-goal-sheet | AppDetailScreens | F/Q: 개별 요일 미구현·저장 접근성 |

분류: D=데이터·권한 조건, L=레이아웃, F=코드 미구현, S=스크롤 범위 누락, Q=접근성/기기 실측 필요.

## 3. 코드 확인 사항

- 홈: 사용 시간, 이전 기간 비교, 목표 진행률, 상위 앱 3개 및 주간 변화가 조건부로 구현되어 있습니다. 테스트 기기의 데이터 부족을 기능 부재로 단정하지 않습니다. 홈의 정원 XP 요약은 별도 개선 대상입니다.
- 분석: 오늘/7일/30일, 카테고리·앱 목록, 사용 시간·실행 횟수 정렬은 존재합니다. 앱명 검색·이름순 정렬·차트 막대 선택은 아직 없습니다. 목록의 AppSymbol은 실제 앱 아이콘이 아닌 문자 대체 표시입니다.
- 앱 상세: 권한, Android 앱 정보 이동, 집중 방해 목록과 분석 제외가 구현되어 있습니다. 목표 진행률 막대와 기간 전환은 없습니다.
- 목표 시트: 15분 단위 목표, 80%·100% 경고, 매일 적용 스위치, 저장·해제는 존재합니다. 월~일 요일별 설정은 없습니다.
- 집중: 타이머·일시정지·종료·보상 저장은 존재합니다. 다른 앱을 강제로 차단하는 기능과 방해 앱 사용 자동 판정은 구현된 것으로 광고하면 안 됩니다.
- 정원: 경험치·아이템 목록·배치 이름은 구현되어 있으나 GardenScene은 고정된 일러스트입니다.
- 리포트: 7일/30일 사용량·집중 완료 세션·집중 시간·XP는 표시하지만, 기획의 일부 세부 KPI는 아직 없습니다.
- 공통 디자인: 검정·회색 토큰과 하단 5탭은 구현되어 있습니다. 3D 휴대전화 테두리 및 그림자는 실제 앱 UI 요구사항이 아닙니다.

## 4. UI-R00 테스트 확장

- 기존 23장에 실제 스크롤 하단 14장을 추가합니다(24~37번). 대상: 온보딩, 설정, 권한, 정원, 홈, 오늘/7일/30일 분석, 앱 상세, 리포트, 다크 화면.
- 기존 원본 23장과 확대 14장의 PNG 크기·파일명·해시·변화 여부를 자동 검증하여 captures/ui-r00-audit-report.json에 기록합니다.
- Android AppOps로 허용/거부를 분리하여 UsageRepository 결과의 PermissionRequired 및 Ready 상태를 점검합니다.
- 허용 상태에서 오늘·7일·30일 앱 개수·합계·관측 일수·시간대별 이벤트를 OS 원본만으로 확인합니다. 샘플 사용량은 조작하거나 임의 생성하지 않습니다.
- 홍보 이미지에 충분한 데이터 판단: 3개 이상의 앱 + 2일 이상의 실기록. 부족하면 limited_real_data로 기록하되 UI 기능 테스트 결과와 구분합니다.
- 기존 FocusPersistenceQaTest로 Room 재열기, 보상 중복 방지, 재부팅 이후 임의 자동 완료 방지를 검증합니다. 보상 로직 테스트가 완료 UI 실화면 검증을 대체하지는 않습니다.
- Pixel 6 API36 외 API29/35, 삼성 실기기, 폴더블, 글꼴 160% 검증은 UI-R08의 대상입니다.

## 5. 후속 PR 분리 기준

- UI-R01: 폰트·카드·간격·버튼·공통 아이콘·라이트/다크 조정.
- UI-R03: 홈 목표·상위 앱·정원 XP·주간 변화 정보 위계.
- UI-R04: 분석 차트 선택·검색·이름 정렬·앱 아이콘·실제 데이터 상태.
- UI-R05: 앱 상세 목표 막대·기간 전환 및 시트 요일 선택·스크롤.
- UI-R02: 온보딩 정렬·목표 초기화 흐름(공통 시스템 정리 후).
- UI-R06: 집중 결과·정원 배치의 실제 시각 변화.
- UI-R07: 리포트·설정·권한 세부 흐름.
- UI-R08: API29/35/36, 태블릿·폴더블, 160% 글꼴/터치/실기기 종합 QA.
- UI-R09: 최신 실제 Android 캡처로 Play 스토어 이미지 8장 재제작·전용 업로드. 그전까지 STORE-01 유지, STORE-02-A PR #16 검수 대기.

## 6. 범위·완료 기준

운영 코드 app/src/main, 서명/AAB/APK, 아이콘·Play 스토어 이미지 및 테스트 트랙은 UI-R00에서 변경하지 않습니다. PR CI가 통과하고 QA 증거가 보관되기 전에는 main 병합하지 않습니다.
완료 조건: 10개 디자인↔23개 기준 캡처 매핑, 23장 고정 SHA 일치, 추가 14장(총 37장) 실제 Android 계측, 권한별 OS 사용 데이터 상태 보고, 기존 Room 검사, 미검증 영역 명시.

## 7. 자료

원본 디자인: https://drive.google.com/drive/folders/1Y30NtM4eK-kQuQCa5hF8SPRX4bCSyrUa
실제 Android QA: https://github.com/hebi10/FocusLock/actions/runs/42945780305
이전 기획·로드맵 문서: https://docs.google.com/document/d/1mJ0oCZeqQ8_PviTsQixYr-ykVpyjNtY94yY_RjqkNDY/edit
작업 추적: https://github.com/hebi10/FocusLock/issues/17
