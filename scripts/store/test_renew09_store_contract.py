import unittest
from render_images import CAPTURES, BG, INK, MUTED, ACCENT

class Renew09StoreContractTests(unittest.TestCase):
    def test_latest_store_copy_is_analysis_first(self):
        self.assertEqual(len(CAPTURES), 8)
        self.assertEqual(CAPTURES[0][1], "14-home-granted.png")
        self.assertEqual(CAPTURES[1][1], "15-analysis-today.png")
        self.assertEqual(CAPTURES[-1][1], "20-report.png")
        self.assertEqual(CAPTURES[-1][2], "사용 변화 리포트")
        self.assertTrue(all("자동 차단" not in x[2] + x[3] for x in CAPTURES))
        self.assertTrue(all("AI" not in x[2] + x[3] for x in CAPTURES))

    def test_listing_palette_is_neutral(self):
        self.assertEqual(ACCENT, "#373737")
        self.assertEqual(BG, "#F7F7F7")
        self.assertEqual(INK, "#151515")
        self.assertEqual(MUTED, "#656565")

if __name__ == "__main__":
    unittest.main()
