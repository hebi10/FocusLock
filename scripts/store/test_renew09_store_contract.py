import unittest
import hashlib
import json
import tempfile
from pathlib import Path
from render_images import CAPTURES, BG, INK, MUTED, ACCENT, FEATURE_SOURCES, verify_manifest_provenance, verify_qa_evidence

class Renew09StoreContractTests(unittest.TestCase):
    def test_latest_store_copy_is_analysis_first(self):
        self.assertEqual(len(CAPTURES), 8)
        self.assertEqual(CAPTURES[0][1], "14-home-granted.png")
        self.assertEqual(CAPTURES[1][1], "15-analysis-today.png")
        self.assertEqual(CAPTURES[3][1], "qa-flfix08-captures/goal-manager.png")
        self.assertEqual(CAPTURES[-1][1], "qa-r08/api36-phone-report-day-selection.png")
        self.assertEqual(CAPTURES[-1][2], "사용 변화 리포트")
        self.assertTrue(all("자동 차단" not in x[2] + x[3] for x in CAPTURES))
        self.assertTrue(all("AI" not in x[2] + x[3] for x in CAPTURES))

    def test_provenance_requires_exact_main_sha_and_workflow_run(self):
        verified = {"source_commit": "a" * 40, "source_workflow_run": "44004871393"}
        self.assertIsNone(verify_manifest_provenance(verified))
        invalid = [
            {"source_commit": "old-main", "source_workflow_run": "44004871393"},
            {"source_commit": "A" * 40, "source_workflow_run": "44004871393"},
            {"source_commit": "a" * 40, "source_workflow_run": "0"},
            {"source_commit": "a" * 40, "source_workflow_run": "not-a-run"},
            {"source_commit": "a" * 40, "source_workflow_run": 44004871393},
        ]
        for value in invalid:
            with self.subTest(value=value), self.assertRaises(ValueError):
                verify_manifest_provenance(value)

    def test_exact_qa_evidence_hashes_and_run_required(self):
        # Byte fixtures exercise provenance only; these are not screenshots and are never rendered.
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            captures = root / "qa-captures"
            captures.mkdir()
            commit, run = "a" * 40, "42"
            sources = {row[1] for row in CAPTURES} | set(FEATURE_SOURCES)
            primary = {name for name in sources if "/" not in name}
            primary.update(f"fixture-{i}.png" for i in range(37 - len(primary)))
            extra = {name for name in sources if "/" in name}
            extra.update(f"qa-flfix08-captures/fixture-{i}.png" for i in range(7 - len(extra)))
            def make_files(names, folder):
                result = {}
                for name in names:
                    path = folder / name
                    path.parent.mkdir(parents=True, exist_ok=True)
                    data = name.encode()
                    path.write_bytes(data)
                    result[name] = hashlib.sha256(data).hexdigest()
                return result
            baseline = {"status": "emulator_evidence_verified", "tested_commit": commit,
                        "android_capture_count": 37, "screenshot_sha256": make_files(primary, captures)}
            additional = {"status": "additional_emulator_evidence_verified", "tested_commit": commit,
                          "workflow_run": run, "screenshot_count": 7, "screenshot_sha256": make_files(extra, root)}
            (root / "ui-r00-audit-report.json").write_text(json.dumps(baseline))
            (root / "fl-fix08-evidence.json").write_text(json.dumps(additional))
            self.assertEqual(set(verify_qa_evidence(captures, commit, run)), sources)
            with self.assertRaises(ValueError):
                verify_qa_evidence(captures, commit, "43")
            (root / CAPTURES[3][1]).write_bytes(b"changed")
            with self.assertRaises(ValueError):
                verify_qa_evidence(captures, commit, run)

    def test_listing_palette_is_neutral(self):
        self.assertEqual(ACCENT, "#373737")
        self.assertEqual(BG, "#F7F7F7")
        self.assertEqual(INK, "#151515")
        self.assertEqual(MUTED, "#656565")

if __name__ == "__main__":
    unittest.main()
