# 提案名：Q-Scout for Spring 新規チャット開始時プロンプト v3.0

以下の前提で、本プロジェクト「Q-Scout for Spring」の引継ぎ担当として作業してください。

この文書の目的は、**新規チャット開始時点で、現行リポジトリの実装実態・docs 構成・運用ルール・未対応範囲を、リポジトリ連携を使って短時間で同期すること**です。  
古い設計文書と現行コードが矛盾する場合は、**現行リポジトリ実装・README・テストを優先**して判断してください。

---

## 1. 基本方針

- 本プロジェクトでは、**リポジトリ連携を利用して現行状態を確認すること**を前提とする
- 文書だけで判断せず、**README / docs / src / templates / tests / scripts を照合**する
- docs は整理済みであり、**docs 全体索引 → 企画 → 将来構想 → 要件定義 → 詳細設計** の順で読む
- Codex 作業結果確認は、原則として **リポジトリルートの `CodexExec.result` を起点にし、`pr url` がある場合は PR 差分を最優先で確認** する
- 文書と実装がズレる場合は、**ズレを明示した上で現行実装を優先**する
- 既存 CLI 資産を活かし、**Web 層は薄く、解析コア共有**の原則を崩さない

---

## 2. 新規チャット開始時に最初に行う精査

新規チャット開始時は、まずリポジトリ連携で以下を確認してください。

### 2.1 まず確認するもの
1. 対象リポジトリと対象ブランチ
2. `README.md`
3. `docs/README.md`
4. `docs/00_プロジェクト管理/05_横断運用規程/CodexExec.result運用ルール.md`
5. 必要に応じて `CodexExec.result`

### 2.2 次に確認するもの
6. `docs/10_企画/Q-Scout-for-Spring-プロジェクト企画書-v2.0.md`
7. `docs/10_企画/15_将来構想/Q-Scout-AI利用戦略-v1.0.md`
8. `docs/20_要件定義/20_要件定義ガイド.md`
9. `docs/20_要件定義/` 配下の現行正本
10. `docs/20_要件定義/` 配下の現行補助仕様
11. 必要に応じて `docs/40_詳細設計/`
12. `pom.xml`
13. `src/main/resources/application.properties`
14. 必要な `src/main/java`, `src/test/java`, `src/main/resources/templates`, `scripts`

### 2.3 精査時の原則
- docs の構成理解は、**個別ファイルの丸読み前に索引ファイルを読む**
- 現状確認は、**README とコードとテストで裏取り**する
- 未解決課題や途中作業の確認には、必要に応じて **`CodexExec.result`** を参照する
- 旧版開始時プロンプトに書かれていた固定ファイル一覧を鵜呑みにせず、**現行リポジトリ内の実在ファイルを優先**する

---

## 3. 最初に参照すべき情報源

### 3.1 最優先の索引・運用文書
- `README.md`
- `docs/README.md`
- `docs/00_プロジェクト管理/05_横断運用規程/CodexExec.result運用ルール.md`

### 3.2 企画・将来構想
- `docs/10_企画/Q-Scout-for-Spring-プロジェクト企画書-v2.0.md`
- `docs/10_企画/15_将来構想/Q-Scout-AI利用戦略-v1.0.md`

### 3.3 要件定義
- `docs/20_要件定義/20_要件定義ガイド.md`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-Web-MVP-最小要件定義書-v1.0.md`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-全体設計方針書-v1.0.md`
- `docs/20_要件定義/Q-Scout-for-Spring_多言語化対応仕様書_v1.0.md`
- `docs/20_要件定義/Web実行制約・運用前提-v0.1.md`

### 3.4 要件補助仕様
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-既存CLI資産からの移行・流用方針-v1.0.md`
- `docs/20_要件定義/Q-Scout-for-Spring-Web化フェーズ-最小Webアーキテクチャ設計書-v1.0.md`
- `docs/20_要件定義/ControllerToRepositoryDirectAccessRule_見直し方針書.md`

### 3.5 詳細設計
- `docs/40_詳細設計/` 配下の現行 `.md` / `.txt`
- 特に UI 差分設計・画面改善系の `.md`

### 3.6 実装確認用
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/**`
- `src/test/java/**`
- `src/main/resources/templates/**`
- `scripts/**`

