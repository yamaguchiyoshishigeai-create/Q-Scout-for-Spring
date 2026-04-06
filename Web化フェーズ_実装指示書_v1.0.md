# Codexにそのまま渡せる Web化実装指示書
## Q-Scout for Spring Web化フェーズ 実装指示書 v1.0
## 提案名：CLI資産活用型Webラップ実装指示書
## 優先度：最優先
## 理由：
## - 既存CLI資産を壊さずにWeb MVPへ最短到達するため
## - 設計再議論ではなく、Codexがそのまま連続実装できる粒度へ落とし込む段階に入っているため
## - Render公開とDocker化が必須要件として確定しているため

## 文書の位置づけ
本指示書は、既存CLI資産を活かしたQ-Scout for SpringのWeb MVP実装を、Codexが順番にそのまま進められるようにした実装作業指示書である。
要件・設計・移行方針は確定済みであり、本書では再設計ではなく実装タスク化を行う。

参照前提：
- 要件定義書 v0.2 :contentReference[oaicite:0]{index=0}
- 最小API設計 v0.1（パッケージ/API/DTO/ルール/実行フロー/設計原則） :contentReference[oaicite:1]{index=1} :contentReference[oaicite:2]{index=2} :contentReference[oaicite:3]{index=3} :contentReference[oaicite:4]{index=4} :contentReference[oaicite:5]{index=5} :contentReference[oaicite:6]{index=6}
- 既存CLI実装の現況調査結果 :contentReference[oaicite:7]{index=7}
- Web実行制約・運用前提 v0.1 :contentReference[oaicite:8]{index=8}
- Web化フェーズ全体設計方針書 v1.0 :contentReference[oaicite:9]{index=9}
- Web MVP最小要件定義書 v1.0 :contentReference[oaicite:10]{index=10}
- 最小Webアーキテクチャ設計書 v1.0 :contentReference[oaicite:11]{index=11}
- 既存CLI資産からの移行・流用方針 v1.0 :contentReference[oaicite:12]{index=12}
- 引継ぎプロンプト :contentReference[oaicite:13]{index=13}

--------------------------------------------------
1. 実装目的
--------------------------------------------------

### 提案名
Web MVP実装（CLI資産活用型）

### 優先度
最優先

### 理由
CLIとして成立済みの解析資産を最大流用し、ブラウザ利用可能な最小Webサービスへ拡張することが今回の主目的であるため。

### 実行担当
Codex

### 実行担当の理由
既存Javaコードベースに対する段階的改修、新規クラス追加、Controller/Service/Template/Dockerfile整備を連続実装する作業であり、Codexが最も適しているため。

### 作るもの
- Spring Boot Webアプリとして起動するWeb入口
- zipアップロード画面
- 同期実行の解析フロー
- 解析結果画面表示
- 人間用Markdown/AI用Markdownのダウンロード機能
- requestId単位の一時ディレクトリ運用
- Dockerfile
- Renderで動作可能な起動設定

### 作らないもの
- 解析ロジックの全面再設計
- 6ルールの高度化
- AI API自動連携
- JSON出力
- 非同期ジョブ
- 履歴管理
- 認証
- マルチモジュール対応
- Gradle対応
- SPA化
- DB導入

--------------------------------------------------
2. 実装の最重要原則
--------------------------------------------------

### 提案名
CLI非破壊・共通解析化原則

### 優先度
最優先

### 理由
今回の価値は「既存CLIを捨ててWebを作ること」ではなく、「CLI資産をそのまま使ってWeb化すること」にあるため。

### 実行担当
Codex

### 実行担当の理由
実装中に逸脱しやすい論点をコードレベルで防ぐ必要があるため。

### 原則
1. CLI資産を壊さない
2. 解析本体はSharedAnalysisServiceへ集約する
3. Web専用責務のみ新設する
4. 既存6ルール本体は原則触らない
5. MVP範囲を超える実装をしない
6. Render/Docker前提で構築する
7. ステートレス前提で/tmp運用する
8. 外部コマンド実行をしない
9. zipアップロードは非信頼入力として扱う
10. CLIとWebの入口は分離し、内部処理だけ共通化する

--------------------------------------------------
3. 変更対象の整理
--------------------------------------------------

### 提案名
変更対象4分類整理

### 優先度
最優先

### 理由
Codexがどこを触ってよいか、どこを温存すべきかを最初に明確化しないと回帰リスクが高まるため。

### 実行担当
Codex

### 実行担当の理由
実装範囲の逸脱防止に直結するため。

