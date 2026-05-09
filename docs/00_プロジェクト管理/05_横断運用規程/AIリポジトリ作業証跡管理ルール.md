# AIリポジトリ作業証跡管理ルール

## 1. 位置づけ

本書は、Q-Scout-for-Spring の既存CI互換性を維持するための中枢参照スタブである。

共通運用ルールの正本は `yamaguchiyoshishigeai-create/chatgpt-ops-rules` とする。

本書に共通運用ルール本文を重複保持しない。詳細なAIリポジトリ作業証跡管理ルールは `chatgpt-ops-rules` 側の現行規程を参照する。

## 2. PRブランチ検証先行ルール

Q-Scout-for-Spring では、AIまたは外部実行支援により作成した変更について、main反映前にPRブランチ上で検証結果・変更範囲・未反映事項を確認する。

この項目名は、既存のRepository policy checksが参照しているため、Q-Scout側のactive文書に保持する。

## 3. Q-Scout側に残す事項

Q-Scout側では、以下のリポジトリ固有事項のみを扱う。

- Q-Scout固有の検証対象。
- Q-Scout固有の公開サンプル評価。
- Q-Scout固有のWeb / CLI / Maven検証手順。
- Q-Scout側改善タスクとの紐づけ。

## 4. 共通正本

共通的なAI_REPO_RESULT、PRコメント証跡、Codex連携、ChatExec2方式、実行結果サマリ、秘密情報非掲載、長大ログ非掲載等の詳細は `chatgpt-ops-rules` を参照する。

## 5. 旧本文履歴

TSK-046で整理した旧本文は、履歴参照用に以下へ移動済みである。

`docs/90_アーカイブ/旧横断運用規程/TSK-046_QScout_旧横断運用規程/AIリポジトリ作業証跡管理ルール.md`

当該アーカイブは履歴確認用であり、現行運用ルールの正本ではない。
