#!/usr/bin/env python3
"""TSK registry consistency checker.

This script validates docs/00_プロジェクト管理/02_改善タスク管理/改善タスク課題一覧.md
against individual TSK files.

Checks include:
- duplicate TSK IDs in the compact registry index
- link path existence
- solved tasks link to 解決済み/TSK-***.md
- active tasks link to TSK-***.md
- individual file status matches registry status
- maximum TSK number display

The checker supports both the current compact index format and the legacy
11-column detailed table format while the repository is migrating.

Usage:
    python scripts/check_tsk_registry.py
    python scripts/check_tsk_registry.py --root .
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
VALID_ACTIVE_STATUSES = {"未解決", "解決中", "確認待ち", "保留"}
SOLVED_STATUS = "解決済み"
VALID_STATUSES = VALID_ACTIVE_STATUSES | {SOLVED_STATUS}

ROW_ID_RE = re.compile(r"^TSK-(?P<num>\d{3})$")
LINK_RE = re.compile(r"\[(?P<label>[^\]]+)\]\((?P<path>[^)]+)\)")
STATUS_RE = re.compile(r"^-\s*状態\s*:\s*(?P<status>.+?)\s*$", re.MULTILINE)


@dataclass(frozen=True)
class RegistryRow:
    line_number: int
    task_id: str
    status: str
    link_label: str
    link_path: str


@dataclass(frozen=True)
class Finding:
    severity: str
    rule_id: str
    message: str
    suggestion: str


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Check TSK registry consistency.")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="Repository root. Default: current directory.",
    )
    return parser.parse_args(argv)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8-sig")


def split_markdown_row(line: str) -> list[str]:
    stripped = line.strip()
    if not stripped.startswith("|") or not stripped.endswith("|"):
        return []
    return [cell.strip() for cell in stripped.strip("|").split("|")]


def row_status_and_link(cells: list[str]) -> tuple[str, str] | None:
    if len(cells) == 4:
        # Compact index: 管理ID / 状態 / 件名 / 個別ファイル
        return cells[1], cells[3]
    if len(cells) >= 11:
        # Legacy detailed table: 管理ID / 区分 / 件名 / 状態 / ... / 個別ファイル
        return cells[3], cells[-1]
    return None


def parse_registry(registry_path: Path) -> tuple[list[RegistryRow], list[Finding]]:
    findings: list[Finding] = []
    rows: list[RegistryRow] = []
    lines = read_text(registry_path).splitlines()

    for line_number, line in enumerate(lines, start=1):
        cells = split_markdown_row(line)
        if len(cells) < 4:
            continue
        task_id = cells[0]
        if not ROW_ID_RE.match(task_id):
            continue
        parsed = row_status_and_link(cells)
        if parsed is None:
            findings.append(
                Finding(
                    "FAIL",
                    "REGISTRY_ROW_FORMAT_UNSUPPORTED",
                    f"{task_id} row has unsupported column count. line={line_number} columns={len(cells)}",
                    "Use the compact 4-column index or the legacy 11-column detailed table format.",
                )
            )
            continue
        status, link_cell = parsed
        link_match = LINK_RE.search(link_cell)
        if not link_match:
            findings.append(
                Finding(
                    "FAIL",
                    "REGISTRY_LINK_MISSING",
                    f"{task_id} row does not contain a Markdown link in the individual file column. line={line_number}",
                    "Add a Markdown link such as [TSK-000](TSK-000.md) or [TSK-000](解決済み/TSK-000.md).",
                )
            )
            continue
        rows.append(
            RegistryRow(
                line_number=line_number,
                task_id=task_id,
                status=status,
                link_label=link_match.group("label"),
                link_path=link_match.group("path"),
            )
        )
    return rows, findings


def read_individual_status(path: Path) -> str | None:
    if not path.exists() or not path.is_file():
        return None
    text = read_text(path)
    match = STATUS_RE.search(text)
    if not match:
        return None
    return match.group("status").strip()


def validate_rows(task_dir: Path, rows: list[RegistryRow]) -> list[Finding]:
    findings: list[Finding] = []
    seen: dict[str, int] = {}

    for row in rows:
        if row.task_id in seen:
            findings.append(
                Finding(
                    "FAIL",
                    "DUPLICATE_TASK_ID",
                    f"{row.task_id} appears more than once. first_line={seen[row.task_id]} duplicate_line={row.line_number}",
                    "Keep only one registry row for each TSK ID.",
                )
            )
        else:
            seen[row.task_id] = row.line_number

        if row.status not in VALID_STATUSES:
            findings.append(
                Finding(
                    "FAIL",
                    "UNKNOWN_STATUS",
                    f"{row.task_id} has unknown status in registry: {row.status}",
                    f"Use one of: {', '.join(sorted(VALID_STATUSES))}.",
                )
            )

        if row.link_label != row.task_id:
            findings.append(
                Finding(
                    "FAIL",
                    "LINK_LABEL_MISMATCH",
                    f"{row.task_id} link label is {row.link_label}.",
                    f"Set the link label to [{row.task_id}].",
                )
            )

        expected_active_path = f"{row.task_id}.md"
        expected_solved_path = f"{SOLVED_DIR_NAME}/{row.task_id}.md"
        if row.status == SOLVED_STATUS and row.link_path != expected_solved_path:
            findings.append(
                Finding(
                    "FAIL",
                    "SOLVED_LINK_PATH_MISMATCH",
                    f"{row.task_id} is 解決済み but links to {row.link_path}.",
                    f"Use {expected_solved_path} for solved tasks.",
                )
            )
        if row.status in VALID_ACTIVE_STATUSES and row.link_path != expected_active_path:
            findings.append(
                Finding(
                    "FAIL",
                    "ACTIVE_LINK_PATH_MISMATCH",
                    f"{row.task_id} is {row.status} but links to {row.link_path}.",
                    f"Use {expected_active_path} for non-solved tasks.",
                )
            )

        linked_file = task_dir / row.link_path
        if not linked_file.exists():
            findings.append(
                Finding(
                    "FAIL",
                    "LINK_TARGET_MISSING",
                    f"{row.task_id} link target does not exist: {row.link_path}",
                    "Create the individual TSK file or fix the registry link path.",
                )
            )
            continue

        individual_status = read_individual_status(linked_file)
        if individual_status is None:
            findings.append(
                Finding(
                    "FAIL",
                    "INDIVIDUAL_STATUS_MISSING",
                    f"{row.task_id} individual file does not contain '- 状態: ...'. path={row.link_path}",
                    "Add a '- 状態: <status>' line to the individual TSK file.",
                )
            )
            continue
        if individual_status != row.status:
            findings.append(
                Finding(
                    "FAIL",
                    "STATUS_MISMATCH",
                    f"{row.task_id} status mismatch. registry={row.status} individual={individual_status} path={row.link_path}",
                    "Make the registry status and individual file status match.",
                )
            )

    return findings


def validate_unlisted_files(task_dir: Path, rows: list[RegistryRow]) -> list[Finding]:
    findings: list[Finding] = []
    listed_paths = {row.link_path for row in rows}
    expected_file_pattern = re.compile(r"^TSK-\d{3}\.md$")

    for path in sorted(task_dir.glob("TSK-*.md")):
        if expected_file_pattern.match(path.name) and path.name not in listed_paths:
            findings.append(
                Finding(
                    "FAIL",
                    "INDIVIDUAL_FILE_UNLISTED",
                    f"Individual task file is not listed in registry: {path.relative_to(task_dir)}",
                    "Add a registry row for the file or move/remove the stale file.",
                )
            )

    solved_dir = task_dir / SOLVED_DIR_NAME
    if solved_dir.exists():
        for path in sorted(solved_dir.glob("TSK-*.md")):
            rel = f"{SOLVED_DIR_NAME}/{path.name}"
            if expected_file_pattern.match(path.name) and rel not in listed_paths:
                findings.append(
                    Finding(
                        "FAIL",
                        "SOLVED_FILE_UNLISTED",
                        f"Solved task file is not listed in registry: {rel}",
                        "Add a registry row for the solved file or remove the stale file.",
                    )
                )
    return findings


def max_task_number(rows: list[RegistryRow]) -> int | None:
    numbers: list[int] = []
    for row in rows:
        match = ROW_ID_RE.match(row.task_id)
        if match:
            numbers.append(int(match.group("num")))
    return max(numbers) if numbers else None


def print_findings(findings: list[Finding], rows: list[RegistryRow]) -> None:
    max_number = max_task_number(rows)
    max_text = f"TSK-{max_number:03d}" if max_number is not None else "none"
    print(f"[INFO] registry rows: {len(rows)}")
    print(f"[INFO] max TSK number: {max_text}")

    if not findings:
        print("[PASS] TSK registry consistency check passed.")
        return

    fail_count = sum(1 for finding in findings if finding.severity == "FAIL")
    warn_count = sum(1 for finding in findings if finding.severity == "WARN")
    print(f"[FAIL] TSK registry consistency check found {fail_count} failure(s), {warn_count} warning(s).")
    for finding in findings:
        print(f"\n[{finding.severity}] {finding.rule_id}")
        print(f"  reason: {finding.message}")
        print(f"  fix   : {finding.suggestion}")


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = args.root.resolve()
    task_dir = root / TASK_DIR
    registry_path = task_dir / REGISTRY_NAME

    if not registry_path.exists():
        print(f"[FAIL] registry not found: {registry_path}", file=sys.stderr)
        return 2

    rows, findings = parse_registry(registry_path)
    findings.extend(validate_rows(task_dir, rows))
    findings.extend(validate_unlisted_files(task_dir, rows))
    print_findings(findings, rows)
    return 1 if any(f.severity == "FAIL" for f in findings) else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
