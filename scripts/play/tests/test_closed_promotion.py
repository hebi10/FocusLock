import copy
import pathlib
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(pathlib.Path(__file__).parents[1]))
import closed_promotion
import publisher
import source

SHA = 'a' * 40
ENV = {'GITHUB_REPOSITORY': 'hebi10/FocusLock', 'GITHUB_REF': 'refs/heads/main',
       'GITHUB_EVENT_NAME': 'workflow_run', 'PLAY_RELEASE_MODE': 'closed',
       'SOURCE_SHA': SHA}
EVENT = {'workflow_run': {'name': 'Android CI', 'path': '.github/workflows/android-ci.yml',
                          'event': 'push', 'conclusion': 'success', 'head_branch': 'main',
                          'head_repository': {'full_name': 'hebi10/FocusLock'},
                          'head_sha': SHA}}


def track(name, code=None, status='completed'):
    releases = [] if code is None else [{'name': '0.3.0 (2)',
                                          'versionCodes': [str(code)], 'status': status}]
    return {'track': name, 'releases': releases}


class Server:
    def __init__(self, bundles=True, alpha=True, internal=True, alpha_code=None, stale_readback=False):
        self.tracks = [track('production'), track('beta')]
        if internal:
            self.tracks.append(track('internal', 2))
        if alpha:
            self.tracks.append(track('alpha', alpha_code))
        self.bundles = [{'versionCode': 2, 'sha256': 'a' * 64}] if bundles else []
        self.pending = None
        self.committed = False
        self.stale_readback = stale_readback
        self.calls = []
        self.edit_no = 0

    def __call__(self, method, path, body=None, content_type=None):
        self.calls.append((method, path, body))
        if method == 'POST' and path == '/edits':
            self.edit_no += 1
            return {'id': str(self.edit_no)}
        if method == 'DELETE' and path.startswith('/edits/'):
            return {}
        if method == 'GET' and path.endswith('/bundles'):
            return {'bundles': copy.deepcopy(self.bundles)}
        if method == 'GET' and path.endswith('/apks'):
            return {'apks': []}
        if method == 'GET' and path.endswith('/tracks'):
            return {'tracks': copy.deepcopy(self.tracks)}
        if method == 'PUT' and path.endswith('/tracks/alpha'):
            self.pending = copy.deepcopy(body)
            return copy.deepcopy(body)
        if method == 'POST' and path.endswith(':validate'):
            return {}
        if method == 'POST' and ':commit?' in path:
            self.committed = True
            if not self.stale_readback:
                self.tracks = [self.pending if t['track'] == 'alpha' else t for t in self.tracks]
            return {}
        raise AssertionError((method, path))