### A. 既存クラスで改修対象
- CliApplication
- Main（必要最小限のみ。原則はそのまま）
- ReportArtifactの扱い
- 既存の出力先パスの受け渡し経路
- 必要に応じてpom.xml（Spring Boot Web依存、テンプレート依存、プラグイン整理）

### B. 新設クラス
- application.SharedAnalysisService
- web.controller.WebPageController
- web.controller.WebDownloadController
- web.service.WebAnalysisService
- web.service.UploadValidationService
- web.service.ZipExtractionService
- web.service.TempWorkspaceService
- web.service.DownloadArtifactService
- web.dto.WebAnalysisResponse
- web.dto.DownloadLinkView
- web.dto.ErrorViewModel
- web.dto.ExecutionLimitView
- web.exception.InvalidUploadException
- web.exception.InvalidProjectStructureException
- web.exception.AnalysisTimeoutException
- web.exception.ArtifactExpiredException

### C. 温存対象
- MainのCLI起動方式
- ArgumentParser
- 6ルール群
- ProjectScanner
- RuleEngine
- ScoreCalculator
- ReportGenerator
- AiMarkdownGenerator
- domain配下の既存DTO群
- infrastructure配下の既存実装
- util配下の既存ユーティリティ

### D. 触らない対象
- 6ルールの検出ロジック精度改善
- AI API設計
- JSON出力設計
- 非同期化設計
- DB/永続化
- Gradle/マルチモジュール対応

--------------------------------------------------
4. パッケージ構成の最終案
--------------------------------------------------

### 提案名
既存構成温存＋web追加方式（最終版）

### 優先度
最優先

### 理由
既存構成の理解容易性を維持しつつ、Web責務だけを明確分離できるため。

### 実行担当
Codex

### 実行担当の理由
新規追加パッケージと既存責務境界をコードで正しく表現する必要があるため。

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
  - Rule
  - ControllerToRepositoryDirectAccessRule
  - FieldInjectionRule
  - TransactionalMisuseRule
  - ExceptionSwallowingRule
  - MissingTestRule
  - PackageDependencyViolationRule

- infrastructure
  - DefaultProjectScanner
  - DefaultRuleEngine
  - DefaultScoreCalculator
  - MarkdownReportGenerator
  - AiMarkdownFileGenerator

- util
  - FileCollector
  - CodeSnippetExtractor
  - MarkdownWriter

- resources
  - templates
    - index.html
    - fragments/error.html（任意。1ファイル完結でも可）
  - application.properties

--------------------------------------------------
5. 各クラスの責務
--------------------------------------------------

### 提案名
責務固定化方針

### 優先度
最優先

### 理由
Controllerに解析を持たせたり、Webサービスにドメインロジックを混在させる事故を防ぐため。

### 実行担当
Codex

### 実行担当の理由
責務逸脱はコード生成時に起こりやすく、事前固定が有効なため。

#### 5.1 SharedAnalysisService
責務：
- AnalysisRequestを受け取る
- ProjectScanner.scan を呼ぶ
- RuleEngine.analyze を呼ぶ
- ScoreCalculator.calculate を呼ぶ
- ReportGenerator.generate を呼ぶ
- AiMarkdownGenerator.generate を呼ぶ
- ReportArtifactを生成する
- CLI/Webの双方で再利用可能な解析結果を返す

注意：
- HTTPやMultipartFileを知らないこと
- requestIdやUI表示責務を持たないこと
- CLI引数にも依存しないこと

#### 5.2 CliApplication
責務：
- String[] args を受け取る
- ArgumentParserでAnalysisRequestを生成する
- SharedAnalysisServiceを呼び出す
- ExecutionSummaryを返す

注意：
- 解析本体を内部に持たない
- 既存CLI外部仕様を維持する

#### 5.3 WebPageController
責務：
- GET / で初期画面表示
- POST /analyze でアップロード受付
- WebAnalysisService呼び出し
- ViewModelをテンプレートへ渡す
- エラー時はユーザー向け表示を返す

注意：
- zip解凍やファイルI/O詳細を持たない
- 解析ロジックを持たない

#### 5.4 WebDownloadController
責務：
- GET /download/{requestId}/{fileKey} を受ける
- DownloadArtifactServiceで対象ファイル解決
- ダウンロードレスポンスを返す

注意：
- パス結合をControllerで直接行わない
- fileKeyは固定値のみ許可する

#### 5.5 WebAnalysisService
責務：
- MultipartFile受領
- UploadValidationService呼び出し
- TempWorkspaceServiceで作業領域作成
- zip保存
- ZipExtractionServiceで安全解凍
- projectRoot確定
- SharedAnalysisService呼び出し
- WebAnalysisResponse生成
- cleanup方針制御

