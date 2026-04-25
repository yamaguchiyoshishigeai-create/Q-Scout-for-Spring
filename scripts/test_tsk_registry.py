#!/usr/bin/env python3
"""Regression tests for check_tsk_registry.py.

The tests create temporary minimal registries and individual TSK files, then
execute the checker as a subprocess.

Usage:
    python scripts/test_tsk_registry.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CHECKER = REPO_ROOT / "scripts" / "check_tsk_registry.py"
TASK_DIR = Path("docs/00_プロジェクト管理/02_改善タスク管理")
REGISTRY_NAME = "改善タスク課題一覧.md"

HEADER = """# 改善タスク課題一覧

| 管理ID | 区分 | 件名 | 状態 | 優先度 | 実施主体 | 関連箇所 | 概要 | 完了条件 | 根拠 / 関連PR / 備考 | 個別ファイル |
|---|---|---|---|---|---|---|---|---|---|---|
"""


def row(task_id: str, status: str, link_path: str | None = None, link_label: str | None = None) -> str:
    link_path = link_path if link_path is not None else f"{task_id}.md"
    link_label = link_label if link_label is not None else task_id
    return f"| {task_id} | 運用 | 件名 | {status} | 高 | ChatGPT(リポジトリ編集) | docs | 概要 | 完了条件 | 備考 | [{link_label}]({link_path}) |\n"


def tsk_file(task_id: str, status: str) -> str:
    return f"# {task_id} 件名\n\n- 区分: 運用\n- 件名: 件名\n- 状態: {status}\n- 優先度: 高\n"


@dataclass(frozen=True)
class Case:
    name: str
    registry_rows: tuple[str, ...]
    files: dict[str, str]
    expected_returncode: int
    expected_substrings: tuple[str, ...]


CASES: tuple[Case, ...] = (
    Case(
        name="valid_active_and_solved_pass",
        registry_rows=(
            row("TSK-001", "未解決"),
            row("TSK-002", "解決済み", "解決済み/TSK-002.md"),
        ),
        files={
            "TSK-001.md": tsk_file("TSK-001", "未解決"),
            "解決済み/TSK-002.md": tsk_file("TSK-002", "解決済み"),
        },
        expected_returncode=0,
        expected_substrings=("[PASS]", "max TSK number: TSK-002"),
    ),
    Case(
        name="duplicate_task_id_fails",
        registry_rows=(row("TSK-001", "未解決"), row("TSK-001", "未解決")),
        files={"TSK-001.md": tsk_file("TSK-001", "未解決")},
        expected_returncode=1,
        expected_substrings=("DUPLICATE_TASK_ID",),
    ),
    Case(
        name="missing_link_target_fails",
        registry_rows=(row("TSK-001", "未解決"),),
        files={},
        expected_returncode=1,
        expected_substrings=("LINK_TARGET_MISSING",),
    ),
    Case(
        name="active_link_to_solved_dir_fails",
        registry_rows=(row("TSK-001", "未解決", "解決済み/TSK-001.md"),),
        files={"解決済み/TSK-001.md": tsk_file("TSK-001", "未解決")},
        expected_returncode=1,
        expected_substrings=("ACTIVE_LINK_PATH_MISMATCH",),
    ),
    Case(
        name="solved_link_to_active_dir_fails",
        registry_rows=(row("TSK-001", "解決済み", "TSK-001.md"),),
        files={"TSK-001.md": tsk_file("TSK-001", "解決済み")},
        expected_returncode=1,
        expected_substrings=("SOLVED_LINK_PATH_MISMATCH",),
    ),
    Case(
        name="status_mismatch_fails",
        registry_rows=(row("TSK-001", "未解決"),),
        files={"TSK-001.md": tsk_file("TSK-001", "確認待ち")},
        expected_returncode=1,
        expected_substrings=("STATUS_MISMATCH",),
    ),
    Case(
        name="unlisted_active_file_fails",
        registry_rows=(row("TSK-001", "未解決"),),
        files={
            "TSK-001.md": tsk_file("TSK-001", "未解決"),
            "TSK-002.md": tsk_file("TSK-002", "未解決"),
        },
        expected_returncode=1,
        expected_substrings=("INDIVIDUAL_FILE_UNLISTED",),
    ),
    Case(
        name="unlisted_solved_file_fails",
        registry_rows=(row("TSK-001", "未解決"),),
        files={
            "TSK-001.md": tsk_file("TSK-001", "未解決"),
            "解決済み/TSK-002.md": tsk_file("TSK-002", "解決済み"),
        },
        expected_returncode=1,
        expected_substrings=("SOLVED_FILE_UNLISTED",),
    ),
    Case(
        name="link_label_mismatch_fails",
        registry_rows=(row("TSK-001", "未解決", "TSK-001.md", "TSK-999"),),
        files={"TSK-001.md": tsk_file("TSK-001", "未解決")},
        expected_returncode=1,
        expected_substrings=("LINK_LABEL_MISMATCH",),
    ),
)


def write_case(root: Path, case: Case) -> None:
    task_dir = root / TASK_DIR
    task_dir.mkdir(parents=True, exist_ok=True)
    (task_dir / "解決済み").mkdir(parents=True, exist_ok=True)
    (task_dir / REGISTRY_NAME).write_text(HEADER + "".join(case.registry_rows), encoding="utf-8")
    for rel_path, content in case.files.items():
        path = task_dir / rel_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")


def run_case(case: Case, work_root: Path) -> bool:
    case_root = work_root / case.name
    write_case(case_root, case)
    command = [sys.executable, str(CHECKER), "--root", str(case_root)]
    completed = subprocess.run(command, cwd=REPO_ROOT, text=True, capture_output=True)
    output = completed.stdout + completed.stderr

    ok = completed.returncode == case.expected_returncode and all(
        expected in output for expected in case.expected_substrings
    )

    status = "PASS" if ok else "FAIL"
    print(f"[{status}] {case.name}")
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
    if not CHECKER.exists():
        print(f"[FAIL] checker not found: {CHECKER}", file=sys.stderr)
        return 2

    with tempfile.TemporaryDirectory(prefix="tsk_registry_tests_") as tmp:
        work_root = Path(tmp)
        results = [run_case(case, work_root) for case in CASES]

    passed = sum(1 for result in results if result)
    total = len(results)
    if passed == total:
        print(f"[PASS] all TSK registry regression cases passed: {passed}/{total}")
        return 0

    print(f"[FAIL] TSK registry regression cases failed: {passed}/{total} passed")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