class ClosedPromotionTests(unittest.TestCase):
    def test_exact_marker_routes_to_closed_not_internal_or_icon(self):
        env = {'GITHUB_REPOSITORY': 'hebi10/FocusLock', 'GITHUB_EVENT_NAME': 'workflow_run'}
        event = EVENT
        self.assertEqual(source.release_mode(env, event, 'feat: approved [promote-play-closed]'), 'closed')
        self.assertEqual(source.release_mode(env, event, 'feat: unrelated'), 'deploy')
        self.assertEqual(source.release_mode(env, event, 'feat: [promote-play-closed]-suffix'), 'deploy')
        self.assertEqual(source.release_mode(env, event, 'feat: x[promote-play-closed]'), 'deploy')
        for extra in ('[initialize-play]', '[upload-play-icon]'):
            with self.assertRaisesRegex(source.SourceError, 'cannot be combined'):
                source.release_mode(env, event, 'feat: [promote-play-closed] ' + extra)
        for field, value in (('event', 'pull_request'), ('head_branch', 'feature'),
                             ('head_repository', {'full_name': 'fork/FocusLock'})):
            with self.assertRaises(source.SourceError):
                source.release_mode(env, {'workflow_run': {**EVENT['workflow_run'], field: value}},
                                    '[promote-play-closed]')

    def test_manual_dispatch_cannot_promote_even_with_commit_marker(self):
        env = {'GITHUB_REPOSITORY': 'hebi10/FocusLock', 'GITHUB_REF': 'refs/heads/main',
               'GITHUB_EVENT_NAME': 'workflow_dispatch', 'GITHUB_SHA': SHA,
               'PLAY_REQUESTED_MODE': 'closed'}
        with self.assertRaises(source.SourceError):
            source.release_mode(env, {}, '[promote-play-closed]')

    def test_runtime_identity_blocks_untrusted_context_without_api(self):
        for key, value in (('GITHUB_REPOSITORY', 'other/FocusLock'),
                           ('GITHUB_REF', 'refs/heads/feature'),
                           ('GITHUB_EVENT_NAME', 'workflow_dispatch'),
                           ('PLAY_RELEASE_MODE', 'deploy'),
                           ('SOURCE_SHA', 'invalid')):
            with self.subTest(key=key), self.assertRaises(publisher.ReleaseError):
                closed_promotion.validate_context({**ENV, key: value})

    def test_runtime_requires_event_commit_marker(self):
        with tempfile.TemporaryDirectory() as folder:
            event_file = pathlib.Path(folder) / 'event.json'
            event_file.write_text('{"workflow_run":{}}', encoding='utf-8')
            with mock.patch.object(source, 'resolve', return_value=SHA), \
                 mock.patch.object(source, 'release_mode', return_value='deploy'), \
                 mock.patch.object(closed_promotion.subprocess, 'run', return_value=mock.Mock(returncode=0, stdout='ordinary')):
                with self.assertRaisesRegex(publisher.ReleaseError, 'marker'):
                    closed_promotion.validate_context({**ENV, 'GITHUB_EVENT_PATH': str(event_file)})

    def test_inspect_is_read_only(self):
        server = Server()
        result = closed_promotion.inspect(server)
        self.assertEqual(result['status'], 'ready_for_closed_promotion')
        self.assertEqual(result['deployment'], 'not_attempted')
        self.assertFalse(any(method == 'PUT' or ':commit' in path for method, path, _ in server.calls))

    def test_promote_existing_code_without_new_bundle_or_other_track_changes(self):
        server = Server()
        other_before = copy.deepcopy([x for x in server.tracks if x['track'] != 'alpha'])
        result = closed_promotion.promote(server)
        self.assertEqual(result['status'], 'closed_api_committed_and_verified')
        self.assertEqual(result['versionCode'], 2)
        self.assertEqual(result['releaseStatus'], 'completed')
        self.assertFalse(result['newBundleUploaded'])
        self.assertEqual(result['testers'], 'requires_console_check')
        self.assertTrue(server.committed)
        self.assertEqual([x for x in server.tracks if x['track'] != 'alpha'], other_before)
        self.assertEqual(server.tracks[-1]['releases'][0]['versionCodes'], ['2'])
        self.assertEqual(server.edit_no, 2)
        self.assertEqual([path for method, path, _ in server.calls if method == 'PUT'],
                         ['/edits/1/tracks/alpha'])
        self.assertFalse(any('uploadType' in path for _, path, _ in server.calls))

    def test_already_completed_same_code_never_changes_release(self):
        server = Server(alpha_code=2)
        result = closed_promotion.promote(server)
        self.assertEqual(result['status'], 'already_on_closed')
        self.assertFalse(server.committed)
        self.assertFalse(any(method == 'PUT' for method, _, _ in server.calls))

    def test_existing_draft_same_code_may_be_completed(self):
        server = Server(alpha_code=2)
        server.tracks[-1]['releases'][0]['status'] = 'draft'
        self.assertEqual(closed_promotion.promote(server)['status'], 'closed_api_committed_and_verified')

    def test_missing_track_bundle_or_source_release_never_mutates(self):
        servers = (Server(bundles=False), Server(alpha=False), Server(internal=False),
                   Server(alpha_code=1))
        for server in servers:
            with self.subTest(tracks=server.tracks), self.assertRaises(publisher.ReleaseError):
                closed_promotion.promote(server)
            self.assertFalse(server.committed)
            self.assertFalse(any(method == 'PUT' for method, _, _ in server.calls))

    def test_commit_without_readback_is_failure_not_success(self):
        server = Server(stale_readback=True)
        with self.assertRaisesRegex(publisher.ReleaseError, 'readback failed') as caught:
            closed_promotion.promote(server)
        self.assertTrue(server.committed)
        self.assertTrue(getattr(caught.exception, 'committed', False))
        self.assertEqual(getattr(caught.exception, 'stage', ''), 'readback')


if __name__ == '__main__':
    unittest.main()
