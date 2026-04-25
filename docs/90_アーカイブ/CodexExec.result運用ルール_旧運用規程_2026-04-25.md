# CodexExec.result運用ルール 旧運用規程

このファイルは、リポジトリルートの `CodexExec.result` を git 管理対象の先頭追記型ログとして扱っていた旧運用規程のアーカイブである。

現行運用では、`docs/00_プロジェクト管理/05_横断運用規程/AIリポジトリ作業証跡管理ルール.md` を正本とする。

現行運用において、リポジトリルートの `CodexExec.result` はユーザーが直近の Codex 実行結果を確認するためのローカル作業ファイルであり、毎回上書き運用とし、git 管理対象にしない。

この旧運用規程は、過去の運用経緯を確認するための履歴資料であり、新規 Codex 作業の判断基準としては使用しない。

---

# CodexExec.result運用ルール

## 1. 目的

`CodexExec.result` は、Codex の実行結果を ChatGPT 側が確認するための標準結果ファイルである。  
長文の実行ログをチャットへ貼り付ける代わりに、このファイルを確認基準として用いる。

本ルールの目的は、以下を同時に満たすことである。

- ChatGPT がリポジトリ上の単一標準ファイルを基準に結果確認できること
- Codex の実行結果を簡潔かつ再確認可能な形で残せること
- ChatGPT が `CodexExec.result` を起点に、`pr url` がある場合は PR 差分を最優先で確認できること
- 並列作業、重複作業、ブランチ統合時でも `CodexExec.result` が破綻しにくいこと
- 履歴ファイルを増殖させず、単一ファイル運用を維持すること

---

## 2. 配置と更新ルール

- `CodexExec.result` はリポジトリルートに配置する
- `CodexExec.result` は commit / push 対象に必ず含める
- サブディレクトリ側の同名ファイルは標準結果ファイルとして扱わない
- `CodexExec.result` は **先頭追記型の実行ログファイル** として運用する
- 新しい実行結果は、既存内容を保持したまま **先頭に 1 ブロック追加** する
- `CodexExec.result` は、実行結果メモであると同時に **PR 確認先を示す索引付き実行ログ** として扱う
- ChatGPT は `CodexExec.result` 全体を漫然と読むのではなく、対象作業のログブロックを特定して確認する

### 2.1 並列作業時の重要原則

- 通常作業では、**新規ログブロック 1 件の先頭追記以外を行ってはならない**
- **過去ログブロックの本文を編集してはならない**
- 過去ログブロックの `summary`、`concerns`、`pr status`、`notes` 等を、後から更新してはならない
- `CodexExec.result` 内の整形、旧形式変換、誤字修正、並び替え、不要行削除は、**専用修復タスク** として分離する
- 通常作業で conflict marker や旧ブロックの乱れを見つけても、その場で全体整形を行わず、必要最小限の競合解消に留める
- `pr status` は **PR 作成時点の状態を記録する項目** とし、後から open / closed / merged に追記修正しない

### 2.2 Git マージ前提の補助ルール

- `CodexExec.result` は、独立したログブロックの集合として扱う
- Git マージ時の競合低減のため、`.gitattributes` により **union merge** を適用してよい
- ただし union merge 適用後でも、ブロックの完結性確認と conflict marker 残存確認は必要である

---

## 3. ログブロックの基本形式

各実行結果は、以下のような **BEGIN / END で囲まれた 1 ブロック** として記録する。

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
push: success / failed / none
pr: <PR number / none>
pr url: <URL / none>
pr base: <base branch / none>
pr head: <head branch / none>
pr status: <open / merged / closed / failed / none>
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
- 1 回の Codex 実行に対して、1 つの完結したログブロックを記録する
- 途中失敗した場合も、失敗ブロックとして完結させる
- 既存の古いブロック本文を部分編集してはならない
- 新しい作業結果を過去ブロックへ混在させてはならない

### 4.1 通常作業で禁止すること

- 過去ブロックの文面修正
- 過去ブロックの項目追加
- 過去ブロックの PR 状態更新
- 旧ログ形式の整形
- conflict marker 除去を口実にした全体整形
- ブロック順の入れ替え
- BEGIN / END の付け替えや旧ブロックの再構成

### 4.2 例外的に許容すること

- 自分が今追加する **最新 1 ブロック** の記述
- 自分の作業で発生した競合を解消するための **最小限の修復**
- ただしこの場合も、**他人の過去ブロックの意味内容は変えない** こと

---

## 5. 記載方針

- 出力は簡潔でよい
- 詳細ログ全文は不要
- ChatGPT への長文の実行結果貼り付けは原則不要とする
- ChatGPT は `CodexExec.result` のうち、対象作業の **該当ログブロック** を基準に結果確認を行う
- 該当ブロックの特定には、原則として **`TASK_ID` / `TITLE` / `EXECUTED_AT`** を用いる
- `pr url` が存在する場合、ChatGPT は **PR 差分を最優先で確認** する
- `CodexExec.result` 本文は、PR 差分確認を補助する根拠として扱う
- `pr url` がない場合のみ、変更対象ファイルや commit を直接確認する
- 直近作業を確認する場合でも、ChatGPT は対象ブロックを明示的に確認する

