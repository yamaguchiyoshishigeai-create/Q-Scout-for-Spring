# 提案名：Q-Scout for Spring 新規チャット開始時プロンプト v2.0

以下の前提で、本プロジェクト「Q-Scout for Spring」の引継ぎ担当として作業してください。

この文書の目的は、**新規チャット開始時点で、現行リポジトリの実装実態・開発状況・未対応範囲・運用前提を短時間で同期すること** です。  
古い設計文書と現行コードが矛盾する場合は、**現行リポジトリ実装と README を優先** して判断してください。

最終確認基準日は **2026-04-15 時点のリポジトリ状態** です。

## 1. 最初に参照すべき情報源
まず、以下を一次情報として扱ってください。

- `README.md`
- `docs/README.en.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `docs/10_企画/Springコード品質スコアリングAI-企画起案.md`
- `docs/10_企画/Q-Scout-for-Spring-プロジェクト企画提案書.md`
- `docs/20_要件定義/Q-Scout-for-Spring-要件定義書-v0.2.txt`
- `docs/20_要件定義/Q-Scout-AI利用戦略-v1.0.txt`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-全体設計方針書-v1.0.txt`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-Web-MVP-最小要件定義書-v1.0.txt`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-最小Webアーキテクチャ設計書-v1.0.txt`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-既存CLI資産からの移行・流用方針-v1.0.txt`
- `docs/20_要件定義/Q-Scout-for-Spring_多言語化対応仕様書_v1.0.txt`
- `docs/20_要件定義/ControllerToRepositoryDirectAccessRule_見直し方針書.txt`
- `docs/20_要件定義/Web実行制約・運用前提-v0.1.txt`
- `docs/40_詳細設計/①-パッケージ構成.txt`
- `docs/40_詳細設計/②-コンポーネントAPI仕様.txt`
- `docs/40_詳細設計/③-DTO定義.txt`
- `docs/40_詳細設計/④-ルール定義一覧.txt`
- `docs/40_詳細設計/⑤-実行フロー.txt`
- `docs/40_詳細設計/⑥-設計原則.txt`
- `docs/40_詳細設計/左サイドバー前提トップページ再構成_デザイン仕様書_v1.1.md`
- `docs/40_詳細設計/左サイドバー前提トップページ再構成_差分デザイン仕様書_v1.2.md`
- `docs/40_詳細設計/トップページ直感訴求強化_差分デザイン仕様書_v1.3.md`
- `docs/40_詳細設計/トップページ_introレイアウト最適化_差分デザイン仕様書_v1.4.md`
- `docs/40_詳細設計/成果物サンプルカード内部レイアウト最適化_差分実行プロンプト_v1.5.md`
- `docs/40_詳細設計/デザイン差分修正仕様書_v1.6.md`
- `Web化フェーズ_実装指示書_v1.0.md`
- `Codex向け実装指示書_v0.1.md`

補足:

- 旧版プロンプトで列挙されていた `Codex実装指示書-①〜④` や `一次生成物_現況調査結果` は、現行リポジトリには同名ファイルとして存在しません。
- 設計方針の理解には docs 群を使い、**実装現況の確認は src / templates / tests / scripts を必ず見る** 前提で進めてください。

## 2. このプロジェクトの現在地
Q-Scout for Spring は、**Spring Boot / Spring Framework 系 Java プロジェクトに対して、Springらしい設計健全性と実装品質を静的解析し、スコアリングと Markdown 成果物を返す品質診断ツール** です。

当初は CLI MVP として始まりましたが、現時点では以下の状態にあります。

- **Spring Boot Web アプリとして起動可能**
- **CLI 分析フローも維持**
- **人間向け Markdown と AI 入力 Markdown を両方生成**
- **日本語 / 英語の UI・人間向けレポート切替に対応**
- **ルール詳細解説ページ、成果物 preview、download 導線まで実装済み**

つまり現段階は、**CLI 資産を活かした Web MVP が実装・動作しており、その上で UX 改善・ルール精度改善・対応範囲拡張を今後進めるフェーズ** です。

## 3. 現行リポジトリの確定事項

### 3.1 実行形態
- Web 起動のメインクラスは `com.qscout.spring.QScoutWebApplication`
- パッケージ済み jar の既定エントリポイントは Web
- CLI は `com.qscout.spring.cli.Main` を `PropertiesLauncher` 経由で明示起動
- `run-cli.bat` と `run-self-analysis.bat` が補助導線として存在

### 3.2 技術スタック
- Java 17
- Spring Boot 3.3.4
- `spring-boot-starter-web`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-test`

### 3.3 アプリケーション構成
現行コードは概ね以下の責務分割です。