注意：
- ドメイン解析ロジックは持たない
- 例外を適切にWeb例外へ変換する

#### 5.6 UploadValidationService
責務：
- ファイル未指定検証
- .zip拡張子検証
- サイズ上限検証
- content type補助確認
- 破損zipの基本妥当性確認

#### 5.7 TempWorkspaceService
責務：
- requestId発行
- /tmp/qscout/{requestId}/ 作成
- upload.zip / extracted / output のPath提供
- 保持期限判定
- cleanup実行

#### 5.8 ZipExtractionService
責務：
- Zip Slip防止付き解凍
- projectRoot探索
- pom.xml存在確認
- 複数候補時エラー化
- 外部コマンド不使用

#### 5.9 DownloadArtifactService
責務：
- requestId妥当性確認
- fileKeyを実ファイルへ安全マッピング
- 期限切れ確認
- ファイル存在確認
- Resource/ResponseEntity生成

#### 5.10 Web DTO / ViewModel
WebAnalysisResponse：
- requestId
- finalScore
- totalViolations
- highCount
- mediumCount
- lowCount
- humanDownloadLink
- aiDownloadLink
- message
- timedOut
- completed

DownloadLinkView：
- label
- url
- fileName

ErrorViewModel：
- userMessage
- detailCode
- retryable

ExecutionLimitView：
- maxUploadSizeMb
- maxExecutionSeconds
- allowedFileType

#### 5.11 Exception
InvalidUploadException：
- zip不正、未指定、サイズ超過

InvalidProjectStructureException：
- pom.xml不在、ルート不明、複数候補

AnalysisTimeoutException：
- 60秒超過

ArtifactExpiredException：
- ダウンロード保持期限切れ

#### 5.12 Template
index.html：
- 単画面完結UI
- アップロードフォーム
- 実行中表示
- 結果表示
- エラー表示
- ダウンロードリンク表示

#### 5.13 Docker関連
Dockerfile：
- Maven build
- 実行jar作成
- Render上で起動可能なコンテナ生成

--------------------------------------------------
6. 主要メソッド案
--------------------------------------------------

### 提案名
主要メソッド固定案

### 優先度
高

### 理由
Codexがクラス責務を具体的な実装単位へ落とし込みやすくするため。

### 実行担当
Codex

### 実行担当の理由
命名の揺れを減らし、実装速度と一貫性を上げるため。

#### SharedAnalysisService
- execute(AnalysisRequest request) : SharedAnalysisResult
  - 入力：AnalysisRequest
  - 出力：SharedAnalysisResult（新規内部用record/classで可）
  - 責務：scan/analyze/score/report/ai mdの一連実行
  - 注意：CLI/Web共通で使える戻り値にすること

#### CliApplication
- run(String[] args) : ExecutionSummary
  - 入力：CLI引数
  - 出力：ExecutionSummary
  - 責務：CLI入力から共通解析を呼ぶ
  - 注意：usageや例外契約は既存互換維持

#### WebAnalysisService
- analyze(MultipartFile projectZip) : WebAnalysisResponse
  - 入力：アップロードzip
  - 出力：画面表示用レスポンス
  - 責務：Web固有オーケストレーション
  - 注意：cleanup失敗が主処理成功を壊さないようにすること

#### UploadValidationService
- validate(MultipartFile file) : void
  - 入力：MultipartFile
  - 出力：なし
  - 責務：基本検証
  - 注意：メッセージはユーザー理解可能にすること

#### TempWorkspaceService
- createWorkspace() : WorkspaceContext
  - 入力：なし
  - 出力：WorkspaceContext（requestId, rootDir, uploadZipPath, extractedDir, outputDir, createdAt）
  - 責務：一時作業領域生成

- cleanupNow(WorkspaceContext workspace) : void
  - 責務：即時削除

- isExpired(String requestId) : boolean
  - 責務：保持期限判定

#### ZipExtractionService
- saveUpload(MultipartFile file, Path uploadZipPath) : void
  - 責務：アップロード保存

- extract(Path zipPath, Path extractedDir) : void
  - 責務：安全解凍

- resolveProjectRoot(Path extractedDir) : Path
  - 責務：pom.xmlを持つ解析対象ルート確定

#### DownloadArtifactService
- resolveForDownload(String requestId, String fileKey) : DownloadArtifact
  - 入力：requestId, fileKey
  - 出力：DownloadArtifact（Resource, fileName, contentType など）
  - 責務：安全な成果物解決
  - 注意：fileKeyは "human" / "ai" のみ許可

#### WebPageController
- showIndex(Model model) : String
- analyze(@RequestParam("projectZip") MultipartFile file, Model model) : String

