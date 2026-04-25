# サンプル Codex安全プロンプト

## 1. 目的

このサンプルは、`scripts/check_codex_prompt_git_safety.py` のPASS確認用に使える最小プロンプトである。

## 2. 参照ルール

Codex連携運用ルール.md を読み、そこに記載された関連ルールに従うこと。
特に CodexPrompt_GitScriptEscalation_Rules.md を読み、適用すること。

## 3. Git操作ポリシー

Git書き込み操作を通常コマンドで直接実行しないでください。
通常実行で許可するのは、`git status`、`git diff`、`git log --oneline -n 5` などの読み取り専用操作だけです。

通常実行で禁止する作業は、branch作成、commit、push、PR作成などのGit書き込み操作です。
これらが必要な場合は、codex_start_branch または codex_finish_pr の専用スクリプトへ切り出してください。

通常実行で失敗した同一コマンドを、権限付きまたは権限昇格で再実行してはいけません。

## 4. 作業内容

対象ファイルを確認し、必要な差分案を作成してください。

## 5. 完了報告

最後に変更ファイル、確認結果、未確認事項を報告してください。