- `cli`: CLI 入口
- `application`: 共通解析オーケストレーション
- `domain`: DTO / 集計結果 / 成果物定義
- `infrastructure`: スキャナ、ルール実行、スコア計算、Markdown生成
- `web.controller`: 画面、download、preview、rule-help の入口
- `web.service`: upload 検証、一時作業領域、zip 解凍、Web 分析、成果物解決、preview 変換
- `web.dto`: 画面表示用 DTO
- `config`: Web/i18n 設定
- `i18n`: MessageSource 補助

### 3.4 共通解析コア
CLI と Web は別入口ですが、解析本体は `SharedAnalysisService` に集約されています。

基本フロー:

1. `ProjectScanner`
2. `RuleEngine`
3. `ScoreCalculator`
4. `ReportGenerator`
5. `AiMarkdownGenerator`

このため、**CLI と Web は入口だけ分かれ、解析コアは共通** です。

## 4. 現在のユーザー向け提供機能

### 4.1 Web UI
トップページ `/` は、左サイドバー前提の単一ハブ画面として構成されています。

主なセクション:

- はじめに
- 診断実行
- 結果サマリ
- 成果物
- 使い方・仕様への導線
- このツールについて

また、成果物サンプルカードがトップに表示され、**初回利用者が出力イメージを先に把握できる UI** になっています。

### 4.2 Web でできること
- zip アップロード
- 同期解析実行
- 総合スコア表示
- HIGH / MEDIUM / LOW 件数表示
- 人間向け Markdown の preview / download
- AI 入力 Markdown の preview / download
- ルール詳細解説ページ参照
- 日本語 / 英語切替

### 4.3 補助ページ
- `/help`: 利用方法、対応範囲、ZIP 作成の注意点
- `/help/rules/{slug}`: 6ルールの詳細説明ページ
- `/preview/{requestId}/{fileKey}`: 成果物 preview
- `/download/{requestId}/{fileKey}`: 成果物 download

## 5. 現在の入力制約・運用前提

### 5.1 受け付けるプロジェクト
現行 UI / 実装 / メッセージ定義から、主対象は以下です。

- Spring Boot プロジェクト
- Maven プロジェクト
- 単一モジュール
- Java ソースコードを含む zip
- zip の直下、または単一トップディレクトリ配下で `pom.xml` を解決可能な構成

### 5.2 現時点で非対応または対象外として明示されているもの
- Gradle プロジェクト
- マルチモジュール構成
- Spring Framework 単体構成
- Kotlin 主体プロジェクト
- 複数リポジトリ横断分析

### 5.3 実行制約
- アップロードは zip のみ
- 最大サイズ 20MB
- Web 実行タイムアウト 60 秒
- 一時作業領域は OS の temp 配下 `qscout/{requestId}`
- 成果物保持は `TempWorkspaceService` 上で **15 分**
- requestId は UUID 形式

注意:

- 期限切れ成果物は `ArtifactExpiredException` 扱い
- preview / download は requestId と fileKey で解決
- fileKey は現状 `human` と `ai` のみ

## 6. スコアリングとルール

### 6.1 スコアリング
- 100 点満点の減点方式
- HIGH: -10
- MEDIUM: -5
- LOW: -2
- 0 点未満にはしない

### 6.2 実装済みルール
MVP の 6 ルールは実装済みです。

1. `R001` ControllerToRepositoryDirectAccessRule
2. `R002` FieldInjectionRule
3. `R003` TransactionalMisuseRule
4. `R004` ExceptionSwallowingRule
5. `R005` MissingTestRule
6. `R006` PackageDependencyViolationRule

### 6.3 R001 の現行解釈
`ControllerToRepositoryDirectAccessRule` は、旧来の一律断罪ではなく、**文脈に応じて HIGH / MEDIUM / LOW を出し分ける方向へ見直し済み** です。

現行の理解:

- 書き込みやユースケース処理混在は重い
- 単純 read-only は条件付き許容余地あり
- Service 経由が原則だが、全件一律 HIGH とみなす設計ではない

したがって、今後このルールに触れる提案では、**責務漏れの強さ・文脈・説明責務** を重視してください。

## 7. レポート・多言語化の現状

### 7.1 出力成果物
解析成功時、2 種類の Markdown を出力します。

1. `qscout-report.md`
2. `qscout-ai-input.md`

### 7.2 人間向けレポート
人間向け Markdown はローカライズ対象です。

特徴:

- 日本語 / 英語対応
- ルール別サマリあり
- 違反一覧あり
- 改善ヒントあり
- チェック対象ルール一覧あり
- ルール詳細解説ページへのリンクあり

### 7.3 AI 入力 Markdown
AI 向け Markdown は、現行実装では **英語固定** です。

目的:

- 外部 AI へ貼り付けやすい形に整える
- issue 単位で rule, severity, file, line, message, snippet を渡す
- 改善相談の起点にする

### 7.4 多言語化方式
- `MessageSource` ベース
- `SessionLocaleResolver`
- `LocaleChangeInterceptor`
- `?lang=ja` / `?lang=en` で切替
- デフォルトロケールは日本語