#### WebDownloadController
- download(@PathVariable String requestId, @PathVariable String fileKey) : ResponseEntity<Resource>

--------------------------------------------------
7. 一時ディレクトリ設計
--------------------------------------------------

### 提案名
requestId単位一時ワークスペース方式

### 優先度
最優先

### 理由
同時実行許可、ステートレス運用、成果物管理、cleanup制御の全てに必要な中核設計であるため。

### 実行担当
Codex

### 実行担当の理由
ファイルI/Oとダウンロード設計がここに依存するため。

### パス構成
/tmp/qscout/{requestId}/
  upload.zip
  extracted/
  output/

output配下：
- qscout-report.md
- qscout-ai-input.md

### requestId方針
- UUIDまたは十分長いランダム文字列を使用
- 推測困難であること
- URLに含めてもよいが、パス探索に使わせないこと

### cleanup方針
- 成功時：即削除しない
- 保持時間：10〜15分
- エラー時：原則即削除
- 起動中に過去期限切れワークスペースを見つけた場合、アクセス時または新規作成時に掃除してよい

### 実装上の現実解
MVPでは専用スケジューラを必須にしない。
以下の簡易方式でよい。
- 新規解析開始時に /tmp/qscout/ 配下の期限切れディレクトリを簡易掃除
- ダウンロード要求時に期限切れなら削除して期限切れ扱い

--------------------------------------------------
8. ダウンロード設計
--------------------------------------------------

### 提案名
固定fileKey安全ダウンロード方式

### 優先度
最優先

### 理由
ダウンロード機能はMVP完成条件に含まれ、かつパストラバーサル事故の主要発生点であるため。

### 実行担当
Codex

### 実行担当の理由
セキュアなパス解決とHTTPレスポンス組み立てが実装課題になるため。

### エンドポイント
- GET /download/{requestId}/human
- GET /download/{requestId}/ai

### fileKeyの扱い
- human -> qscout-report.md
- ai -> qscout-ai-input.md
- 上記2つ以外は拒否

### 禁止事項
- fileNameをURLから直接受け取らない
- 任意パスを連結しない
- requestId/fileKeyからPathを文字列連結のみで生成しない

### 安全な解決手順
1. requestIdの形式を検証
2. workspace rootを固定ルールで解決
3. fileKeyを固定ファイル名へ変換
4. normalizeしたPathがworkspace配下にあることを確認
5. 存在確認
6. 期限切れ確認
7. ダウンロードレスポンス返却

### 有効期限切れ時
- ユーザー向け文言：
  「ダウンロード期限が切れました。再度アップロードして解析を実行してください。」
- HTTP：
  404または410のいずれかで統一してよい
- MVP推奨：
  410 Gone 相当の意味づけを内部で持ちつつ、実装簡略化のため404でも可
- ただし画面メッセージは明確に期限切れと分かること

--------------------------------------------------
9. 例外 / エラーハンドリング方針
--------------------------------------------------

### 提案名
ユーザー理解可能エラー統一方針

### 優先度
最優先

### 理由
MVPであっても失敗時に「何が悪いか」が分からないとプロダクトとして成立しないため。

### 実行担当
Codex

### 実行担当の理由
ControllerAdvice相当や例外変換実装が必要になるため。

### 例外分類
- InvalidUploadException
- InvalidProjectStructureException
- AnalysisTimeoutException
- ArtifactExpiredException
- RuntimeException（予期しない障害）

### ユーザー向け文言例
- zip未指定：
  「zipファイルを選択してください。」
- zip以外：
  「アップロード可能なのはzipファイルのみです。」
- サイズ超過：
  「ファイルサイズが上限を超えています。20MB以下のzipを指定してください。」
- pom.xmlなし：
  「pom.xml が見つかりません。Spring Boot / Mavenプロジェクトをアップロードしてください。」
- 解凍失敗：
  「zipファイルの解凍に失敗しました。破損していないか確認してください。」
- タイムアウト：
  「解析が時間制限を超えました。より小さいプロジェクトで再試行してください。」
- 期限切れ：
  「ダウンロード期限が切れました。再度解析を実行してください。」
- 想定外障害：
  「解析中に予期しないエラーが発生しました。時間をおいて再試行してください。」

### 開発者向けログ
- requestId
- 例外クラス名
- 原因メッセージ
- stack trace
- 可能なら処理段階（validate/extract/resolveProjectRoot/analyze/download）

### HTTPの扱い
- 入力不正：400系
- 構造不正：400系
- タイムアウト：408または504相当の内部扱いでよい
- 実行失敗：500系
- 期限切れ：404または410系

### 推奨実装
- @ControllerAdvice を1つ追加してもよい
- ただし過設計は避ける
- MVPでは画面返却中心でよい