---

## 4. docs 構造の現在地

現行 docs は概ね以下の構造として理解してください。

- `docs/00_プロジェクト管理/`  
  プロジェクト全体または docs 全体に横断的に効く管理文書、運用文書、規程文書の親フォルダです。  
  特に `05_横断運用規程/` は、`CodexExec.result` 関連運用を含む正式な横断運用規程の集約先です。

- `docs/10_企画/`  
  プロジェクトの背景、目的、提供価値、将来構想の起点となる企画文書群です。

- `docs/20_要件定義/`  
  現行要件、制約、運用前提、補助仕様を定義する層です。  
  読み順や正本・補助仕様の区別は `20_要件定義ガイド.md` を起点に判断してください。

- `docs/30_基本設計/`  
  要件を受けて、システム全体をどの構成・責務分担・主要処理方式で実現するかを定義する正式な骨格設計層です。  
  全体構成、CLI と Web の関係、共通解析コアの位置づけは `30_基本設計/基本設計.md` を優先してください。

- `docs/40_詳細設計/`  
  個別 UI、差分設計、Figma 運用、実装指示書、整理方針などを扱う詳細設計層です。  
  入口文書は `40_詳細設計/詳細設計.md` です。

- `docs/50_実装/`  
  実装工程に継続適用する正式文書の配置先です。現時点ではプレースホルダ運用です。

- `docs/60_単体テスト/`  
  単体テスト工程に継続適用する正式文書の配置先です。現時点ではプレースホルダ運用です。

- `docs/70_結合テスト/`  
  結合テスト工程に継続適用する正式文書の配置先です。現時点ではプレースホルダ運用です。

- `docs/90_アーカイブ/`  
  履歴資料、旧フェーズ資料、現行正本ではない過去文書の配置先です。

重要:
- **管理系文書の現行主軸は `00_プロジェクト管理/` です**
- **将来構想は要件定義ではなく、企画の延長として扱います**
- **履歴資料は現行仕様判断の基準にしません**
- **現行要件判断は `20_要件定義ガイド.md`、全体骨格判断は `30_基本設計/基本設計.md` を起点に行ってください**

---

## 5. このプロジェクトの現在地

Q-Scout for Spring は、**Spring Boot / Spring Framework 系 Java プロジェクトに対して、Springらしい設計健全性と実装品質を静的解析し、スコアリングと Markdown 成果物を返す品質診断ツール**です。

当初は CLI MVP として始まりましたが、現時点では以下の状態にあります。

- **Spring Boot Web アプリとして起動可能**
- **CLI 分析フローも維持**
- **人間向け Markdown と AI 入力 Markdown を両方生成**
- **日本語 / 英語の UI・人間向けレポート切替に対応**
- **ルール詳細解説ページ、成果物 preview、download 導線まで実装済み**

つまり現段階は、**CLI 資産を活かした Web MVP が実装・動作しており、その上で UX 改善・ルール精度改善・対応範囲拡張を今後進めるフェーズ**です。

---

## 6. 現行リポジトリの確定事項

### 6.1 実行形態
- Web 起動のメインクラスは `com.qscout.spring.QScoutWebApplication`
- パッケージ済み jar の既定エントリポイントは Web
- CLI は `com.qscout.spring.cli.Main` を `PropertiesLauncher` 経由で明示起動
- `run-cli.bat` と `run-self-analysis.bat` が補助導線として存在

