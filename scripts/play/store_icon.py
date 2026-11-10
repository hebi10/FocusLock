#!/usr/bin/env python3
"""Play ko-KR icon updater. Uses the existing Google Play edit transport and no new credentials."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys

from publisher import HttpTransport, PlayApiError, Publisher, ReleaseError, PACKAGE

PNG_SIGNATURE = b'\x89PNG\r\n\x1a\n'
ICON_PATH = 'assets/store/FocusLock_Play_Icon_512.png'
ICON_LISTING = 'ko-KR'
ICON_TYPE = 'icon'
MAX_ICON_BYTES = 1024 * 1024


def validate_icon(path):
    if path.as_posix() != ICON_PATH:
        raise ReleaseError('Only the approved FocusLock store icon path is supported')
    data = path.read_bytes()
    if (len(data) < 33 or len(data) > MAX_ICON_BYTES or
            not data.startswith(PNG_SIGNATURE) or data[12:16] != b'IHDR' or
            int.from_bytes(data[16:20], 'big') != 512 or
            int.from_bytes(data[20:24], 'big') != 512):
        raise ReleaseError('FocusLock icon must be a real 512x512 PNG of at most 1 MB')
    return data


def validate_context(env):
    if (env.get('GITHUB_REF') != 'refs/heads/main' or
            env.get('GITHUB_REPOSITORY') != 'hebi10/FocusLock' or
            env.get('PLAY_RELEASE_MODE') != 'icon'):
        raise ReleaseError('Store icon changes require an explicit icon mode on trusted FocusLock main')


def _matching_icon(images, sha256):
    return any(isinstance(image, dict) and
               isinstance(image.get('sha256'), str) and
               image['sha256'].lower() == sha256 for image in images)


def update_icon(request, filename):
    data = validate_icon(Path(filename))
    sha256 = hashlib.sha256(data).hexdigest()
    publisher = Publisher(request)
    updated = False

    with publisher.initial_stage('edit_create'), publisher.edit() as edit:
        listing = edit + f'/listings/{ICON_LISTING}'
        path = listing + '/' + ICON_TYPE
        try:
            with publisher.initial_stage('listing'):
                registered = request('GET', listing)
        except PlayApiError as exc:
            if exc.http_status == 404:
                raise ReleaseError('Korean Play store listing is missing; create it in Console before uploading the icon') from None
            raise
        if not isinstance(registered, dict) or not registered.get('language'):
            raise ReleaseError('Korean store listing was not confirmed; icon replacement cancelled')
        with publisher.initial_stage('inventory'):
            current = request('GET', path).get('images', [])
        if not isinstance(current, list):
            raise ReleaseError('Existing icon inventory is malformed')
        if len(current) == 1 and _matching_icon(current, sha256):
            return {'package': PACKAGE, 'language': ICON_LISTING,
                    'status': 'icon_already_present', 'sha256': sha256,
                    'availability': 'requires_console_check'}

        with publisher.initial_stage('upload'):
            request('DELETE', path)
            uploaded = request('POST', path + '?uploadType=media', data, 'image/png')
        image = uploaded.get('image', {}) if isinstance(uploaded, dict) else {}
        if (not isinstance(image, dict) or
                not isinstance(image.get('sha256'), str) or
                image['sha256'].lower() != sha256):
            raise ReleaseError('Uploaded Play icon SHA-256 did not match approved image; edit not committed')
        # Google Play may reject changesNotSentForReview even for an icon-only edit.
        # Match the verified initial draft's safe commit policy: never alter an in-review edit.
        with publisher.initial_stage('commit'):
            commit_result = publisher.commit(edit, fail_if_in_review=True)
        updated = True

    if updated:
        try:
            with publisher.initial_stage('readback'), publisher.edit() as edit:
                images = request('GET', edit + f'/listings/{ICON_LISTING}/{ICON_TYPE}').get('images', [])
            if len(images) != 1 or not _matching_icon(images, sha256):
                raise ReleaseError('Icon readback does not match approved image')
        except (ReleaseError, KeyError, TypeError, ValueError):
            raise ReleaseError('Play icon edit was committed but fresh readback failed; inspect Console before retrying') from None

    return {'package': PACKAGE, 'language': ICON_LISTING,
            'status': 'icon_committed_and_verified', 'sha256': sha256,
            'availability': 'requires_console_check',
            'reviewHoldParameterOmitted': bool(commit_result and commit_result.get('reviewHoldParameterOmitted'))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--file', default=ICON_PATH)
    args = parser.parse_args()
    result = {'package': PACKAGE, 'language': ICON_LISTING,
              'status': 'icon_upload_blocked', 'availability': 'not_verified'}
    try:
        validate_context(os.environ)
        result = update_icon(HttpTransport(os.environ.get('PLAY_ACCESS_TOKEN', '')), args.file)
        code = 0
    except (ReleaseError, OSError, ValueError, KeyError) as exc:
        # Keep error context coarse, never disclose a Play response body, credentials or secrets.
        if isinstance(exc, ReleaseError):
            result['stage'] = getattr(exc, 'stage', 'unknown')
            if getattr(exc, 'http_status', None) is not None:
                result['httpStatus'] = exc.http_status
            if getattr(exc, 'api_status', None):
                result['apiStatus'] = exc.api_status
        print('Play icon update blocked: ' + str(exc), file=sys.stderr)
        code = 1
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False))
    return code


if __name__ == '__main__':
    sys.exit(main())
