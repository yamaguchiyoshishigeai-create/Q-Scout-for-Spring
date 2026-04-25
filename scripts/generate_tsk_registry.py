#!/usr/bin/env python3
"""Generate or check the compact TSK registry index from individual TSK files.

TSK-043 migrated 改善タスク課題一覧.md from a large detailed table to a
compact index. Individual TSK files are the detailed source. This script reads
normal TSK-***.md files and solved files under 解決済み/, then generates a
compact index with task ID, status, title, and link.

Usage:
    python scripts/generate_tsk_registry.py --mode summary
    python scripts/generate_tsk_registry.py --mode check
    python scripts/generate_tsk_registry.py --mode generate --output tmp/generated_tsk_registry.md
    python scripts/generate_tsk_registry.py --mode generate --write
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


TASK_DIR = Path("docs/00_プロジェクト管理/02_改善タスク管理")
REGISTRY_NAME = "改善タスク課題一覧.md"
ARCHIVE_NAME = "アーカイブ/改善タスク課題一覧_詳細版_2026-04-26.md"
SOLVED_DIR_NAME = "解決済み"
SOLVED_STATUS = "解決済み"
VALID_ACTIVE_STATUSES = {"未解決", "解決中", "確認待ち", "保留"}
VALID_STATUSES = VALID_ACTIVE_STATUSES | {SOLVED_STATUS}
COMPACT_COLUMNS = ("管理ID", "状態", "件名", "個別ファイル")

ROW_ID_RE = re.compile(r"^TSK-(?P<num>\d{3})$")
TITLE_RE = re.compile(r"^#\s+(?P<task_id>TSK-\d{3})\s+(?P<title>.+?)\s*$", re.MULTILINE)
META_RE = re.compile(r"^-\s*(?P<key>[^:：]+)\s*[:：]\s*(?P<value>.+?)\s*$", re.MULTILINE)
LINK_RE = re.compile(r"\[(?P<label>[^\]]+)\]\((?P<path>[^)]+)\)")


@dataclass(frozen=True)
class RegistryRow:
    task_id: str
    status: str
    title: str
    link_path: str

    @property
    def number(self) -> int:
        match = ROW_ID_RE.match(self.task_id)
        return int(match.group("num")) if match else 0

    def to_markdown(self) -> str:
        return f"| {self.task_id} | {self.status} | {self.title} | [{self.task_id}]({self.link_path}) |"

    def cells(self) -> list[str]:
        return [self.task_id, self.status, self.title, f"[{self.task_id}]({self.link_path})"]


@dataclass(frozen=True)
class TaskMetadata:
    task_id: str
    title: str
    status: str
    rel_path: str

    @property
    def number(self) -> int:
        match = ROW_ID_RE.match(self.task_id)
        return int(match.group("num")) if match else 0


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate or check the compact TSK registry index.")
    parser.add_argument("--root", type=Path, default=Path("."), help="Repository root. Default: current directory.")
    parser.add_argument(
        "--mode",
        choices=("summary", "check", "generate"),
        default="check",
        help="summary prints counts, check compares generated output, generate writes generated output.",
    )
    parser.add_argument("--output", type=Path, default=None, help="Output path for --mode generate.")
    parser.add_argument(
        "--write",
        action="store_true",
        help="With --mode generate, overwrite the canonical registry file.",
    )
    return parser.parse_args(argv)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8-sig")


def normalize_text(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.replace("\r\n", "\n").replace("\r", "\n").split("\n")).rstrip() + "\n"


def split_markdown_row(line: str) -> list[str]:
    stripped = line.strip()
    if not stripped.startswith("|") or not stripped.endswith("|"):
        return []
    return [cell.strip() for cell in stripped.strip("|").split("|")]


def parse_registry_text(text: str) -> dict[str, RegistryRow]:
    rows: dict[str, RegistryRow] = {}
    for line in text.splitlines():
        cells = split_markdown_row(line)
        if len(cells) < 4:
            continue
        task_id = cells[0]
        if not ROW_ID_RE.match(task_id):
            continue

        # Compact index format: 管理ID / 状態 / 件名 / 個別ファイル
        if len(cells) == 4:
            status = cells[1]
            title = cells[2]
            link_cell = cells[3]
        # Legacy detailed table format: 管理ID / 区分 / 件名 / 状態 / ... / 個別ファイル
        else:
            title = cells[2]
            status = cells[3]
            link_cell = cells[-1]

        link_match = LINK_RE.search(link_cell)
        link_path = link_match.group("path") if link_match else expected_link_path(task_id, status)
        rows[task_id] = RegistryRow(task_id=task_id, status=status, title=title, link_path=link_path)
    return rows


def parse_existing_registry(registry_path: Path) -> dict[str, RegistryRow]:
    if not registry_path.exists():
        return {}
    return parse_registry_text(read_text(registry_path))


def expected_link_path(task_id: str, status: str) -> str:
    if status == SOLVED_STATUS:
        return f"{SOLVED_DIR_NAME}/{task_id}.md"
    return f"{task_id}.md"


def parse_metadata(text: str) -> dict[str, str]:
    metadata: dict[str, str] = {}
    for match in META_RE.finditer(text):
        metadata[match.group("key").strip()] = match.group("value").strip()
    return metadata


def parse_task_metadata(path: Path, task_dir: Path) -> TaskMetadata:
    text = read_text(path)
    title_match = TITLE_RE.search(text)
    if not title_match:
        raise ValueError(f"missing title with TSK ID: {path.relative_to(task_dir)}")

    task_id = title_match.group("task_id")
    title = title_match.group("title").strip()
    metadata = parse_metadata(text)
    status = metadata.get("状態", "")
    if status not in VALID_STATUSES:
        raise ValueError(f"unknown or missing status for {task_id}: {status!r}")

    rel_path = path.relative_to(task_dir).as_posix()
    expected_path = expected_link_path(task_id, status)
    if rel_path != expected_path:
        raise ValueError(f"path/status mismatch for {task_id}: path={rel_path} expected={expected_path}")

    return TaskMetadata(
        task_id=task_id,
        title=metadata.get("件名", title),
        status=status,
        rel_path=rel_path,
    )


def collect_task_paths(task_dir: Path) -> list[Path]:
    paths = list(task_dir.glob("TSK-*.md"))
    solved_dir = task_dir / SOLVED_DIR_NAME
    if solved_dir.exists():
        paths.extend(solved_dir.glob("TSK-*.md"))
    return sorted(paths, key=lambda path: path.name)


def generate_rows(task_dir: Path) -> list[RegistryRow]:
    rows: list[RegistryRow] = []
    seen: set[str] = set()
    for path in collect_task_paths(task_dir):
        metadata = parse_task_metadata(path, task_dir)
        if metadata.task_id in seen:
            raise ValueError(f"duplicate individual task file for {metadata.task_id}")
        seen.add(metadata.task_id)
        rows.append(
            RegistryRow(
                task_id=metadata.task_id,
                status=metadata.status,
                title=metadata.title,
                link_path=metadata.rel_path,
            )
        )
    return sorted(rows, key=lambda row: row.number)


def status_counts(rows: list[RegistryRow]) -> dict[str, int]:
    counts = {status: 0 for status in sorted(VALID_STATUSES)}
    for row in rows:
        counts[row.status] = counts.get(row.status, 0) + 1
    return counts


def max_task_number(rows: list[RegistryRow]) -> int:
    return max((row.number for row in rows), default=0)


def render_registry(rows: list[RegistryRow]) -> str:
    max_number = max_task_number(rows)
    counts = status_counts(rows)
    body = "\n".join(row.to_markdown() for row in rows)
    text = f"""# 改善タスク課題一覧

