#!/usr/bin/env python3
"""Failure diagnostics regression tests for generate_tsk_registry.py."""

from __future__ import annotations

import tempfile
from pathlib import Path

from tsk_registry_test_support import Case, run_cases, tsk


CASES = (
    Case(
        "check_detects_status_drift",
        [("TSK-001", "未解決", "件名", "TSK-001.md")],
        {"TSK-001.md": tsk("TSK-001", "解決中")},
        ("--mode", "check"),
        1,
        ("[DIFF] row mismatch: TSK-001", "状態"),
    ),
    Case(
        "path_status_mismatch_fails",
        [("TSK-001", "未解決", "件名", "TSK-001.md")],
        {"TSK-001.md": tsk("TSK-001", "解決済み")},
        ("--mode", "check"),
        1,
        ("path/status mismatch",),
    ),
)


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="tsk_registry_failure_tests_") as tmp:
        return run_cases(CASES, Path(tmp), "TSK registry generator failure cases")


if __name__ == "__main__":
    raise SystemExit(main())
