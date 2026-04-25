#!/usr/bin/env python3
"""Codex prompt Git-operation safety checker.

This script performs a lightweight static check for Codex prompts.
It flags direct Git write operations that should be routed through
codex_start_branch/codex_finish_pr scripts according to the repository's
CodexPrompt_GitScriptEscalation_Rules.md.

Usage:
    python scripts/check_codex_prompt_git_safety.py CodexExec.md
    python scripts/check_codex_prompt_git_safety.py prompt.md --allow-script-blocks
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
    line_no: int
    rule_id: str
    matched: str
    message: str
    suggestion: str


@dataclass(frozen=True)
class Rule:
    rule_id: str
    pattern: re.Pattern[str]
    message: str
    suggestion: str
    severity: str = "FAIL"


DIRECT_GIT_WRITE_RULES: tuple[Rule, ...] = (
    Rule(
        "GIT_BRANCH_WRITE",
        re.compile(r"\bgit\s+(?:branch|checkout\s+-b|switch\s+-c)\b", re.IGNORECASE),
        "Git branch creation or branch switching with creation is a Git write operation.",
        "Move branch creation into codex_start_branch.ps1 or codex_start_branch.sh.",
    ),
    Rule(
        "GIT_ADD_COMMIT_PUSH",
        re.compile(r"\bgit\s+(?:add|commit|push)\b", re.IGNORECASE),
        "Git add/commit/push must not be instructed as normal Codex commands.",
        "Move add/commit/push into codex_finish_pr.ps1 or codex_finish_pr.sh.",
    ),
    Rule(
        "GIT_HISTORY_WRITE",
        re.compile(r"\bgit\s+(?:merge|rebase|reset|tag|update-ref|filter-branch)\b", re.IGNORECASE),
        "This Git operation mutates history, refs, or working tree state.",
        "Avoid this operation or isolate the approved safe flow inside a reviewed script.",
    ),
    Rule(
        "GITHUB_PR_CREATE",
        re.compile(r"\bgh\s+pr\s+create\b", re.IGNORECASE),
        "PR creation should not be instructed as a normal Codex command.",
        "Move PR creation into codex_finish_pr.ps1 or codex_finish_pr.sh.",
    ),
    Rule(
        "DOT_GIT_DIRECT_ACCESS",
        re.compile(r"(?:^|[\s'\"`])\.git(?:/|\\|\b)", re.IGNORECASE),
        "Direct .git access is prohibited.",
        "Do not write to or manipulate .git directly.",
    ),
    Rule(
        "ELEVATION_RETRY",
        re.compile(r"(?:権限昇格|権限付き|sudo|管理者権限|再実行).*(?:git|同じ|同一)|(?:git|同じ|同一).*(?:権限昇格|権限付き|sudo|管理者権限|再実行)", re.IGNORECASE),
        "The prompt appears to allow retrying the same Git operation with elevated permissions.",
        "Explicitly prohibit retrying failed Git write commands with elevation; use generated scripts first.",
    ),
)

REQUIRED_POLICY_PATTERNS: tuple[tuple[str, re.Pattern[str], str], ...] = (
    (
        "READ_RULES_FILE",
        re.compile(r"CodexPrompt_GitScriptEscalation_Rules\.md", re.IGNORECASE),
        "Mention and apply CodexPrompt_GitScriptEscalation_Rules.md.",
    ),
    (
        "NO_DIRECT_GIT_WRITES",
        re.compile(r"(?:Git書き込み操作を通常コマンドで直接|通常実行で禁止|直接実行しない|通常コマンド経路で直接実行しない)", re.IGNORECASE),
        "State that Git write operations must not be run as normal Codex commands.",
    ),
    (
        "USE_SCRIPT_GATE",
        re.compile(r"(?:codex_start_branch|codex_finish_pr|スクリプト.*(?:経由|切り出し)|Git操作スクリプト昇格ゲート方式)", re.IGNORECASE),
        "Require codex_start_branch/codex_finish_pr or the Git script escalation gate.",
    ),
    (
        "NO_ELEVATION_RETRY",
        re.compile(r"(?:失敗後|通常実行で失敗|同一コマンド).*?(?:権限付き|権限昇格|再実行).*?(?:禁止|してはいけません|しないでください)|(?:権限付き|権限昇格).*?(?:同一コマンド|同じGit操作).*?(?:禁止|してはいけません|しないでください)", re.IGNORECASE),
        "Prohibit retrying the same failed Git write operation with elevated permissions.",
    ),
)

SCRIPT_BLOCK_HINTS = (
    "codex_start_branch.ps1",
    "codex_start_branch.sh",
    "codex_finish_pr.ps1",
    "codex_finish_pr.sh",
)

PROHIBITION_CONTEXT_PATTERN = re.compile(
    r"(?:禁止|してはいけません|しないでください|試行しない|実行しない|must\s+not|do\s+not|禁止します)",
    re.IGNORECASE,
)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Check Codex prompt text for direct Git write instructions."
    )
    parser.add_argument("prompt_file", type=Path, help="Path to Codex prompt text/Markdown file.")
    parser.add_argument(
        "--allow-script-blocks",
        action="store_true",
        help="Do not flag Git write commands that appear inside codex_* script examples.",
    )
    parser.add_argument(
        "--warn-only-missing-policy",
        action="store_true",
        help="Treat missing required policy text as WARN instead of FAIL.",
    )
    return parser.parse_args(argv)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8-sig")


def is_script_context(lines: list[str], index: int) -> bool:
    start = max(0, index - 20)
    end = min(len(lines), index + 20)
    nearby = "\n".join(lines[start:end])
    return any(hint in nearby for hint in SCRIPT_BLOCK_HINTS)


def is_prohibition_context(line: str) -> bool:
    """Return true when a line clearly prohibits the risky action."""

    return bool(PROHIBITION_CONTEXT_PATTERN.search(line))


def check_direct_git_writes(text: str, allow_script_blocks: bool) -> list[Finding]:
    findings: list[Finding] = []
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if allow_script_blocks and is_script_context(lines, index):
            continue
        for rule in DIRECT_GIT_WRITE_RULES:
            match = rule.pattern.search(line)
            if not match:
                continue
            if rule.rule_id == "ELEVATION_RETRY" and is_prohibition_context(line):
                continue
            findings.append(
                Finding(
                    severity=rule.severity,
                    line_no=index + 1,
                    rule_id=rule.rule_id,
                    matched=match.group(0).strip(),
                    message=rule.message,
                    suggestion=rule.suggestion,
                )
            )
    return findings


def check_required_policy(text: str, warn_only: bool) -> list[Finding]:
    findings: list[Finding] = []
    severity = "WARN" if warn_only else "FAIL"
    for rule_id, pattern, suggestion in REQUIRED_POLICY_PATTERNS:
        if not pattern.search(text):
            findings.append(
                Finding(
                    severity=severity,
                    line_no=0,
                    rule_id=rule_id,
                    matched="<missing>",
                    message="Required Codex Git safety policy text is missing.",
                    suggestion=suggestion,
                )
            )
    return findings


def print_findings(findings: list[Finding]) -> None:
    if not findings:
        print("[PASS] Codex prompt Git safety check passed.")
        return

    fail_count = sum(1 for finding in findings if finding.severity == "FAIL")
    warn_count = sum(1 for finding in findings if finding.severity == "WARN")
    print(f"[FAIL] Codex prompt Git safety check found {fail_count} failure(s), {warn_count} warning(s).")
    for finding in findings:
        location = f"line {finding.line_no}" if finding.line_no else "whole file"
        print(f"\n[{finding.severity}] {finding.rule_id} at {location}")
        print(f"  matched: {finding.matched}")
        print(f"  reason : {finding.message}")
        print(f"  fix    : {finding.suggestion}")


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    if not args.prompt_file.exists():
        print(f"[FAIL] file not found: {args.prompt_file}", file=sys.stderr)
        return 2
    if not args.prompt_file.is_file():
        print(f"[FAIL] not a file: {args.prompt_file}", file=sys.stderr)
        return 2

    text = read_text(args.prompt_file)
    findings = []
    findings.extend(check_direct_git_writes(text, args.allow_script_blocks))
    findings.extend(check_required_policy(text, args.warn_only_missing_policy))
    print_findings(findings)
    return 1 if any(f.severity == "FAIL" for f in findings) else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
