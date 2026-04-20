# CodexExec.result運用ルール

## 1. 目的

`CodexExec.result` は、Codex の実行結果を ChatGPT 側が確認するための標準結果ファイルである。  
長文の実行ログをチャットへ貼り付ける代わりに、このファイルを確認基準として用いる。

本ルールの目的は、以下を同時に満たすことである。

- ChatGPT がリポジトリ上の単一標準ファイルを基準に結果確認できること
- Codex の実行結果を簡潔かつ再確認可能な形で残せること
- 並列作業、重複作業、ブランチ統合時でも `CodexExec.result` が破綻しにくいこと
- 履歴ファイルを増殖させず、単一ファイル運用を維持すること

---

## 2. 配置と更新ルール

- `CodexExec.result` はリポジトリルートに配置する
- `CodexExec.result` は commit / push 対象に必ず含める
- サブディレクトリ側の同名ファイルは標準結果ファイルとして扱わない
- **実行のたびに毎回上書きしてはならない**
- `CodexExec.result` は **先頭追記型の実行ログファイル** として運用する
- 新しい実行結果は、既存内容を保持したまま **先頭に1ブロック追加** する
- ChatGPT は `CodexExec.result` 全体を漫然と読むのではなく、対象作業のログブロックを特定して確認する

---

## 3. ログブロックの基本形式

各実行結果は、以下のような **BEGIN / END で囲まれた1ブロック** として記録する。

```text
===== CODEX_RESULT_BEGIN =====
TASK_ID: <作業識別子>
TITLE: <表題>
EXECUTED_AT: <ISO 8601 形式の日付時刻>
STATUS: [OK] / [FAIL]
STEP: <STEP 名>
changed files:
- ...

summary:
- ...

untouched:
- ...

commit: <SHA / same commit / none>
message: <commit message / none>
message source: CodexExec.md 指示採用 / 自動生成 / none
===== CODEX_RESULT_END =====
```

### 3.1 必須ヘッダ
各ブロックの先頭には、少なくとも以下を必須とする。

- `TASK_ID`
- `TITLE`
- `EXECUTED_AT`
- `STATUS`
- `STEP`

### 3.2 `TASK_ID` の方針
- `TASK_ID` は、同一作業を識別できる英数字・アンダースコア中心の識別子とする
- 例: `SAFE_ZIP_AUTO_EXCLUDE_V1`, `SECURITY_HARDENING_V1`, `DOCS_40_RESTRUCTURE_V1`
- 同じ表題の再実行でも、必要に応じて `TASK_ID` を変えるか、`EXECUTED_AT` により区別できるようにする

### 3.3 `EXECUTED_AT` の方針
- 日付時刻は ISO 8601 形式を推奨する
- 例: `2026-04-17T15:03:26+09:00`
- ChatGPT は `TASK_ID` と `EXECUTED_AT` を組み合わせて対象ブロックを特定する

---

## 4. 追記ルール

- 新しい実行結果は **必ず先頭へ追記** する
- 既存の古いログブロックは保持する
- 1回の Codex 実行に対して、1つの完結したログブロックを記録する
- 途中失敗した場合も、失敗ブロックとして完結させる
- 既存の古いブロック本文を部分編集してはならない
- 新しい作業結果を過去ブロックへ混在させてはならない

---

## 5. 記載方針

- 出力は簡潔でよい
- 詳細ログ全文は不要
- ChatGPT への長文の実行結果貼り付けは原則不要とする
- ChatGPT は `CodexExec.result` のうち、対象作業の **該当ログブロック** を基準に結果確認を行う
- 該当ブロックの特定には、原則として **`TASK_ID` / `TITLE` / `EXECUTED_AT`** を用いる
- 直近作業を確認する場合でも、ChatGPT は対象ブロックを明示的に確認する

---

## 6. コミット運用

- Codex 修正が入り、`CodexExec.result` の出力まで完了した作業は、原則として git コミットまで行う
- コミットコメントは `CodexExec.md` 内に指示がある場合、その文言を採用する
- `CodexExec.md` 内にコミットコメントの指示がない場合は、作業内容から適切な日本語短文コメントを生成して採用する
- `CodexExec.result` には、実際に採用したコミットコメントと、そのコメントが `CodexExec.md` 指示採用か自動生成かを明記する
- `CodexExec.result` 自身を更新したコミットを同じ結果ブロックで記録する場合、必要に応じて `commit: same commit` のような表現を用いてよい

---

## 7. 最低限含める項目

