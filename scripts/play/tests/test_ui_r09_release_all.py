import json
import unittest
from pathlib import Path
import sys
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ui_r09_release_all import (
    eligible_run, allowed_owner, allowed_owner_closed_resume,
    resume_closed_only, ReleaseSequenceError, workflow_runs
)


SHA = "a" * 40
EVENT = {
    "issue": {"number": 14},
    "comment": {"user": {"login": "hebi10"}, "body": "/ui-r09-all"},
}
ENV = {"GITHUB_REPOSITORY": "hebi10/FocusLock", "GITHUB_EVENT_NAME": "issue_comment"}


def record(**kw):
    value = {"id": 150, "head_sha": SHA, "head_branch": "main", "event": "workflow_dispatch"}
    value.update(kw)
    return value


class OwnerReleaseTests(unittest.TestCase):
    def test_only_exact_authorized_issue_comment_can_start(self):
        self.assertTrue(allowed_owner(ENV, EVENT))
        variants = [
            ({**ENV, "GITHUB_REPOSITORY": "other/repo"}, EVENT),
            ({**ENV, "GITHUB_EVENT_NAME": "workflow_dispatch"}, EVENT),
            (ENV, {**EVENT, "issue": {"number": 15}}),
            (ENV, {**EVENT, "comment": {"user": {"login": "attacker"}, "body": "/ui-r09-all"}}),
            (ENV, {**EVENT, "comment": {"user": {"login": "hebi10"}, "body": "/ui-r09-capture"}}),
        ]
        for env, event in variants:
            with self.subTest(event=event):
                self.assertFalse(allowed_owner(env, event))

    def test_closed_only_resume_requires_exact_owner_command(self):
        command = {**EVENT, "comment": {
            "user": {"login": "hebi10"},
            "body": "/ui-r09-resume-closed-v4"
        }}
        self.assertTrue(allowed_owner_closed_resume(ENV, command))
        self.assertFalse(allowed_owner_closed_resume(ENV, EVENT))
        self.assertFalse(allowed_owner_closed_resume(
            ENV, {**command, "comment": {"user": {"login": "someone"}, "body": "/ui-r09-resume-closed-v4"}}
        ))
        self.assertFalse(allowed_owner_closed_resume(
            ENV, {**command, "issue": {"number": 12}}
        ))

    def test_closed_only_resume_rechecks_main_and_does_not_republish_images_or_aab(self):
        with (
            mock.patch("ui_r09_release_all.main_sha", return_value=SHA),
            mock.patch("ui_r09_release_all.launch", side_effect=[14, 15, 16]) as launch,
            mock.patch("ui_r09_release_all.assert_current"),
            mock.patch("ui_r09_release_all.play_readback", return_value=4) as readback,
            mock.patch("ui_r09_release_all.summary"),
        ):
            resume_closed_only()
        self.assertEqual(launch.call_count, 3)
        self.assertEqual(
            [call.args[0] for call in launch.call_args_list],
            ["android-ci.yml", "emulator-smoke.yml", "play-internal.yml"]
        )
        self.assertEqual(
            launch.call_args_list[-1].args[2],
            {"mode": "closed", "expected_internal_version_code": 4}
        )
        readback.assert_called_once_with(16, "play-closed-promotion.json", "alpha", expected=4)

    def test_identifies_only_new_exact_sha_manual_main_run(self):
        self.assertTrue(eligible_run(record(), {100}, SHA))
        for candidate, old, expected in [
            (record(id=150), {150}, SHA),
            (record(head_sha="b" * 40), set(), SHA),
            (record(event="pull_request"), set(), SHA),
            (record(head_branch="feature"), set(), SHA),
            (record(id=True), set(), SHA),
            (record(id=0), set(), SHA),
            (record(), set(), "b" * 40),
        ]:
            with self.subTest(candidate=candidate):
                self.assertFalse(eligible_run(candidate, old, expected))


    def test_closed_report_uses_closed_promotion_artifact(self):
        from ui_r09_release_all import play_readback
        cases = [
            ("internal", "play-release.json", "focuslock-play-report-152",
             "api_committed_and_verified"),
            ("alpha", "play-closed-promotion.json", "focuslock-closed-promotion-152",
             "closed_api_committed_and_verified"),
        ]
        for track, filename, expected_artifact, status in cases:
            with self.subTest(track=track):
                def download(run_id, name, output):
                    self.assertEqual(name, expected_artifact)
                    (output / filename).write_text(json.dumps({
                        "track": track, "versionCode": 5, "status": status
                    }), encoding="utf-8")
                with mock.patch("ui_r09_release_all.artifact", side_effect=download):
                    self.assertEqual(play_readback(152, filename, track), 5)

    def test_unapproved_workflow_never_contacts_github(self):
        with mock.patch("ui_r09_release_all.gh_api") as api:
            with self.assertRaisesRegex(ReleaseSequenceError, "Unapproved"):
                workflow_runs("arbitrary-workflow.yml")
            api.assert_not_called()

    def test_malformed_run_list_rejected(self):
        with mock.patch("ui_r09_release_all.gh_api", return_value={"workflow_runs": None}):
            with self.assertRaisesRegex(ReleaseSequenceError, "missing"):
                workflow_runs("emulator-smoke.yml")


if __name__ == "__main__":
    unittest.main()
