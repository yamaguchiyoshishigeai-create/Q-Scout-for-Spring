# AIリポジトリ作業証跡管理ルール

## 1. 目的

本書は、AI がリポジトリへ変更を加えた場合の修正履歴、確認結果、未確認事項、参照先を一貫して残すための横断運用規程である。

長大ログを Pull Request 差分へ混入させず、PR 本文、PR コメント、コミットメッセージ本文を中心に証跡を残すことを目的とする。

## 2. 適用対象

本書は、以下の作業主体と作業に適用する。

- ChatGPT(リポジトリ編集)
- ChatGPT によるリポジトリ操作
- Codex
- AI が branch / commit / PR 作成、docs 更新、README 更新、設定変更、実装変更、テスト・build 確認などを伴うリポジトリ作業を行う場合

## 3. 共通原則

1. PR あり作業では、PR 本文または PR コメントに作業結果を記録する。
2. PR あり作業でも、コミットメッセージ本文に短縮記録を残す。
3. PR なし作業では、コミットメッセージ本文に短縮記録を残す。
4. 長大ログ、生成物、サンプル出力、ローカル実行結果ファイルは原則 git 管理しない。
5. 証跡には、作業主体、`TASK_ID`、`STATUS`、changed files、summary、verification、commit、PR URL、未確認事項を含める。
6. コミットメッセージ本文の短縮記録は、原則 5 から 10 行程度、最大でも 20 行以内とする。
7. PR 本文または PR コメントの記録は、レビュー時に読める粒度で要約し、長大ログ全文は貼らない。

## 4. ChatGPT(リポジトリ編集 / リポジトリ操作) の記録ルール

1. ChatGPT が GitHub connector 等でリポジトリを直接編集し、branch / commit / PR 作成を行う場合に適用する。
2. PR 本文には、目的、変更内容、変更対象外、確認観点、テスト実行有無、未確認事項を記録する。
3. 必要に応じて PR コメントへ `AI_REPO_RESULT` ブロックを残す。
4. コミットメッセージ本文には `ChatGPT-Repo-Result` の短縮記録を残す。
5. ChatGPT は `CodexExec.result` を自分の証跡ファイルとして使用しない。

## 5. Codex の記録ルール

1. `CodexExec.md` は Codex に渡す実行指示書として使用する。
2. リポジトリルートの `CodexExec.result` は、ユーザーが直近の Codex 実行結果を確認するためのローカル作業ファイルとして使用する。
3. `CodexExec.result` は毎回上書き運用とし、git 管理対象にしない。
4. Codex の実行結果は、PR 本文、PR コメント、コミットメッセージ本文に記録する。
5. 旧運用ログは `docs/90_アーカイブ/CodexExec.result旧運用ログ_2026-04-25.md` に保存する。
6. 旧運用規程は `docs/90_アーカイブ/CodexExec.result運用ルール_旧運用規程_2026-04-25.md` に保存する。

## 6. AI_REPO_RESULT ブロックテンプレート

```text
===== AI_REPO_RESULT_BEGIN =====
ACTOR: ChatGPT(リポジトリ編集) / Codex
TASK_ID: <作業ID>
TITLE: <表題>
STATUS: [OK] / [FAIL]
changed files:
- ...

summary:
- ...

verification:
- ...

unverified:
- ...

commit:
- <commit sha / none>
message:
- <commit message>
push:
- success / failed / none
pr:
- <PR number / none>
pr url:
- <URL / none>
===== AI_REPO_RESULT_END =====
```

## 7. コミットメッセージ短縮記録テンプレート

ChatGPT 用:

```text
ChatGPT-Repo-Result:
- TASK_ID: <作業ID>
- STATUS: OK
- changed: <主要変更ファイル>
- verification: <確認結果要約>
- details: PR body / PR comment
```

Codex 用:

```text
Codex-Result:
- TASK_ID: <作業ID>
- STATUS: OK
- changed: <主要変更ファイル>
- verification: <確認結果要約>
- details: PR body / PR comment
```

## 8. 確認時の原則

1. ChatGPT が AI リポジトリ作業の結果を確認する際は、PR 本文、PR コメント、コミットメッセージ本文、PR 差分を優先する。
2. PR がない場合は、コミットメッセージ本文と変更ファイルを確認する。
3. `CodexExec.result` はローカル直近確認用の補助ファイルであり、正式な履歴保管先ではない。
4. 旧運用時点の履歴確認が必要な場合のみ `docs/90_アーカイブ/` を参照する。

## 9. 旧運用との優先関係

本書は、AI によるリポジトリ作業証跡管理の現行正本である。

旧 `CodexExec.result` 先頭追記型運用は新規作業の判断基準として使用しない。

他文書に旧運用や Codex 専用の記載が残っていた場合は、本書を優先し、必要に応じて当該文書を修正する。
