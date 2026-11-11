import copy
import json
import struct
import tempfile
import unittest
from pathlib import Path

import ui_r00_verify as audit


class UiR00ContractTests(unittest.TestCase):
    def setUp(self):
        self.source = audit.read_manifest(Path("docs/qa/UI-R00-baseline.json"))

    def test_manifest_freezes_designs_and_real_qa(self):
        self.assertEqual(self.source["package"], "com.haebi.focuslock")
        self.assertEqual(len(self.source["design_sources"]), 10)
        self.assertEqual(len(self.source["screenshot_files"]), 23)
        self.assertEqual(len(self.source["supplemental_scroll_captures"]), 14)
        self.assertEqual(self.source["source"]["workflow_run"], 42945780305)

    def test_source_has_unique_names_and_no_fake_screens(self):
        primary = set(self.source["screenshot_files"])
        scrolls = set(self.source["supplemental_scroll_captures"])
        self.assertFalse(primary & scrolls)
        for design in self.source["design_sources"]:
            self.assertTrue(set(design["actual_baseline_files"]).issubset(primary))
            self.assertTrue(design["google_drive_file_id"])

    def test_png_header_validates_android_dimensions(self):
        header = audit.PNG_MAGIC + b"\0\0\0\rIHDR" + struct.pack(">II", 1080, 2400)
        self.assertEqual(audit.png_dimensions(header), (1080, 2400))
        with self.assertRaises(ValueError):
            audit.png_dimensions(header[:-8] + struct.pack(">II", 1080, 1920))
        with self.assertRaises(ValueError):
            audit.png_dimensions(b"not a screenshot")

    def test_manifest_rejects_removed_source_screen(self):
        broken = copy.deepcopy(self.source)
        broken["screenshot_files"].pop("01-onboarding-intro.png")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "manifest.json"
            path.write_text(json.dumps(broken), encoding="utf-8")
            with self.assertRaises(ValueError):
                audit.read_manifest(path)


if __name__ == "__main__":
    unittest.main()
