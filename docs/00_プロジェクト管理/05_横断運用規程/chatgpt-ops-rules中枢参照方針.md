# chatgpt-ops-rules 中枢参照方針

## 1. 目的

本書は、Q-Scout for Spring における共通運用ルールの参照先を明確化するための文書である。

共通運用ルールの正本は、`yamaguchiyoshishigeai-create/chatgpt-ops-rules` とする。

本リポジトリには、Q-Scout固有の業務仕様、実装仕様、公開サンプル評価仕様、環境依存手順、Public Free版としての製品前提を残す。

## 2. 正本の分離

| 区分 | 管理場所 |
|---|---|
| 共通運用ルール | `yamaguchiyoshishigeai-create/chatgpt-ops-rules` |
| Q-Scout固有の業務仕様 | `Q-Scout-for-Spring` |
| Q-Scout固有の実装仕様 | `Q-Scout-for-Spring` |
| Q-Scout固有の公開サンプル評価仕様 | `Q-Scout-for-Spring` |
| Q-Scout固有の環境依存手順 | `Q-Scout-for-Spring` |
| 改善タスク個票 | 対象作業が属するリポジトリ |

## 3. 作業開始時の参照順

Q-Scout for Spring の作業開始時は、以下の順に確認する。

1. Memory上のプロジェクト運用ルール。
2. `chatgpt-ops-rules` の `PROJECT_START_PROMPT.md`。
3. `chatgpt-ops-rules` の横断運用規程入口。
4. Q-Scout側の `PROJECT_START_PROMPT.md`。
5. Q-Scout側の `README.md`。
6. Q-Scout側の `docs/README.md`。
7. Q-Scout側の改善タスク課題一覧。
8. Q-Scout固有の要件定義、設計、実装、公開サンプル評価文書。

## 4. 衝突時の優先順位

共通運用ルールとQ-Scout側文書に矛盾がある場合は、原則として `chatgpt-ops-rules` を優先する。

ただし、Q-Scout固有の業務仕様、品質診断ツールとしての製品仕様、Public Free版 / Private Pro版の分離方針、Web UI / CLI / Markdownレポート / AI向け入力生成の仕様、公開サンプル評価仕様、環境依存手順はQ-Scout側文書を正とする。

## 5. Q-Scout側に残す事項

以下はQ-Scout側に残す。

- Q-Scout for Spring は Spring Boot / Spring Framework 向け品質診断ツールであるという製品前提。
- Public Free版 / Private Pro版の分離方針。
- ルールベース診断、スコアリング、Markdownレポート、AI向け入力生成の仕様。
- Web UI、CLI、補助スクリプト、公開サンプル評価の仕様。
- Maven Wrapper、Docker、ローカル起動、テスト手順。
- Q-Scout側改善タスク課題一覧と個票。

## 6. Q-Scout側に重複保持しない事項

以下の共通運用ルール本文は、Q-Scout側へ重複コピーしない。

- ChatExec / ChatExec2方式の一般規程。
- ChatExec2 Windows bat標準。
- ChatExec2 FullFlow bat一括実行方針。
- ChatExec2 worktree分離実行方針。
- 通常PR自動merge方針。
- 個票先行main反映ゲート。
- Codex投入前ハンドオフゲート。
- 安全チェック発生時の仕様不変切替方針。
- 発生・残存課題の個票化最優先ルール。
- 実行計画と実行指示の分離方針。

必要な場合は、Q-Scout側に本文を再掲せず、`chatgpt-ops-rules` への参照として扱う。