---

## 6. commit / push / PR 運用

- Codex 修正が入り、`CodexExec.result` の出力まで完了した作業は、原則として **commit → push → Pull Request 作成** まで行う
- コミットコメントは `CodexExec.md` 内に指示がある場合、その文言を採用する
- `CodexExec.md` 内にコミットコメントの指示がない場合は、作業内容から適切な日本語短文コメントを生成して採用する
- `CodexExec.result` には、実際に採用したコミットコメントと、そのコメントが `CodexExec.md` 指示採用か自動生成かを明記する
- `CodexExec.result` 自身を更新したコミットを同じ結果ブロックで記録する場合、必要に応じて `commit: same commit` のような表現を用いてよい
- Pull Request を作成できた場合は、同じログブロックへ `push` / `pr` / `pr url` / `pr base` / `pr head` / `pr status` を追記する
- Pull Request 作成に失敗した場合は、その時点で停止し、成功した最終工程・失敗工程・エラーメッセージ・push 済み branch 名・compare URL を可能な範囲で `reason` に記録する

### 6.1 自己参照ずれ防止ルール

- 最新ログブロックが**自分自身を含む最終コミット**を記録対象にする場合、`commit` 欄は raw SHA ではなく **`same commit` を優先**する
- raw SHA を `commit` 欄へ記載してよいのは、**最新ブロック自身を含まない既存コミット群**を記録対象とする場合に限る
- `push` / `pr` / `pr url` / `pr status` を確定値で記録する必要がある場合、`CodexExec.result` の先頭追記は **最終専用コミット** として分離してよい
- この場合の標準順序は、**実ファイル変更のコミット群を先に確定し、その後に `CodexExec.result` 記録専用コミットを作成し、最後に push / Pull Request 作成を行う** ものとする
- これにより、最新ブロックの `commit` が 1 手前の SHA になったり、`push: pending` のまま確定ログとして残る再発を防ぐ

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
- `push`
- `pr`
- `pr url`
- `pr base`
- `pr head`
- `pr status`

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

commit: same commit
message: 不要物自動除外付き安全解凍を実装
message source: CodexExec.md 指示採用
push: success
pr: 12
pr url: https://github.com/OWNER/REPO/pull/12
pr base: main
pr head: sample-task-branch
pr status: open
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
- last successful step: push
- failed step: Pull Request 作成
- error: Validation Failed
- pushed branch: sample-task-branch
- compare url: https://github.com/OWNER/REPO/compare/main...sample-task-branch

commit: none
message: none
message source: none
push: success
pr: none
pr url: none
pr base: main
pr head: sample-task-branch
pr status: failed
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

### 11.1 競合低減の追加ルール

- `.gitattributes` により `CodexExec.result merge=union` を設定してよい
- union merge を適用しても、`<<<<<<<`, `=======`, `>>>>>>>` が残っていないかを必ず確認すること
- union merge 後に重複や順序乱れが見つかった場合も、**当該競合解消に必要な最小範囲のみ修復** する
- conflict 解消の機会を使って過去ブロック全体を整理してはならない
- 旧ログの形式修復や一括整形は、**専用 TASK_ID を持つ修復作業** として分離すること

---

## 12. ChatGPT 側の確認原則

- ChatGPT は、原則として `CodexExec.result` を確認する
- ただし、確認対象は **ファイル全体ではなく、該当作業のログブロック** とする
- ユーザーまたは指示文に `TASK_ID` / `TITLE` / `EXECUTED_AT` が示されている場合、まずそれに一致するブロックを探す
- 複数候補がある場合は、`EXECUTED_AT` が一致するものを優先する
- `pr url` が存在する場合は、**PR 差分確認を最優先** とする
- `CodexExec.result` 本文は、PR 差分確認後の補助根拠として扱う
- `pr url` がない場合のみ、変更対象ファイルや commit を直接確認する
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

今後の Codex 指示では、`CodexExec.result` をリポジトリルートへ **先頭追記型ログファイル** として出力し、commit / push / Pull Request 対応関係を含めて運用してよい。  
結果確認は本ファイルを起点に行い、`pr url` がある場合は PR 差分確認を第一優先とする。  
コミットコメントを採用した根拠も、以後は `CodexExec.result` の該当ログブロックへ併記するものとする。  
また、並列作業や重複作業が起きても、対象ブロックを特定し、PR 確認先を含めて確認する運用を原則とする。

### 14.1 再発防止の最終原則

- **通常作業では、自分の新規ログブロック 1 件以外に触らない**
- **過去ブロック修正は専用修復タスクに分離する**
- **union merge は競合低減策であり、旧ログ全体整形の許可ではない**
- **conflict marker を残したまま push / PR 作成してはならない**
- **最新ブロックが自分自身を含む最終コミットを記録する場合、`commit` は `same commit` を優先する**
- **`push` / `pr` の確定値が必要な場合、`CodexExec.result` 追記は最終専用コミットとして分離する**