--------------------------------------------------
10. 画面仕様
--------------------------------------------------

### 提案名
単画面完結型MVP UI実装指示

### 優先度
最優先

### 理由
要件上、単画面でアップロードから結果表示まで完結することが確定しているため。 :contentReference[oaicite:14]{index=14}

### 実行担当
Codex

### 実行担当の理由
テンプレート、フォーム、結果表示領域、エラー表示をまとめて実装する必要があるため。

### 画面に表示するもの
- タイトル
- ツール説明（簡潔）
- 対応形式：zip
- 上限サイズ：20MB
- 必須条件：pom.xml を含む Spring Boot / Maven / 単一モジュール / Java 17 想定
- ファイル選択
- 実行ボタン
- 実行中表示
- 結果表示
  - 総合スコア
  - 総違反件数
  - HIGH / MEDIUM / LOW 件数
  - ダウンロードリンク
- エラーメッセージ表示

### 実行中表示の扱い
MVPでは詳細進捗表示は不要。
以下で十分。
- 実行ボタン押下後、ボタン無効化
- 「解析中です。しばらくお待ちください。」等の簡易表示
- 同期処理完了後に結果表示へ切替

### エラー時の見せ方
- 同一画面内に赤系または明確なエラー領域表示
- 原因を1文で説明
- 再試行方法を簡潔に表示

### 画面実装方式
- サーバレンダリング
- 1テンプレートで完結
- JavaScriptは最小限で可
- SPA化しない

--------------------------------------------------
11. 実装順序
--------------------------------------------------

### 提案名
段階的安全実装順序

### 優先度
最優先

### 理由
まず共通解析を固めてCLI回帰を防ぎ、その後Web層を積み上げるのが最も失敗率が低いため。 :contentReference[oaicite:15]{index=15}

### 実行担当
Codex

### 実行担当の理由
連続実装可能な順序でそのまま着手させることが目的であるため。

#### STEP 1: pom.xmlをWebアプリ前提へ更新する
目的：
- Spring Boot Webアプリとして起動可能にする
- テンプレート描画とmultipart受信を可能にする

作業内容：
- Spring Boot starter web を追加
- テンプレートエンジン依存を追加（Thymeleaf推奨）
- 既存テスト依存は維持
- jar起動できる構成にする
- 既存CLI Mainも維持する

完了条件：
- アプリがWebアプリとして起動可能
- 既存CLIコンパイルが壊れていない

#### STEP 2: SharedAnalysisServiceを新設する
目的：
- CLI/Webで共有する解析本体を抽出する

作業内容：
- SharedAnalysisServiceをapplication配下に追加
- 既存ProjectScanner / RuleEngine / ScoreCalculator / ReportGenerator / AiMarkdownGeneratorを注入または内部組み立て
- 共通戻り値として SharedAnalysisResult を定義してよい
- ReportArtifactを正式活用する

完了条件：
- AnalysisRequestから解析・スコア・2種Markdown生成まで1メソッドで完了できる
- HTTP知識やCLI知識を含まない

#### STEP 3: CliApplicationをSharedAnalysisService委譲型へ改修する
目的：
- CLI互換を維持しながら内部共通化する

作業内容：
- run(String[] args) 内の解析本体処理をSharedAnalysisService呼び出しへ置換
- ArgumentParserは維持
- ExecutionSummaryの返却形式を維持

完了条件：
- 既存CLIと同じ引数で動く
- 標準出力契約を壊さない

#### STEP 4: CLI回帰テストを通す
目的：
- Web追加前に既存機能が壊れていないことを確認する

作業内容：
- 既存テストを実行
- 必要ならSharedAnalysisServiceに合わせて最小修正
- CLI実行の手動確認を1回行う

完了条件：
- 既存CLI主要テストが通る
- 既存出力ファイルが生成される

#### STEP 5: webパッケージ骨格を追加する
目的：
- Web責務の配置先を確定する

作業内容：
- controller/service/dto/exception を作成
- クラスの空定義または最小定義を追加

完了条件：
- パッケージ構成が最終案どおりになる

#### STEP 6: UploadValidationServiceを実装する
目的：
- アップロード不正を最初に遮断する

作業内容：
- 未指定検証
- zip拡張子検証
- サイズ制限20MB検証
- content type補助確認
- 基本的な妥当性確認

完了条件：
- 不正入力で明確な例外が投げられる
- 正常zipは通過する

#### STEP 7: TempWorkspaceServiceを実装する
目的：
- requestId単位の作業ディレクトリを安全管理する

