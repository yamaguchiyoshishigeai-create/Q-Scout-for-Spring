# Public-Free版・Private-Pro版分離運用方針

## 1. 目的

本書は、`Q-Scout-for-Spring` の Public リポジトリを、ポートフォリオ兼 Free 試供版として完成させつつ、将来的な商用 Pro 版を別 Private リポジトリで管理するための運用方針を定義する。

この方針により、Public 版へ入れる機能、Private Pro 版へ回す機能、依存バージョン更新、Dependabot PR の採否判断を一貫させる。

## 2. 前提

- 現行の `Q-Scout-for-Spring` リポジトリは、すでに Public なポートフォリオとして公開済みである。
- このシステムを販売物として製作する場合、Pro 版相当の商用価値を含むコード・設計・検証資産は、別の Private リポジトリで管理する。
- Public リポジトリを後から Private 化するのではなく、Public Free 版と Private Pro 版を分けて運用する。
- Public 版は未完成品として放置せず、無料で試せる範囲が明確に動作する試供版として整備する。

## 3. Public Free版の位置づけ

Public Free 版は、以下の位置づけとする。

- ポートフォリオ
- Free 試供版
- 基本診断機能のデモ
- 無償で公開して問題ない範囲の機能群
- Private Pro 版へ誘導するための入口

Public Free 版は、有料機能を含まない一方で、利用者が試した際に「最低限の価値が分かる」状態まで完成させる。

## 4. Private Pro版の位置づけ

Private Pro 版は、以下の位置づけとする。

- 商用機能の管理先
- 高度診断機能の実装先
- 上位バージョン対応の検証先
- 組織向け・実務導入向け機能の開発先
- 有料価値の中核となる検証済み互換性マトリクスの管理先

Private Pro 版では、Spring Boot 4.x、Spring Framework 7、Java 21 / 25、Gradle、マルチモジュール等の対応を段階的に進める。

## 5. Free版に含める範囲

Public Free 版では、原則として以下を対象とする。

- Spring Boot 3.5.x 系まで
- Java 17 / 21
- Maven 単一モジュール
- 基本6ルール
- Markdown レポート出力
- Web UI
- 日本語 / 英語切替
- 基本的な公開サンプル評価
- Docker / Render / ローカル起動手順
- Free 版としての対応範囲・非対応範囲の明示

## 6. Pro版に回す範囲

以下は、原則として Private Pro 版の対応領域とする。

- Spring Boot 4.x
- Spring Framework 7
- Java 25
- Gradle
- マルチモジュール
- Kotlin
- 追加診断ルール
- ルールカスタマイズ
- CI/CD 連携
- 診断履歴管理
- 組織向けレポート
- 誤検知抑制設定
- 大規模プロジェクト向け非同期解析
- 商用向け互換性マトリクス

ただし、Public Free 版の完成度や安全性を高めるために必要な基盤修正は、Pro 版由来であっても内容を選別して Public 側へ取り込んでよい。

## 7. Dependabot / 依存更新方針

Public Free 版では、依存更新を以下の方針で扱う。

- Spring Boot は 3.5.x 系までの更新を基本対象とする。
- Spring Boot 4.x への major update は、Public Free 版では原則として即採用しない。
- Spring Boot 4.x 対応は、Private Pro 版の検証テーマとして扱う。
- GitHub Actions、Maven wrapper、テスト関連依存など、Free 版の品質維持に必要な更新は継続して確認する。
- major update の Dependabot PR は、Public Free 版の方針に照らして close / ignore / Pro 版検証移管を判断する。

## 8. Public / Private 間の取り込み方針

Public 版と Private 版は、以下の関係で運用する。

- Public → Private は、安定した Free 版基盤の取り込み元とする。
- Private → Public は、Pro 機能を漏らさず、Free 版に必要な修正だけを選別して戻す。
- Private 版で得た知見のうち、Public 版の品質・安全性・説明性を高めるものは、機能境界を守ったうえで Public 版へ反映する。
- Public 版に商用価値の中核となる高度機能を入れすぎない。

## 9. 現時点の実務判断

現時点では、以下を実務判断とする。

- Public Free 版 main へ Spring Boot 4.x 対応を直接 merge しない。
- Public Free 版では、Spring Boot 3.5.x 系への更新を優先する。
- Dependabot PR #38 のような Spring Boot 4.x major update は、Public Free 版では採用せず、Private Pro 版の検証材料として扱う。
- Public Free 版として不足している基本品質、README、docs 導線、サンプル評価、CI、利用説明を整備する。

## 10. 今後の優先作業

今後の優先作業は以下とする。

1. Public Free 版を Spring Boot 3.5.x 系へ更新する。
2. Public Free 版として不足している基本品質を洗い出す。
3. README と docs に Free 版の位置づけを明記する。
4. Spring Boot 4.x 対応は Private Pro 版の検証テーマとして分離する。
5. Public / Private 間の取り込みルールを、実運用に応じて必要最小限で補強する。
