#!/usr/bin/env python3
"""Run all generate_tsk_registry.py regression test suites."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
TEST_SCRIPTS = (
    REPO_ROOT / "scripts" / "test_generate_tsk_registry_basic.py",
    REPO_ROOT / "scripts" / "test_generate_tsk_registry_failures.py",
)


def main() -> int:
    failures = 0
    for script in TEST_SCRIPTS:
        print(f"[INFO] running {script.name}")
        completed = subprocess.run([sys.executable, str(script)], cwd=REPO_ROOT)
        if completed.returncode != 0:
            failures += 1
    if failures:
        print(f"[FAIL] generate_tsk_registry regression suites failed: {failures}/{len(TEST_SCRIPTS)}")
        return 1
    print(f"[PASS] all generate_tsk_registry regression suites passed: {len(TEST_SCRIPTS)}/{len(TEST_SCRIPTS)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
