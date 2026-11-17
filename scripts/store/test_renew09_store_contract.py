import unittest
from render_images import CAPTURES, BG, INK, MUTED, ACCENT, verify_manifest_provenance

class Renew09StoreContractTests(unittest.TestCase):
    def test_latest_store_copy_is_analysis_first(self):
        self.assertEqual(len(CAPTURES), 8)
        self.assertEqual(CAPTURES[0][1], "14-home-granted.png")
        self.assertEqual(CAPTURES[1][1], "15-analysis-today.png")
        self.assertEqual(CAPTURES[-1][1], "20-report.png")
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

    def test_listing_palette_is_neutral(self):
        self.assertEqual(ACCENT, "#373737")
        self.assertEqual(BG, "#F7F7F7")
        self.assertEqual(INK, "#151515")
        self.assertEqual(MUTED, "#656565")

if __name__ == "__main__":
    unittest.main()
