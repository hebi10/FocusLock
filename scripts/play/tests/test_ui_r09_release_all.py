import unittest
from pathlib import Path
import sys
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ui_r09_release_all import eligible_run, allowed_owner, ReleaseSequenceError, workflow_runs


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
