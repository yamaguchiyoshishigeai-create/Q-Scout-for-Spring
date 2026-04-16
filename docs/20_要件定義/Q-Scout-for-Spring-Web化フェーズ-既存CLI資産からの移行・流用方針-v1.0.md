# Q-Scout for Spring
## Web化フェーズ 既存CLI資産からの移行・流用方針 v1.0

---

## 1. 文書概要

本書は、Q-Scout for Spring の Web化フェーズにおいて、
既存CLI資産をどのように移行・流用するかを定義するものである。

本方針書の目的は以下の通りである。

- 既存CLI実装の価値を最大限維持する
- Web化で必要な追加責務を明確にする
- 改修対象、新設対象、温存対象を明示する
- Codexが迷わず実装に着手できる粒度まで整理する

本書は以下の前提に基づく。

- 既存CLIは実行可能なMVP一次生成物である
- Web化はCLI資産を活かした拡張である
- Web MVPは同期処理・単画面・zipアップロードを前提とする
- Render前提のステートレス構成とする

---

## 2. 基本結論

### 提案名
コア温存・入口再編型移行方式

### 優先度
最優先

### 方針概要
- 解析コアは最大限温存する
- CLI入口とWeb入口は分離する
- 解析実行処理は共通サービスへ抽出する
- Web化で必要な責務のみを新設する
- CLIは削除せず維持する
- MVPでは最小改修で成立性を優先する

---

## 3. 移行方針の基本原則

### 3.1 解析資産最大流用原則

既存の解析本体は、Web化でもそのまま価値を持つ。
以下は原則として最大限流用する。

- ProjectScanner
- RuleEngine
- ScoreCalculator
- ReportGenerator
- AiMarkdownGenerator
- Rule群
- DTO群
- 主要ユーティリティ

理由：
- 既存CLIで既に実行可能である
- Web化の本質は解析ロジック変更ではなく提供形態変更である
- 品質と手戻りリスクを抑えられる

---

### 3.2 入口分離・内部共有原則

CLI入口とWeb入口は分離する。

CLI入口：
- Main
- CliApplication
- ArgumentParser

Web入口：
- WebPageController
- WebDownloadController
- WebAnalysisService

共通化対象：
- 解析実行処理

非推奨：
- Web側から String[] args を組み立てて CliApplication.run() を直接呼ぶこと

理由：
- CLIとWebでは入力モデルが異なる
- 責務混線を避ける必要がある
- 将来API化や非同期化の妨げを防ぐ

---

### 3.3 薄い移行優先原則

MVPでは大規模再設計を避け、薄い改修で成立させる。

方針：
- 既存コアを壊さない
- Web専用層だけを追加する
- 入口とI/Oの差分だけを吸収する
- 同期MVP前提で過設計を避ける

理由：
- 最短でWeb MVPへ到達できる
- CLI回帰確認が容易
- Codexへの指示が明確になる

---

## 4. 流用分類の全体整理

本移行では、既存資産を以下の5分類で扱う。

A. そのまま流用するもの
B. 薄い改修で流用するもの
C. Web専用に新設するもの
D. 破棄せず温存するもの
E. 今回未着手とするもの

---

## 5. A分類：そのまま流用するもの

### 5.1 ドメインDTO群

提案名：DTO完全流用方針
優先度：最優先

流用対象：
- AnalysisRequest
- ProjectContext
- Violation
- Severity
- RuleResult
- AnalysisResult
- ScoreSummary
- ReportArtifact
- ExecutionSummary

方針：
- DTO定義自体は変更しない
- Web表示用には別途Web DTOを追加する
- ドメインDTOは解析コアの共通表現として維持する

理由：
- CLI/Webで意味が共通
- Violation中心のモデルが既に成立している
- レポート生成、画面表示、AI入力生成に再利用できる

---

### 5.2 6ルール群

提案名：6ルール無改変優先流用
優先度：最優先

流用対象：
- ControllerToRepositoryDirectAccessRule
- FieldInjectionRule
- TransactionalMisuseRule
- ExceptionSwallowingRule
- MissingTestRule
- PackageDependencyViolationRule

方針：
- Web化を理由にルール本体は変更しない
- 精度改善は別テーマとして分離する
- ルール拡張や重大度見直しも今回対象外とする

理由：
- Web化は診断内容ではなく利用形態の変更である
- CLIで既に最低限の有効性が確認されている
- 余計な変更は回帰リスクを増やす

