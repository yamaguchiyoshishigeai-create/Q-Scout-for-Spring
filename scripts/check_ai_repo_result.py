#!/usr/bin/env python3
"""AI repository work evidence checker.

This script validates whether a text or Markdown file contains the evidence
items required by AIリポジトリ作業証跡管理ルール.md.

It supports two formats:
- Full AI_REPO_RESULT block
- Short commit-message style records: ChatGPT-Repo-Result / Codex-Result

Usage:
    python scripts/check_ai_repo_result.py pr_body.md
    python scripts/check_ai_repo_result.py commit_message.txt --format short
    python scripts/check_ai_repo_result.py pr_body.md --allow-missing-unverified
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Finding:
    severity: str
    rule_id: str
    message: str
    suggestion: str


FULL_REQUIRED_FIELDS: tuple[str, ...] = (
    "ACTOR",
    "TASK_ID",
    "TITLE",
    "STATUS",
    "changed files",
    "summary",
    "verification",
    "unverified",
    "commit",
    "push",
    "pr",
    "pr url",
)

SHORT_REQUIRED_FIELDS: tuple[str, ...] = (
    "TASK_ID",
    "STATUS",
    "changed",
    "verification",
    "details",
)

FULL_BLOCK_PATTERN = re.compile(
    r"=====\s*AI_REPO_RESULT_BEGIN\s*=====(?P<body>.*?)=====\s*AI_REPO_RESULT_END\s*=====",
    re.IGNORECASE | re.DOTALL,
)

SHORT_BLOCK_PATTERN = re.compile(
    r"(?P<header>ChatGPT-Repo-Result|Codex-Result)\s*:\s*(?P<body>.*)",
    re.IGNORECASE | re.DOTALL,
)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Check AI_REPO_RESULT or short AI repository evidence text."
    )
    parser.add_argument("target_file", type=Path, help="Path to PR body, PR comment, or commit message file.")
    parser.add_argument(
        "--format",
        choices=("auto", "full", "short"),
        default="auto",
        help="Evidence format to validate. Default: auto.",
    )
    parser.add_argument(
        "--allow-missing-unverified",
        action="store_true",
        help="Do not fail when only the unverified field is missing in a full block.",
    )
    return parser.parse_args(argv)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8-sig")


def field_pattern(field_name: str) -> re.Pattern[str]:
    escaped = re.escape(field_name)
    return re.compile(rf"(?im)^\s*-?\s*{escaped}\s*:\s*(?:\S|$)")


def section_pattern(section_name: str) -> re.Pattern[str]:
    escaped = re.escape(section_name)
    return re.compile(rf"(?im)^\s*{escaped}\s*:\s*$")


def has_full_field(block_body: str, field_name: str) -> bool:
    if field_name in {"changed files", "summary", "verification", "unverified", "commit", "push", "pr", "pr url"}:
        return bool(section_pattern(field_name).search(block_body))
    return bool(field_pattern(field_name).search(block_body))


def has_short_field(block_body: str, field_name: str) -> bool:
    return bool(field_pattern(field_name).search(block_body))


def validate_full(text: str, allow_missing_unverified: bool) -> list[Finding]:
    findings: list[Finding] = []
    match = FULL_BLOCK_PATTERN.search(text)
    if not match:
        return [
            Finding(
                severity="FAIL",
                rule_id="FULL_BLOCK_MISSING",
                message="AI_REPO_RESULT block markers are missing.",
                suggestion="Add AI_REPO_RESULT_BEGIN and AI_REPO_RESULT_END markers with required evidence fields.",
            )
        ]

    block_body = match.group("body")
    for field_name in FULL_REQUIRED_FIELDS:
        if allow_missing_unverified and field_name == "unverified":
            continue
        if not has_full_field(block_body, field_name):
            findings.append(
                Finding(
                    severity="FAIL",
                    rule_id=f"FULL_FIELD_MISSING:{field_name}",
                    message=f"Required full evidence field is missing: {field_name}",
                    suggestion=f"Add '{field_name}:' to the AI_REPO_RESULT block.",
                )
            )
    return findings


def validate_short(text: str) -> list[Finding]:
    findings: list[Finding] = []
    match = SHORT_BLOCK_PATTERN.search(text)
    if not match:
        return [
            Finding(
                severity="FAIL",
                rule_id="SHORT_BLOCK_MISSING",
                message="Short evidence block header is missing.",
                suggestion="Add 'ChatGPT-Repo-Result:' or 'Codex-Result:' with required short fields.",
            )
        ]

    block_body = match.group("body")
    for field_name in SHORT_REQUIRED_FIELDS:
        if not has_short_field(block_body, field_name):
            findings.append(
                Finding(
                    severity="FAIL",
                    rule_id=f"SHORT_FIELD_MISSING:{field_name}",
                    message=f"Required short evidence field is missing: {field_name}",
                    suggestion=f"Add '- {field_name}: ...' to the short evidence block.",
                )
            )
    return findings


def detect_format(text: str) -> str:
    if FULL_BLOCK_PATTERN.search(text):
        return "full"
    if SHORT_BLOCK_PATTERN.search(text):
        return "short"
    return "full"


def print_findings(findings: list[Finding], evidence_format: str) -> None:
    if not findings:
        print(f"[PASS] AI repository evidence check passed. format={evidence_format}")
        return

    fail_count = sum(1 for finding in findings if finding.severity == "FAIL")
    warn_count = sum(1 for finding in findings if finding.severity == "WARN")
    print(f"[FAIL] AI repository evidence check found {fail_count} failure(s), {warn_count} warning(s). format={evidence_format}")
    for finding in findings:
        print(f"\n[{finding.severity}] {finding.rule_id}")
        print(f"  reason: {finding.message}")
        print(f"  fix   : {finding.suggestion}")


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    if not args.target_file.exists():
        print(f"[FAIL] file not found: {args.target_file}", file=sys.stderr)
        return 2
    if not args.target_file.is_file():
        print(f"[FAIL] not a file: {args.target_file}", file=sys.stderr)
        return 2

    text = read_text(args.target_file)
    evidence_format = detect_format(text) if args.format == "auto" else args.format

    if evidence_format == "full":
        findings = validate_full(text, args.allow_missing_unverified)
    else:
        findings = validate_short(text)

    print_findings(findings, evidence_format)
    return 1 if any(f.severity == "FAIL" for f in findings) else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
