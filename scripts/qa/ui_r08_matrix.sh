#!/usr/bin/env bash
set -Eeuo pipefail

api="${1:?Android API level required}"
root="captures/ui-r08-api${api}"
mkdir -p "${root}/logs" "${root}/screens"
failures=0
tests=0

cleanup() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
  adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
}
trap cleanup EXIT

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_case() {
  local method="$1" variant="$2" font="$3"
  local safe="${method##*#}"
  local log="${root}/logs/${variant}-${safe}.txt"
  echo "::group::UI-R08 ${variant} ${method}"
  tests=$((tests + 1))
  if ! adb shell am instrument -w -r \
      -e class "${method}" \
      -e qa_variant "${variant}" \
      -e expected_font_scale "${font}" \
      com.haebi.focuslock.test/androidx.test.runner.AndroidJUnitRunner \
      2>&1 | tee "${log}"; then
    echo "::error::Android instrumentation process failed: ${method} ${variant}"
    failures=$((failures + 1))
  fi
  if ! grep -Eq '^OK \([0-9]+ tests?\)' "${log}"; then
    echo "::error::Android instrumentation did not report success: ${method} ${variant}"
    failures=$((failures + 1))
  fi
  echo "::endgroup::"
}

set_viewport() {
  local variant="$1" size="$2" density="$3" font="$4"
  echo "::group::Viewport ${variant}"
  adb shell wm size "${size}"
  adb shell wm density "${density}"
  adb shell settings put system font_scale "${font}"
  adb shell am force-stop com.haebi.focuslock || true
  sleep 3
  echo "variant=${variant}, api=${api}, size=${size}, density=${density}, font_scale=${font}"
  adb shell wm size
  adb shell wm density
  adb shell settings get system font_scale
  run_case 'com.haebi.focuslock.FocusLockResponsiveQaTest#homeReportSettingsAndPermissionsRemainAccessible' "${variant}" "${font}"
  run_case 'com.haebi.focuslock.FocusLockResponsiveQaTest#focusStartAndGardenNavigationRemainReachable' "${variant}" "${font}"
  echo "::endgroup::"
}

adb shell appops set com.haebi.focuslock GET_USAGE_STATS ignore
run_case 'com.haebi.focuslock.FocusLockUsageStateAuditTest#permissionDeniedReallyReturnsPermissionRequired' "api${api}-denied" 1.0

adb shell appops set com.haebi.focuslock GET_USAGE_STATS allow
adb shell am start -a android.settings.SETTINGS >/dev/null
sleep 20
adb shell input keyevent KEYCODE_HOME
run_case 'com.haebi.focuslock.FocusLockUsageStateAuditTest#permissionGrantedCapturesRealUsageCoverage' "api${api}-granted" 1.0
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#storedRewardSurvivesDatabaseReopenAndIsNotDuplicated' "api${api}-room" 1.0
run_case 'com.haebi.focuslock.FocusPersistenceQaTest#rebootPausesSessionAndNeverInventsCompletion' "api${api}-reboot" 1.0

if [[ "${api}" == "29" || "${api}" == "35" ]]; then
  adb shell wm size reset
  adb shell wm density reset
  adb shell settings put system font_scale 1.0
  set_viewport "api${api}-phone" "1080x2400" "420" "1.0"
else
  set_viewport "api36-phone" "1080x2400" "420" "1.0"
  set_viewport "api36-compact-font160" "880x2200" "420" "1.6"
  set_viewport "api36-tablet-portrait" "1280x1920" "240" "1.0"
  set_viewport "api36-tablet-landscape" "1920x1280" "240" "1.0"
  set_viewport "api36-fold-closed-sim" "880x2260" "420" "1.0"
  set_viewport "api36-fold-open-sim" "1760x2200" "420" "1.0"
fi

if ! adb pull /sdcard/Android/data/com.haebi.focuslock/files/qa-r08/ "${root}/screens/"; then
  echo "::error::Unable to retrieve actual device screenshots"
  failures=$((failures + 1))
fi
expected=6
if [[ "${api}" == "36" ]]; then expected=36; fi
count=$(find "${root}/screens" -type f -name '*.png' -size +0c | wc -l)
echo "API ${api}: ${tests} instrumentation cases, ${count} captured screenshots, ${failures} failures."
if [[ "${count}" -lt "${expected}" ]]; then
  echo "::error::Expected at least ${expected} actual device screenshots; found ${count}"
  failures=$((failures + 1))
fi

printf 'api=%s\nvariant_tests=%s\ncaptures=%s\nfailures=%s\n' "${api}" "${tests}" "${count}" "${failures}" >"${root}/summary.txt"
if [[ "${failures}" -ne 0 ]]; then exit 1; fi
