# AI_REPO_RESULT証跡チェッカー利用手順

## 1. 目的

本書は、`scripts/check_ai_repo_result.py` の利用手順を定義する。

このチェッカーは、PR本文、PRコメント、コミットメッセージ本文、作業結果テキストに、`AIリポジトリ作業証跡管理ルール.md` で求められる証跡項目が含まれているかを静的検査する。

## 2. 対象ファイル

- PR本文を保存したMarkdownファイル
- PRコメント本文
- コミットメッセージ本文
- ChatGPTまたはCodexの作業結果テキスト
- `AI_REPO_RESULT` ブロックを含む任意のテキストファイル

## 3. フル証跡ブロックの基本コマンド

リポジトリルートで以下を実行する。

    python scripts/check_ai_repo_result.py pr_body.md

安全な証跡の場合は以下のように出力される。

    [PASS] AI repository evidence check passed. format=full

不備がある場合は、以下を含む `FAIL` が出力される。

- ルールID
- 不足項目
- 理由
- 推奨追記内容

## 4. 短縮証跡の検査

コミットメッセージ本文など、以下の短縮形式を検査する場合は自動判定できる。

- `ChatGPT-Repo-Result:`
- `Codex-Result:`

明示的に短縮形式として検査する場合は以下を使用する。

    python scripts/check_ai_repo_result.py commit_message.txt --format short

## 5. 検査対象項目

フル形式では主に以下を検査する。

- `AI_REPO_RESULT_BEGIN`
- `AI_REPO_RESULT_END`
- `ACTOR`
- `TASK_ID`
- `TITLE`
- `STATUS`
- `changed files`
- `summary`
- `verification`
- `unverified`
- `commit`
- `push`
- `pr`
- `pr url`

短縮形式では主に以下を検査する。

- `TASK_ID`
- `STATUS`
- `changed`
- `verification`
- `details`

## 6. unverified 欠落を許容する場合

軽微なPRや解決済み化専用PRなどで、未確認事項が意図的にない場合は、以下のオプションで `unverified` 欠落を許容できる。

    python scripts/check_ai_repo_result.py pr_body.md --allow-missing-unverified

ただし、原則として未確認事項がない場合でも `unverified:` に `none` 等を明記する方が望ましい。

## 7. 回帰テスト

チェッカー本体を変更した場合、または検査ルールを追加・修正した場合は、以下を実行する。

    python scripts/test_ai_repo_result.py

この回帰テストでは、以下の観点をまとめて確認する。

- 正常なフル証跡ブロックが `PASS` になること
- フル証跡ブロックの必須項目不足が `FAIL` になること
- `unverified` 欠落を通常時は `FAIL` にし、オプション指定時は許容できること
- `ChatGPT-Repo-Result` の短縮証跡が `PASS` になること
- `Codex-Result` の短縮証跡が `PASS` になること
- 短縮証跡の必須項目不足が `FAIL` になること
- 証跡ブロックがないファイルが `FAIL` になること

期待結果は以下である。

    [PASS] all AI_REPO_RESULT regression cases passed: 9/9

## 8. 運用タイミング

以下のタイミングで実行する。

- ChatGPT(リポジトリ編集)がPR本文を作成した後
- Codex作業結果のPR本文またはPRコメントを確認するとき
- コミットメッセージ本文へ短縮証跡を残した後
- 解決済み化専用PRのPR本文を作成した後
- `AIリポジトリ作業証跡管理ルール.md` を変更したとき
- `scripts/check_ai_repo_result.py` を変更したとき

## 9. 注意事項

このチェッカーは静的検査であり、証跡内容の意味的妥当性までは判定しない。

以下は人間またはAIレビューを残す。

- summary の内容妥当性
- verification の意味的十分性
- unverified の網羅性
- 実際の変更内容と証跡内容の整合
- 証跡に長大ログや不要情報が混入していないか

## 10. 関連文書

- `docs/00_プロジェクト管理/05_横断運用規程/AIリポジトリ作業証跡管理ルール.md`
- `docs/00_プロジェクト管理/02_改善タスク管理/TSK-032.md`
