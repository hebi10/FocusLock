import contextlib
import io
import os
from pathlib import Path
import sys
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).parents[1]))
import preflight
import publisher
import source

SHA = 'a' * 40
ANDROID = '.github/workflows/android-ci.yml'
EMULATOR = '.github/workflows/emulator-smoke.yml'


def run(path, name, run_id=1, conclusion='success', status='completed'):
    return {'id': run_id, 'name': name, 'path': path, 'head_sha': SHA, 'head_branch': 'main',
            'event': 'push', 'conclusion': conclusion, 'status': status,
            'created_at': '2026-11-10T00:00:00Z', 'run_started_at': '2026-11-10T00:00:00Z', 'run_attempt': 1}


class FocusLockGuardTests(unittest.TestCase):
    def ci_fetch(self, emulator_runs):
        def fetch(path):
            if '/workflows/android-ci.yml/' in path:
                return {'workflow_runs': [run(ANDROID, 'Android CI')]}
            if '/workflows/emulator-smoke.yml/' in path:
                return {'workflow_runs': emulator_runs()}
            if '/runs/1/jobs' in path:
                return {'jobs': [{'name': 'verify', 'conclusion': 'success'}]}
            if '/runs/2/jobs' in path:
                return {'jobs': [{'name': 'emulator', 'conclusion': 'success'}]}
            self.fail(path)
        return fetch

    def test_android_success_waits_for_same_sha_emulator_and_only_then_returns_both_runs(self):
        clock = [0]
        def sleep(seconds):
            clock[0] += seconds
        def emulator():
            return [] if clock[0] == 0 else [run(EMULATOR, 'Android full-screen emulator QA', run_id=2)]
        with contextlib.redirect_stdout(io.StringIO()):
            result = source.wait_for_ci(SHA, self.ci_fetch(emulator), 60, sleep, lambda: clock[0])
        self.assertEqual(result, {ANDROID: 1, EMULATOR: 2})
        self.assertEqual(clock[0], 30)

    def test_manual_dispatch_missing_emulator_fails_without_wait_or_release(self):
        with self.assertRaisesRegex(source.SourceError, 'missing or incomplete'):
            source.wait_for_ci(SHA, self.ci_fetch(lambda: []), sleep=lambda _: self.fail('manual must not wait'))

    def test_failed_latest_emulator_does_not_wait_or_reuse_older_success(self):
        events = [run(EMULATOR, 'Android full-screen emulator QA', run_id=2),
                  run(EMULATOR, 'Android full-screen emulator QA', run_id=3, conclusion='failure')]
        def fetch(path):
            return {'jobs': [{'name': 'emulator', 'conclusion': 'success'}]} if '/runs/3/jobs' in path else self.ci_fetch(lambda: events)(path)
        with self.assertRaises(source.SourceError):
            source.wait_for_ci(SHA, fetch, 60, sleep=lambda _: self.fail('failed job must not wait'))

    def test_missing_emulator_times_out_without_returning_android_only(self):
        clock = [0]
        def sleep(seconds):
            clock[0] += seconds
        with contextlib.redirect_stdout(io.StringIO()), self.assertRaisesRegex(source.SourceError, 'missing or incomplete'):
            source.wait_for_ci(SHA, self.ci_fetch(lambda: []), 45, sleep, lambda: clock[0])
        self.assertEqual(clock[0], 45)

    def test_wif_pair_is_required_and_json_cannot_replace_it(self):
        self.assertEqual(preflight.auth_mode({'PLAY_WORKLOAD_IDENTITY_PROVIDER': 'provider', 'PLAY_SERVICE_ACCOUNT': 'account'}), 'wif')
        with self.assertRaises(preflight.PreflightError):
            preflight.auth_mode({'PLAY_SERVICE_ACCOUNT_JSON': 'fixture-never-use'})

    def test_internal_only_cli_rejects_promotion_before_any_api(self):
        with mock.patch('sys.argv', ['publisher.py', 'promote', '--version-code', '1']), mock.patch.object(publisher, 'HttpTransport') as transport, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as result:
                publisher.main()
            self.assertEqual(result.exception.code, 2)
            transport.assert_not_called()

    def test_only_android_ci_triggers_automatic_release_to_prevent_duplicate_uploads(self):
        event = run(EMULATOR, 'Android full-screen emulator QA')
        event['head_repository'] = {'full_name': 'hebi10/FocusLock'}
        env = {'GITHUB_REPOSITORY': 'hebi10/FocusLock', 'GITHUB_EVENT_NAME': 'workflow_run'}
        with self.assertRaises(source.SourceError):
            source.resolve(env, {'workflow_run': event})
        for key, value in [('name', 'Android CI'), ('event', 'pull_request'), ('head_repository', {'full_name': 'other/repo'})]:
            with self.assertRaises(source.SourceError):
                source.resolve(env, {'workflow_run': dict(event, **{key: value})})

    def test_emulator_requires_exact_successful_job_and_rejects_new_failed_jobs(self):
        event = run(EMULATOR, 'Android full-screen emulator QA')
        jobs = [{'name': 'emulator', 'conclusion': 'success', 'status': 'completed'}]
        source.verify_ci(SHA, event, jobs)
        for invalid in ([], [dict(jobs[0], conclusion='skipped')], jobs + [{'name': 'future-instrumentation', 'conclusion': 'failure'}]):
            with self.assertRaises(source.SourceError):
                source.verify_ci(SHA, event, invalid)
        with self.assertRaises(source.SourceError):
            source.verify_ci('b' * 40, event, jobs)

    def test_latest_attempt_must_succeed_instead_of_reusing_older_success(self):
        runs = [run(ANDROID, 'Android CI'), run(ANDROID, 'Android CI', run_id=2, conclusion='failure')]
        latest = source.latest_ci_run(SHA, ANDROID, runs)
        self.assertEqual(latest['id'], 2)
        with self.assertRaises(source.SourceError):
            source.verify_ci(SHA, latest, [{'name': 'verify', 'conclusion': 'success'}])
        with self.assertRaises(source.SourceError):
            source.latest_ci_run(SHA, EMULATOR, runs)

    def test_incomplete_run_cannot_release_even_with_success_conclusion(self):
        event = run(ANDROID, 'Android CI', status='in_progress')
        with self.assertRaises(source.SourceError):
            source.verify_ci(SHA, event, [{'name': 'verify', 'conclusion': 'success'}])


