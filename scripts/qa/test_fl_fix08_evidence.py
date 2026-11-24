import struct
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from fl_fix08_evidence import REQUIRED, verify


def header(width=1080, height=2400):
    # Header-only fixture for the existing PNG dimension parser; not actual screenshot evidence.
    return b"\x89PNG\r\n\x1a\n" + struct.pack(">I", 13) + b"IHDR" + struct.pack(">II", width, height)


class EvidenceTests(unittest.TestCase):
    def test_missing_or_invalid_screen_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaises(ValueError):
                verify(root)
            for name in REQUIRED:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(header())
            (root / REQUIRED[-1]).write_bytes(header(720, 1280))
            with self.assertRaises(ValueError):
                verify(root)

    def test_inventory_hashes_and_provenance(self):
        with tempfile.TemporaryDirectory() as tmp, patch.dict("os.environ", {"GITHUB_SHA": "a" * 40, "GITHUB_RUN_ID": "42"}):
            root = Path(tmp)
            for name in REQUIRED:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(header())
            result = verify(root)
            self.assertEqual(result["tested_commit"], "a" * 40)
            self.assertEqual(result["workflow_run"], "42")
            self.assertEqual(result["screenshot_count"], len(REQUIRED))
            self.assertEqual(set(result["screenshot_sha256"]), set(REQUIRED))


if __name__ == "__main__":
    unittest.main()
