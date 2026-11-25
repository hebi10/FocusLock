"""OPS-04 offline tests. No Google Play or GitHub mutations."""
import importlib.util
import os
from pathlib import Path
import re
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "ops04" / "internal.py"
spec = importlib.util.spec_from_file_location("ops04_internal", SCRIPT)
internal = importlib.util.module_from_spec(spec)
spec.loader.exec_module(internal)

class Ops04SafetyTests(unittest.TestCase):
    def environment(self, mode="publish"):
        return {
            "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_REF": "refs/heads/main",
            "GITHUB_SHA": "a" * 40,
            "EXPECTED_MAIN_SHA": "a" * 40,
            "GITHUB_REPOSITORY": "hebi10/fixture",
            "BATCH_ID": "2026-11-13-fixture",
            "BUILD_RUN_ID": "45564726965",
            "BUILD_WORKFLOW_FILE": ".github/workflows/play-internal.yml",
            "ARTIFACT_NAME": "verified-signed-aab-123",
            "VERSION_CODE": "123",
            "VERSION_NAME": "1.0.0",
            "AAB_SHA256": "b" * 64,
            "DEPLOY_MODE": mode,
            "FINAL_RELEASE_APPROVED": "false",
            "APPROVAL_PHRASE": "",
        }

    def test_publish_requires_separate_approval_before_network(self):
        with patch.dict(os.environ, self.environment(), clear=True):
            with patch.object(internal, "github") as api:
                with self.assertRaises(internal.GateError):
                    internal.guard()
                api.assert_not_called()

    def test_bad_main_sha_rejected_before_network(self):
        data = self.environment(mode="check")
        data["EXPECTED_MAIN_SHA"] = "c" * 40
        with patch.dict(os.environ, data, clear=True):
            with patch.object(internal, "github") as api:
                with self.assertRaises(internal.GateError):
                    internal.guard()
                api.assert_not_called()

    def test_invalid_artifact_name_rejected(self):
        data = self.environment(mode="check")
        data["ARTIFACT_NAME"] = "../other-run"
        with patch.dict(os.environ, data, clear=True):
            with patch.object(internal, "github") as api:
                with self.assertRaises(internal.GateError):
                    internal.guard()
                api.assert_not_called()

    def evidence(self, data, **run_changes):
        run = {
            "id": int(data["BUILD_RUN_ID"]),
            "head_sha": data["EXPECTED_MAIN_SHA"],
            "head_branch": "main",
            "event": "workflow_dispatch",
            "status": "completed",
            "conclusion": "success",
            "repository": {"full_name": data["GITHUB_REPOSITORY"]},
            "path": data["BUILD_WORKFLOW_FILE"],
        }
        run.update(run_changes)
        return [
            {"object": {"sha": data["EXPECTED_MAIN_SHA"]}},
            run,
            {"total_count": 1, "artifacts": [{
                "name": data["ARTIFACT_NAME"], "expired": False, "size_in_bytes": 10001,
            }]},
        ]

    def test_real_eleven_digit_and_int64_run_ids_are_accepted(self):
        for run_id in ("45564726965", "9223372036854775807"):
            with self.subTest(run_id=run_id):
                data = self.environment(mode="check")
                data["BUILD_RUN_ID"] = run_id
                with patch.dict(os.environ, data, clear=True):
                    with patch.object(internal, "github", side_effect=self.evidence(data)) as api:
                        internal.guard()
                        self.assertEqual(api.call_args_list[1].args,
                                         ("repos/hebi10/fixture/actions/runs/" + run_id,))
                        self.assertEqual(api.call_count, 3)

    def test_malformed_and_out_of_range_run_ids_rejected_before_network(self):
        for run_id in ("", "0", "045564726965", "-1", "+1", "1/2", "1?x=2",
                       "1\n", "１", "9223372036854775808", "1" * 20):
            with self.subTest(run_id=run_id):
                data = self.environment(mode="check")
                data["BUILD_RUN_ID"] = run_id
                with patch.dict(os.environ, data, clear=True):
                    with patch.object(internal, "github") as api:
                        with self.assertRaisesRegex(internal.GateError, "Invalid source run ID"):
                            internal.guard()
                        api.assert_not_called()

    def test_valid_long_id_does_not_relax_provenance(self):
        data = self.environment(mode="check")
        for changes in ({"id": 45564726966}, {"id": "45564726965"},
                        {"head_sha": "c" * 40}, {"head_branch": "other"},
                        {"event": "push"}, {"conclusion": "failure"},
                        {"repository": {"full_name": "hebi10/other"}},
                        {"path": ".github/workflows/other.yml"}):
            with self.subTest(changes=changes):
                with patch.dict(os.environ, data, clear=True):
                    with patch.object(internal, "github", side_effect=self.evidence(data, **changes)) as api:
                        with self.assertRaises(internal.GateError):
                            internal.guard()
                        self.assertEqual(api.call_count, 2)

    def test_missing_bundle_fails_closed(self):
        with tempfile.TemporaryDirectory() as folder:
            with patch.object(internal, "BUNDLE_PATH", Path(folder)):
                with self.assertRaises(internal.GateError):
                    internal.verify()

    def test_sha256_streaming_matches_reference(self):
        with tempfile.TemporaryDirectory() as folder:
            bundle = Path(folder) / "x.aab"
            bundle.write_bytes(b"ops04-immutable-byte-fixture")
            import hashlib
            self.assertEqual(internal.digest(bundle), hashlib.sha256(bundle.read_bytes()).hexdigest())

    def test_workflow_never_builds_or_starts_on_push(self):
        source = (ROOT / ".github" / "workflows" / "play-internal-release.yml").read_text(encoding="utf-8")
        self.assertIn("  workflow_dispatch:", source)
        self.assertNotRegex(source, r"(?m)^  (push|workflow_run|issue_comment|schedule|pull_request):")
        self.assertIn("inputs.mode == 'publish'", source)
        self.assertIn("PUBLISH_VERIFIED_INTERNAL_AAB", source)
        self.assertIn("scripts/ops04/internal.py verify", source)
        self.assertNotIn("bundleRelease", source)
        self.assertNotIn("assembleRelease", source)
        self.assertNotIn("gh release create", source)
        self.assertIn("androidpublisher", source)

    def test_verification_uses_existing_environment_certificate_without_play_auth(self):
        source = (ROOT / ".github" / "workflows" / "play-internal-release.yml").read_text(encoding="utf-8")
        verify, publish = source.split("  publish:\n", 1)
        verify = verify.split("  verify:\n", 1)[1]
        self.assertIn("    environment: google-play", verify)
        self.assertIn("    environment: google-play", publish)
        self.assertNotIn("id-token: write", verify)
        self.assertNotIn("google-github-actions/auth@", verify)
        for job in (verify, publish):
            self.assertIn("PINNED_UPLOAD_CERT: ${{ secrets.ANDROID_RELEASE_CERT_SHA256 }}", job)
            self.assertIn("ANDROID_RELEASE_CERT_SHA256: ${{ secrets.ANDROID_RELEASE_CERT_SHA256 }}", job)
            self.assertLess(job.index("scripts/ops04/internal.py guard"), job.index("scripts/ops04/internal.py verify"))

if __name__ == "__main__":
    unittest.main()
