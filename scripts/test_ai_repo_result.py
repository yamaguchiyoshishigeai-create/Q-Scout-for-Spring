#!/usr/bin/env python3
"""Regression tests for check_ai_repo_result.py.

The tests create temporary evidence files and execute the checker as a
subprocess. They cover full AI_REPO_RESULT blocks, missing fields,
short ChatGPT/Codex records, explicit format selection, and optional
unverified handling.

Usage:
    python scripts/test_ai_repo_result.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
CHECKER = REPO_ROOT / "scripts" / "check_ai_repo_result.py"

FULL_OK = """
===== AI_REPO_RESULT_BEGIN =====
ACTOR: ChatGPT(リポジトリ編集)
TASK_ID: TSK-032
TITLE: AI_REPO_RESULT証跡チェッカー初版実装
STATUS: [OK]
changed files:
- scripts/check_ai_repo_result.py

summary:
- 証跡チェッカーを追加

verification:
- 回帰テストを追加

unverified:
- ユーザー確認

commit:
- abcdef0
message:
- add checker
push:
- success
pr:
- PR #00
pr url:
- https://example.invalid/pr/00
===== AI_REPO_RESULT_END =====
""".strip()

FULL_MISSING_VERIFICATION = """
===== AI_REPO_RESULT_BEGIN =====
ACTOR: ChatGPT(リポジトリ編集)
TASK_ID: TSK-032
TITLE: Missing verification sample
STATUS: [OK]
changed files:
- scripts/check_ai_repo_result.py

summary:
- 証跡チェッカーを追加

unverified:
- ユーザー確認

commit:
- abcdef0
push:
- success
pr:
- PR #00
pr url:
- https://example.invalid/pr/00
===== AI_REPO_RESULT_END =====
""".strip()

FULL_MISSING_UNVERIFIED = """
===== AI_REPO_RESULT_BEGIN =====
ACTOR: ChatGPT(リポジトリ編集)
TASK_ID: TSK-032
TITLE: Missing unverified sample
STATUS: [OK]
changed files:
- scripts/check_ai_repo_result.py

summary:
- 証跡チェッカーを追加

verification:
- 回帰テストを追加

commit:
- abcdef0
push:
- success
pr:
- PR #00
pr url:
- https://example.invalid/pr/00
===== AI_REPO_RESULT_END =====
""".strip()

SHORT_CHATGPT_OK = """
ChatGPT-Repo-Result:
- TASK_ID: TSK-032
- STATUS: OK
- changed: scripts/check_ai_repo_result.py
- verification: regression tests added
- details: PR body
""".strip()

SHORT_CODEX_OK = """
Codex-Result:
- TASK_ID: TSK-032
- STATUS: OK
- changed: scripts/check_ai_repo_result.py
- verification: regression tests added
- details: PR body
""".strip()

SHORT_MISSING_DETAILS = """
ChatGPT-Repo-Result:
- TASK_ID: TSK-032
- STATUS: OK
- changed: scripts/check_ai_repo_result.py
- verification: regression tests added
""".strip()

NO_EVIDENCE = """
This PR updates a script.
There is no structured evidence block here.
""".strip()


@dataclass(frozen=True)
class Case:
    name: str
    content: str
    args: tuple[str, ...]
    expected_returncode: int
    expected_substrings: tuple[str, ...]


CASES: tuple[Case, ...] = (
    Case("full_ok_auto_passes", FULL_OK, (), 0, ("[PASS]", "format=full")),
    Case("full_ok_explicit_passes", FULL_OK, ("--format", "full"), 0, ("[PASS]", "format=full")),
    Case("full_missing_verification_fails", FULL_MISSING_VERIFICATION, (), 1, ("FULL_FIELD_MISSING:verification",)),
    Case("full_missing_unverified_fails_by_default", FULL_MISSING_UNVERIFIED, (), 1, ("FULL_FIELD_MISSING:unverified",)),
    Case("full_missing_unverified_can_be_allowed", FULL_MISSING_UNVERIFIED, ("--allow-missing-unverified",), 0, ("[PASS]",)),
    Case("short_chatgpt_ok_passes", SHORT_CHATGPT_OK, (), 0, ("[PASS]", "format=short")),
    Case("short_codex_ok_passes", SHORT_CODEX_OK, (), 0, ("[PASS]", "format=short")),
    Case("short_missing_details_fails", SHORT_MISSING_DETAILS, ("--format", "short"), 1, ("SHORT_FIELD_MISSING:details",)),
    Case("no_evidence_fails", NO_EVIDENCE, (), 1, ("FULL_BLOCK_MISSING",)),
)


def run_case(case: Case, temp_dir: Path) -> bool:
    target_file = temp_dir / f"{case.name}.md"
    target_file.write_text(case.content, encoding="utf-8")

    command = [sys.executable, str(CHECKER), str(target_file), *case.args]
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

    with tempfile.TemporaryDirectory(prefix="ai_repo_result_tests_") as tmp:
        temp_dir = Path(tmp)
        results = [run_case(case, temp_dir) for case in CASES]

    passed = sum(1 for result in results if result)
    total = len(results)
    if passed == total:
        print(f"[PASS] all AI_REPO_RESULT regression cases passed: {passed}/{total}")
        return 0

    print(f"[FAIL] AI_REPO_RESULT regression cases failed: {passed}/{total} passed")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