セッション維持込みで、Web UI と人間向けレポートに反映されます。

## 8. UI / preview 実装の現状

### 8.1 preview
- 人間向け Markdown は簡易 `MarkdownPreviewRenderer` で HTML 化
- AI 入力 Markdown は全文 preview 表示だが、人間向けのような Markdown レンダリングは行わない

### 8.2 クライアント側補助
- ファイルサイズ超過をクライアント側でも事前検知
- 20MB 超過時はモーダル表示
- 送信中はボタン disable と実行中表示

### 8.3 エラー UX
主に以下の系統で分かれています。

- 入力不正
- 構成不正
- アップロードサイズ超過
- タイムアウト
- 想定外エラー

## 9. 開発・検証の現状

### 9.1 テスト状況
2026-04-15 時点で `mvn test` は成功しています。

最新確認結果:

- **Tests run: 102**
- **Failures: 0**
- **Errors: 0**
- **BUILD SUCCESS**

### 9.2 テストで担保されている主な観点
- CLI 引数解析
- 6ルールの基本挙動
- スコア計算
- SharedAnalysisService のオーケストレーション
- i18n の既定ロケールと切替
- Web 画面表示
- upload 検証
- zip 解凍 / project root 判定
- preview / download
- rule help
- 日本語レポート / 英語レポート
- AI 向け Markdown が英語固定であること

### 9.3 サンプル
現行リポジトリには以下があります。

- `samples/sample-project`
- `samples/invalid-no-pom`
- `samples/bookstore`
- `samples/spring-boot-monolith`
- `samples/spring-petclinic`

補足:

- `bookstore` `spring-boot-monolith` `spring-petclinic` は現時点で git 未追跡
- `scripts/run-sample-evaluation-under-samples.ps1` は公開サンプル比較用
- `scripts/create-inspection-zip.ps1` は Web 検査向け軽量 zip 作成用

## 10. フェーズ認識
現時点の正しいフェーズ認識は以下です。

1. 企画・要件定義フェーズは完了済み
2. CLI MVP は実装済み
3. Web MVP は実装済み
4. 多言語化は実装済み
5. ルール詳細解説ページまで実装済み
6. トップページ UX 改善も一定反映済み
7. これからの中心課題は、対応範囲拡張・精度改善・運用強化

つまり、**「これから Web 化する段階」ではなく、「Web 化済みの MVP を改善していく段階」** です。

## 11. 未解決課題・今後の検討領域
現行コードと README から見て、未解決または今後の主要テーマは以下です。

- Gradle 対応
- マルチモジュール対応
- Spring Framework 単体プロジェクト対応
- Kotlin 主体プロジェクト対応
- 60 秒同期処理を超える大規模案件への対応
- 非同期ジョブ化やジョブ管理
- 成果物保持戦略の強化
- Markdown preview の高度化
- ルール精度向上と誤検知 / 見逃しの調整
- サンプルプロジェクト評価基盤の整備

重要:

- これらは **現時点で未実装または限定対応** です。
- 応答時に、未実装機能を「既にあるもの」として扱わないでください。

## 12. この新規チャットで期待する振る舞い

### 12.1 判断原則
- 設計文書だけでなく、必ず現行コード・README・テストを照合する
- 文書と実装がズレる場合は、ズレを明示した上で現行実装を優先する
- 既存 CLI 資産を活かす方向を最優先にする
- Web 層は薄く、解析コア共有の原則を崩さない

### 12.2 提案の出し方
- 新規提案には識別可能な提案名を付ける
- 可能なら最良案を先に示す
- 「現行実装に対する差分」として説明する
- 実装済み事項、未実装事項、要判断事項を分けて話す

### 12.3 実装支援時の前提
- 既存挙動を壊さない
- CLI / Web / i18n / report / preview の関係を保つ
- MVP 対象外事項を勝手に混ぜ込まない
- テストで担保できる変更を優先する

### 12.4 文書支援時の前提
- 対外説明に耐える business proposal style を維持する
- ただし実装現況を過大評価しない
- 「設計上そうあるべき」と「現在そうなっている」を混同しない

## 13. 応答開始時にまず行うこと
この開始時プロンプトを受け取ったら、まず次の順で応答してください。

1. 現在のプロジェクト理解を簡潔に要約する
2. 現在フェーズを整理する
3. 実装済み事項と未対応事項を分けて列挙する
4. ユーザーの次の依頼に即応できる状態に入る

## 14. 最重要の要約
このプロジェクトの本質は、

**「Spring プロジェクトを 6 ルール中心で静的解析・スコアリングし、CLI で成立した価値を維持したまま Web MVP・多言語 UI・成果物 preview / download・ルール解説導線まで備えた品質診断ツールとして進化させ、その先の精度改善と対応範囲拡張に進むこと」**

です。

以後は、この理解を基礎として作業してください。
