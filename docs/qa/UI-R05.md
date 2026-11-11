# UI-R05 — 앱 상세·목표 설정 통합 개선

기준: [Google Drive 전체 화면 디자인 D04·D05](https://docs.google.com/document/d/160M4wHLgML5FHsKuORsU73akWD5ktMAAQp6nRbLW4W4/edit), [R00 QA 기준](UI-R00.md), [R04 실제 Android 화면](UI-R04.md).
대상: FocusLock `com.haebi.focuslock` · Kotlin/Compose/Android UsageStatsManager.
상태: 완료 — 동일 HEAD Android CI·API36 실제화면 QA 통과, PR #25 main 병합. Google Play 미배포.

## 1. 앱 상세

- 앱 상세 상단 `오늘 / 7일 / 30일` 기간 전환과 실제 해당 기간 앱 사용 시간 연결.
- `UsageSnapshot.window(period)`에서 앱별 선택 기간 수치를 조회. 해당 기간에 기록이 없는 앱은 0분·이벤트 확인 불가 상태로 표시하고 이전 기간 수치를 재사용하지 않음.
- 오늘의 앱 목표에만 일일 목표 진행률 막대와 남음/초과 분을 표시. 7일/30일 집계를 일일 목표와 혼동하지 않음.
- 최근 7일·30일의 앱별 일간 분포를 실제 UsageStatsManager의 일별 앱 데이터로 표시. `appDaily30` 신규 필드를 스냅샷 끝에 기본값으로 추가해 기존 호출자 호환성을 보존.
- 날짜별 막대는 가로 스크롤 및 48dp 터치 영역·선택 수치·접근성 설명 제공. Android에서 조회 불가한 앱의 실제 권한/배터리 수치는 생성하지 않음.
- 기존 사용 목표 버튼, 앱 권한, 기기 영향, 분석 제외/방해 앱 상태는 유지.

## 2. 목표 설정

- 15~240분/15분 간격·80/100% 알림·저장/해제 기능 유지.
- 작은 화면에서도 CTA에 접근할 수 있도록 `ModalBottomSheet`를 **최초부터 완전 펼침** 상태로 표시하고, 콘텐츠 스크롤과 고정 저장/해제 영역을 분리합니다. 시트 배경은 중성 흰색/차콜 토큰을 사용하며 하단 시스템 안전 영역을 확보합니다.
- 월~일 7개 요일 선택, 전체 선택/해제. ISO 월=bit0, 일=bit6의 7비트 마스크를 `goalDays.<appId>`에 기기 내 저장.
- **정확한 현재 의미:** 일일 목표 시간은 모든 날 저장되어 조회 가능하고, 체크한 요일에만 자동 목표 경고를 확인합니다. 일부 요일에 목표 자체가 존재하지 않는 것처럼 표시하지 않음.
- 최초 마이그레이션: 과거 `everyday.<appId>=true`는 7일 모두 선택, `false`는 모든 알림 요일 해제. 새 마스크가 있는 경우 마스크를 우선 적용. 기존 목표 시간·알림 80/100 옵션 데이터 보존.
- 요일을 모두 해제하면 자동 경고 중단·WorkManager 작업 취소(다른 활성 목표가 없을 때). 저장 버튼 동작과 기존 경고 중복 방지 로직 유지.

## 3. 테스트 및 배포 보호

- `GoalDayPolicyTest`: 이전 설정 호환성, 월요일부터 일요일까지 ISO 비트 매핑, 전체/없음, 토글.
- Android CI: Unit + Lint + Debug APK + Play 안전 가드.
- 기존 API36 전체 42개 화면(기본 37 + R04 5) 재검증, 목표 시트 저장, 앱 상세 진입 및 Android OS 실제 기록 확인.
- API36 계측: 목표 시트 첫 진입 저장 CTA 가시성, 월요일 선택 토글 후 실제 SharedPreferences 요일 마스크 저장 검증. 실제 7일/30일 앱 상세 단독 캡처, 160% 글꼴·소형 실기기·API29/35는 후속 QA로 분리.
- API29/35, Galaxy 실기기/폴더블, 160% 글꼴은 이번 API36 QA에서 결과를 단정하지 않음.
- `UI-R03` PR #22는 미병합, `FocusLockApp.kt`의 변경 부위가 겹치므로 병합·리베이스 전 통합 QA 필요.
- 앱 ID, 버전·서명·AAB·Play 트랙·스토어 이미지 불변. 스토어 이미지는 UI-R09 재제작 대상으로 유지.

**병합 조건:** 동일 HEAD SHA Android CI·API36 실화면 QA 통과, 실제 화면·데이터/요일 보존 검수, PR #22 간섭 확인 후 main 병합. Google Play 자동 배포는 별도 승인 범위.

- UI QA 주의: 초기 API36 목표 시트 스크린샷에서는 부분 펼침 때문에 저장 버튼이 보이지 않았습니다. 이후 `skipPartiallyExpanded=true` 및 저장 버튼 첫 진입 가시성 계측을 추가하였고, **수정 커밋의 UI 검증 결과를 받아야 최종 완료로 판정**합니다.

- UI-R05 색상 회귀 수정: 기본 Material3 보라색 선택 칩을 라이트/다크 공통 `primary/onPrimary`와 중성 `surfaceVariant`로 교체, 작은 모서리 모양을 사용합니다. 최종 동일 SHA CI·에뮬레이터 QA 성공 후 병합합니다.

## 최종 QA 및 병합 기록 (2026-11-11)

- GitHub PR: https://github.com/hebi10/FocusLock/pull/25
- 최종 검증 head: `7293a3c7b3508366a156a30f8db1a96eb23034dc`.
- Android CI: https://github.com/hebi10/FocusLock/actions/runs/42985487530 — **success**.
- API36 실제 Android 전체 화면 QA: https://github.com/hebi10/FocusLock/actions/runs/42985487348 — **success**.
- 실화면 QA: R00 37/37장 + R04 5/5장 = 총 **42장**. artifact `13839284233`, SHA256 `a200650f38c64ee31c7791c7df84500ecb56818aa01e757de09493777c351977`.
- Android UI 계측에서 요일 `월` 선택 후 정확한 `goalDays.com.android.settings` 저장 마스크 확인. 최초 시트에서 스크롤 없이 `저장` 버튼을 인식하고 클릭 가능, 저장 후 시트 종료.
- 월~일 선택 칩의 기본 Material 보라색을 제거하고 FocusLock의 무채색(선택된 칩=기본 text color/선택 글자=onPrimary)으로 통일한 실제 Android 캡처 시각 검수 완료.
- 기존 기간별 분석/앱 권한/집중·Room/목표 알림 회귀 테스트 통과. 구형 `everyday.*` 호환 정책 단위 테스트 통과.
- 실제 데이터는 `limited_real_data` (관찰 대상 Android 앱 2개, 1일). 3개 이상 앱·다중 일자·API29/35·160% 글꼴·실기기·태블릿·폴더블은 UI-R08에 남음.
- `main` squash 병합 `d23ca2585042824a059ad1eaebb3de3b5d0f68d0` (`[skip ci]`). Google Play AAB·서명·버전·트랙·스토어 이미지는 변경하지 않음.
- UI-R03 PR #22는 아직 미병합이며, `FocusLockApp.kt` 공유 변경이 있어 별도 브랜치 업데이트·통합 QA 후 병합해야 함.
