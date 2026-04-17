# CodexExec.result運用ルール

## 1. 目的

`CodexExec.result` は、Codex の実行結果を ChatGPT 側が確認するための標準結果ファイルである。  
長文の実行ログをチャットへ貼り付ける代わりに、このファイルを確認基準として用いる。

---

## 2. 配置と更新ルール

- `CodexExec.result` はリポジトリルートに配置する
- 実行のたびに毎回上書きする
- `CodexExec.result` は commit / push 対象に必ず含める
- サブディレクトリ側の同名ファイルは標準結果ファイルとして扱わない

---

## 3. 記載方針

- 出力は簡潔でよい
- 詳細ログ全文は不要
- ChatGPT への長文の実行結果貼り付けは原則不要とする
- ChatGPT は `CodexExec.result` を基準に結果確認を行う

---

## 4. コミット運用

- Codex 修正が入り、`CodexExec.result` の出力まで完了した作業は、原則として git コミットまで行う
- コミットコメントは `CodexExec.md` 内に指示がある場合、その文言を採用する
- `CodexExec.md` 内にコミットコメントの指示がない場合は、作業内容から適切な日本語短文コメントを生成して採用する
- `CodexExec.result` には、実際に採用したコミットコメントと、そのコメントが `CodexExec.md` 指示採用か自動生成かを明記する

---

## 5. 最低限含める項目

- 実行結果（`[OK]` / `[FAIL]`）
- `STEP`
- `changed files`
- `summary`
- `untouched` または `reason`
- `commit`
- `message`
- `message source`

---

## 6. 成功時の出力例

```text
[OK] SAMPLE_STEP_DONE
STEP: SAMPLE_STEP
changed files:
- path/to/file1
- CodexExec.result

summary:
- 実施内容を簡潔に記載

untouched:
- other files unchanged

commit: <SHA>
message: <commit message>
message source: CodexExec.md 指示採用 / 自動生成
```

---

## 7. 失敗時の出力例

```text
[FAIL] SAMPLE_STEP_FAILED
STEP: SAMPLE_STEP
changed files:
- <途中で変更したファイルがあれば記載>

summary:
- どこまで実施したかを簡潔に記載

reason:
- 失敗理由

commit: <SHA または none>
message: <commit message または none>
message source: CodexExec.md 指示採用 / 自動生成 / none
```

---

## 8. 今後の運用

今後の Codex 指示では、`CodexExec.result` をリポジトリルートへ上書き出力し、commit / push 対象に含める前提で運用してよい。  
結果確認は本ファイルを基準に行い、必要な場合のみ補足説明をチャットで追加する。コミットコメントを採用した根拠も、以後は `CodexExec.result` に併記するものとする。