改善タスクの縮小索引。各タスクの詳細正本は個別 `TSK-***.md` および `解決済み/TSK-***.md` を参照すること。

旧11列詳細表は履歴参照用として [`{ARCHIVE_NAME}`]({ARCHIVE_NAME}) に退避する。

## 集計

- 最大TSK番号: TSK-{max_number:03d}
- 保留: {counts.get('保留', 0)}
- 未解決: {counts.get('未解決', 0)}
- 確認待ち: {counts.get('確認待ち', 0)}
- 解決中: {counts.get('解決中', 0)}
- 解決済み: {counts.get('解決済み', 0)}

## 個別TSK索引

| 管理ID | 状態 | 件名 | 個別ファイル |
|---|---|---|---|
{body}
"""
    return normalize_text(text)


def print_summary(rows: list[RegistryRow]) -> None:
    max_number = max_task_number(rows)
    counts = status_counts(rows)
    print(f"[INFO] generated registry rows: {len(rows)}")
    print(f"[INFO] max TSK number: TSK-{max_number:03d}" if max_number else "[INFO] max TSK number: none")
    for status in sorted(counts):
        print(f"[INFO] status {status}: {counts[status]}")


def report_registry_diff(current: str, generated: str) -> None:
    current_rows = parse_registry_text(current)
    generated_rows = parse_registry_text(generated)

    current_ids = set(current_rows)
    generated_ids = set(generated_rows)
    missing_ids = sorted(current_ids - generated_ids)
    extra_ids = sorted(generated_ids - current_ids)

    if missing_ids:
        print(f"[DIFF] rows missing from generated registry: {', '.join(missing_ids)}")
    if extra_ids:
        print(f"[DIFF] rows only in generated registry: {', '.join(extra_ids)}")

    common_ids = sorted(current_ids & generated_ids, key=lambda task_id: int(task_id.split('-')[1]))
    row_mismatch_found = False
    for task_id in common_ids:
        current_cells = current_rows[task_id].cells()
        generated_cells = generated_rows[task_id].cells()
        diffs = []
        for index, (current_cell, generated_cell) in enumerate(zip(current_cells, generated_cells)):
            if current_cell != generated_cell:
                column = COMPACT_COLUMNS[index]
                diffs.append((column, current_cell, generated_cell))
        if diffs:
            row_mismatch_found = True
            print(f"[DIFF] row mismatch: {task_id}")
            for column, current_cell, generated_cell in diffs:
                print(f"  - {column}: committed={current_cell!r} generated={generated_cell!r}")

    if not missing_ids and not extra_ids and not row_mismatch_found:
        current_lines = normalize_text(current).splitlines()
        generated_lines = normalize_text(generated).splitlines()
        for line_no, (current_line, generated_line) in enumerate(zip(current_lines, generated_lines), start=1):
            if current_line != generated_line:
                print(f"[DIFF] non-row text mismatch at line {line_no}")
                print(f"  committed={current_line!r}")
                print(f"  generated ={generated_line!r}")
                return
        if len(current_lines) != len(generated_lines):
            print(f"[DIFF] line count mismatch: committed={len(current_lines)} generated={len(generated_lines)}")


def check_registry(registry_path: Path, generated: str) -> int:
    if not registry_path.exists():
        print(f"[FAIL] registry not found: {registry_path}")
        return 2
    current = normalize_text(read_text(registry_path))
    if current == generated:
        print("[PASS] generated TSK registry matches committed registry.")
        return 0
    print("[FAIL] generated TSK registry differs from committed registry.")
    report_registry_diff(current, generated)
    print("[INFO] Run with --mode generate --output <path> to inspect the generated registry.")
    return 1


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = args.root.resolve()
    task_dir = root / TASK_DIR
    registry_path = task_dir / REGISTRY_NAME

    if not task_dir.exists():
        print(f"[FAIL] task directory not found: {task_dir}", file=sys.stderr)
        return 2

    try:
        rows = generate_rows(task_dir)
        generated = render_registry(rows)
    except ValueError as exc:
        print(f"[FAIL] {exc}", file=sys.stderr)
        return 1

    if args.mode == "summary":
        print_summary(rows)
        return 0

    if args.mode == "check":
        print_summary(rows)
        return check_registry(registry_path, generated)

    if args.write and args.output is not None:
        print("[FAIL] --write and --output cannot be used together", file=sys.stderr)
        return 2
    if args.write:
        registry_path.write_text(generated, encoding="utf-8")
        print(f"[OK] wrote registry: {registry_path}")
        return 0
    if args.output is not None:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(generated, encoding="utf-8")
        print(f"[OK] wrote generated registry: {args.output}")
        return 0

    print(generated, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
