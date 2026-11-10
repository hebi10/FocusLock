import hashlib
from pathlib import Path
import sys
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).parents[1]))
import store_icon
from publisher import PlayApiError, ReleaseError, REVIEW_HOLD_FORBIDDEN_REASON

ICON = Path(store_icon.ICON_PATH)
DIGEST = hashlib.sha256(ICON.read_bytes()).hexdigest()


class IconPlayServer:
    def __init__(self, icon=None, listing=True, wrong_upload_hash=False, bad_readback=False, reject_review_hold=False):
        self.icons = list(icon or [])
        self.listing = listing
        self.wrong_upload_hash = wrong_upload_hash
        self.bad_readback = bad_readback
        self.reject_review_hold = reject_review_hold
        self.pending = None
        self.calls = []
        self.edit_counter = 0
        self.committed = False

    def __call__(self, method, path, body=None, content_type=None):
        self.calls.append((method, path))
        if path == '/edits' and method == 'POST':
            self.edit_counter += 1
            return {'id': str(self.edit_counter)}
        if method == 'DELETE' and path.startswith('/edits/') and path.count('/') == 2:
            return {}
        if method == 'GET' and path.endswith('/listings/ko-KR'):
            if not self.listing:
                raise PlayApiError(method, 404)
            return {'language': 'ko-KR'}
        if method == 'GET' and path.endswith('/listings/ko-KR/icon'):
            icons = [] if self.bad_readback and self.committed else self.icons
            return {'images': icons}
        if method == 'DELETE' and path.endswith('/listings/ko-KR/icon'):
            self.pending = []
            return {}
        if method == 'POST' and path.endswith('/listings/ko-KR/icon?uploadType=media'):
            assert content_type == 'image/png'
            payload_hash = hashlib.sha256(body).hexdigest()
            accepted_hash = '0' * 64 if self.wrong_upload_hash else payload_hash
            self.pending = [{'sha256': payload_hash}]
            return {'image': {'sha256': accepted_hash}}
        if method == 'POST' and ':validate' in path:
            return {}
        if method == 'POST' and ':commit' in path:
            if self.reject_review_hold and 'changesNotSentForReview=true' in path:
                raise PlayApiError(method, 400, safe_reason=REVIEW_HOLD_FORBIDDEN_REASON)
            if self.pending is not None:
                self.icons = self.pending
            self.committed = True
            return {}
        raise AssertionError((method, path))


class IconTests(unittest.TestCase):
    def test_approved_icon_is_valid_512_png(self):
        self.assertEqual(hashlib.sha256(store_icon.validate_icon(ICON)).hexdigest(), DIGEST)

    def test_disallow_any_other_icon_file(self):
        with self.assertRaisesRegex(ReleaseError, 'approved FocusLock'):
            store_icon.validate_icon(Path('/tmp/icon.png'))

    def test_reject_untrusted_context(self):
        valid = {'GITHUB_REF': 'refs/heads/main', 'GITHUB_REPOSITORY': 'hebi10/FocusLock',
                 'PLAY_RELEASE_MODE': 'icon'}
        store_icon.validate_context(valid)
        for key, value in (('GITHUB_REF', 'refs/heads/dev'),
                           ('GITHUB_REPOSITORY', 'another/repository'),
                           ('PLAY_RELEASE_MODE', 'deploy')):
            with self.subTest(key=key), self.assertRaisesRegex(ReleaseError, 'explicit icon mode'):
                store_icon.validate_context({**valid, key: value})

    def test_same_icon_is_noop(self):
        server = IconPlayServer(icon=[{'sha256': DIGEST}])
        result = store_icon.update_icon(server, ICON)
        self.assertEqual(result['status'], 'icon_already_present')
        self.assertFalse(server.committed)
        self.assertFalse(any('/icon?uploadType=' in p for _, p in server.calls))

    def test_register_icon_and_verify_from_fresh_edit(self):
        server = IconPlayServer(icon=[{'sha256': 'a' * 64}])
        result = store_icon.update_icon(server, ICON)
        self.assertEqual(result['status'], 'icon_committed_and_verified')
        self.assertEqual(result['sha256'], DIGEST)
        self.assertEqual(server.edit_counter, 2)
        self.assertTrue(server.committed)
        self.assertEqual(server.icons[0]['sha256'], DIGEST)
        self.assertTrue(any(':commit?changesNotSentForReview=true&changesInReviewBehavior=ERROR_IF_IN_REVIEW' in p for _, p in server.calls))

    def test_review_hold_forbidden_retries_only_with_review_guard(self):
        server = IconPlayServer(reject_review_hold=True)
        result = store_icon.update_icon(server, ICON)
        self.assertEqual(result['status'], 'icon_committed_and_verified')
        self.assertTrue(result['reviewHoldParameterOmitted'])
        self.assertTrue(server.committed)
        self.assertTrue(any(':commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW' in p for _, p in server.calls))
        self.assertFalse(any(p.endswith(':commit') for _, p in server.calls))

    def test_rejected_upload_is_categorized_without_commit(self):
        class FailedUpload(IconPlayServer):
            def __call__(self, method, path, body=None, content_type=None):
                if method == 'POST' and '/icon?uploadType=media' in path:
                    raise PlayApiError(method, 400)
                return super().__call__(method, path, body, content_type)
        server = FailedUpload()
        with self.assertRaises(PlayApiError) as raised:
            store_icon.update_icon(server, ICON)
        self.assertEqual(getattr(raised.exception, 'stage', None), 'upload')
        self.assertFalse(server.committed)

    def test_missing_store_listing_never_uploads(self):
        server = IconPlayServer(listing=False)
        with self.assertRaisesRegex(ReleaseError, 'listing is missing'):
            store_icon.update_icon(server, ICON)
        self.assertFalse(any('/icon?uploadType=' in p for _, p in server.calls))

    def test_sha_mismatch_aborts_without_commit(self):
        server = IconPlayServer(wrong_upload_hash=True)
        with self.assertRaisesRegex(ReleaseError, 'did not match'):
            store_icon.update_icon(server, ICON)
        self.assertFalse(server.committed)

    def test_failed_fresh_readback_reports_uncertain_commit(self):
        server = IconPlayServer(bad_readback=True)
        with self.assertRaisesRegex(ReleaseError, 'inspect Console before retrying'):
            store_icon.update_icon(server, ICON)
        self.assertTrue(server.committed)


if __name__ == '__main__':
    unittest.main()
