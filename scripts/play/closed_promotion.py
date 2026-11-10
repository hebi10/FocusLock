#!/usr/bin/env python3
"""Promote exactly the existing FocusLock internal version 2 to closed testing."""
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
VERSION = '2'


def validate_context(env):
    if (env.get('GITHUB_REPOSITORY') != 'hebi10/FocusLock'
            or env.get('GITHUB_REF') != 'refs/heads/main'
            or env.get('GITHUB_EVENT_NAME') != 'workflow_run'
            or env.get('PLAY_RELEASE_MODE') != 'closed'):
        raise ReleaseError('Closed promotion requires the explicit trusted main workflow-run mode')
    if not re.fullmatch(r'[a-f0-9]{40}', env.get('SOURCE_SHA', '')):
        raise ReleaseError('Verified source SHA is missing')
    try:
        event = json.loads(Path(env['GITHUB_EVENT_PATH']).read_text(encoding='utf-8'))
        if source.resolve(env, event) != env['SOURCE_SHA']:
            raise ReleaseError('Verified source and promotion workflow do not match')
        subject = subprocess.run(['git', 'show', '-s', '--format=%s', env['SOURCE_SHA']],
                                 capture_output=True, check=False, text=True)
        if subject.returncode or source.release_mode(env, event, subject.stdout) != 'closed':
            raise ReleaseError('One-time [promote-play-closed] main commit marker is required')
    except (source.SourceError, OSError, ValueError, KeyError) as error:
        if isinstance(error, ReleaseError):
            raise
        raise ReleaseError('Closed promotion source metadata could not be verified') from None


def release_is(release, status):
    return (isinstance(release, dict)
            and release.get('status') == status
            and [str(value) for value in release.get('versionCodes', [])] == [VERSION])


def snapshot(publisher, edit):
    bundles, apks, tracks = publisher.inventory(edit)
    if (not isinstance(bundles, list) or not isinstance(apks, list)
            or not isinstance(tracks, list)):
        raise ReleaseError('Play inventory returned an invalid structure')
    codes = [str(item.get('versionCode')) for item in bundles if isinstance(item, dict)]
    if codes.count(VERSION) != 1:
        raise ReleaseError('Verified internal release AAB versionCode 2 is not registered exactly once')
    by_name = {}
    for track in tracks:
        if not isinstance(track, dict) or not isinstance(track.get('track'), str):
            raise ReleaseError('Play track inventory is malformed')
        name = track['track']
        if name in by_name:
            raise ReleaseError('Duplicate Play tracks prevent safe promotion')
        by_name[name] = track
    internal = by_name.get(SOURCE_TRACK)
    if (not internal or not isinstance(internal.get('releases'), list)
            or not any(release_is(release, 'completed') for release in internal['releases'])):
        raise ReleaseError('Internal track does not contain completed versionCode 2')
    if TRACK not in by_name:
        raise ReleaseError('Closed testing alpha track is absent; set it up in Play Console first')
    closed = by_name[TRACK]
    releases = closed.get('releases', [])
    if not isinstance(releases, list):
        raise ReleaseError('Closed testing releases are malformed')
    if releases and (len(releases) != 1 or
                     not (release_is(releases[0], 'completed') or release_is(releases[0], 'draft'))):
        raise ReleaseError('Closed track already has another release; refusing to replace it')
    other = {name: value for name, value in by_name.items() if name != TRACK}
    return releases, other


def inspect(transport):
    publisher = Publisher(transport)
    with publisher.initial_stage('inventory'), publisher.edit(ignore_delete_error=False) as edit:
        releases, _ = snapshot(publisher, edit)
    ready = releases == [] or release_is(releases[0], 'draft')
    return {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
            'versionCode': int(VERSION),
            'status': 'ready_for_closed_promotion' if ready else 'already_on_closed',
            'deployment': 'not_attempted', 'testers': 'requires_console_check'}


def promote(transport):
    publisher = Publisher(transport)
    committed = False
    review_parameter_omitted = False
    with publisher.initial_stage('edit_create'), publisher.edit() as edit:
        with publisher.initial_stage('inventory'):
            releases, previous_tracks = snapshot(publisher, edit)
        if releases and release_is(releases[0], 'completed'):
            return {'package': PACKAGE, 'sourceTrack': SOURCE_TRACK, 'targetTrack': TRACK,
                    'versionCode': int(VERSION), 'releaseStatus': 'completed',
                    'status': 'already_on_closed', 'deployment': 'not_attempted',
                    'testers': 'requires_console_check'}
        with publisher.initial_stage('track_update'):
            publisher.request('PUT', edit + '/tracks/' + TRACK,
                              {'track': TRACK, 'releases': [
                                  {'name': '0.3.0 (2)', 'versionCodes': [VERSION],
                                   'status': 'completed'}]})
        with publisher.initial_stage('commit'):
            response = publisher.commit(edit, fail_if_in_review=True)
            review_parameter_omitted = bool(response and response.get('reviewHoldParameterOmitted'))
        committed = True

    try:
        with publisher.initial_stage('readback'), publisher.edit(ignore_delete_error=False) as edit:
            releases, current_tracks = snapshot(publisher, edit)
            if (len(releases) != 1 or not release_is(releases[0], 'completed')
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
            'versionCode': int(VERSION), 'releaseStatus': 'completed',
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
              'versionCode': int(VERSION), 'status': 'closed_promotion_blocked',
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
