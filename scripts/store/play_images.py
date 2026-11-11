#!/usr/bin/env python3
"""Replace only FocusLock ko-KR phone screenshots and feature graphic in Google Play."""
import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "play"))
from publisher import HttpTransport, Publisher, ReleaseError, PACKAGE
from render_images import validate

LOCALE = "ko-KR"
TYPES = ("phoneScreenshots", "featureGraphic")

def digest(data):
    return hashlib.sha256(data).hexdigest()

def expected_image_files(folder):
    manifest = validate(folder)
    files = [(item["file"], item["sha256"]) for item in manifest["screenshots"]]
    graphic = manifest["feature_graphic"]
    return {"phoneScreenshots": files, "featureGraphic": [(graphic["file"], graphic["sha256"])]}

def matches(images, expected):
    return isinstance(images, list) and [str(i.get("sha256", "")).lower() for i in images] == [hash_ for _, hash_ in expected]

def require_context(env):
    if (env.get("GITHUB_REPOSITORY") != "hebi10/FocusLock" or
            env.get("GITHUB_REF") != "refs/heads/main" or
            env.get("PLAY_RELEASE_MODE") != "store-images"):
        raise ReleaseError("Only explicit FocusLock main store-images workflow may write to Play")

def update_only_images(folder, request):
    expected = expected_image_files(folder)
    publisher = Publisher(request)
    changed = False
    with publisher.edit(ignore_delete_error=False) as edit:
        listing = edit + "/listings/" + LOCALE
        record = request("GET", listing)
        if not isinstance(record, dict) or record.get("language") != LOCALE:
            raise ReleaseError("Existing ko-KR store listing was not verified")
        tracks_before = request("GET", edit + "/tracks").get("tracks", [])
        original_icon = request("GET", listing + "/icon").get("images", [])
        current = {kind: request("GET", listing + "/" + kind).get("images", []) for kind in TYPES}
        if not all(matches(current[kind], expected[kind]) for kind in TYPES):
            for kind in TYPES:
                if matches(current[kind], expected[kind]):
                    continue
                changed = True
                endpoint = listing + "/" + kind
                request("DELETE", endpoint)
                for filename, expected_hash in expected[kind]:
                    data = (folder / filename).read_bytes()
                    if digest(data) != expected_hash:
                        raise ReleaseError("Manifest checksum changed before upload")
                    uploaded = request("POST", endpoint + "?uploadType=media", data, "image/png")
                    actual_hash = uploaded.get("image", {}).get("sha256", "").lower()
                    if actual_hash != expected_hash:
                        raise ReleaseError("Play upload checksum did not match; edit not committed")
            publisher.commit(edit, fail_if_in_review=True)
    # Fresh edit, not the previously staged transaction.
    with publisher.edit(ignore_delete_error=False) as edit:
        listing = edit + "/listings/" + LOCALE
        readback = {kind: request("GET", listing + "/" + kind).get("images", []) for kind in TYPES}
        tracks_after = request("GET", edit + "/tracks").get("tracks", [])
        icon_after = request("GET", listing + "/icon").get("images", [])
    if any(not matches(readback[kind], expected[kind]) for kind in TYPES):
        raise ReleaseError("Play image edit committed but fresh hash/order readback differs; inspect Console before retry")
    if tracks_before != tracks_after or original_icon != icon_after:
        raise ReleaseError("Unexpected concurrent icon or track change; inspect Console before retry")
    return {
        "package": PACKAGE, "locale": LOCALE, "status": "images_committed_and_verified" if changed else "images_already_present",
        "screenshots": len(expected["phoneScreenshots"]), "featureGraphic": 1,
        "preserved": ["icon", "all release tracks", "other localizations", "AAB/APK"],
        "availability": "requires_console_review_and_visibility_check",
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", type=Path, default=Path("assets/play-store/ko-KR"))
    parser.add_argument("--report", type=Path, default=Path("output/store-01-play-report.json"))
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    report = {"package": PACKAGE, "locale": LOCALE, "status": "not_submitted"}
    try:
        expected = expected_image_files(args.dir)
        if args.apply:
            require_context(os.environ)
            report = update_only_images(args.dir, HttpTransport(os.environ.get("PLAY_ACCESS_TOKEN", "")))
        else:
            report.update({"status": "local_validation_passed", "screenshots": len(expected["phoneScreenshots"])})
        code = 0
    except (ReleaseError, OSError, ValueError, KeyError, TypeError) as exc:
        report["status"] = "blocked_or_not_verified"
        report["stage"] = str(getattr(exc, "stage", "unknown"))
        if getattr(exc, "http_status", None):
            report["httpStatus"] = exc.http_status
        if getattr(exc, "api_status", None):
            report["apiStatus"] = exc.api_status
        print("STORE-01 Google Play update blocked: " + str(exc), file=sys.stderr)
        code = 1
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))
    return code

if __name__ == "__main__":
    sys.exit(main())