---

### 5.3 解析コア

提案名：解析コア無改変優先方針
優先度：最優先

流用対象：
- ProjectScanner
- RuleEngine
- ScoreCalculator
- ReportGenerator
- AiMarkdownGenerator

実装流用対象：
- DefaultProjectScanner
- DefaultRuleEngine
- DefaultScoreCalculator
- MarkdownReportGenerator
- AiMarkdownFileGenerator

方針：
- インターフェースと実装の責務は維持する
- 呼び出し元だけをCLI/Web両対応へ整理する
- 解析処理そのものはWeb用に作り直さない

理由：
- 最小API設計と既存実装が一致している
- CLI実行フローの中心であり、Webでも必要
- 最大流用が最も合理的

---

### 5.4 主要ユーティリティ

提案名：ユーティリティ流用方針
優先度：高

流用対象：
- FileCollector
- CodeSnippetExtractor
- MarkdownWriter

方針：
- 既存ユーティリティは基本維持する
- Web化で必要な追加ユーティリティは別途追加する
- 既存責務をWeb責務で汚染しない

理由：
- 既存出力方式に適合している
- Markdown生成で再利用価値が高い

---

## 6. B分類：薄い改修で流用するもの

### 6.1 CliApplication

提案名：CliApplication薄型化方針
優先度：最優先

現状：
- run(String[] args) を受けるCLIの中心アプリケーション

改修方針：
- run(String[] args) の外部仕様は維持する
- CLI引数解析後の解析本体処理は SharedAnalysisService に委譲する
- CliApplication 自体はCLI向けアダプタへ縮退させる
- ExecutionSummary 返却は維持する

理由：
- Web側から直接流用しにくい入口依存構造を解消するため
- CLI互換を維持しながら共通化するため

---

### 6.2 ArgumentParser周辺

提案名：入力生成経路分離方針
優先度：高

方針：
- ArgumentParser はCLI専用として維持する
- AnalysisRequest の生成責務はCLI側では継続
- Web側では WebAnalysisService が AnalysisRequest を生成する
- AnalysisRequest 自体の定義は変更しない

理由：
- CLIとWebで入力形式が根本的に異なる
- DTO共通化は維持したい
- 生成経路のみを分離するのが最も自然

---

### 6.3 ReportArtifact

提案名：ReportArtifact再活用方針
優先度：高

現状：
- DTOとして存在するが、実行フロー上の活用が弱い

改修方針：
- SharedAnalysisService 内で ReportArtifact を正式に生成する
- humanReportPath と aiReportPath を共通出力成果物として扱う
- CLIはそれを元に ExecutionSummary を構築する
- Webはそれを元にダウンロード情報を構築する

理由：
- 人間用とAI用の成果物管理を共通化できる
- Webダウンロード提供に自然に接続できる

---

### 6.4 MainとCLI標準出力

提案名：CLI互換維持方針
優先度：高

方針：
- Main は維持する
- 標準出力フォーマットは維持する
- 終了コード運用も維持する
- Web化に伴いCLI利用不能にしない

理由：
- ローカル検証手段として価値が高い
- 回帰確認に使える
- 将来CI転用にも役立つ

---

### 6.5 出力先パス制御

提案名：出力先制御整理方針
優先度：高

方針：
- CLIでは第2引数 outputDirectory を使う現在方式を維持する
- Webでは /tmp/qscout/{requestId}/output/ を outputDirectory として渡す
- 出力生成側は outputDirectory を前提とした現在設計を維持する

理由：
- ReportGenerator / AiMarkdownGenerator の責務を変更せず流用できる
- Web側でディレクトリだけ差し替えれば成立する

---

## 7. C分類：Web専用に新設するもの

### 7.1 Web Controller群

提案名：Web入口新設方針
優先度：最優先

新設対象：
- WebPageController
- WebDownloadController

責務：
- 画面表示
- アップロード受付
- ダウンロード提供
- エラーレスポンス制御
- ViewModel組み立て

制約：
- 解析ロジックは持たない
- ファイル展開ロジックを持たない
- 直接Rule群を触らない

---

### 7.2 WebAnalysisService

提案名：Web実行統括サービス新設
優先度：最優先

責務：
- アップロードファイル受領
- バリデーション呼び出し
- ワークスペース準備
- zip展開
- projectRoot確定
- AnalysisRequest生成
- SharedAnalysisService呼び出し
- 結果DTO組み立て
- ダウンロード情報生成
- cleanup制御

