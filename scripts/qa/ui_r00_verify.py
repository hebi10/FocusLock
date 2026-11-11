#!/usr/bin/env python3
"""UI-R00 baseline provenance and real Android screenshot/state verification.

No OCR, synthetic screenshots, data mutation, or Android binary modification.
"""
import argparse
import hashlib
import json
import os
import struct
import sys
import zipfile
from pathlib import Path

DEFAULT_MANIFEST = Path("docs/qa/UI-R00-baseline.json")
PACKAGE = "com.haebi.focuslock"
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
EXPECTED_PIXELS = (1080, 2400)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def png_dimensions(data):
    if len(data) < 24 or data[:8] != PNG_MAGIC or data[12:16] != b"IHDR":
        raise ValueError("Not an authentic PNG file")
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != EXPECTED_PIXELS:
        raise ValueError(f"Android capture is {width}x{height}, expected {EXPECTED_PIXELS}")
    return width, height


def read_manifest(path):
    manifest = json.loads(Path(path).read_text(encoding="utf-8"))
    if (manifest.get("task") != "UI-R00" or manifest.get("package") != PACKAGE or
            len(manifest.get("design_sources", [])) != 10 or
            len(manifest.get("screenshot_files", {})) != 23 or
            len(manifest.get("supplemental_scroll_captures", {})) != 14):
        raise ValueError("UI-R00 manifest counts, app identity or source set invalid")
    actual_names = set(manifest["screenshot_files"])
    for item in manifest["design_sources"]:
        if not set(item["actual_baseline_files"]).issubset(actual_names):
            raise ValueError("Design mapping references missing actual capture")
    return manifest


def verify_frozen_zip(manifest, archive):
    archive = Path(archive)
    data = archive.read_bytes()
    if digest(data) != manifest["source"]["artifact_sha256"]:
        raise ValueError("Frozen Android QA archive SHA-256 differs from pinned artifact")
    with zipfile.ZipFile(archive) as source:
        expected = manifest["screenshot_files"]
        paths = sorted(n for n in source.namelist() if n.startswith("qa-captures/") and n.endswith(".png"))
        if len(paths) != len(expected):
            raise ValueError("Pinned capture count differs")
        checked = []
        for zip_path in paths:
            name = Path(zip_path).name
            if name not in expected:
                raise ValueError("Unexpected baseline screenshot")
            image = source.read(zip_path)
            png_dimensions(image)
            if digest(image) != expected[name]:
                raise ValueError(f"Pinned screenshot SHA-256 mismatch: {name}")
            checked.append(name)
    if set(checked) != set(expected):
        raise ValueError("Frozen Android capture inventory incomplete")
    return {"status": "baseline_verified", "source_workflow_run": manifest["source"]["workflow_run"],
            "source_sha": manifest["source"]["commit"], "capture_count": len(checked),
            "pixel_size": list(EXPECTED_PIXELS), "design_count": len(manifest["design_sources"])}


def verify_run(manifest, capture_root):
    root = Path(capture_root)
    folder = root / "qa-captures"
    existing = manifest["screenshot_files"]
    supplement = manifest["supplemental_scroll_captures"]
    expected = set(existing) | set(supplement)
    actual = {p.name for p in folder.glob("*.png")}
    missing = sorted(expected - actual)
    if missing:
        raise ValueError("Missing Android QA screenshots: " + ", ".join(missing))
    invalid = sorted(actual - expected)
    if invalid:
        raise ValueError("Unrecognized screenshot names: " + ", ".join(invalid))
    screenshot_hashes = {}
    for filename in sorted(expected):
        data = (folder / filename).read_bytes()
        png_dimensions(data)
        screenshot_hashes[filename] = digest(data)
    visual = []
    for filename, parent in supplement.items():
        visual.append({"capture": filename, "base_capture": parent,
                       "pixels_changed_after_scroll": screenshot_hashes[filename] != screenshot_hashes[parent]})
    qa_data = root / "qa-r00"
    denied = json.loads((qa_data / "permission-denied.json").read_text(encoding="utf-8"))
    granted = json.loads((qa_data / "permission-granted.json").read_text(encoding="utf-8"))
    if denied.get("observedState") != "PermissionRequired":
        raise ValueError("Android denied appop did not produce PermissionRequired")
    if granted.get("observedState") != "Ready":
        raise ValueError("Android granted appop did not produce Ready")
    if denied.get("dataInjected") is not False or granted.get("dataInjected") is not False:
        raise ValueError("QA must use real Android usage history without injection")
    return {
        "task": "UI-R00", "status": "emulator_evidence_verified",
        "repository": "hebi10/FocusLock",
        "tested_commit": os.environ.get("GITHUB_SHA", "local-run-unknown"),
        "baseline_commit": manifest["source"]["commit"],
        "baseline_workflow_run": manifest["source"]["workflow_run"],
        "design_reference_count": len(manifest["design_sources"]),
        "primary_capture_count": len(existing),
        "supplemental_capture_count": len(supplement),
        "android_capture_count": len(expected),
        "resolution": list(EXPECTED_PIXELS),
        "scroll_checks": visual,
        "scroll_different_count": sum(x["pixels_changed_after_scroll"] for x in visual),
        "usage_data": granted,
        "permission_denied_data": denied,
        "rich_usage_coverage": "supported" if granted.get("enoughDiverseHistoryForStoreReview") is True else "limited_real_data",
        "source_truth": "Real Android API 36 capture with actual UsageStatsManager data, no DemoData injection",
        "verification_boundary": "Does not prove 160% text, foldable, Samsung hardware, long-term history or Google Play visibility.",
        "screenshot_sha256": screenshot_hashes,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("baseline", "run"))
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--baseline-archive", type=Path)
    parser.add_argument("--capture-root", type=Path, default=Path("captures"))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        manifest = read_manifest(args.manifest)
        if args.mode == "baseline":
            if args.baseline_archive is None:
                parser.error("baseline mode requires --baseline-archive")
            result = verify_frozen_zip(manifest, args.baseline_archive)
        else:
            result = verify_run(manifest, args.capture_root)
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({k: v for k, v in result.items() if k != "screenshot_sha256"}, ensure_ascii=False))
        return 0
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as exc:
        print("UI-R00 evidence verification failed: " + str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