作業内容：
- /tmp/qscout/{requestId}/ 作成
- upload.zip / extracted / output Path生成
- cleanup機能
- 期限判定機能
- 新規解析時の簡易期限切れ掃除

完了条件：
- requestId単位で独立ワークスペースが作れる
- cleanupできる

#### STEP 8: ZipExtractionServiceを実装する
目的：
- zipを安全に解凍し、解析対象ルートを確定する

作業内容：
- upload.zip保存
- Zip Slip防止付き解凍
- extracted直下優先でpom.xml探索
- 1階層補助探索
- 複数候補時エラー
- pom.xml不在時エラー

完了条件：
- 正常zipから解析対象Pathが決定できる
- 不正zipは明確に失敗する

#### STEP 9: WebAnalysisServiceを実装する
目的：
- Web固有の一連処理をまとめる

作業内容：
- validate
- workspace create
- upload save
- extract
- resolveProjectRoot
- AnalysisRequest生成
- SharedAnalysisService呼び出し
- WebAnalysisResponse生成
- cleanup制御

完了条件：
- MultipartFileから画面表示用レスポンスが返せる

#### STEP 10: Web DTO / Exceptionを実装する
目的：
- 画面表示責務とドメイン責務を分離する

作業内容：
- WebAnalysisResponse
- DownloadLinkView
- ErrorViewModel
- ExecutionLimitView
- 各Exception

完了条件：
- ControllerがドメインDTO直出ししなくて済む

#### STEP 11: WebPageControllerを実装する
目的：
- 単画面UIから解析実行できるようにする

作業内容：
- GET /
- POST /analyze
- 例外時の画面返却
- Modelへの値詰め

完了条件：
- 画面からアップロード実行できる

#### STEP 12: DownloadArtifactServiceを実装する
目的：
- 安全な成果物ダウンロードを提供する

作業内容：
- requestId / fileKey検証
- 成果物Path解決
- 期限切れ判定
- ResponseEntity<Resource>用情報生成

完了条件：
- human / ai の2種類のみダウンロード可能

#### STEP 13: WebDownloadControllerを実装する
目的：
- ダウンロードURLを公開する

作業内容：
- /download/{requestId}/human
- /download/{requestId}/ai
- 期限切れ時の応答
- パストラバーサル防止

完了条件：
- ブラウザから成果物取得できる

#### STEP 14: テンプレートを実装する
目的：
- MVP画面を成立させる

作業内容：
- index.html作成
- 入力部、実行中表示、結果部、エラー部を実装
- 最小限の見やすさを確保

完了条件：
- 単画面でアップロードから結果確認までできる

#### STEP 15: application.propertiesを設定する
目的：
- multipart上限と実行環境設定を明示する

作業内容：
- spring.servlet.multipart.max-file-size=20MB
- spring.servlet.multipart.max-request-size=20MB
- 必要ならserver.port=${PORT:8080}
- 必要ならテンプレートキャッシュ調整

注意：
Spring Bootのmultipart設定はプロパティで上限制御可能であり、max-file-size と max-request-size を明示できる。公式ドキュメント上、multipartのサイズ制御と一時保存先設定は標準機能として提供されている。 :contentReference[oaicite:16]{index=16}

完了条件：
- 20MB制約がアプリ設定として反映される

#### STEP 16: 60秒制限の実装を入れる
目的：
- Web運用前提に合わせて無制限処理を避ける

作業内容：
- WebAnalysisService内で解析呼び出しに時間制限を設ける
- 実装方式は簡易でよい
- 例：Future + get(timeout) など
- タイムアウト時はAnalysisTimeoutExceptionへ変換

完了条件：
- 60秒超過時に画面で明確なエラーとなる

#### STEP 17: Dockerfileを作成する
目的：
- Renderへデプロイ可能な実行単位を作る

作業内容：
- multi-stage build推奨
- build stage で mvn package
- runtime stage で jar実行
- 0.0.0.0 バインドで起動
- PORT環境変数追従

補足：
Render公式ドキュメントでは、WebサービスはPORT環境変数で指定されたポートへバインドすることが推奨されており、ホスト/ポート誤設定はデプロイトラブル原因になる。 :contentReference[oaicite:17]{index=17}

完了条件：
- docker build できる
- docker run でアプリ起動できる

#### STEP 18: Render前提の起動確認を行う
目的：
- 公開前提の最終整合確認

作業内容：
- PORT環境変数で起動することを確認
- /tmp 運用前提を確認
- ステートレス前提で永続化依存がないことを確認

完了条件：
- Renderへ持ち込める構成になっている

--------------------------------------------------
12. Docker / Render対応
--------------------------------------------------

### 提案名
Render対応Docker実装方針