if __name__ == '__main__':
    unittest.main()

class FinalCiFreshnessTests(unittest.TestCase):
    def test_queued_rerun_newer_update_revokes_an_earlier_success(self):
        queued = dict(run(ANDROID, 'Android CI', run_id=100, status='queued', conclusion=None),
                      run_attempt=2, run_started_at='2026-11-10T10:00:00Z', updated_at='2026-11-10T12:00:00Z')
        success = dict(run(ANDROID, 'Android CI', run_id=101), run_started_at='2026-11-10T11:00:00Z')
        self.assertEqual(source.latest_ci_run(SHA, ANDROID, [queued, success])['id'], 100)

    def test_attempt_clock_must_be_valid_and_only_first_attempt_can_fallback_to_creation(self):
        first = run(ANDROID, 'Android CI')
        first.pop('run_started_at')
        self.assertEqual(source.latest_ci_run(SHA, ANDROID, [first])['id'], 1)
        for event in (dict(first, run_attempt=2), dict(first, run_started_at='bad'),
                      dict(first, run_started_at='2026-11-10T12:00:00')):
            with self.subTest(event=event), self.assertRaises(source.SourceError):
                source.latest_ci_run(SHA, ANDROID, [event])

    def test_preupload_assert_current_rechecks_latest_ci_and_blocks_failure_or_running_attempt(self):
        for status, conclusion in (('completed', 'failure'), ('in_progress', None)):
            seen = []
            def fetch(path):
                seen.append(path)
                if '/git/ref/' in path:
                    return {'object': {'sha': SHA}}
                event = run(ANDROID, 'Android CI', conclusion=conclusion, status=status)
                return {'workflow_runs': [event]} if '/workflows/' in path else {'jobs': [{'name': 'verify', 'conclusion': 'success'}]}
            with self.subTest(status=status), mock.patch.object(source, 'gh', side_effect=fetch), mock.patch('sys.argv', ['source.py', '--assert-current', SHA]), contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(source.main(), 1)
                self.assertTrue(any('/workflows/android-ci.yml/runs' in path for path in seen))

    def test_lower_run_id_latest_rerun_is_selected_over_earlier_new_run_success(self):
        later_rerun = dict(run(ANDROID, 'Android CI', run_id=100, conclusion='failure'),
                           run_attempt=2, run_started_at='2026-11-10T12:00:00Z')
        earlier_new_run = dict(run(ANDROID, 'Android CI', run_id=101),
                               run_attempt=1, run_started_at='2026-11-10T11:00:00Z')
        for status, conclusion in (('completed', 'failure'), ('in_progress', None)):
            event = dict(later_rerun, status=status, conclusion=conclusion)
            with self.subTest(status=status):
                self.assertEqual(source.latest_ci_run(SHA, ANDROID, [event, earlier_new_run])['id'], 100)

    def test_assert_current_success_requires_both_ci_and_checks_main_again_after_ci(self):
        seen = []
        def fetch(path):
            seen.append(path)
            if '/git/ref/' in path:
                return {'object': {'sha': SHA}}
            return FocusLockGuardTests().ci_fetch(lambda: [run(EMULATOR, 'Android full-screen emulator QA', run_id=2)])(path)
        with mock.patch.object(source, 'gh', side_effect=fetch), mock.patch('sys.argv', ['source.py', '--assert-current', SHA]), contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(source.main(), 0)
        self.assertTrue(any('/workflows/emulator-smoke.yml/runs' in path for path in seen))
        self.assertIn('/git/ref/heads/main', seen[-1])
        self.assertEqual(sum('/git/ref/heads/main' in path for path in seen), 2)
