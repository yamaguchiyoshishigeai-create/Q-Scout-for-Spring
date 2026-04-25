#!/usr/bin/env python3
"""Regression tests for generate_tsk_registry.py.

The tests create temporary minimal TSK registries and individual files, then
execute the generator as a subprocess.

Usage:
    python scripts/test_generate_tsk_registry.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
GENERATOR = REPO_ROOT / "scripts" / "generate_tsk_registry.py"
TASK_DIR = Path("docs/00_プロジェクト管理/02_改善タスク管理")
REGISTRY_NAME = "改善タスク課題一覧.md"

HEADER = """# 改善タスク課題一覧

改善タスクの俯瞰用一覧正本。各タスクの詳細は個別ファイルを参照すること。

| 管理ID | 区分 | 件名 | 状態 | 優先度 | 実施主体 | 関連箇所 | 概要 | 完了条件 | 根拠 / 関連PR / 備考 | 個別ファイル |
|---|---|---|---|---|---|---|---|---|---|---|
"""


def registry_row(task_id: str, status: str, link_path: str, title: str = "件名") -> str:
    return (
        f"| {task_id} | 運用 | {title} | {status} | 高 | ChatGPT(リポジトリ編集) | docs | "
        f"概要 | 完了条件 | 備考 | [{task_id}]({link_path}) |\n"
    )


def tsk_file(task_id: str, status: str, title: str = "件名") -> str:
    return (
        f"# {task_id} {title}\n\n"
        f"- 区分: 運用\n"
        f"- 件名: {title}\n"
        f"- 状態: {status}\n"
        f"- 優先度: 高\n"
        f"- 実施主体: ChatGPT(リポジトリ編集)\n"
        f"- 関連箇所: docs\n"
    )


@dataclass(frozen=True)
class Case:
    name: str
    registry: str
    files: dict[str, str]
    command_args: tuple[str, ...]
    expected_returncode: int
    expected_substrings: tuple[str, ...]


CASES: tuple[Case, ...] = (
    Case(
        name="check_matches_committed_registry",
        registry=HEADER
        + registry_row("TSK-001", "未解決", "TSK-001.md")
        + registry_row("TSK-002", "解決済み", "解決済み/TSK-002.md"),
        files={
            "TSK-001.md": tsk_file("TSK-001", "未解決"),
            "解決済み/TSK-002.md": tsk_file("TSK-002", "解決済み"),
        },
        command_args=("--mode", "check"),
        expected_returncode=0,
        expected_substrings=("[PASS] generated TSK registry matches committed registry.", "generated registry rows: 2"),
    ),
    Case(
        name="check_detects_status_drift",
        registry=HEADER + registry_row("TSK-001", "未解決", "TSK-001.md"),
        files={"TSK-001.md": tsk_file("TSK-001", "解決中")},
        command_args=("--mode", "check"),
        expected_returncode=1,
        expected_substrings=("status mismatch for TSK-001",),
    ),
    Case(
        name="summary_reports_counts",
        registry=HEADER
        + registry_row("TSK-001", "未解決", "TSK-001.md")
        + registry_row("TSK-002", "解決済み", "解決済み/TSK-002.md"),
        files={
            "TSK-001.md": tsk_file("TSK-001", "未解決"),
            "解決済み/TSK-002.md": tsk_file("TSK-002", "解決済み"),
        },
        command_args=("--mode", "summary"),
        expected_returncode=0,
        expected_substrings=("generated registry rows: 2", "max TSK number: TSK-002"),
    ),
    Case(
        name="path_status_mismatch_fails",
        registry=HEADER + registry_row("TSK-001", "未解決", "TSK-001.md"),
        files={"TSK-001.md": tsk_file("TSK-001", "解決済み")},
        command_args=("--mode", "check"),
        expected_returncode=1,
        expected_substrings=("path/status mismatch",),
    ),
)


def write_case(root: Path, case: Case) -> None:
    task_dir = root / TASK_DIR
    task_dir.mkdir(parents=True, exist_ok=True)
    (task_dir / "解決済み").mkdir(parents=True, exist_ok=True)
    (task_dir / REGISTRY_NAME).write_text(case.registry, encoding="utf-8")
    for rel_path, content in case.files.items():
        path = task_dir / rel_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")


def run_case(case: Case, work_root: Path) -> bool:
    case_root = work_root / case.name
    write_case(case_root, case)
    command = [sys.executable, str(GENERATOR), "--root", str(case_root), *case.command_args]
    completed = subprocess.run(command, cwd=REPO_ROOT, text=True, capture_output=True)
    output = completed.stdout + completed.stderr

    ok = completed.returncode == case.expected_returncode and all(
        expected in output for expected in case.expected_substrings
    )

    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {case.name}")
    if ok and case.expected_returncode != 0:
        print(f"  expected failure return code: {case.expected_returncode}")
        print("  verified diagnostic substrings:")
        for expected in case.expected_substrings:
            print(f"    - {expected}")
    if not ok:
        print(f"  expected return code: {case.expected_returncode}")
        print(f"  actual return code  : {completed.returncode}")
        print("  expected substrings:")
        for expected in case.expected_substrings:
            print(f"    - {expected}")
        print("  output:")
        print(output.rstrip())
    return ok


def main() -> int:
    if not GENERATOR.exists():
        print(f"[FAIL] generator not found: {GENERATOR}", file=sys.stderr)
        return 2

    with tempfile.TemporaryDirectory(prefix="tsk_registry_generator_tests_") as tmp:
        work_root = Path(tmp)
        results = [run_case(case, work_root) for case in CASES]

    passed = sum(1 for result in results)
    total = len(results)
    if passed == total:
        print(f"[PASS] all TSK registry generator regression cases passed: {passed}/{total}")
        return 0

    print(f"[FAIL] TSK registry generator regression cases failed: {passed}/{total} passed")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