各ログブロックには、最低限以下を含める。

- `TASK_ID`
- `TITLE`
- `EXECUTED_AT`
- 実行結果（`[OK]` / `[FAIL]`）
- `STEP`
- `changed files`
- `summary`
- `untouched` または `reason`
- `commit`
- `message`
- `message source`

必要に応じて、以下を追加してよい。

- `unimplemented`
- `concerns`
- `notes`
- `follow-up`

---

## 8. 成功時の出力例

```text
===== CODEX_RESULT_BEGIN =====
TASK_ID: SAFE_ZIP_AUTO_EXCLUDE_V1
TITLE: 不要物自動除外付き安全解凍方式 v1.0
EXECUTED_AT: 2026-04-17T15:03:26+09:00
STATUS: [OK]
STEP: SAFE_ZIP_AUTO_EXCLUDE_V1
changed files:
- src/main/java/com/example/A.java
- src/test/java/com/example/ATest.java
- CodexExec.result

summary:
- 危険性検査を維持したまま不要物自動除外を追加した
- UI と i18n を更新した

untouched:
- other files unchanged

commit: abc1234
message: 不要物自動除外付き安全解凍を実装
message source: CodexExec.md 指示採用
===== CODEX_RESULT_END =====
```

---

## 9. 失敗時の出力例

```text
===== CODEX_RESULT_BEGIN =====
TASK_ID: SAFE_ZIP_AUTO_EXCLUDE_V1
TITLE: 不要物自動除外付き安全解凍方式 v1.0
EXECUTED_AT: 2026-04-17T15:03:26+09:00
STATUS: [FAIL]
STEP: SAFE_ZIP_AUTO_EXCLUDE_V1
changed files:
- src/main/java/com/example/A.java
- CodexExec.result

summary:
- ZIP 判定ロジック途中まで修正した
- テスト追加途中で停止した

reason:
- 既存テストとの整合が取れず完了できなかった

commit: none
message: none
message source: none
===== CODEX_RESULT_END =====
```

---

## 10. サイズ管理ルール

- `CodexExec.result` が一定サイズを超えた場合は、**末尾から古いログブロック単位で削除** する
- 途中行や途中ブロックで切断してはならない
- 削除は **最も古い完全ブロック** から順に行う
- サイズ調整後も、最新側のログブロックが完全な形で残るようにする

### 10.1 重要原則
- サイズ調整のために最新ブロックを壊してはならない
- `BEGIN` / `END` の片側だけが残る状態を作ってはならない
- ブロック削除後も、ファイル全体が人間と ChatGPT の双方にとって読めることを維持する

---

## 11. 競合発生時の対処ルール

- `CodexExec.result` に merge conflict が発生した場合、**競合記号を残したまま commit してはならない**
- conflict 解消時は、各ブランチで追加されたログブロックを **ブロック単位で保全して整形統合** する
- どちらか一方のログブロックを無断で捨ててはならない
- conflict 解消後も、各ブロックが `BEGIN / END` を備えた完結形で残ること
- 先頭側により新しいログ、末尾側により古いログが来る順序を維持することを原則とする

---

## 12. ChatGPT 側の確認原則

- ChatGPT は、原則として `CodexExec.result` を確認する
- ただし、確認対象は **ファイル全体ではなく、該当作業のログブロック** とする
- ユーザーまたは指示文に `TASK_ID` / `TITLE` / `EXECUTED_AT` が示されている場合、まずそれに一致するブロックを探す
- 複数候補がある場合は、`EXECUTED_AT` が一致するものを優先する
- 直近作業の確認でも、安易に先頭ブロックだけを採用せず、対象作業名との一致を確認する

---

## 13. 実行指示書側の推奨記載

今後の `CodexExec.md` では、可能な限り以下を明示することを推奨する。

- `TASK_ID`
- `TITLE`
- `STEP`
- 想定コミットコメント

これにより、Codex が `CodexExec.result` へ一貫したログブロックを書きやすくなり、ChatGPT 側の確認精度も上がる。

---

## 14. 今後の運用

今後の Codex 指示では、`CodexExec.result` をリポジトリルートへ **先頭追記型ログファイル** として出力し、commit / push 対象に含める前提で運用してよい。  
結果確認は本ファイルを基準に行い、必要な場合のみ補足説明をチャットで追加する。  
コミットコメントを採用した根拠も、以後は `CodexExec.result` の該当ログブロックへ併記するものとする。  
また、並列作業や重複作業が起きても、対象ブロックを特定して確認する運用を原則とする。
