#!/usr/bin/env python3
"""Basic regression tests for generate_tsk_registry.py."""

from __future__ import annotations

import tempfile
from pathlib import Path

from tsk_registry_test_support import Case, run_cases, tsk


CASES = (
    Case(
        "check_matches_committed_registry",
        [("TSK-001", "未解決", "件名", "TSK-001.md"), ("TSK-002", "解決済み", "件名", "解決済み/TSK-002.md")],
        {"TSK-001.md": tsk("TSK-001", "未解決"), "解決済み/TSK-002.md": tsk("TSK-002", "解決済み")},
        ("--mode", "check"),
        0,
        ("[PASS] generated TSK registry matches committed registry.", "generated registry rows: 2"),
    ),
    Case(
        "summary_reports_counts",
        [("TSK-001", "未解決", "件名", "TSK-001.md"), ("TSK-002", "解決済み", "件名", "解決済み/TSK-002.md")],
        {"TSK-001.md": tsk("TSK-001", "未解決"), "解決済み/TSK-002.md": tsk("TSK-002", "解決済み")},
        ("--mode", "summary"),
        0,
        ("generated registry rows: 2", "max TSK number: TSK-002"),
    ),
)


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="tsk_registry_basic_tests_") as tmp:
        return run_cases(CASES, Path(tmp), "TSK registry generator basic cases")


if __name__ == "__main__":
    raise SystemExit(main())
