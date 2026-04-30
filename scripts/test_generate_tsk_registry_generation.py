#!/usr/bin/env python3
"""Generation regression tests for generate_tsk_registry.py."""

from __future__ import annotations

import subprocess
import sys
import tempfile
from pathlib import Path

from tsk_registry_test_support import GENERATOR, REPO_ROOT, REGISTRY_NAME, TASK_DIR, registry, tsk


def write_task_files(root: Path) -> Path:
    task_dir = root / TASK_DIR
    task_dir.mkdir(parents=True, exist_ok=True)
    (task_dir / "解決済み").mkdir(parents=True, exist_ok=True)
    (task_dir / "TSK-001.md").write_text(tsk("TSK-001", "未解決"), encoding="utf-8")
    (task_dir / "解決済み" / "TSK-002.md").write_text(tsk("TSK-002", "解決済み"), encoding="utf-8")
    return task_dir


def expected_registry() -> str:
    return registry(
        [
            ("TSK-001", "未解決", "件名", "TSK-001.md"),
            ("TSK-002", "解決済み", "件名", "解決済み/TSK-002.md"),
        ]
    )


def run(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=REPO_ROOT, text=True, capture_output=True)


def check_output_generation(root: Path) -> bool:
    task_dir = write_task_files(root)
    output_path = root / "tmp" / "generated_tsk_registry.md"
    completed = run(
        [
            sys.executable,
            str(GENERATOR),
            "--root",
            str(root),
            "--mode",
            "generate",
            "--output",
            str(output_path),
        ]
    )
    output = completed.stdout + completed.stderr
    ok = (
        completed.returncode == 0
        and output_path.exists()
        and output_path.read_text(encoding="utf-8") == expected_registry()
        and "[OK] wrote generated registry" in output
        and not (task_dir / REGISTRY_NAME).exists()
    )
    print(f"[{'PASS' if ok else 'FAIL'}] generate_writes_output_file")
    if not ok:
        print(output.rstrip())
    return ok


def check_write_generation(root: Path) -> bool:
    task_dir = write_task_files(root)
    registry_path = task_dir / REGISTRY_NAME
    registry_path.write_text("# stale registry\n", encoding="utf-8")
    completed = run(
        [
            sys.executable,
            str(GENERATOR),
            "--root",
            str(root),
            "--mode",
            "generate",
            "--write",
        ]
    )
    output = completed.stdout + completed.stderr
    check_completed = run(
        [
            sys.executable,
            str(GENERATOR),
            "--root",
            str(root),
            "--mode",
            "check",
        ]
    )
    check_output = check_completed.stdout + check_completed.stderr
    ok = (
        completed.returncode == 0
        and registry_path.read_text(encoding="utf-8") == expected_registry()
        and "[OK] wrote registry" in output
        and check_completed.returncode == 0
        and "[PASS] generated TSK registry matches committed registry." in check_output
    )
    print(f"[{'PASS' if ok else 'FAIL'}] generate_write_updates_registry_and_check_passes")
    if not ok:
        print((output + check_output).rstrip())
    return ok


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="tsk_registry_generation_tests_") as tmp:
        root = Path(tmp)
        results = [check_output_generation(root / "output"), check_write_generation(root / "write")]
    passed = sum(1 for result in results if result)
    total = len(results)
    if passed == total:
        print(f"[PASS] TSK registry generator generation cases: {passed}/{total}")
        return 0
    print(f"[FAIL] TSK registry generator generation cases: {passed}/{total} passed")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
