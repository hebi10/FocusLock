from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


class ReleaseConfigurationTests(unittest.TestCase):
    def test_internal_workflow_uses_focuslock_package_and_waits_for_both_ci_sources(self):
        path = ROOT / '.github/workflows/play-internal.yml'
        self.assertTrue(path.is_file(), 'FocusLock internal release workflow is required')
        workflow = path.read_text()
        self.assertIn('group: play-com-haebi-focuslock', workflow)
        self.assertIn('workflows: ["Android CI"]', workflow)
        self.assertIn('PLAY_CI_WAIT_SECONDS:', workflow)
        self.assertNotIn('PLAY_SERVICE_ACCOUNT_JSON', workflow)
        self.assertNotIn('promote --', workflow)
        self.assertFalse((ROOT / '.github/workflows/play-promote.yml').exists())

    def test_emulator_gate_runs_for_main_push_without_changing_test_script(self):
        workflow = (ROOT / '.github/workflows/emulator-smoke.yml').read_text()
        self.assertIn('push:\n    branches: [main]', workflow)
        self.assertIn('script: bash scripts/run-emulator-qa.sh', workflow)

    def test_gradle_preserves_version_and_requires_existing_release_signer(self):
        gradle = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertIn('versionName = "0.3.0"', gradle)
        self.assertIn('versionCode = releaseVersionCode', gradle)
        self.assertIn('.orElse("2").get()', gradle)
        for dependency in ('alias(libs.plugins.ksp)', 'implementation(libs.room.runtime)', 'ksp(libs.room.compiler)', 'implementation(libs.work.runtime)'):
            self.assertIn(dependency, gradle)
        self.assertIn('if (name == "preReleaseBuild") dependsOn(verifyReleaseSigning)', gradle)
        self.assertIn('"verify-keystore"', gradle)


if __name__ == '__main__':
    unittest.main()
