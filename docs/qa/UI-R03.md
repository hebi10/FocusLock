# UI-R03 — 홈 대시보드 리디자인

기준일: 2026-11-11
상태: 완료 — 동일 SHA Android CI·API36 실제 화면 QA 통과, PR #22 main 병합. Google Play 미배포.
대상: `com.haebi.focuslock`

## 기준과 보호 범위

- 기획 디자인: [D02 홈 대시보드 v1.0](https://docs.google.com/document/d/160M4wHLgML5FHsKuORsU73akWD5ktMAAQp6nRbLW4W4/edit)
- 실제 UI-R00 캡처 37장과 [UI-R01](UI-R01.md) 디자인 토큰을 사용합니다.
- 앱 패키지명·권한·UsageStatsManager·Room DB schema version·목표/집중 보상 지급 로직을 변경하지 않습니다.
- 아이콘, Google Play 상품정보와 이미지, AAB/APK, 내부/비공개 테스트 트랙은 변경하지 않습니다.

## 적용 사항

1. 상단은 오늘의 실제 앱 사용 시간, 전일 비교, 앱별 목표 합산 사용률을 표시합니다.
2. 권한이 없을 때는 접근 안내·Android 설정 진입을 보이고 **집중 시작**을 유지합니다.
3. 사용 기록이 없거나 목표가 없는 상태를 별개로 설명합니다. 실제 기록이 없는 경우 임의 사용량을 생성하지 않습니다.
4. 사용 기록이 있으면 상단 목표 설정 CTA는 그 기록의 실제 앱을 선택해 기존 GoalSheet로 이동합니다. 기존 목표 수정도 실기록에서 확인 가능한 앱을 사용합니다.
5. **집중 시작**을 홈의 별도 대표 CTA로 배치합니다.
6. 많이 사용한 앱 상위 3개는 실제 Android 사용 시간·점유율과 앱별 목표의 남음/초과 상태를 표시합니다. 데이터 수집/제외 정책은 유지합니다.
7. 정원 카드는 누적 XP와 **오늘 획득 XP**를 구분합니다. 오늘 XP는 Room `focus_rewards.earnedAtMillis` 기준 현지 날짜 범위의 실제 지급 경험치 합계입니다. DB 구조/보상 규칙 변경은 없습니다.
8. 지난 7일 카드에는 실제 7일 사용량 합계와 이전 7일 비교를 표시하고 기존 리포트로 진입합니다.

## 검증 매트릭스

| 상태 | 확인할 표시 및 조작 |
| --- | --- |
| 권한 거부 | 접근 안내 및 설정 CTA + 집중 시작 유지 |
| 오늘 실데이터 없음 | 기록 없음 안내, 새로고침, 목표값/사용량 허위 생성 없음 |
| 목표 미설정·오늘 실기록 존재 | 첫 앱의 실제 GoalSheet 진입, 저장 뒤 홈 상태 갱신 |
| 목표 설정 | 관리 앱 목표 합산 진행률과 앱별 목표 진행률/초과 표시 |
| 앱 분석 제외 | 상위 앱 랭킹에 미노출, 전체 실제 사용량은 UsageStats 원천 유지 |
| 집중 보상 획득 | Room 보상 기록과 오늘/누적 XP 일치, 중복 보상 없음 |
| 자정 경계 | 오늘 XP가 새 날짜에 0으로 집계되고 누적 XP는 유지 |
| 라이트/다크 | 모노톤/상태 전용 색상, CTA·내용 배치·전체 스크롤 확인 |

## 필수 게이트

- 동일 PR SHA의 Android CI(단위 테스트/Lint/Debug APK) 성공
- Android API 36 Pixel 6 실제 UI QA 성공, 기존 37장 캡처 및 주요 홈 상태 육안 비교
- Room Persistence QA의 자정 경계/중복 보상 검증
- 글꼴 확대·API29/35·폴더블/실기기는 종합 `UI-R08`에서 확대하며 현재 통과로 단정하지 않습니다.
- QA 통과 및 시각적 검수 후에만 PR 병합. Google Play 자동 배포는 본 작업에서 수행하지 않습니다.

후속: `UI-R04` 사용 분석 → `UI-R05` 앱 상세·목표 → `UI-R02` 온보딩.

작업 추적: https://github.com/hebi10/FocusLock/issues/21


## UI-R02·04·05 이후 통합 (2026-11-11)

- PR #22의 홈 화면·Room 오늘 XP 집계 및 자정 경계 테스트를 최신 main에 적용하고, UI-R02 온보딩·R04 분석·R05 앱 상세/요일 목표 저장·알림을 보존합니다.
- 이전 PR head: `c44b373dca3d8f6ea1cef17c23cc78f94984a3d6`.
- 검증: Android CI·API36 전체 화면 37장+R04 5장, R05 요일·저장 테스트, R03 오늘 XP 자정 경계 테스트가 **동일 통합 SHA**에서 모두 통과해야 main 병합 가능합니다.
- 실제 Android UsageStats 소량 데이터는 별도 기록하며, 다중 앱·날짜/API29·35/큰 글꼴/폴더블/실기기는 UI-R08 후속입니다.
- Google Play 바이너리·서명·트랙·이미지는 변경하지 않습니다.


## UI-R03 최종 통합 QA 및 main 병합 (2026-11-11)

- [PR #22](https://github.com/hebi10/FocusLock/pull/22): 기존 Draft 상태를 Ready로 전환한 후 통합 HEAD `24a456191a09366984f182791f3c5fc18192bb0a` 검증 완료 및 squash 병합.
- [Android CI #42989795818](https://github.com/hebi10/FocusLock/actions/runs/42989795818): success — 단위 테스트·Lint·Debug APK 빌드/안전 가드.
- [Android API36 QA #42989795866](https://github.com/hebi10/FocusLock/actions/runs/42989795866): success — 권한 거부·허용, Android 실사용 기록·목표·하단 스크롤, 분석 기간/검색, Room 복구·보상 검사.
- `FocusPersistenceQaTest#todayExperienceIsLimitedToEarnedRewardsFromLocalDay`: **성공** — 오늘 보상 XP와 전체 누적 XP를 구분하고 자정 이후 오늘 XP만 0으로 초기화되는 집계 경계 검증.
- 실제 API36 Android 화면 **42장**: R00 기준 스크린샷 **37/37**, R04 분석 전용 **5/5**. 아티팩트 [#13841179463](https://github.com/hebi10/FocusLock/actions/runs/42989795866/artifacts/13841179463), ZIP SHA256 `0c3c3b36d61b809e84ac38642c61b3b3a0e4a54540a970e6463e2a5b0edf3fb9`.
- 실제 캡처 수동 비교: 기획 D02 홈 라이트/다크와 권한 미허용·허용 홈, 홈 하단 정원/7일 요약, R05 목표 시트의 상태 및 버튼 접근성 확인. **실제 2개 앱·2분·하루 사용 기록**만 수집되어 풍부한 통계 시안과 동일한 데이터 밀도는 검증 불가.
- UI-R02 온보딩, R04 사용 분석(검색/이름 정렬/7·30일), R05 앱 상세·월~일 목표 알림 정책 및 저장 회귀 보존 확인.
- main squash 커밋: `6c52e8d0c47e38d3ac0fca871a7413853f684fdf` (`[skip ci]`). Google Play AAB·서명·버전·테스트 트랙·스토어 이미지는 변경하지 않음.
- 미검증: Android API29/35, 화면 글꼴 160%, 태블릿·폴더블·삼성 실기기, 3개 이상 앱·2일 이상 장기간 실제 사용 데이터. 이 항목은 `UI-R08`에서 검증해야 함.
