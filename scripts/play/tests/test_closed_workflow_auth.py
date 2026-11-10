from pathlib import Path
import unittest

WORKFLOW = Path(__file__).parents[3] / '.github/workflows/play-internal.yml'


class ClosedWorkflowAuthTests(unittest.TestCase):
    def test_closed_mode_auth_step_has_id_used_by_play_requests(self):
        text = WORKFLOW.read_text(encoding='utf-8')
        self.assertIn('  closed:\n', text)
        closed = text.split('  closed:\n', 1)[1]
        self.assertIn('      - name: Authenticate with Workload Identity Federation\n        id: wif\n', closed)
        self.assertIn('          PLAY_ACCESS_TOKEN: ${{ steps.wif.outputs.access_token }}\n', closed)
        self.assertIn("    if: needs.source.outputs.mode == 'closed'\n", closed)
        self.assertNotIn('ANDROID_RELEASE_KEYSTORE_BASE64', closed)
        self.assertNotIn('bundleRelease', closed)


if __name__ == '__main__':
    unittest.main()
