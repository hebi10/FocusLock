#!/usr/bin/env python3
"""Safely promote the current completed internal AAB to closed alpha testing."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from publisher import HttpTransport, PACKAGE, Publisher, ReleaseError, PlayApiError
import source

TRACK = 'alpha'
SOURCE_TRACK = 'internal'
VERSION = '2'  # legacy test policy default; live target comes from verified internal


def validate_context(env):
    event_name = env.get('GITHUB_EVENT_NAME')
    manual = event_name == 'workflow_dispatch' and env.get('PLAY_REQUESTED_MODE') == 'closed'
    automatic = event_name == 'workflow_run'
    if (env.get('GITHUB_REPOSITORY') != 'hebi10/FocusLock'
            or env.get('GITHUB_REF') != 'refs/heads/main'
            or env.get('PLAY_RELEASE_MODE') != 'closed'
            or not (manual or automatic)):
        raise ReleaseError('Closed promotion requires explicit trusted main mode')
    if not re.fullmatch(r'[a-f0-9]{40}', env.get('SOURCE_SHA', '')):
        raise ReleaseError('Verified source SHA is missing')
    try:
        event = json.loads(Path(env['GITHUB_EVENT_PATH']).read_text(encoding='utf-8'))
        if source.resolve(env, event) != env['SOURCE_SHA']:
            raise ReleaseError('Verified source and promotion workflow do not match')
        subject = subprocess.run(['git', 'show', '-s', '--format=%s', env['SOURCE_SHA']],
                                 capture_output=True, check=False, text=True)
        if subject.returncode or source.release_mode(env, event, subject.stdout) != 'closed':
            raise ReleaseError('Manual closed input or one-time [promote-play-closed] marker is required')
    except (source.SourceError, OSError, ValueError, KeyError):
        raise ReleaseError('Closed promotion source metadata could not be verified') from None


def release_is(release, status, version=VERSION):
    return (isinstance(release, dict)
            and release.get('status') == status
            and [str(value) for value in (release.get('versionCodes') or [])] == [str(version)])


def alpha_release_policy(releases, version=VERSION):
    version = str(version)
    if not releases:
        return 'empty'
    if len(releases) != 1 or not isinstance(releases[0], dict):
        raise ReleaseError('Multiple existing closed track releases require Console review')
    release = releases[0]
    codes = [str(value) for value in (release.get('versionCodes') or [])]
    status = release.get('status')
    if codes == [version] and status == 'completed':
        return 'already'
    if codes == [version] and status == 'draft':
        return 'same_version_draft'
    if (len(codes) == 1 and codes[0].isascii() and codes[0].isdigit()
            and int(codes[0]) >= 1 and int(codes[0]) < int(version)
            and status in ('draft', 'completed')):
        return 'older_version'
    if not codes and status == 'draft':
        return 'empty_draft'
    safe_codes = ','.join(c for c in codes[:3] if c.isascii() and c.isdigit()) or 'none'
    safe_status = status if status in ('draft', 'completed', 'halted', 'inProgress') else 'unknown'
    raise ReleaseError('Closed track requires Console review; existing code=' + safe_codes + ', status=' + safe_status)


def snapshot(publisher, edit):
    bundles, apks, tracks = publisher.inventory(edit)
    if (not isinstance(bundles, list) or not isinstance(apks, list)
            or not isinstance(tracks, list)):
        raise ReleaseError('Play inventory returned an invalid structure')
    by_name = {}
    for track in tracks:
        if not isinstance(track, dict) or not isinstance(track.get('track'), str):
            raise ReleaseError('Play track inventory is malformed')
        name = track['track']
        if name in by_name:
            raise ReleaseError('Duplicate Play tracks prevent safe promotion')
        by_name[name] = track
    internal = by_name.get(SOURCE_TRACK)
    if not internal or not isinstance(internal.get('releases'), list):
        raise ReleaseError('Internal track is absent or malformed')
    active = internal['releases']
    if len(active) != 1 or not isinstance(active[0], dict) or active[0].get('status') != 'completed':
        raise ReleaseError('Exactly one completed internal release is required before closed promotion')
    codes = [str(value) for value in (active[0].get('versionCodes') or [])]
    if (len(codes) != 1 or not codes[0].isascii() or not codes[0].isdigit()
            or not 2 <= int(codes[0]) <= 2100000000):
        raise ReleaseError('Internal release must have one valid registered versionCode')
    code = codes[0]
    minimum = os.environ.get('MIN_CLOSED_VERSION_CODE', '2')
    if (not minimum.isascii() or not minimum.isdigit()
            or not 2 <= int(minimum) <= 2100000000):
        raise ReleaseError('Invalid minimum closed promotion version policy')
    if int(code) < int(minimum):
        raise ReleaseError('Latest completed internal release is older than the approved minimum version')
    bundle_codes = [str(item.get('versionCode')) for item in bundles if isinstance(item, dict)]
    if bundle_codes.count(code) != 1:
        raise ReleaseError('Current completed internal AAB is not registered exactly once')
    if TRACK not in by_name:
        raise ReleaseError('Closed testing alpha track is absent; set it up in Play Console first')
    releases = by_name[TRACK].get('releases', [])
    if not isinstance(releases, list):
        raise ReleaseError('Closed testing releases are malformed')
    alpha_release_policy(releases, code)
    other = {name: value for name, value in by_name.items() if name != TRACK}
    return releases, other, code


def inspect(transport):
    publisher = Publisher(transport)
    with publisher.initial_stage('inventory'), publisher.edit(ignore_delete_error=False) as edit:
        releases, _, code = snapshot(publisher, edit)
    ready = alpha_release_policy(releases, code) != 'already'
    return {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
            'versionCode': int(code),
            'status': 'ready_for_closed_promotion' if ready else 'already_on_closed',
            'deployment': 'not_attempted', 'testers': 'requires_console_check'}


def promote(transport):
    publisher = Publisher(transport)
    committed = False
    review_parameter_omitted = False
    with publisher.initial_stage('edit_create'), publisher.edit() as edit:
        with publisher.initial_stage('inventory'):
            releases, previous_tracks, code = snapshot(publisher, edit)
        if releases and release_is(releases[0], 'completed', code):
            return {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
                    'versionCode': int(code), 'releaseStatus': 'completed',
                    'status': 'already_on_closed', 'deployment': 'not_attempted',
                    'testers': 'requires_console_check'}
        with publisher.initial_stage('track_update'):
            publisher.request('PUT', edit + '/tracks/' + TRACK,
                              {'track': TRACK, 'releases': [
                                  {'name': f'0.3.0 ({code})', 'versionCodes': [code],
                                   'status': 'completed'}]})
        with publisher.initial_stage('commit'):
            response = publisher.commit(edit, fail_if_in_review=True)
            review_parameter_omitted = bool(response and response.get('reviewHoldParameterOmitted'))
        committed = True

    try:
        with publisher.initial_stage('readback'), publisher.edit(ignore_delete_error=False) as edit:
            releases, current_tracks, readback_code = snapshot(publisher, edit)
            if (readback_code != code or len(releases) != 1
                    or not release_is(releases[0], 'completed', code)
                    or current_tracks != previous_tracks):
                raise ReleaseError('Closed track or unaffected track readback mismatch')
    except (ReleaseError, KeyError, TypeError, ValueError) as error:
        failure = ReleaseError('Closed track commit may have succeeded but fresh readback failed; inspect Console before retrying')
        failure.stage = 'readback'
        failure.committed = committed
        if isinstance(error, PlayApiError):
            failure.http_status = error.http_status
            failure.api_status = error.api_status
        raise failure from None

    return {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
            'versionCode': int(code), 'releaseStatus': 'completed',
            'status': 'closed_api_committed_and_verified',
            'availability': 'requires_console_review_and_testers',
            'testers': 'requires_console_check', 'newBundleUploaded': False,
            'internalTrackPreserved': True,
            'reviewHoldParameterOmitted': review_parameter_omitted}



def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('command', choices=['inspect', 'promote'])
    args = parser.parse_args()
    report = {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
              'versionCode': None, 'status': 'closed_promotion_blocked',
              'availability': 'not_verified'}
    try:
        validate_context(os.environ)
        transport = HttpTransport(os.environ.get('PLAY_ACCESS_TOKEN', ''))
        report = inspect(transport) if args.command == 'inspect' else promote(transport)
        status = 0
    except (ReleaseError, OSError, ValueError, KeyError, TypeError) as error:
        report['stage'] = getattr(error, 'stage', 'preflight')
        if getattr(error, 'http_status', None) is not None:
            report['httpStatus'] = error.http_status
        if getattr(error, 'api_status', None):
            report['apiStatus'] = error.api_status
        if getattr(error, 'committed', False) or report['stage'] in ('commit', 'readback'):
            report['commitOutcome'] = 'unconfirmed'
            report['retry'] = 'inspect_console_before_retry'
        print('Closed promotion blocked: ' + str(error), file=sys.stderr)
        status = 1
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False))
    return status


if __name__ == '__main__':
    sys.exit(main())