### 優先度
最優先

### 理由
本件はRender公開が前提であり、Docker化が必須条件として確定しているため。

### 実行担当
Codex

### 実行担当の理由
アプリ構成とビルド構成をコードベースと一体で整える必要があるため。

### Dockerfile方針
- マルチステージビルド推奨
- build stage:
  - Maven + JDK 17
  - mvn -DskipTests package または状況に応じて test 実行
- runtime stage:
  - JRE 17
  - 生成jarをコピー
  - java -jar で起動

### 起動方式
- Webアプリを主起動対象にする
- CLI Mainは削除しない
- jar起動に統一してよい

### 環境変数
- PORT を使用
- 必要であれば server.port=${PORT:8080} を設定
- 一時ファイルは /tmp を使う
- 永続ディスク前提の設定を入れない

### Render前提
- ステートレス
- 永続ストレージなし
- HTTPサーバはPORTにバインド
- 失敗時もコンテナ再起動で成立する設計
- blueprint/render.yaml は今回必須ではないが、後続で追加可能

--------------------------------------------------
13. テスト方針
--------------------------------------------------

### 提案名
CLI回帰優先＋Web最低限テスト方針

### 優先度
最優先

### 理由
今回の最大リスクは既存CLI破壊であり、その次がWeb入口の不成立であるため。

### 実行担当
Codex

### 実行担当の理由
既存テスト維持と新規テスト追加を一括で進められるため。

### 既存CLI回帰確認
最低限維持すること：
- 引数解析
- pom.xml なし時エラー
- フィールドインジェクション検出
- 例外握りつぶし検出
- スコア計算
- レポート生成

既存CLIの実行可能性と出力契約は調査結果でも成立済みであり、Main -> CliApplication.run(String[] args) 構成、2つの位置引数、qscout-report.md / qscout-ai-input.md 出力が確認されている。 :contentReference[oaicite:18]{index=18}

### Web追加分の最低限テスト
1. zip未指定でエラー
2. zip以外でエラー
3. 20MB超過でエラー
4. pom.xmlなしzipでエラー
5. 正常zipで解析成功
6. スコアと違反件数が画面に出る
7. humanダウンロード成功
8. aiダウンロード成功
9. 不正fileKeyで拒否
10. 期限切れrequestIdで失敗

### テスト手段
- Service単体テスト
- ControllerレベルのWebMvcTest相当
- 必要最小限の統合テスト
- 手動確認も可。ただし受け入れ条件は明示しておくこと

--------------------------------------------------
14. 受け入れ条件
--------------------------------------------------

### 提案名
Web MVP受け入れ条件（確定版）

### 優先度
最優先

### 理由
実装完了判定の揺れをなくすため。

### 実行担当
人間

### 実行担当の理由
最終的な「完成判定」は利用者視点・要件視点の確認を伴うため、人間によるレビューが必要であるため。

### A. Web MVP完成条件
1. ブラウザからzipアップロードできる
2. 同期で解析実行できる
3. スコアが画面表示される
4. 違反件数と重大度内訳が表示される
5. 人間用Markdownをダウンロードできる
6. AI用Markdownをダウンロードできる
7. エラー時に原因が理解できる
8. 60秒超過時にタイムアウト扱いになる
9. /tmp運用で一時ファイルが管理される
10. requestId単位で成果物が分離される

### B. CLIが壊れていない条件
1. Mainから従来どおり起動できる
2. projectRootPath / outputDirectory の2引数契約が維持される
3. qscout-report.md が生成される
4. qscout-ai-input.md が生成される
5. 標準出力の基本項目が維持される
6. 既存主要テストが通る

### C. Render公開前条件
1. Docker build 成功
2. PORT追従で起動可能
3. 0.0.0.0 バインドで待受できる
4. /tmp へ書き込み可能
5. 永続ストレージ依存がない
6. 20MB制限が有効
7. パストラバーサル脆弱性がない
8. 外部コマンド未使用
9. 期限切れ成果物を取得できない

--------------------------------------------------
15. Codex向け実装上の注意
--------------------------------------------------

### 提案名
過設計防止ルール

### 優先度
最優先

### 理由
今回の失敗パターンは「綺麗に作り込みすぎてMVPが遅れること」であり、それを防ぐ必要があるため。

### 実行担当
Codex

### 実行担当の理由
コード生成時の逸脱抑止が目的であるため。