理由：
- Web特有の一連処理をCliApplicationへ混ぜないため
- Web専用オーケストレーション層が必要なため

---

### 7.3 SharedAnalysisService

提案名：共通解析サービス新設
優先度：最優先

責務：
- AnalysisRequest 受領
- ProjectScanner.scan 実行
- RuleEngine.analyze 実行
- ScoreCalculator.calculate 実行
- ReportGenerator.generate 実行
- AiMarkdownGenerator.generate 実行
- ReportArtifact 生成
- CLI/Web両方が利用可能な解析結果を返す

方針：
- CLIとWebの共有中心層とする
- 解析本体の責務をここへ統合する
- 入口依存の処理は持たない

理由：
- 今回の移行の中核
- CLI/Web両方から自然に利用できる構造を作るため

---

### 7.4 UploadValidationService

提案名：アップロード検証サービス新設
優先度：最優先

責務：
- ファイル未指定確認
- zip拡張子確認
- content type補助確認
- サイズ上限確認
- 基本的なzip妥当性確認

理由：
- CLIには存在しないWeb固有責務だから

---

### 7.5 ZipExtractionService

提案名：安全解凍サービス新設
優先度：最優先

責務：
- zip解凍
- Zip Slip防止
- 展開先固定
- 異常zipの検知
- 外部コマンド不使用
- projectRoot候補の準備

理由：
- セキュリティ上Webで最重要な追加責務だから

---

### 7.6 TempWorkspaceService

提案名：一時ワークスペース管理サービス新設
優先度：最優先

責務：
- requestId生成
- /tmp/qscout/{requestId}/ の作成
- upload.zip パス管理
- extracted/ パス管理
- output/ パス管理
- cleanup実行

理由：
- Render前提のステートレス設計を支える基盤責務だから

---

### 7.7 DownloadArtifactService

提案名：成果物ダウンロード提供サービス新設
優先度：高

責務：
- requestId検証
- fileKeyから実ファイル解決
- ファイル存在確認
- ダウンロードレスポンス生成
- パストラバーサル防止
- 有効期限切れ判定

理由：
- Webダウンロード提供はCLIに存在しない責務だから

---

### 7.8 Web DTO / ViewModel / Exception

提案名：Web表示責務分離方針
優先度：高

新設対象：
- WebAnalysisResponse
- DownloadLinkView
- ErrorViewModel
- ExecutionLimitView

例外候補：
- InvalidUploadException
- InvalidProjectStructureException
- AnalysisTimeoutException
- ArtifactExpiredException

方針：
- ドメインDTOをそのまま画面へ出さない
- 画面表示責務と解析責務を分離する
- ユーザー向けメッセージを整理する

理由：
- UI表示・エラー変換・ダウンロード導線を整理しやすくするため

---

## 8. D分類：破棄せず温存するもの

### 8.1 CLI起動構成

提案名：CLI温存方針
優先度：最優先

温存対象：
- Main
- CliApplication
- ArgumentParser
- CLI実行方法
- usage文言
- 標準出力サマリー

方針：
- Web化してもCLIは削除しない
- CLIはローカル検証や回帰確認に使う
- Webの追加によってCLI機能を壊さない

理由：
- 既存資産として価値が高い
- 実装回帰を検知しやすい
- CI転用余地がある

---

### 8.2 既存パッケージ構成の骨格

提案名：既存構成温存方針
優先度：高

温存対象：
- cli
- application
- domain
- rule
- infrastructure
- util

方針：
- 既存構成は基本維持する
- 追加は web パッケージ中心で行う
- application に SharedAnalysisService を追加する

理由：
- 既存構造は理解しやすく、責務分離も妥当だから

---

## 9. E分類：今回未着手とするもの

提案名：今回スコープ外明示方針
優先度：最優先

今回未着手：
- JSON出力
- AI API自動連携
- 非同期ジョブ
- 進行状況詳細表示
- 認証
- 履歴管理
- マルチモジュール対応
- Gradle対応
- 高度なルール精度改善
- SaaS向けマルチテナント設計

理由：
- Web MVPの最小成立を優先するため
- CLI資産活用型Web化の本筋から外れるため
- 実装コストと設計複雑性を抑えるため

---

## 10. 破棄方針

