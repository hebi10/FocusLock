from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
import source

EVENT = {
    'workflow_run': {
        'name': 'Android CI',
        'path': '.github/workflows/android-ci.yml',
        'event': 'push',
        'conclusion': 'success',
        'head_branch': 'main',
        'head_repository': {'full_name': 'hebi10/FocusLock'},
        'head_sha': 'a' * 40,
    }
}
ENV = {'GITHUB_REPOSITORY': 'hebi10/FocusLock',
       'GITHUB_EVENT_NAME': 'workflow_run',
       'GITHUB_REF': 'refs/heads/main'}


class IconTriggerTest(unittest.TestCase):
    def test_exact_subject_marker_runs_only_icon_mode(self):
        self.assertEqual(source.release_mode(ENV, EVENT, 'feat: store icon [upload-play-icon]'), 'icon')
        self.assertEqual(source.release_mode(ENV, EVENT, '[upload-play-icon]'), 'icon')

    def test_other_subjects_remain_normal_internal_deploy(self):
        self.assertEqual(source.release_mode(ENV, EVENT, 'feat: store icon'), 'deploy')
        self.assertEqual(source.release_mode(ENV, EVENT, 'feat: store icon[upload-play-icon]'), 'deploy')

    def test_conflicting_initialization_and_icon_flags_block(self):
        with self.assertRaisesRegex(source.SourceError, 'cannot be combined'):
            source.release_mode(ENV, EVENT, 'x [initialize-play] [upload-play-icon]')

    def test_manual_mode_requires_main(self):
        env = {**ENV, 'GITHUB_EVENT_NAME': 'workflow_dispatch',
               'GITHUB_SHA': 'a' * 40, 'PLAY_REQUESTED_MODE': 'icon'}
        self.assertEqual(source.release_mode(env, {}, ''), 'icon')
        with self.assertRaises(source.SourceError):
            source.release_mode({**env, 'GITHUB_REF': 'refs/heads/feature'}, {}, '')


if __name__ == '__main__':
    unittest.main()
