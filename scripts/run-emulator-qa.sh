#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p captures/qa-logs
failed=0

# Android 36 can show a transient System UI ANR immediately after its first cold boot.
adb shell input keyevent KEYCODE_HOME
sleep 20
adb shell input keyevent KEYCODE_HOME

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_case() {
  local test_name="$1"
  local log_name="$2"
  local log_path="captures/qa-logs/${log_name}.txt"
  echo "::group::${test_name}"

  if ! adb shell am instrument -w -r -e class "${test_name}" \
      com.haebi.focuslock.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee "${log_path}"; then
    echo "::error::Instrumentation command failed: ${test_name}"
    failed=1
  fi
  if ! grep -Eq '^OK \([0-9]+ tests?\)' "${log_path}"; then
    echo "::error::Instrumentation did not report passing tests: ${test_name}"
    failed=1
  fi

  echo "::endgroup::"
}

adb shell appops set com.haebi.focuslock GET_USAGE_STATS ignore
run_case 'com.haebi.focuslock.FocusLockUsageStateAuditTest#permissionDeniedReallyReturnsPermissionRequired' ui-r00-usage-denied
run_case 'com.haebi.focuslock.FocusLockScreenQaTest#onboardingAndPermissionDeniedScreens' permission-denied
run_case 'com.haebi.focuslock.FocusLockOnboardingQaTest#deniedPermissionAndNoGoalStillEnterUsableHome' ui-r02-denied-skip
run_case 'com.haebi.focuslock.FocusLockOnboardingQaTest#permissionSettingsReturnRefreshesStepAndGoalIsSkippable' ui-r02-settings-return

adb shell appops set com.haebi.focuslock GET_USAGE_STATS allow
adb shell pm grant com.haebi.focuslock android.permission.POST_NOTIFICATIONS
adb shell am start -a android.settings.SETTINGS
sleep 50
adb shell input keyevent KEYCODE_HOME
run_case 'com.haebi.focuslock.FocusLockScreenQaTest#grantedUsageAndDetailScreens' permission-granted
run_case 'com.haebi.focuslock.FocusLockAnalysisQaTest#usageAnalysisSearchSortAndPeriodAreOperableWithRealAndroidData' ui-r04-analysis-search-sort
run_case 'com.haebi.focuslock.FocusLockAnalysisQaTest#periodTabsShowWeeklyAndMonthlyCharts' ui-r04-periods
run_case 'com.haebi.focuslock.FocusLockUsageStateAuditTest#permissionGrantedCapturesRealUsageCoverage' ui-r00-usage-granted
run_case 'com.haebi.focuslock.RenewUsageCoverageQaTest#actualAndroidUsageSummaryKeepsRawTotalsAndCoverageHonest' renew-03-usage-coverage
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#storedRewardSurvivesDatabaseReopenAndIsNotDuplicated' room-reopen-reward
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#todayExperienceIsLimitedToEarnedRewardsFromLocalDay' room-today-experience
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#rebootPausesSessionAndNeverInventsCompletion' room-reboot-recovery
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#repeatedStartKeepsActiveSessionAndEarlyStopNeverEarnsRewards' room-duplicate-start-and-early-stop
run_case 'com.haebi.focuslock.RenewNavigationQaTest#reportsAreTopLevelAndGardenRetainsFocusTab' renew-02a-navigation
run_case 'com.haebi.focuslock.RenewHomeQaTest#homePresentsUsageBeforeOptionalFocusAndAllowsAnalysis' renew-02b-home

if ! adb pull /sdcard/Android/data/com.haebi.focuslock/files/qa-r00/ captures/; then
  echo "::error::Could not pull UI-R00 real usage evidence"
  failed=1
fi

if ! adb pull /sdcard/Android/data/com.haebi.focuslock/files/qa-r04-captures/ captures/; then
  echo "::error::Could not pull UI-R04 real Android analysis captures"
  failed=1
fi

for name in r04-analysis-today r04-analysis-search-empty r04-analysis-sort-name r04-analysis-week r04-analysis-month; do
  if [[ ! -s "captures/qa-r04-captures/${name}.png" ]]; then
    echo "::error::Missing UI-R04 actual Android capture: ${name}.png"
    failed=1
  fi
done

if ! adb pull /sdcard/Android/data/com.haebi.focuslock/files/qa-captures/ captures/; then
  echo "::error::Could not pull screenshots from the Android emulator"
  failed=1
fi

expected=(
  01-onboarding-intro 02-onboarding-permission 03-onboarding-goal
  04-home-denied 05-analysis-denied 06-settings 07-permission-management-denied
  08-focus-setup 09-focus-running 10-focus-stop-confirm 11-focus-result
  12-garden 13-garden-item 14-home-granted 15-analysis-today
  16-analysis-week 17-analysis-month 18-app-detail 19-goal-sheet
  20-report 21-dark-settings 22-dark-home 23-permission-management-granted
  24-onboarding-intro-scroll 25-onboarding-goal-scroll
  26-settings-scroll 27-permissions-denied-scroll 28-garden-scroll
  29-home-granted-scroll 30-analysis-today-scroll 31-analysis-week-scroll
  32-analysis-month-scroll 33-app-detail-scroll 34-report-scroll
  35-dark-settings-scroll 36-dark-home-scroll 37-permissions-granted-scroll
)
for name in "${expected[@]}"; do
  if [[ ! -s "captures/qa-captures/${name}.png" ]]; then
    echo "::error::Missing or empty actual emulator capture: ${name}.png"
    failed=1
  fi
done

echo "Captured $(find captures/qa-captures -name '*.png' -type f 2>/dev/null | wc -l) of ${#expected[@]} required screens"
if ! python3 scripts/qa/ui_r00_verify.py run --capture-root captures --output captures/ui-r00-audit-report.json; then
  echo "::error::UI-R00 screenshot, permission or usage-state report validation failed"
  failed=1
fi
exit "${failed}"
