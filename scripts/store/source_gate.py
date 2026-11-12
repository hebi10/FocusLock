#!/usr/bin/env python3
"""Accept screenshot artifacts only from a successful emulator run of the exact main commit."""
import argparse
import json
import re
from pathlib import Path

SHA = re.compile(r"^[a-f0-9]{40}$")


def select_capture(runs, main_sha):
    if not isinstance(main_sha, str) or not SHA.fullmatch(main_sha):
        raise ValueError("Invalid current main SHA")
    for run in runs:
        if (
            isinstance(run, dict)
            and run.get("head_branch") == "main"
            and run.get("event") in ("push", "workflow_dispatch")
            and run.get("status") == "completed"
            and run.get("conclusion") == "success"
            and run.get("head_sha") == main_sha
            and type(run.get("id")) is int
            and run["id"] > 0
        ):
            return run["id"], main_sha
    raise ValueError(
        "UI-R09 blocked: current main has no successful matching emulator QA; "
        "do not use stale or pull-request screenshots"
    )


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--runs", type=Path, required=True)
    p.add_argument("--main-ref", type=Path, required=True)
    args = p.parse_args()
    records = json.loads(args.runs.read_text(encoding="utf-8"))
    ref = json.loads(args.main_ref.read_text(encoding="utf-8"))
    run_id, sha = select_capture(records.get("workflow_runs", []), ref["object"]["sha"])
    print(f"run_id={run_id}")
    print(f"source_sha={sha}")


if __name__ == "__main__":
    main()
