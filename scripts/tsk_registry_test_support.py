from __future__ import annotations

import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
GENERATOR = REPO_ROOT / "scripts" / "generate_tsk_registry.py"
TASK_DIR = Path("docs/00_プロジェクト管理/02_改善タスク管理")
REGISTRY_NAME = "改善タスク課題一覧.md"
ARCHIVE_NAME = "アーカイブ/改善タスク課題一覧_詳細版_2026-04-26.md"


@dataclass(frozen=True)
class Case:
    name: str
    rows: list[tuple[str, str, str, str]]
    files: dict[str, str]
    command_args: tuple[str, ...]
    expected_returncode: int
    expected_substrings: tuple[str, ...]


def registry(rows: list[tuple[str, str, str, str]]) -> str:
    counts = {"保留": 0, "未解決": 0, "確認待ち": 0, "解決中": 0, "解決済み": 0}
    for _, status, _, _ in rows:
        counts[status] += 1
    max_id = max((row[0] for row in rows), default="TSK-000")
    header = (
        "# 改善タスク課題一覧\n\n"
        "改善タスクの縮小索引。各タスクの詳細正本は個別 `TSK-***.md` および `解決済み/TSK-***.md` を参照すること。\n\n"
        f"旧11列詳細表の履歴参照導線は [`{ARCHIVE_NAME}`]({ARCHIVE_NAME}) に残す。\n\n"
        "## 集計\n\n"
        f"- 最大TSK番号: {max_id}\n"
        f"- 保留: {counts['保留']}\n"
        f"- 未解決: {counts['未解決']}\n"
        f"- 確認待ち: {counts['確認待ち']}\n"
        f"- 解決中: {counts['解決中']}\n"
        f"- 解決済み: {counts['解決済み']}\n\n"
        "## 個別TSK索引\n\n"
        "| 管理ID | 状態 | 件名 | 個別ファイル |\n"
        "|---|---|---|---|\n"
    )
    return header + "".join(
        f"| {task_id} | {status} | {title} | [{task_id}]({link_path}) |\n"
        for task_id, status, title, link_path in rows
    )


def tsk(task_id: str, status: str, title: str = "件名") -> str:
    return f"# {task_id} {title}\n\n- 件名: {title}\n- 状態: {status}\n"


def write_case(root: Path, case: Case) -> None:
    task_dir = root / TASK_DIR
    task_dir.mkdir(parents=True, exist_ok=True)
    (task_dir / "解決済み").mkdir(parents=True, exist_ok=True)
    (task_dir / REGISTRY_NAME).write_text(registry(case.rows), encoding="utf-8")
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
    ok = completed.returncode == case.expected_returncode and all(s in output for s in case.expected_substrings)
    print(f"[{'PASS' if ok else 'FAIL'}] {case.name}")
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


def run_cases(cases: tuple[Case, ...], work_root: Path, suite_name: str) -> int:
    results = [run_case(case, work_root) for case in cases]
    passed = sum(1 for result in results if result)
    total = len(results)
    if passed == total:
        print(f"[PASS] {suite_name}: {passed}/{total}")
        return 0
    print(f"[FAIL] {suite_name}: {passed}/{total} passed")
    return 1
