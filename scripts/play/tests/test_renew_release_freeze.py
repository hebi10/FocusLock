import unittest
from pathlib import Path


class FocusLockRenewReleaseFreezeTest(unittest.TestCase):
    def test_play_release_requires_manual_dispatch(self):
        path = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "play-internal.yml"
        workflow = path.read_text(encoding="utf-8")
        header = workflow.split("\npermissions:", 1)[0]
        self.assertIn("\n  workflow_dispatch:", header)
        self.assertNotIn("\n  workflow_run:", header)
        self.assertIn("    if: github.event_name == 'workflow_dispatch'", workflow)

    def test_store_upload_remains_explicitly_gated(self):
        path = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "store-01-images.yml"
        workflow = path.read_text(encoding="utf-8")
        self.assertIn("github.event.comment.user.login == 'hebi10'", workflow)
        self.assertIn("inputs.mode == 'upload'", workflow)
        self.assertNotIn("\n  push:", workflow.split("\npermissions:", 1)[0])


if __name__ == "__main__":
    unittest.main()
