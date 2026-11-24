from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


class ReleaseConfigurationTests(unittest.TestCase):
    def test_internal_workflow_requires_explicit_dispatch_after_renew_freeze(self):
        path = ROOT / '.github/workflows/play-internal.yml'
        self.assertTrue(path.is_file(), 'FocusLock internal release workflow is required')
        workflow = path.read_text()
        self.assertIn('group: play-com-haebi-focuslock', workflow)
        self.assertIn('workflow_dispatch:', workflow)
        self.assertNotIn('workflow_run:', workflow.split('permissions:', 1)[0])
        self.assertIn("    if: github.event_name == 'workflow_dispatch'", workflow)
        self.assertIn('PLAY_CI_WAIT_SECONDS:', workflow)
        self.assertNotIn('PLAY_SERVICE_ACCOUNT_JSON', workflow)
        self.assertNotIn('promote --', workflow)
        self.assertFalse((ROOT / '.github/workflows/play-promote.yml').exists())

    def test_final_emulator_gate_requires_manual_exact_main_and_preserves_tests(self):
        workflow = (ROOT / '.github/workflows/emulator-smoke.yml').read_text()
        triggers = workflow.split('permissions:', 1)[0]
        self.assertIn('workflow_dispatch:', triggers)
        self.assertIn('expected_main_sha:', triggers)
        self.assertNotIn('  pull_request:', triggers)
        self.assertNotIn('  push:', triggers)
        self.assertIn('needs: validate-source', workflow)
        self.assertIn('"$EXPECTED_MAIN_SHA" == "$GITHUB_SHA"', workflow)
        self.assertIn('gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/main"', workflow)
        self.assertIn('script: bash scripts/run-emulator-qa.sh', workflow)
        release = (ROOT / '.github/workflows/release-build.yml').read_text()
        self.assertIn('REQUIRED_CI_JSON: \'["Android CI","Android full-screen emulator QA"]\'', release)

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