提案名：即時破棄対象なし方針
優先度：最優先

結論：
- 現時点で即時に削除すべき主要資産はない
- 既存CLI資産は全て温存対象とする
- コメントアウト運用や暫定削除も避ける
- 使わない将来縮退候補があっても今回削除しない

理由：
- Web化段階で削除すると回帰検証が困難になる
- 既存CLIは実行可能で価値がある
- 今回の目的は削減ではなく拡張である

---

## 11. パッケージ構成の更新方針

提案名：既存構成温存＋web追加方式
優先度：最優先

更新後の推奨構成：

```text
com.qscout.spring

- cli
  - Main
  - CliApplication
  - ArgumentParser

- web
  - controller
    - WebPageController
    - WebDownloadController
  - service
    - WebAnalysisService
    - UploadValidationService
    - ZipExtractionService
    - TempWorkspaceService
    - DownloadArtifactService
  - dto
    - WebAnalysisResponse
    - DownloadLinkView
    - ErrorViewModel
    - ExecutionLimitView
  - exception
    - InvalidUploadException
    - InvalidProjectStructureException
    - AnalysisTimeoutException
    - ArtifactExpiredException

- application
  - ProjectScanner
  - RuleEngine
  - ScoreCalculator
  - ReportGenerator
  - AiMarkdownGenerator
  - SharedAnalysisService

- domain
  - AnalysisRequest
  - ProjectContext
  - AnalysisResult
  - RuleResult
  - Violation
  - Severity
  - ScoreSummary
  - ReportArtifact
  - ExecutionSummary

- rule
  - 既存維持

- infrastructure
  - 既存維持

- util
  - 既存維持
```

方針：
- 既存骨格は保持
- web を追加
- application に共通解析サービスを追加
- 解析本体は既存パッケージ群に残す

---

## 12. Codex向け改修優先順位

提案名：段階的移行実装順序
優先度：最優先

推奨順序：

1. SharedAnalysisService を新設する
2. CliApplication から解析本体処理を SharedAnalysisService へ移す
3. CLIが従来通り動くことを維持する
4. web パッケージを追加する
5. UploadValidationService を追加する
6. TempWorkspaceService を追加する
7. ZipExtractionService を追加する
8. WebAnalysisService を実装する
9. WebPageController を実装する
10. DownloadArtifactService を実装する
11. WebDownloadController を実装する
12. Web用DTO / Exception を追加する
13. 画面テンプレートを追加する
14. エラーハンドリングを整える
15. Docker / Render対応へ進む

理由：
- まず共通解析を固めることでCLI回帰を防げる
- その後にWeb入口を足す方が安全
- Web周辺I/Oは共通解析確立後の方が実装しやすい

---

## 13. 確定事項

本方針で確定する事項は以下の通りである。

1. そのまま流用するもの
- 6ルール
- DTO群
- ProjectScanner
- RuleEngine
- ScoreCalculator
- ReportGenerator
- AiMarkdownGenerator
- 主要ユーティリティ

2. 薄い改修で流用するもの
- CliApplication
- ArgumentParser周辺
- AnalysisRequest生成経路
- ReportArtifactの再活用
- 出力先パス制御
- MainとCLI標準出力

3. Web専用に新設するもの
- WebPageController
- WebDownloadController
- WebAnalysisService
- SharedAnalysisService
- UploadValidationService
- ZipExtractionService
- TempWorkspaceService
- DownloadArtifactService
- Web DTO / ViewModel / Exception

4. 破棄しないもの
- Main
- CLI起動方式
- usage文言
- 既存CLIの主要構成全体
- 既存パッケージ構成骨格

5. 今回未着手のもの
- JSON出力
- AI API自動連携
- 非同期ジョブ
- 認証
- 履歴管理
- 高度なルール改善
- Gradle対応
- マルチモジュール対応

---

## 14. 最終結論

本移行・流用方針の本質は、既存CLI資産を捨てずに、
解析コアを温存したまま、Web入口とWeb周辺I/O責務だけを追加することにある。

すなわち、今回のWeb化フェーズは以下の形で進める。

- CLIは維持する
- 解析コアは維持する
- 解析実行処理は共通化する
- Web専用責務は新設する
- 破棄は行わない
- MVPは最小改修で成立させる

この方針により、
既存CLIの価値を保持しつつ、Render公開可能なWeb MVPへの移行を、
最小コスト・最小リスクで実現できる。

---
