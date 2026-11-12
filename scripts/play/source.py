#!/usr/bin/env python3
"""Resolve an immutable main source and require its completed verification run."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

REPO = 'hebi10/FocusLock'
WORKFLOW = '.github/workflows/android-ci.yml'
REQUIRED_WORKFLOWS = {
    WORKFLOW: ('Android CI', {'verify'}),
    '.github/workflows/emulator-smoke.yml': ('Android full-screen emulator QA', {'emulator'}),
}


class SourceError(RuntimeError):
    pass


def resolve(env, event):
    if env.get('GITHUB_REPOSITORY') != REPO:
        raise SourceError('Release workflow is restricted to ' + REPO)
    if env.get('GITHUB_EVENT_NAME') == 'workflow_dispatch':
        if env.get('GITHUB_REF') != 'refs/heads/main':
            raise SourceError('Manual Play workflows must run from main')
        sha = env.get('GITHUB_SHA', '')
    elif env.get('GITHUB_EVENT_NAME') == 'workflow_run':
        run = event.get('workflow_run', {})
        if (run.get('name') != 'Android CI' or run.get('path') != WORKFLOW or run.get('event') != 'push'
                or run.get('conclusion') != 'success' or run.get('head_branch') != 'main'
                or run.get('head_repository', {}).get('full_name') != REPO):
            raise SourceError('Automatic deploy requires a successful same-repository main push verification')
        sha = run.get('head_sha', '')
    else:
        raise SourceError('Unsupported release event')
    if not re.fullmatch(r'[a-f0-9]{40}', sha):
        raise SourceError('Release source must be a complete immutable commit SHA')
    return sha


def require_current_main(sha, current_main):
    if not re.fullmatch(r'[a-f0-9]{40}', sha) or sha != current_main:
        raise SourceError('Release source is stale; rerun from the current verified main commit')


def release_mode(env, event, commit_subject):
    resolve(env, event)
    if env.get('GITHUB_EVENT_NAME') == 'workflow_dispatch':
        mode = env.get('PLAY_REQUESTED_MODE', 'check')
        if mode not in ('check', 'icon', 'build', 'deploy', 'initialize', 'closed'):
            raise SourceError('Unsupported Play release mode')
        return mode
    subject = commit_subject.splitlines()[0] if commit_subject else ''
    initialize = bool(re.search(r'(?<!\S)\[initialize-play\](?!\S)', subject))
    icon = bool(re.search(r'(?<!\S)\[upload-play-icon\](?!\S)', subject))
    closed = bool(re.search(r'(?<!\S)\[promote-play-closed\](?!\S)', subject))
    if sum((initialize, icon, closed)) > 1:
        raise SourceError('Play action markers cannot be combined')
    if closed:
        return 'closed'
    if icon:
        return 'icon'
    return 'initialize' if initialize else 'deploy'


def verify_ci(sha, run, jobs):
    configuration = REQUIRED_WORKFLOWS.get(run.get('path'))
    if (not configuration or run.get('head_sha') != sha or run.get('head_branch') != 'main'
            or run.get('conclusion') != 'success' or run.get('status') != 'completed'
            or run.get('event') not in ('push', 'workflow_dispatch')):
        raise SourceError('Required main verification has not succeeded for this exact source SHA')
    successful = {job.get('name') for job in jobs if job.get('conclusion') == 'success'}
    if not configuration[1].issubset(successful) or any(job.get('conclusion') != 'success' for job in jobs):
        raise SourceError('All required Android and emulator jobs must succeed without skipped or failed jobs')


def latest_ci_run(sha, workflow, runs):
    candidates = [run for run in runs if run.get('head_sha') == sha and run.get('head_branch') == 'main'
                  and run.get('path') == workflow and run.get('event') in ('push', 'workflow_dispatch')]
    if not candidates:
        raise SourceError('Required main verification is missing for the exact source SHA: ' + workflow)
    def order(run):
        attempt = run.get('run_attempt', 1)
        if isinstance(attempt, bool) or not isinstance(attempt, int) or attempt < 1:
            raise SourceError('Invalid CI attempt metadata')
        timestamp = run.get('run_started_at') or (run.get('created_at') if attempt == 1 else None)
        def parse(value):
            try:
                result = datetime.fromisoformat(value.replace('Z', '+00:00'))
                if result.tzinfo is None:
                    raise ValueError()
                return result
            except (AttributeError, TypeError, ValueError):
                raise SourceError('CI attempt start time is missing or invalid') from None
        started = parse(timestamp)
        # A queued rerun may not yet have a fresh start time. Its newer update
        # must revoke an older success until the attempt has completed.
        if run.get('status') != 'completed' and run.get('updated_at'):
            started = max(started, parse(run['updated_at']))
        return started, run['id'], attempt
    return max(candidates, key=order)


def wait_for_ci(sha, fetch, wait_seconds=0, sleep=time.sleep, monotonic=time.monotonic):
    deadline = monotonic() + wait_seconds
    while True:
        verified, pending = {}, []
        for workflow in REQUIRED_WORKFLOWS:
            runs = fetch(f'repos/{REPO}/actions/workflows/{Path(workflow).name}/runs?head_sha={sha}&branch=main&per_page=100')['workflow_runs']
            try:
                run = latest_ci_run(sha, workflow, runs)
            except SourceError:
                pending.append(workflow)
                continue
            if run.get('status') != 'completed':
                pending.append(workflow)
                continue
            jobs = fetch(f'repos/{REPO}/actions/runs/{run["id"]}/jobs?per_page=100')['jobs']
            verify_ci(sha, run, jobs)
            verified[workflow] = run['id']
        if not pending:
            return verified
        if monotonic() >= deadline:
            raise SourceError('Exact-source CI is missing or incomplete: ' + ', '.join(pending))
        print('Waiting for exact-source Android and emulator CI', flush=True)
        sleep(min(30, max(0, deadline - monotonic())))


def gh(path):
    result = subprocess.run(['gh', 'api', path], capture_output=True, check=False, text=True)
    if result.returncode:
        raise SourceError('Could not read required GitHub verification metadata')
    try:
        return json.loads(result.stdout)
    except ValueError:
        raise SourceError('Invalid GitHub verification metadata') from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--assert-current', metavar='SHA')
    args = parser.parse_args()
    try:
        if args.assert_current:
            current = gh(f'repos/{REPO}/git/ref/heads/main')['object']['sha']
            require_current_main(args.assert_current, current)
            wait_for_ci(args.assert_current, gh)
            require_current_main(args.assert_current, gh(f'repos/{REPO}/git/ref/heads/main')['object']['sha'])
            print('Release source and latest Android/emulator CI still match verified current main')
            return 0
        event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text(encoding='utf-8'))
        sha = resolve(os.environ, event)
        current = gh(f'repos/{REPO}/git/ref/heads/main')['object']['sha']
        require_current_main(sha, current)
        ancestry = subprocess.run(['git', 'merge-base', '--is-ancestor', sha, 'origin/main'], capture_output=True, check=False)
        if ancestry.returncode:
            raise SourceError('Release source is not an ancestor of the trusted main branch')
        wait_seconds = int(os.environ.get('PLAY_CI_WAIT_SECONDS', '0'))
        if not 0 <= wait_seconds <= 3000:
            raise SourceError('CI wait must be between 0 and 3000 seconds')
        verified = wait_for_ci(sha, gh, wait_seconds)
        require_current_main(sha, gh(f'repos/{REPO}/git/ref/heads/main')['object']['sha'])
        subject = subprocess.run(['git', 'show', '-s', '--format=%s', sha], capture_output=True, check=False, text=True)
        if subject.returncode:
            raise SourceError('Could not inspect the trusted release commit subject')
        mode = release_mode(os.environ, event, subject.stdout)
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as output:
            output.write(f'sha={sha}\nverify_run_id={verified[WORKFLOW]}\nrelease_mode={mode}\n')
        print(f'Trusted main source {sha}; required Android and emulator runs passed: {verified}')
        return 0
    except (SourceError, KeyError, OSError, ValueError) as error:
        message = str(error) if isinstance(error, SourceError) else 'Source metadata is missing or invalid'
        print('Release source check failed: ' + message, file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