### 6.2 技術スタック
- Java 17
- Spring Boot 3.3.4
- `spring-boot-starter-web`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-test`

### 6.3 アプリケーション構成
現行コードは概ね以下の責務分割です。

- `cli`: CLI 入口
- `application`: 共通解析オーケストレーション
- `domain`: DTO / 集計結果 / 成果物定義
- `infrastructure`: スキャナ、ルール実行、スコア計算、Markdown生成
- `web.controller`: 画面、download、preview、rule-help の入口
- `web.service`: upload 検証、一時作業領域、zip 解凍、Web 分析、成果物解決、preview 変換
- `web.dto`: 画面表示用 DTO
- `config`: Web / i18n 設定
- `i18n`: MessageSource 補助

### 6.4 共通解析コア
CLI と Web は別入口ですが、解析本体は `SharedAnalysisService` に集約されています。

基本フロー:

1. `ProjectScanner`
2. `RuleEngine`
3. `ScoreCalculator`
4. `ReportGenerator`
5. `AiMarkdownGenerator`

このため、**CLI と Web は入口だけ分かれ、解析コアは共通**です。

---

## 7. 現在のユーザー向け提供機能

### 7.1 Web UI
トップページ `/` は、左サイドバー前提の単一ハブ画面として構成されています。

主なセクション:
- はじめに
- 診断実行
- 結果サマリ
- 成果物
- 使い方・仕様への導線
- このツールについて

また、成果物サンプルカードがトップに表示され、**初回利用者が出力イメージを先に把握できる UI** になっています。

### 7.2 Web でできること
- zip アップロード
- 同期解析実行
- 総合スコア表示
- HIGH / MEDIUM / LOW 件数表示
- 人間向け Markdown の preview / download
- AI 入力 Markdown の preview / download
- ルール詳細解説ページ参照
- 日本語 / 英語切替

### 7.3 補助ページ

- `/help`: 利用方法、対応範囲、ZIP 作成の注意点
- `/help/rules/{slug}`: 6ルールの詳細説明ページ
- `/preview/{requestId}/{fileKey}`: 成果物 preview
- `/download/{requestId}/{fileKey}`: 成果物 download

重要:
- 現行実装では、preview / download は **署名付き URL 方式** で提供されます
- 実アクセス時には、`requestId` / `fileKey` に加えて **有効期限とアクセス用トークン** による検証を前提とします
- したがって、内部識別子は `requestId` と `fileKey` ですが、**利用者向け導線としては署名付き URL により安全に参照させる方式** になっています

---

## 8. 現在の入力制約・運用前提

### 8.1 受け付けるプロジェクト
現行 UI / 実装 / メッセージ定義から、主対象は以下です。

- Spring Boot プロジェクト
- Maven プロジェクト
- 単一モジュール
- Java ソースコードを含む zip
- zip の直下、または単一トップディレクトリ配下で `pom.xml` を解決可能な構成

### 8.2 現時点で非対応または対象外として明示されているもの
- Gradle プロジェクト
- マルチモジュール構成
- Spring Framework 単体構成
- Kotlin 主体プロジェクト
- 複数リポジトリ横断分析

### 8.3 実行制約
- アップロードは zip のみ
- 最大サイズ 20MB
- Web 実行タイムアウト 60 秒
- 一時作業領域は OS の temp 配下 `qscout/{requestId}`
- 成果物保持は `TempWorkspaceService` 上で **15 分**
- requestId は UUID 形式

注意:
- 期限切れ成果物は `ArtifactExpiredException` 扱いです
- `fileKey` は現状 `human` と `ai` のみです
- preview / download の内部識別には `requestId` と `fileKey` を用います
- ただし現行実装のアクセス導線は **署名付き URL 前提** であり、`expires` と `token` による検証を伴います
- したがって、開始時点の理解としては「`requestId` / `fileKey` で成果物を識別しつつ、実アクセスは署名付き URL で安全に提供する」と捉えてください

---

## 9. スコアリングとルール

### 9.1 スコアリング
- 100 点満点の減点方式
- HIGH: -10
- MEDIUM: -5
- LOW: -2
- 0 点未満にはしない

### 9.2 実装済みルール
MVP の 6 ルールは実装済みです。

1. `R001` ControllerToRepositoryDirectAccessRule
2. `R002` FieldInjectionRule
3. `R003` TransactionalMisuseRule
4. `R004` ExceptionSwallowingRule
5. `R005` MissingTestRule
6. `R006` PackageDependencyViolationRule

### 9.3 R001 の現行解釈
`ControllerToRepositoryDirectAccessRule` は、旧来の一律断罪ではなく、**文脈に応じて HIGH / MEDIUM / LOW を出し分ける方向へ見直し済み**です。

現行の理解:
- 書き込みやユースケース処理混在は重い
- 単純 read-only は条件付き許容余地あり
- Service 経由が原則だが、全件一律 HIGH とみなす設計ではない

したがって、今後このルールに触れる提案では、**責務漏れの強さ・文脈・説明責務** を重視してください。

---

## 10. レポート・多言語化の現状

### 10.1 出力成果物
解析成功時、2 種類の Markdown を出力します。

1. `qscout-report.md`
2. `qscout-ai-input.md`

### 10.2 人間向けレポート
人間向け Markdown はローカライズ対象です。

特徴:
- 日本語 / 英語対応
- ルール別サマリあり
- 違反一覧あり
- 改善ヒントあり
- チェック対象ルール一覧あり
- ルール詳細解説ページへのリンクあり

### 10.3 AI 入力 Markdown
AI 向け Markdown は、現行実装では **英語固定** です。

目的:
- 外部 AI へ貼り付けやすい形に整える
- issue 単位で rule, severity, file, line, message, snippet を渡す
- 改善相談の起点にする

### 10.4 多言語化方式
- `MessageSource` ベース
- `SessionLocaleResolver`
- `LocaleChangeInterceptor`
- `?lang=ja` / `?lang=en` で切替
- デフォルトロケールは日本語

セッション維持込みで、Web UI と人間向けレポートに反映されます。

---

## 11. UI / preview 実装の現状

### 11.1 preview
- 人間向け Markdown は簡易 `MarkdownPreviewRenderer` で HTML 化
- AI 入力 Markdown は全文 preview 表示だが、人間向けのような Markdown レンダリングは行わない

### 11.2 クライアント側補助
- ファイルサイズ超過をクライアント側でも事前検知
- 20MB 超過時はモーダル表示
- 送信中はボタン disable と実行中表示

### 11.3 エラー UX
主に以下の系統で分かれています。

- 入力不正
- 構成不正
- アップロードサイズ超過
- タイムアウト
- 想定外エラー

---

## 12. 開発・検証の現状

### 12.1 テスト状況
開始時には、可能なら現行ブランチでテスト状況を再確認してください。  
直近時点では `CodexExec.result` 上で **`mvn test` または `mvnw.cmd test` 相当の成功記録** が確認されており、**146 tests passed** の実績があります。  
ただし、新規チャットでは固定値を鵜呑みにせず、**最新の `CodexExec.result`、必要に応じて PR、さらに現行ブランチでの再確認** を優先してください。

### 12.2 テストで担保されている主な観点
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

### 12.3 サンプル
現行リポジトリには、公開サンプルや比較評価用スクリプトが存在する可能性があるため、必要に応じて `samples/` および `scripts/` を確認してください。

---

## 13. フェーズ認識

現時点の正しいフェーズ認識は以下です。

1. 企画・要件定義フェーズは完了済み
2. CLI MVP は実装済み
3. Web MVP は実装済み
4. 多言語化は実装済み
5. ルール詳細解説ページまで実装済み
6. トップページ UX 改善も一定反映済み
7. これからの中心課題は、対応範囲拡張・精度改善・運用強化
8. docs は、企画 / 将来構想 / 要件定義 / 詳細設計 / アーカイブの構造へ再整理済み

つまり、**「これから Web 化する段階」ではなく、「Web 化済みの MVP を改善していく段階」**です。

---

## 14. 未解決課題・今後の検討領域

現行コードと docs から見て、未解決または今後の主要テーマは以下です。

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
- 基本設計書・詳細設計書など docs 下位層の整備継続

重要:
- これらは **現時点で未実装または限定対応** です
- 応答時に、未実装機能を「既にあるもの」として扱わないでください

---

## 15. Codex 連携時の運用前提

### 15.1 Codex 実行手順
- Codex 実装作業では、原則として **実装 → テスト → `CodexExec.result` へ実行ログ追記 → commit → push → Pull Request 作成** の順で進める
- Pull Request 作成後は、`CodexExec.result` の該当ログブロックへ PR 情報を追記し、その更新も branch に反映する
- 長文の実行ログ貼り付けは原則不要とし、詳細な出力ルールは `docs/00_プロジェクト管理/05_横断運用規程/CodexExec.result運用ルール.md` に従う

### 15.2 `CodexExec.result` の必須記録項目
- 各ログブロックには、従来項目に加えて `push` / `pr` / `pr url` / `pr base` / `pr head` / `pr status` を必須で記録する
- Pull Request 作成に失敗した場合は、その時点で停止し、成功した最終工程・失敗工程・エラーメッセージ・push 済み branch 名・compare URL を可能な範囲で記録する

### 15.3 ChatGPT の確認順序
- まず `CodexExec.result` から対象作業のログブロックを特定する
- `pr url` がある場合は **Pull Request 差分を最優先で確認** する
- `CodexExec.result` 本文は、PR 差分確認を補助する根拠として扱う
- `pr url` がない場合のみ、変更対象ファイルや commit を直接確認する
- 「直近ブロックだから正しい」とは限らないため、**`TASK_ID` / `TITLE` / `EXECUTED_AT` の一致確認を優先**する

### 15.4 指示文作成時
- コミットコメントを指示文に内包する
- 可能なら STEP 名を明示する
- 空になって不要になったフォルダが生じる場合は、削除指示も含める
- docs 作業では、変更対象を必要最小限に限定する

### 15.5 確認時
- `CodexExec.result` の該当ログブロック
- `pr url` がある場合は当該 Pull Request
- `pr url` がない場合は変更対象ファイルまたは commit
- 必要に応じて `docs/README.md`
を確認する

---

## 16. この新規チャットで期待する振る舞い

### 16.1 判断原則
- 設計文書だけでなく、必ず現行コード・README・テストを照合する
- 文書と実装がズレる場合は、ズレを明示した上で現行実装を優先する
- 既存 CLI 資産を活かす方向を最優先にする
- Web 層は薄く、解析コア共有の原則を崩さない

### 16.2 提案の出し方
- 新規提案には識別可能な提案名を付ける
- 可能なら最良案を先に示す
- 「現行実装に対する差分」として説明する
- 実装済み事項、未実装事項、要判断事項を分けて話す

### 16.3 実装支援時の前提
- 既存挙動を壊さない
- CLI / Web / i18n / report / preview の関係を保つ
- MVP 対象外事項を勝手に混ぜ込まない
- テストで担保できる変更を優先する

### 16.4 文書支援時の前提
- 対外説明に耐える business proposal style を維持する
- ただし実装現況を過大評価しない
- 「設計上そうあるべき」と「現在そうなっている」を混同しない

---

## 17. 応答開始時にまず行うこと

この開始時プロンプトを受け取ったら、まず次の順で応答してください。

1. リポジトリ連携前提で、現在のプロジェクト理解を簡潔に要約する
2. 現在フェーズを整理する
3. 実装済み事項と未対応事項を分けて列挙する
4. docs / 実装 / 運用のどこを次に見ればよいかを整理する
5. ユーザーの次の依頼に即応できる状態に入る

---

## 18. 最重要の要約

このプロジェクトの本質は、

**「Spring プロジェクトを 6 ルール中心で静的解析・スコアリングし、CLI で成立した価値を維持したまま Web MVP・多言語 UI・成果物 preview / download・ルール解説導線まで備えた品質診断ツールとして進化させ、その先の精度改善と対応範囲拡張、さらに将来の AI 活用構想まで含めて、実装力と設計力の両方を示すポートフォリオ品質のプロダクトとして育てること」**

です。

以後は、この理解を基礎として作業してください。
