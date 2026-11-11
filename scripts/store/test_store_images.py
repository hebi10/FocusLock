import json
import tempfile
import unittest
from pathlib import Path

from render_images import CAPTURES, validate
from play_images import matches, require_context
from publisher import ReleaseError


class StoreImageTests(unittest.TestCase):
    def test_exactly_eight_unique_screens(self):
        self.assertEqual(len(CAPTURES), 8)
        self.assertEqual(len({row[0] for row in CAPTURES}), 8)
        self.assertEqual(len({row[1] for row in CAPTURES}), 8)

    def test_expected_order(self):
        self.assertEqual([row[0] for row in CAPTURES][0], "01-home.png")
        self.assertEqual([row[0] for row in CAPTURES][-1], "08-report.png")

    def test_missing_listing_is_not_accepted(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp)
            (path / "manifest.json").write_text(json.dumps({
                "package": "com.invalid.demo", "locale": "ko-KR",
                "screenshots": [], "feature_graphic": {}
            }), encoding="utf-8")
            with self.assertRaises(ValueError):
                validate(path)

    def test_write_context_must_be_exact_main_and_mode(self):
        good = {"GITHUB_REPOSITORY": "hebi10/FocusLock",
                "GITHUB_REF": "refs/heads/main",
                "PLAY_RELEASE_MODE": "store-images"}
        self.assertIsNone(require_context(good))
        for change in ({"GITHUB_REF": "refs/heads/other"},
                       {"PLAY_RELEASE_MODE": "deploy"},
                       {"GITHUB_REPOSITORY": "hebi10/GalliPick"}):
            with self.assertRaises(ReleaseError):
                require_context({**good, **change})

    def test_store_hashes_and_order_must_match(self):
        expected = [("a.png", "a" * 64), ("b.png", "b" * 64)]
        self.assertTrue(matches([{"sha256": "a" * 64}, {"sha256": "b" * 64}], expected))
        self.assertFalse(matches([{"sha256": "b" * 64}, {"sha256": "a" * 64}], expected))
        self.assertFalse(matches([], expected))


if __name__ == "__main__":
    unittest.main()
