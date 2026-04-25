#!/usr/bin/env python3
"""Generate or check the TSK registry from individual TSK files.

This script is the first implementation for TSK-043. It treats individual
TSK-***.md files, including solved files under 解決済み/, as the source for
core task metadata and can generate 改善タスク課題一覧.md from those files.

Current scope:
- read individual TSK files
- extract core metadata from the file title and leading '- key: value' lines
- preserve overview / done condition / evidence columns from the existing
  registry when available, because those columns are not yet normalized in all
  individual files
- generate a normalized Markdown registry table
- check whether the committed registry already matches the generated output

Usage:
    python scripts/generate_tsk_registry.py --mode summary
    python scripts/generate_tsk_registry.py --mode check
    python scripts/generate_tsk_registry.py --mode generate --output /tmp/改善タスク課題一覧.generated.md
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
SOLVED_DIR_NAME = "解決済み"
SOLVED_STATUS = "解決済み"
VALID_ACTIVE_STATUSES = {"未解決", "解決中", "確認待ち", "保留"}
VALID_STATUSES = VALID_ACTIVE_STATUSES | {SOLVED_STATUS}

HEADER = """# 改善タスク課題一覧

改善タスクの俯瞰用一覧正本。各タスクの詳細は個別ファイルを参照すること。

| 管理ID | 区分 | 件名 | 状態 | 優先度 | 実施主体 | 関連箇所 | 概要 | 完了条件 | 根拠 / 関連PR / 備考 | 個別ファイル |
|---|---|---|---|---|---|---|---|---|---|---|
"""

ROW_ID_RE = re.compile(r"^TSK-(?P<num>\d{3})$")
TITLE_RE = re.compile(r"^#\s+(?P<task_id>TSK-\d{3})\s+(?P<title>.+?)\s*$", re.MULTILINE)
META_RE = re.compile(r"^-\s*(?P<key>[^:：]+)\s*[:：]\s*(?P<value>.+?)\s*$", re.MULTILINE)
LINK_RE = re.compile(r"\[(?P<label>[^\]]+)\]\((?P<path>[^)]+)\)")


@dataclass(frozen=True)
class RegistryRow:
    task_id: str
    category: str
    title: str
    status: str
    priority: str
    actor: str
    related_area: str
    summary: str
    done_condition: str
    evidence: str
    link_path: str

    @property
    def number(self) -> int:
        match = ROW_ID_RE.match(self.task_id)
        return int(match.group("num")) if match else 0

    def to_markdown(self) -> str:
        return (
            f"| {self.task_id} | {self.category} | {self.title} | {self.status} | "
            f"{self.priority} | {self.actor} | {self.related_area} | {self.summary} | "
            f"{self.done_condition} | {self.evidence} | [{self.task_id}]({self.link_path}) |"
        )


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate or check the TSK registry from individual TSK files.")
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


def parse_existing_registry(registry_path: Path) -> dict[str, RegistryRow]:
    if not registry_path.exists():
        return {}
    rows: dict[str, RegistryRow] = {}
    for line in read_text(registry_path).splitlines():
        cells = split_markdown_row(line)
        if len(cells) < 11:
            continue
        task_id = cells[0]
        if not ROW_ID_RE.match(task_id):
            continue
        link_match = LINK_RE.search(cells[10])
        link_path = link_match.group("path") if link_match else expected_link_path(task_id, cells[3])
        rows[task_id] = RegistryRow(
            task_id=task_id,
            category=cells[1],
            title=cells[2],
            status=cells[3],
            priority=cells[4],
            actor=cells[5],
            related_area=cells[6],
            summary=cells[7],
            done_condition=cells[8],
            evidence=cells[9],
            link_path=link_path,
        )
    return rows


def expected_link_path(task_id: str, status: str) -> str:
    if status == SOLVED_STATUS:
        return f"{SOLVED_DIR_NAME}/{task_id}.md"
    return f"{task_id}.md"


def parse_metadata(text: str) -> dict[str, str]:
    metadata: dict[str, str] = {}
    for match in META_RE.finditer(text):
        metadata[match.group("key").strip()] = match.group("value").strip()
    return metadata


def parse_task_file(path: Path, task_dir: Path, existing: RegistryRow | None) -> RegistryRow:
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

    return RegistryRow(
        task_id=task_id,
        category=metadata.get("区分", existing.category if existing else ""),
        title=metadata.get("件名", title),
        status=status,
        priority=metadata.get("優先度", existing.priority if existing else ""),
        actor=metadata.get("実施主体", existing.actor if existing else ""),
        related_area=metadata.get("関連箇所", existing.related_area if existing else ""),
        summary=existing.summary if existing else "",
        done_condition=existing.done_condition if existing else "",
        evidence=existing.evidence if existing else "",
        link_path=rel_path,
    )


def collect_task_paths(task_dir: Path) -> list[Path]:
    paths = list(task_dir.glob("TSK-*.md"))
    solved_dir = task_dir / SOLVED_DIR_NAME
    if solved_dir.exists():
        paths.extend(solved_dir.glob("TSK-*.md"))
    return sorted(paths, key=lambda path: path.name)


def generate_rows(task_dir: Path, existing_rows: dict[str, RegistryRow]) -> list[RegistryRow]:
    rows: list[RegistryRow] = []
    for path in collect_task_paths(task_dir):
        task_id = path.stem
        rows.append(parse_task_file(path, task_dir, existing_rows.get(task_id)))
    return sorted(rows, key=lambda row: row.number)


def render_registry(rows: list[RegistryRow]) -> str:
    body = "\n".join(row.to_markdown() for row in rows)
    return normalize_text(HEADER + body + "\n")


def print_summary(rows: list[RegistryRow]) -> None:
    max_number = max((row.number for row in rows), default=0)
    status_counts = {status: 0 for status in sorted(VALID_STATUSES)}
    for row in rows:
        status_counts[row.status] = status_counts.get(row.status, 0) + 1
    print(f"[INFO] generated registry rows: {len(rows)}")
    print(f"[INFO] max TSK number: TSK-{max_number:03d}" if max_number else "[INFO] max TSK number: none")
    for status in sorted(status_counts):
        print(f"[INFO] status {status}: {status_counts[status]}")


def check_registry(registry_path: Path, generated: str) -> int:
    if not registry_path.exists():
        print(f"[FAIL] registry not found: {registry_path}")
        return 2
    current = normalize_text(read_text(registry_path))
    if current == generated:
        print("[PASS] generated TSK registry matches committed registry.")
        return 0
    print("[FAIL] generated TSK registry differs from committed registry.")
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
        existing = parse_existing_registry(registry_path)
        rows = generate_rows(task_dir, existing)
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
