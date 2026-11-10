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
run_case 'com.haebi.focuslock.FocusLockScreenQaTest#onboardingAndPermissionDeniedScreens' permission-denied

adb shell appops set com.haebi.focuslock GET_USAGE_STATS allow
adb shell am start -a android.settings.SETTINGS
sleep 50
adb shell input keyevent KEYCODE_HOME
run_case 'com.haebi.focuslock.FocusLockScreenQaTest#grantedUsageAndDetailScreens' permission-granted

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
)
for name in "${expected[@]}"; do
  if [[ ! -s "captures/qa-captures/${name}.png" ]]; then
    echo "::error::Missing or empty actual emulator capture: ${name}.png"
    failed=1
  fi
done

echo "Captured $(find captures/qa-captures -name '*.png' -type f 2>/dev/null | wc -l) of ${#expected[@]} required screens"
exit "${failed}"
