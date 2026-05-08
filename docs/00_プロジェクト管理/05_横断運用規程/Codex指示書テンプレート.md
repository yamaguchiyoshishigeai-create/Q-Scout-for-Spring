# Codex指示書テンプレート

## 1. 目的

本書は、Codexへ実装、複数ファイル修正、テスト更新、ビルド確認、PR作成を依頼する際の標準テンプレートを定義する。

目的は以下である。

- Codex投入指示書の必須項目漏れを防ぐ。
- モデル設定、対象ファイル、要求仕様、テスト、Git公開Step、PR作成、未反映事項を一貫して扱う。
- `LOCAL_SUCCESS` と `DONE` の混同を防ぐ。
- 安全チェックやツール制約が発生しても、仕様を弱めずに別経路へ切り替えられるようにする。

---

## 2. 使用条件

このテンプレートは、以下の場合に使用する。

- Controller、Service、診断ロジック、Template、Testなど複数ファイルを修正する場合。
- テスト期待値または回帰テスト更新を伴う場合。
- ビルドまたはテスト実行をCodexへ依頼する場合。
- commit、push、PR作成までCodexに到達させたい場合。
- ChatGPT(リポジトリ編集)の直接操作が安全チェックまたはツール制約で不安定な場合。

短い単一ファイル修正、局所的なdocs修正、README追記など、ChatGPT(リポジトリ編集)で安全に完了できる作業では、Codex投入前にその方が適切か確認する。

---

## 3. 標準テンプレート

```text
# CodexExec_<作業内容>

## 1. 作業名

提案名: <提案名>

対象リポジトリ:
- <owner>/<repo>

対象ブランチ方針:
- 最新 main から新規クリーンブランチを作成する。
- 旧ブランチを継続利用する場合は、その理由を明記する。
- 推奨ブランチ名: <branch-name>

## 1.1 Codex実行設定

- モデル: <GPT-5.3-Codex または GPT-5.5 等>
- インテリジェンス / reasoning effort: <high 等>
- 速度: <medium 等>

指定設定で実行できない場合は、利用可能な選択肢を報告し、勝手に低いモデルや低い推論設定へ置き換えない。

## 2. 最重要制約

安全チェックやツール制約を回避する目的で、要求仕様、文言、テスト期待値を弱めたり曖昧化したりしないこと。指定どおりに反映できない場合は、代替表現で済ませず、未反映として報告すること。

## 3. 背景

<なぜこの作業が必要か>

## 4. 対象ファイル候補

主対象:
- <file>

必要に応じて確認:
- <file>

変更禁止または注意対象:
- <file または 範囲外作業>

## 5. 実施すること

- <正確に反映すべき仕様>
- <削除する文言>
- <追加する趣旨>
- <テスト期待値>

## 6. 実施しないこと

- mainへの直接push。
- mainへのmerge。
- 仕様を弱める代替表現への置換。
- <その他対象外>

## 7. Git公開Step

以下を必ず実施する。

1. git status で作業ブランチと変更ファイルを確認する。
2. 指定対象ファイルのみを git add する。
3. commit を作成する。
4. 作業ブランチを remote へ push する。
5. GitHub PR を作成する。
6. PR番号、commit SHA、変更ファイル、テスト結果、CI状態、未反映事項を報告する。

Commit未作成、remote push未実施、PR未作成のいずれかが残る場合は、STATUSを SUCCESS または DONE にしてはならない。

## 8. 検証コマンド

<例>
    ./mvnw test
    ./mvnw.cmd test

## 9. 完了条件

- 最新 main 由来の作業ブランチで修正されている。
- 指定仕様が弱められずに反映されている。
- 対象ファイルのみが変更されている、または追加変更理由が明記されている。
- 必要なテスト期待値が更新されている。
- ローカルテストが成功している、または実行不能理由が明記されている。
- commit が作成されている。
- remote branch へ push されている。
- PR が作成されている。
- 未反映事項が none または明示されている。

## 10. PR作成方針

PR:
- base: main
- head: <branch-name>
- title: <PR title>

PR本文に含めること:
- 実施したこと。
- 実施しなかったこと。
- 変更ファイル一覧。
- テストコマンドと結果。
- 未反映事項がある場合は、未反映として明記する。

## 11. 結果報告フォーマット

JOB_ID:
STATUS: LOCAL_SUCCESS | COMMITTED | PUSHED | PR_READY | CI_GREEN | DONE | FAIL | PARTIAL | BLOCKED
Repository:
Branch:
Commit:
Remote Push: DONE | NOT_DONE | BLOCKED
PR: #number | NOT_CREATED | BLOCKED
Local Test:
CI:
Changed Files:
Requirement Mapping:
Unreflected Items:
Blocking Point:
Next Required Action:
```

---

## 4. ステータス判定補足

| 状態 | 使用条件 |
|---|---|
| `LOCAL_SUCCESS` | ローカル差分とローカルテストは成功したが、commit、push、PR作成のいずれかが未完了 |
| `COMMITTED` | commit済みだがremote未push |
| `PUSHED` | remote branchはあるがPR未作成 |
| `PR_READY` | PR作成済み、CI未確認または進行中 |
| `CI_GREEN` | CI success、merge判断待ち |
| `DONE` | main merge済み、必要な整理も完了 |
| `PARTIAL` | 一部反映、未反映事項あり |
| `BLOCKED` | 安全チェック、権限、CLI、認証、remote、CI等で停止 |

`PR: NOT_CREATED` または `Remote Push: NOT_DONE` の場合、`STATUS: SUCCESS`、`STATUS: DONE`、`Unreflected Items: none` として扱ってはならない。

---

## 5. PR未作成時の追補指示テンプレート

```text
前回作業は仕様反映とテスト実行までは完了していますが、PR未作成、Commit未作成、またはremote push未実施のため、運用上は未完了です。

安全チェックやツール制約を回避する目的で、要求仕様、文言、テスト期待値を弱めたり曖昧化したりしないこと。指定どおりに反映できない場合は、代替表現で済ませず、未反映として報告すること。

以下を同じ作業ツリーで実施してください。

1. 現在の変更内容を確認する。
2. 対象ファイルのみをstageする。
3. commitを作成する。
4. 作業ブランチをremoteへpushする。
5. GitHub PRを作成する。
6. PR作成が安全チェック、権限、CLI、remote、認証、GitHub連携制約で失敗した場合は、仕様を変更せず、どの段階で失敗したかを未反映として報告する。
7. 成功後、PR番号、commit SHA、変更ファイル、テスト結果、未反映事項を報告する。
```

---

## 6. 関連規程

- `00_運用ルール索引と実行ゲート.md`
- `安全チェック発生時の仕様不変切替方針.md`
- `Codex投入前ハンドオフゲート方針.md`
- `AI作業分担方針.md`
- `ChatExec2方式.md`
