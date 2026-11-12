import unittest
from source_gate import select_capture


SHA = "a" * 40
OLD = "b" * 40


def run(**kw):
    result = {
        "id": 4321,
        "head_branch": "main",
        "head_sha": SHA,
        "event": "workflow_dispatch",
        "status": "completed",
        "conclusion": "success",
    }
    result.update(kw)
    return result


class SourceGateTest(unittest.TestCase):
    def test_exact_main_capture_accepted(self):
        self.assertEqual((4321, SHA), select_capture([run()], SHA))

    def test_old_main_capture_rejected(self):
        with self.assertRaisesRegex(ValueError, "no successful matching"):
            select_capture([run(head_sha=OLD)], SHA)

    def test_pr_capture_rejected_even_with_same_sha(self):
        with self.assertRaises(ValueError):
            select_capture([run(event="pull_request")], SHA)

    def test_non_main_and_failed_capture_rejected(self):
        with self.assertRaises(ValueError):
            select_capture([run(head_branch="feature"), run(conclusion="failure")], SHA)

    def test_latest_successful_eligible_capture_selected(self):
        self.assertEqual((4323, SHA), select_capture(
            [run(event="pull_request", id=4324), run(id=4323), run(id=4322)],
            SHA,
        ))

    def test_malformed_main_sha_rejected(self):
        with self.assertRaisesRegex(ValueError, "Invalid"):
            select_capture([run()], "invalid")

    def test_incomplete_runs_rejected(self):
        with self.assertRaises(ValueError):
            select_capture([run(status="in_progress")], SHA)


if __name__ == "__main__":
    unittest.main()
