#!/usr/bin/env python3
"""Owner-approved, fail-closed UI-R09 store images, signed internal AAB, then closed alpha."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time

REPO = "hebi10/FocusLock"
SHA_RE = re.compile(r"^[a-f0-9]{40}$")
ALLOWED = {"emulator-smoke.yml", "android-ci.yml", "store-01-images.yml", "play-internal.yml"}


class ReleaseSequenceError(RuntimeError):
    pass


def summary(value):
    print(value, flush=True)
    filename = os.environ.get("GITHUB_STEP_SUMMARY")
    if filename:
        with open(filename, "a", encoding="utf-8") as fd:
            fd.write(value + "\n")


def run(*args):
    result = subprocess.run(args, check=False, capture_output=True, text=True)
    if result.returncode:
        raise ReleaseSequenceError("Command failed (exit " + str(result.returncode) +
                                   "): " + " ".join(args[:3]))
    return result.stdout


def gh_api(path):
    try:
        return json.loads(run("gh", "api", path))
    except (ValueError, TypeError):
        raise ReleaseSequenceError("Invalid GitHub API JSON") from None


def main_sha():
    sha = gh_api("repos/" + REPO + "/git/ref/heads/main")["object"]["sha"]
    if not isinstance(sha, str) or not SHA_RE.fullmatch(sha):
        raise ReleaseSequenceError("Invalid GitHub main SHA")
    return sha


def assert_current(sha):
    if main_sha() != sha:
        raise ReleaseSequenceError("Main branch changed; stop before releasing stale source")


def allowed_owner(env, event):
    return (env.get("GITHUB_REPOSITORY") == REPO and
            env.get("GITHUB_EVENT_NAME") == "issue_comment" and
            event.get("issue", {}).get("number") == 14 and
            event.get("comment", {}).get("user", {}).get("login") == "hebi10" and
            event.get("comment", {}).get("body") == "/ui-r09-all")


def eligible_run(candidate, before, sha):
    return (isinstance(candidate, dict) and
            type(candidate.get("id")) is int and candidate["id"] not in before and
            candidate["id"] > 0 and candidate.get("head_sha") == sha and
            candidate.get("head_branch") == "main" and
            candidate.get("event") == "workflow_dispatch")


def workflow_runs(workflow):
    if workflow not in ALLOWED:
        raise ReleaseSequenceError("Unapproved workflow dispatch")
    data = gh_api("repos/" + REPO + "/actions/workflows/" + workflow +
                  "/runs?branch=main&event=workflow_dispatch&per_page=100")
    result = data.get("workflow_runs")
    if not isinstance(result, list):
        raise ReleaseSequenceError("GitHub workflow runs missing")
    return result


def launch(workflow, sha, inputs=None, timeout=3600):
    inputs = inputs or {}
    assert_current(sha)
    existing = {x["id"] for x in workflow_runs(workflow) if type(x.get("id")) is int}
    cmd = ["gh", "workflow", "run", workflow, "--ref", "main", "--repo", REPO]
    for name, value in inputs.items():
        cmd += ["-f", name + "=" + str(value)]
    run(*cmd)
    summary("Dispatched " + workflow + " for main " + sha[:12])
    deadline = time.monotonic() + timeout
    run_id = None
    while time.monotonic() < deadline:
        assert_current(sha)
        fresh = [x for x in workflow_runs(workflow) if eligible_run(x, existing, sha)]
        if fresh:
            run_id = max(fresh, key=lambda x: x["id"])["id"]
            break
        time.sleep(12)
    if run_id is None:
        raise ReleaseSequenceError("Cannot identify new workflow run: " + workflow)
    link = "https://github.com/" + REPO + "/actions/runs/" + str(run_id)
    summary("Target workflow: " + link)
    while time.monotonic() < deadline:
        if not (workflow == "store-01-images.yml" and inputs.get("mode") == "upload"):
            assert_current(sha)
        state = gh_api("repos/" + REPO + "/actions/runs/" + str(run_id))
        if state.get("head_sha") != sha or state.get("event") != "workflow_dispatch":
            raise ReleaseSequenceError("Unexpected workflow source: " + link)
        if state.get("status") == "completed":
            if state.get("conclusion") != "success":
                raise ReleaseSequenceError("Workflow failed or was cancelled: " + link)
            summary("Verified success: " + workflow + " #" + str(run_id))
            return run_id
        time.sleep(20)
    raise ReleaseSequenceError("Workflow exceeded time limit: " + link)


def artifact(run_id, name, output):
    run("gh", "run", "download", str(run_id), "--repo", REPO,
        "--name", name, "--dir", str(output))


def find_unique(root, filename):
    matches = list(root.rglob(filename))
    if len(matches) != 1:
        raise ReleaseSequenceError("Expected one report: " + filename)
    return matches[0]


def approved_images(run_id, capture_id, sha):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "store"))
    from render_images import validate
    with tempfile.TemporaryDirectory(prefix="focuslock-images-") as name:
        folder = Path(name)
        artifact(run_id, "focuslock-store-01-ko-KR", folder)
        record = validate(folder)
        if record.get("source_commit") != sha or record.get("source_workflow_run") != str(capture_id):
            raise ReleaseSequenceError("Store preview does not match verified exact-main capture")
        if record.get("package") != "com.haebi.focuslock" or len(record.get("screenshots", [])) != 8:
            raise ReleaseSequenceError("Expected eight screenshots for FocusLock")
        digest = hashlib.sha256((folder / "manifest.json").read_bytes()).hexdigest()
        summary("Reviewed manifest SHA256: " + digest)
        return digest


def store_readback(run_id):
    with tempfile.TemporaryDirectory(prefix="focuslock-image-report-") as name:
        folder = Path(name)
        artifact(run_id, "focuslock-store-01-play-result", folder)
        data = json.loads(find_unique(folder, "store-01-play-report.json").read_text(encoding="utf-8"))
    if (data.get("status") not in ("images_committed_and_verified", "images_already_present")
            or data.get("screenshots") != 8 or data.get("featureGraphic") != 1
            or "all release tracks" not in data.get("preserved", [])):
        raise ReleaseSequenceError("Fresh Google Play image readback did not verify expected images")
    summary("Play ko-KR image API readback: " + data["status"])


def play_readback(run_id, filename, track, expected=None):
    with tempfile.TemporaryDirectory(prefix="focuslock-track-") as name:
        folder = Path(name)
        report_artifact = ("focuslock-play-report-" if track == "internal"
                           else "focuslock-closed-promotion-") + str(run_id)
        artifact(run_id, report_artifact, folder)
        data = json.loads(find_unique(folder, filename).read_text(encoding="utf-8"))
    code = data.get("versionCode")
    allowed = {"internal": ("api_committed_and_verified",),
               "alpha": ("closed_api_committed_and_verified", "already_on_closed")}
    if (data.get("status") not in allowed[track]
            or data.get("track", data.get("targetTrack")) != track
            or type(code) is not int or code < 4
            or (expected is not None and code != expected)):
        raise ReleaseSequenceError("Play API track/version readback did not verify: " + track)
    summary("Play " + track + " verified: versionCode " + str(code))
    return code


def execute():
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
    if not allowed_owner(os.environ, event):
        raise ReleaseSequenceError("Only owner issue 14 /ui-r09-all is authorized")
    sha = main_sha()
    summary("# FocusLock UI-R09 sequenced release")
    summary("Initial current main: " + sha)

    capture = launch("emulator-smoke.yml", sha, timeout=3600)
    preview = launch("store-01-images.yml", sha, {"mode": "check"}, timeout=1800)
    digest = approved_images(preview, capture, sha)
    uploaded = launch("store-01-images.yml", sha,
                      {"mode": "upload", "approved_manifest_sha256": digest},
                      timeout=3600)
    store_readback(uploaded)

    # The store publisher is allowed to commit only signed-off images on main.
    sha = main_sha()
    summary("Main after image-only update: " + sha)
    launch("android-ci.yml", sha, timeout=3600)
    launch("emulator-smoke.yml", sha, timeout=3600)
    assert_current(sha)

    internal = launch("play-internal.yml", sha,
                      {"mode": "deploy", "release_status": "completed"}, timeout=5400)
    version_code = play_readback(internal, "play-release.json", "internal")
    assert_current(sha)
    closed = launch("play-internal.yml", sha,
                    {"mode": "closed", "expected_internal_version_code": version_code},
                    timeout=3600)
    play_readback(closed, "play-closed-promotion.json", "alpha", expected=version_code)
    summary("## Google Play internal and closed alpha API readback verified")
    summary("Console review/approval and actual tester installability remain separate checks.")


def main():
    try:
        execute()
        return 0
    except (ReleaseSequenceError, OSError, ValueError, KeyError) as exc:
        summary("BLOCKED: " + str(exc))
        summary("Stopped without starting any additional release stage.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
