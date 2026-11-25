"""Verify the extra real Android captures without modifying UI-R00's frozen baseline."""
import argparse
import hashlib
import json
import os
from pathlib import Path

from ui_r00_verify import png_dimensions

REQUIRED = (
    "qa-flfix08-captures/goal-manager.png",
    "qa-flfix08-captures/goal-history.png",
    "qa-flfix08-captures/local-data.png",
    "qa-flfix08-captures/goal-history-delete-confirm.png",
    "qa-flfix08-captures/focus-history-delete-confirm.png",
    "qa-r08/api36-phone-report-30-axis.png",
    "qa-r08/api36-phone-report-day-selection.png",
)


def verify(root):
    root = Path(root)
    captures = {}
    for relative in REQUIRED:
        path = root / relative
        if not path.is_file():
            raise ValueError(f"Missing FL-FIX-08 screenshot: {relative}")
        data = path.read_bytes()
        png_dimensions(data)
        captures[relative] = hashlib.sha256(data).hexdigest()
    return {
        "task": "FL-FIX-08",
        "status": "additional_emulator_evidence_verified",
        "tested_commit": os.environ.get("GITHUB_SHA", "local-run-unknown"),
        "workflow_run": os.environ.get("GITHUB_RUN_ID", "local-run-unknown"),
        "screenshot_count": len(captures),
        "screenshot_sha256": captures,
        "verification_boundary": "Actual emulator capture inventory, dimensions and hashes only; not a claim of human visual review, physical-device QA, or completed Play release.",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--capture-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    result = verify(args.capture_root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Verified", result["screenshot_count"], "additional Android screen files.")


if __name__ == "__main__":
    main()