### 必須注意
- 過設計しない
- 既存6ルール本体を触りすぎない
- JavaParser導入を今回必須にしない
- AI APIを実装しない
- JSON出力を作らない
- 非同期ジョブを作らない
- DBを入れない
- 認証を入れない
- 履歴を入れない
- SPA化しない
- Web側からCLI引数配列を組み立てて呼ばない
- Web側はSharedAnalysisServiceを直接使う
- 例外文言はユーザー理解可能にする
- 開発者向け詳細はログへ回す
- cleanup失敗で主処理成功を無にしない
- path操作は常にnormalizeして配下判定する
- zip解凍に外部コマンドを使わない
- requestId/fileKeyは信用しない

--------------------------------------------------
16. 推奨実装判断
--------------------------------------------------

### 提案名
現時点での最良実装判断

### 優先度
最優先

### 理由
設計済み情報と既存CLI資産を踏まえると、最短・安全・拡張可能性のバランスが最も良いため。

### 実行担当
ChatGPT

### 実行担当の理由
全資料横断で最終方針を固定し、Codexへ迷いのない実装判断を渡す役割を担うため。

### 推奨判断
- テンプレートエンジンはThymeleafで十分
- SharedAnalysisServiceを移行の中核にする
- Web DTOは必要最小限に留める
- cleanupは簡易な期限切れ掃除で十分
- タイムアウトはMVPでは簡易実装で十分
- ダウンロードは固定fileKeyのみ許可する
- Render向けにはPORT追従 + /tmp運用 + Dockerfile整備を優先する
- render.yaml は今回必須にしない
- まずローカルDockerで動作確認し、その後Renderへ持ち込む

--------------------------------------------------
17. Codex投入用プロンプト最終版
--------------------------------------------------

### 提案名
Codex投入用プロンプト最終版

### 優先度
最優先

### 理由
実装指示書をさらにCodex実行用の作業命令へ直接変換し、着手時の解釈ブレを最小化するため。

### 実行担当
人間

### 実行担当の理由
実際にCodexへ投入する最終操作は人間が行うため。

以下をCodexへそのまま渡してよい。

あなたは既存Javaプロジェクト「Q-Scout for Spring」に対して、CLI資産を壊さずにWeb MVPを追加実装してください。

最重要方針：
1. CLI資産を壊さない
2. 解析本体は SharedAnalysisService に共通化する
3. Web専用責務だけを追加する
4. 既存6ルール本体は原則変更しない
5. 過設計しない
6. Render/Docker前提で実装する
7. AI API、JSON出力、非同期ジョブ、認証、履歴管理は実装しない

実装対象：
- application.SharedAnalysisService を新設
- CliApplication を SharedAnalysisService 委譲型へ変更
- web.controller.WebPageController
- web.controller.WebDownloadController
- web.service.WebAnalysisService
- web.service.UploadValidationService
- web.service.ZipExtractionService
- web.service.TempWorkspaceService
- web.service.DownloadArtifactService
- web.dto.WebAnalysisResponse
- web.dto.DownloadLinkView
- web.dto.ErrorViewModel
- web.dto.ExecutionLimitView
- web.exception.InvalidUploadException
- web.exception.InvalidProjectStructureException
- web.exception.AnalysisTimeoutException
- web.exception.ArtifactExpiredException
- templates/index.html
- application.properties
- Dockerfile

確定仕様：
- 単画面UI
- zipのみアップロード
- 最大20MB
- pom.xml 必須
- 同期処理
- 60秒タイムアウト
- /tmp/qscout/{requestId}/ 配下で作業
- 出力ファイルは qscout-report.md と qscout-ai-input.md
- ダウンロードURLは /download/{requestId}/human と /download/{requestId}/ai
- requestIdごとに分離
- 成功時は即削除せず10〜15分保持
- エラー時は即削除
- 外部コマンド禁止
- パストラバーサル防止必須
- Render向けに PORT 環境変数へ追従

実装順序：
1. pom.xml更新
2. SharedAnalysisService新設
3. CliApplication改修
4. CLI回帰確認
5. webパッケージ追加
6. UploadValidationService
7. TempWorkspaceService
8. ZipExtractionService
9. WebAnalysisService
10. Web DTO / Exception
11. WebPageController
12. DownloadArtifactService
13. WebDownloadController
14. index.html
15. application.properties
16. 60秒タイムアウト
17. Dockerfile
18. ローカル起動確認

完了条件：
- CLIが従来どおり動く
- ブラウザからzipをアップロードして解析できる
- スコアと違反件数が画面表示される
- 2種類のMarkdownがダウンロードできる
- 不正入力時に理解可能なエラーが出る
- Dockerで起動できる
- Renderへ持ち込める構成になっている

出力方針：
- まずは必要ファイルをまとめて実装
- 次に不足修正
- 最後に変更ファイル一覧と確認手順を出力
- 過度な抽象化や不要な拡張は行わない

## 以上