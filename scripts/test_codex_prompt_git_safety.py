#!/usr/bin/env python3
"""Regression tests for check_codex_prompt_git_safety.py.

The tests create temporary Codex prompt files and execute the checker as a
subprocess. They cover safe prompts, missing policy text, direct Git write
instructions, elevation retry false positives, and script-block handling.

Usage:
    python scripts/test_codex_prompt_git_safety.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CHECKER = REPO_ROOT / "scripts" / "check_codex_prompt_git_safety.py"

GIT_WORD = "g" + "it"
GH_WORD = "g" + "h"
DOT_GIT = "." + "git"

SAFE_PROMPT = """
# Safe Codex Prompt

Codex連携運用ルール.md を読み、関連ルールに従うこと。
特に CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。

Git書き込み操作を通常コマンドで直接実行しないでください。
通常実行で許可するのは、読み取り専用操作だけです。
これらが必要な場合は、codex_start_branch または codex_finish_pr の専用スクリプトへ切り出してください。
通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。
""".strip()

MISSING_POLICY_PROMPT = """
# Incomplete Prompt

対象ファイルを確認し、必要な差分案を作成してください。
""".strip()

DIRECT_WRITE_PROMPT = f"""
# Unsafe Prompt

CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。
Git書き込み操作を通常コマンドで直接実行しないでください。
必要な場合は、codex_start_branch または codex_finish_pr を使ってください。
通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。

次に {GIT_WORD} add . を実行してください。
""".strip()

DIRECT_PR_CREATE_PROMPT = f"""
# Unsafe PR Prompt

CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。
Git書き込み操作を通常コマンドで直接実行しないでください。
必要な場合は、codex_start_branch または codex_finish_pr を使ってください。
通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。

最後に {GH_WORD} pr create を実行してください。
""".strip()

DIRECT_DOT_GIT_PROMPT = f"""
# Unsafe dot repository prompt

CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。
Git書き込み操作を通常コマンドで直接実行しないでください。
必要な場合は、codex_start_branch または codex_finish_pr を使ってください。
通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。

{DOT_GIT}/index.lock を直接確認して処理してください。
""".strip()

ELEVATION_ALLOW_PROMPT = """
# Unsafe elevation prompt

CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。
Git書き込み操作を通常コマンドで直接実行しないでください。
必要な場合は、codex_start_branch または codex_finish_pr を使ってください。

失敗した同一コマンドを権限昇格で再実行してください。
""".strip()

SCRIPT_BLOCK_PROMPT = f"""
# Prompt with script block

CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。
Git書き込み操作を通常コマンドで直接実行しないでください。
必要な場合は、codex_start_branch または codex_finish_pr を使ってください。
通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。

codex_finish_pr.sh:
    {GIT_WORD} add .
    {GIT_WORD} commit -m "docs: update"
    {GIT_WORD} push origin HEAD
""".strip()


@dataclass(frozen=True)
class Case:
    name: str
    content: str
    args: tuple[str, ...]
    expected_returncode: int
    expected_substrings: tuple[str, ...]


CASES: tuple[Case, ...] = (
    Case(
        name="safe_prompt_passes",
        content=SAFE_PROMPT,
        args=(),
        expected_returncode=0,
        expected_substrings=("[PASS]",),
    ),
    Case(
        name="missing_policy_fails",
        content=MISSING_POLICY_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("READ_RULES_FILE", "NO_DIRECT_GIT_WRITES", "USE_SCRIPT_GATE", "NO_ELEVATION_RETRY"),
    ),
    Case(
        name="missing_policy_warn_only_passes_with_warnings",
        content=MISSING_POLICY_PROMPT,
        args=("--warn-only-missing-policy",),
        expected_returncode=0,
        expected_substrings=("[FAIL]", "warning"),
    ),
    Case(
        name="direct_write_fails",
        content=DIRECT_WRITE_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("GIT_ADD_COMMIT_PUSH",),
    ),
    Case(
        name="direct_pr_create_fails",
        content=DIRECT_PR_CREATE_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("GITHUB_PR_CREATE",),
    ),
    Case(
        name="dot_git_access_fails",
        content=DIRECT_DOT_GIT_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("DOT_GIT_DIRECT_ACCESS",),
    ),
    Case(
        name="elevation_allowance_fails",
        content=ELEVATION_ALLOW_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("ELEVATION_RETRY",),
    ),
    Case(
        name="script_block_fails_without_allow_option",
        content=SCRIPT_BLOCK_PROMPT,
        args=(),
        expected_returncode=1,
        expected_substrings=("GIT_ADD_COMMIT_PUSH",),
    ),
    Case(
        name="script_block_passes_with_allow_option",
        content=SCRIPT_BLOCK_PROMPT,
        args=("--allow-script-blocks",),
        expected_returncode=0,
        expected_substrings=("[PASS]",),
    ),
)


def run_case(case: Case, temp_dir: Path) -> bool:
    prompt_file = temp_dir / f"{case.name}.md"
    prompt_file.write_text(case.content, encoding="utf-8")

    command = [sys.executable, str(CHECKER), str(prompt_file), *case.args]
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

    with tempfile.TemporaryDirectory(prefix="codex_prompt_git_safety_tests_") as tmp:
        temp_dir = Path(tmp)
        results = [run_case(case, temp_dir) for case in CASES]

    passed = sum(1 for result in results if result)
    total = len(results)
    if passed == total:
        print(f"[PASS] all regression cases passed: {passed}/{total}")
        return 0

    print(f"[FAIL] regression cases failed: {passed}/{total} passed")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
