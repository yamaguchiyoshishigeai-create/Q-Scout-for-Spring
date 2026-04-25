# Codex実行結果管理ルール

## 1. 目的

本書は、Codex 実行結果の記録先を整理し、長大ログを Pull Request 差分へ混入させずに、確認に必要な要約と参照先を一貫して残すための横断運用規程である。

## 2. 基本方針

1. `CodexExec.md` は、Codex に渡す実行指示書として使用する。
2. リポジトリルートの `CodexExec.result` は、ユーザー確認用のローカル作業ファイルとして使用する。
3. リポジトリルートの `CodexExec.result` は毎回上書き運用とし、git 管理対象にしない。
4. 旧運用ログは `docs/90_アーカイブ/CodexExec.result旧運用ログ_2026-04-25.md` に保存する。
5. `CodexExec.md` は今回の運用では `.gitignore` 対象にしない。必要時に別途判断する。

## 3. 記録先ルール

1. PR あり作業では、Codex 実行結果の要約を PR 本文または PR コメントに記録する。
2. PR あり作業でも、コミットメッセージ本文に短縮記録を残す。
3. PR なし作業でも、コミットメッセージ本文に短縮記録を残す。
4. コミットメッセージ本文の短縮記録は、原則 5 から 10 行程度、最大でも 20 行以内とする。
5. 短縮記録には最低限、`TASK_ID`、`STATUS`、changed files、verification 要約、詳細参照先を含める。
6. 長大ログ、サンプル出力、生成物は git 管理しない。
7. `CodexExec.result` を PR 差分に含めない。

## 4. 短縮記録テンプレート

コミットメッセージ本文には、必要に応じて以下のような短縮記録を含める。

```text
Codex-Result:
- TASK_ID: TSK_002_SAMPLE_SCRIPT_JDK17_REVERIFICATION_V1
- STATUS: OK
- changed: scripts/run-sample-evaluation-under-samples.ps1
- verification: syntax OK, package OK, script completed, F5_SUMMARY_DONE found
- details: PR comment
```

`details` には、`PR body`、`PR comment`、`commit body only` など、詳細確認先を明記する。

## 5. 確認時の原則

1. ChatGPT が Codex 実行結果を確認する際は、PR 本文、PR コメント、コミットメッセージを優先して確認する。
2. 旧運用時点の履歴確認が必要な場合のみ、`docs/90_アーカイブ/CodexExec.result旧運用ログ_2026-04-25.md` を参照する。
3. ローカル作業中に生成されたリポジトリルートの `CodexExec.result` は、直近確認用の補助ファイルとして扱い、正式な履歴保管先とはみなさない。

## 6. 補足

- 旧運用で蓄積された履歴はアーカイブ済みファイルを正本とし、内容を欠落させない。
- 今後の詳細実行結果は PR 本文または PR コメントへ要約し、必要に応じて参照先を示す。
- 長大ログをそのままコミットせず、レビューに必要な情報だけを短縮記録として残す。

## 7. 旧運用との優先関係

本書は、Codex 実行結果管理に関する現行正本である。

旧来の `CodexExec.result` 先頭追記型運用に関する規程は、`docs/90_アーカイブ/CodexExec.result運用ルール_旧運用規程_2026-04-25.md` に移管済みであり、新規 Codex 作業の判断基準としては使用しない。

他文書に旧運用と解釈できる記載が残っている場合は、本書を優先し、必要に応じて当該文書を修正する。
