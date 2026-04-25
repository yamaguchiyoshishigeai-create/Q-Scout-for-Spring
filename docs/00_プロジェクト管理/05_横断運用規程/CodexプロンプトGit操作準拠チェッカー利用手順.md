# CodexプロンプトGit操作準拠チェッカー利用手順

## 1. 目的

本書は、`scripts/check_codex_prompt_git_safety.py` の利用手順を定義する。

このチェッカーは、Codex向けプロンプトに、`CodexPrompt_GitScriptEscalation_Rules.md` に反する直接Git書き込み指示や、権限昇格再実行を誘発する表現が含まれていないかを静的検査する。

## 2. 対象ファイル

- Codexへ投入予定のプロンプトファイル
- `CodexExec.md`
- ChatGPTが作成したCodex指示書
- Codex向け作業手順案

## 3. 基本コマンド

リポジトリルートで以下を実行する。

    python scripts/check_codex_prompt_git_safety.py CodexExec.md

安全なプロンプトの場合は以下のように出力される。

    [PASS] Codex prompt Git safety check passed.

不適合候補がある場合は、以下を含む `FAIL` が出力される。

- 行番号
- ルールID
- 検出語
- 理由
- 修正方針

## 4. 検査対象

主な検査対象は以下である。

- `git checkout -b`
- `git switch -c`
- `git add`
- `git commit`
- `git push`
- `git merge`
- `git rebase`
- `git reset`
- `gh pr create`
- `.git` 直接操作
- 同一Git操作の権限昇格再実行を促す表現
- `CodexPrompt_GitScriptEscalation_Rules.md` の読込・適用指示不足
- Git書き込み操作を通常コマンドで直接実行しない旨の不足
- `codex_start_branch` / `codex_finish_pr` 等のスクリプトゲート指示不足

## 5. スクリプト雛形を含むプロンプトの扱い

Codexプロンプト内に `codex_start_branch.ps1`、`codex_start_branch.sh`、`codex_finish_pr.ps1`、`codex_finish_pr.sh` の雛形を含む場合、雛形内部の `git add` や `git push` が検出されることがある。

その場合は、以下のオプションを使用する。

    python scripts/check_codex_prompt_git_safety.py CodexExec.md --allow-script-blocks

このオプションは、`codex_*` スクリプト雛形の近傍にあるGit書き込み操作を検出対象から除外する。

ただし、プロンプト本文で直接Git書き込みを指示していないかは別途確認する。

## 6. 欠落ポリシーの警告扱い

初期検証や途中作成中のプロンプトでは、必須ポリシー不足を警告扱いにしたい場合がある。

その場合は以下を使用する。

    python scripts/check_codex_prompt_git_safety.py CodexExec.md --warn-only-missing-policy

この場合でも、直接Git書き込み指示は `FAIL` として扱う。

## 7. 運用タイミング

以下のタイミングで実行する。

- ChatGPTがCodex向けプロンプトを作成した直後
- Codexへ投入する前
- Git書き込みやPR作成を含む作業指示を作成したとき
- Codexプロンプト作成ルールを変更したとき
- 過去プロンプトを再利用するとき

## 8. 注意事項

このチェッカーは静的検査であり、文脈判断を完全には代替しない。

以下は人間またはAIレビューを残す。

- Git読み取り操作と書き込み操作の文脈上の区別
- スクリプト雛形内の操作が安全要件を満たしているか
- Codexへ渡す作業単位が適切か
- 例外運用の妥当性

## 9. 関連文書

- `docs/00_プロジェクト管理/05_横断運用規程/CodexPrompt_GitScriptEscalation_Rules.md`
- `docs/00_プロジェクト管理/05_横断運用規程/Codex連携運用ルール.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/TSK-031.md`
