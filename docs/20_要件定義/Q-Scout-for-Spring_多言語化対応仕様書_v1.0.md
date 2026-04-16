# Q-Scout for Spring
## 多言語化対応仕様書 v1.0

---

## 1. 文書概要

本書は、Q-Scout for Spring における多言語化対応の仕様を定義するものである。

本仕様の目的は、現状英語中心となっている Web UI 文言および人間用レポート文言を、
主利用者である日本語話者に適した形へ改善しつつ、英語利用も維持できる構造を確立することである。

本仕様は、既存の CLI / Web / Docker / Render 公開構成を前提とし、
過設計を避けながら、将来の追加言語にも拡張可能な最小多言語化構造を定義する。

---

## 2. 背景

### 2.1 現状課題
現状の Q-Scout for Spring では、画面文言および人間用レポート文言が英語中心で出力される。

一方、想定利用者の主軸は日本語話者であり、以下の課題がある。
1. 初見利用時の理解負荷が上がる
2. 結果レポートの読み取り速度が低下する
3. 組織内共有時に補足説明が必要になりやすい
4. 日本語中心運用における自然な UX になっていない

### 2.2 ただし英語は維持価値がある
以下の理由により、英語出力も維持する。
1. 将来的な非日本語話者利用の可能性
2. OSS / 海外向け説明資料との親和性
3. AI用プロンプトや技術資料との整合性
4. 国際化対応済みプロダクトとしての拡張性

---

## 3. 基本結論

### 提案名
日本語デフォルト・英語併用型多言語化方式

### 優先度
最優先

### 結論
Q-Scout for Spring の多言語化は、以下の方針で実装する。

1. デフォルト言語は日本語とする
2. 日本語 / 英語の切替を可能にする
3. Web UI と人間用レポートを主対象とする
4. AI用Markdown は今回は原則英語維持とする
5. Spring 標準の MessageSource ベースで構成する
6. SessionLocaleResolver と LocaleChangeInterceptor を採用する
7. `?lang=ja` / `?lang=en` で言語切替可能とする
8. 将来の追加言語に拡張しやすいキー構造を採用する

---

## 4. スコープ

### 4.1 対象
本仕様の対象は以下とする。

### A. Web UI 文言
- 画面タイトル
- 入力ラベル
- 補助説明文
- ボタン
- 実行中表示
- 結果表示項目
- ダウンロード表示
- エラー表示
- 言語切替表示

### B. 人間用レポート文言
- qscout-report.md の見出し
- セクション名
- サマリ文言
- 重大度内訳ラベル
- 違反なし時文言
- 改善ヒント文言
- 成功メッセージ

### C. 必要最小限のユーザー向けメッセージ
- 画面に出る入力エラー文言
- 画面に出る構造エラー文言
- 画面に出るタイムアウト文言
- 画面に出る予期しないエラー文言

### 4.2 非対象
本仕様では以下を原則対象外とする。

1. AI用Markdown の全面多言語化
2. CLI標準出力の全面多言語化
3. 内部ログ文言の多言語化
4. DBや永続層を伴う設定保存
5. ユーザーごとの言語設定永続化
6. 3言語目以降の実装
7. REST API レスポンス体系の全面多言語化

---

## 5. 設計原則

### 5.1 既存資産保全原則
- CLI資産を壊さない
- Web公開構成を壊さない
- SharedAnalysisService 中心の解析フローを壊さない
- 人間用レポートとAI用Markdownの分離方針を維持する

### 5.2 UI優先原則
- まずは利用者が直接目にする文言から多言語化する
- 内部ログや開発者向け文言は後回しとする

### 5.3 MessageSource集約原則
- 表示文言はできる限り MessageSource へ集約する
- ハードコード文字列を減らす
- 例外 message 直書きは最小限に留める

### 5.4 過設計回避原則
- Locale 永続化用 DB は導入しない
- i18n 専用の大規模抽象化は導入しない
- Spring 標準機能で成立する構成を優先する

### 5.5 将来拡張原則
- キー命名を整理し、将来の追加言語に耐える構造とする
- 日本語/英語の2言語前提で設計するが、3言語目追加が困難な構造にしない

---

## 6. 採用技術方針

### 提案名
Spring標準国際化構成採用方針

### 優先度
最優先

### 採用技術
- MessageSource
- ResourceBundleMessageSource
- SessionLocaleResolver
- LocaleChangeInterceptor
- Thymeleaf の `#{key}` 参照
- 必要に応じて Jakarta Validation のメッセージ外部化

### 非採用
- 独自辞書管理機構
- DB保存型翻訳テーブル
- JavaScript主体の i18n ライブラリ
- SPA前提のフロント側多言語化機構

---

## 7. ロケール方針

### 7.1 対応言語
対応言語は以下の2つとする。
- 日本語（ja）
- 英語（en）

### 7.2 デフォルトロケール
- デフォルトロケールは日本語とする

### 7.3 切替方式
- URL パラメータ `?lang=ja`
- URL パラメータ `?lang=en`

### 7.4 保持方式
- SessionLocaleResolver によりセッション単位で保持する

### 7.5 CLIにおける扱い
- CLI は今回、既定ロケールである日本語出力を基本とする
- CLI引数による言語切替は今回の必須要件としない
- 将来拡張余地として保持する

---

## 8. Web 構成仕様

### 8.1 WebConfig
多言語化対応のために WebConfig を追加または更新し、以下を構成する。

1. MessageSource Bean
2. LocaleResolver Bean
3. LocaleChangeInterceptor Bean
4. InterceptorRegistry への登録

### 8.1.1 MessageSource 設定要件
- basename: messages
- UTF-8 で読み込む
- 日本語/英語を切替可能とする
- 未定義キー時の扱いは既定挙動でよいが、開発中に気付きやすい構成が望ましい

### 8.1.2 LocaleResolver 設定要件
- SessionLocaleResolver を採用する
- defaultLocale は Locale.JAPANESE とする

### 8.1.3 LocaleChangeInterceptor 設定要件
- パラメータ名は `lang`
- `ja` / `en` を受け付ける

---

## 9. メッセージファイル仕様

### 9.1 配置
以下のファイルを配置する。

- src/main/resources/messages_ja.properties
- src/main/resources/messages_en.properties

### 9.2 キー命名方針
キーは責務ごとにプレフィックスを分ける。

推奨プレフィックス例：
- app.*
- page.*
- form.*
- result.*
- download.*
- error.*
- report.*
- validation.*

### 9.3 必須キー分類

### A. アプリ共通
- app.title
- app.language.ja
- app.language.en

### B. ページ関連
- page.home.title
- page.home.description
- page.home.uploadGuide

### C. 入力フォーム関連
- form.upload.label
- form.upload.button
- form.upload.hint.format
- form.upload.hint.size
- form.upload.hint.requirement

### D. 実行状態関連
- result.status.idle
- result.status.running
- result.status.completed
- result.status.error

### E. 結果表示関連
- result.score.label
- result.violations.label
- result.severity.high
- result.severity.medium
- result.severity.low
- result.download.human
- result.download.ai

### F. エラー関連
- error.invalidUpload
- error.invalidProjectStructure
- error.timeout
- error.unexpected
- error.expiredArtifact

### G. レポート関連
- report.title
- report.target
- report.overallScore
- report.severityCounts
- report.ruleSummary
- report.violations
- report.improvementHints
- report.noViolations
- report.noImmediateImprovements
- report.passedAllChecks

### H. ルール名関連
- rule.controllerToRepositoryDirectAccess.name
- rule.fieldInjection.name
- rule.transactionalMisuse.name
- rule.exceptionSwallowing.name
- rule.missingTest.name
- rule.packageDependencyViolation.name

### 9.4 日本語文言方針
- 自然な業務向け日本語を採用する
- 直訳調より、読みやすさと説明性を優先する
- エラー文は利用者が次に何をすべきか分かる表現を優先する

### 9.5 英語文言方針
- 現行文言をできるだけ尊重する
- 既存レポート出力との互換性を意識する
- 過度に説明的な長文化は避ける

---

## 10. Thymeleaf テンプレート仕様

### 10.1 対象
単画面 Web UI を構成するテンプレートを対象とする。

### 10.2 多言語化対象
以下のハードコード文字列は `#{key}` に置換する。

1. ページタイトル
2. 見出し
3. 説明文
4. ファイル選択ラベル
5. 実行ボタン
6. 実行中表示
7. 結果表示ラベル
8. ダウンロードリンク表示文言
9. エラー表示タイトルまたは補助文
10. 言語切替リンク

### 10.3 非対象
- 動的に差し込まれるファイルパスそのもの
- スコア数値や件数値そのもの
- 内部用コード値

---

## 11. 言語切替 UI 仕様

### 提案名
単画面軽量切替UI方式

### 優先度
最優先

### 方針
- 画面上部の分かりやすい位置に言語切替リンクを表示する
- 表示例：
  - 日本語
  - English
- 遷移は `?lang=ja` / `?lang=en` を使用する
- 単画面MVPのシンプルさを崩さない

### 要件
1. 視認性が高い
2. 現在ページを維持したまま切替できる
3. セッションにより切替後状態が保持される

---

## 12. 人間用レポート多言語化仕様

### 12.1 対象
qscout-report.md を対象とする。

### 12.2 多言語化対象項目
以下をロケールに応じて出し分ける。

1. レポートタイトル
2. Target
3. Overall Score
4. Severity Counts
5. Rule Summary
6. Violations
7. Improvement Hints
8. No violations detected.
9. No immediate improvements required.
10. The project passed all current Q-Scout checks.

### 12.3 ルール名表示
ルール名は人間用レポートでは翻訳対象とする。
ただし ruleId のような内部識別子は無理に翻訳しない。

### 12.4 0件時文言
違反0件時は、既に改善済みの自然な成功メッセージ方針を維持しつつ、多言語で出し分ける。

### 日本語例
- 違反は検出されませんでした。
- 現時点で直ちに必要な改善はありません。
- このプロジェクトは現在の Q-Scout チェックをすべて通過しました。

### 英語例
- No violations detected.
- No immediate improvements required.
- The project passed all current Q-Scout checks.

### 12.5 違反あり時文言
違反あり時の既存レポート構造と改善ヒント体系は原則維持する。
見出しやラベルのみ多言語化し、内容構造は壊さない。

### 12.6 レポート生成時のロケール適用方針
- Webから生成されるレポートは現在選択中ロケールに従う
- CLIから生成されるレポートは既定ロケール（日本語）でよい
- 将来、CLI引数でロケール指定可能にする拡張余地は保持する

---

## 13. AI用Markdown の扱い

### 提案名
AI用Markdown 英語維持方針

### 優先度
高

### 方針
AiMarkdownFileGenerator は今回、原則として英語維持とする。

### 理由
1. AI用Markdown はプロンプト用途である
2. 英語構造の安定性を優先したい
3. 人間向けUX改善の主対象は人間用レポートである
4. 現段階で AI 入力テンプレートの全面多言語化は優先度が低い

### 例外
- 将来必要になれば、見出しのみ多言語化する余地はある
- ただし本仕様 v1.0 の必須要件には含めない

---

## 14. 例外・エラー表示多言語化仕様

### 14.1 対象
ユーザーに見えるエラーメッセージを対象とする。

### 14.2 方針
- 例外クラス内部の message 直書きは最小限でよい
- 画面表示時に MessageSource で解決する方式を優先してよい
- 内部ログ文言は対象外とする

### 14.3 対象例
- InvalidUploadException の表示メッセージ
- InvalidProjectStructureException の表示メッセージ
- AnalysisTimeoutException の表示メッセージ
- ArtifactExpiredException の表示メッセージ
- ErrorViewModel に表示するユーザー向け文言

---

## 15. バリデーション文言仕様

### 方針
- multipart / 入力チェック / 必須チェック等、利用者に見える文言は可能な範囲で多言語化する
- Jakarta Validation を使用している箇所がある場合は messages ファイルへ統合する
- 今回は必須対象を「ユーザー向けに直接出る文言」に限定する

---

## 16. パッケージおよびファイル追加方針

### 16.1 追加候補
- web.config.WebConfig
- messages_ja.properties
- messages_en.properties

### 16.2 改修候補
- テンプレートファイル
- WebPageController または画面文言組み立て箇所
- MarkdownReportGenerator
- 必要最小限の ErrorViewModel 利用箇所

### 16.3 原則非改修
- 解析コアのロジック本体
- 6ルール本体
- SharedAnalysisService 本体の解析手順
- AiMarkdownFileGenerator（原則）

---

## 17. テスト仕様

### 17.1 必須テスト
以下は最低限追加または更新する。

### A. ロケール切替テスト
1. デフォルト日本語
2. `?lang=en` で英語
3. `?lang=ja` で日本語へ戻る

### B. MarkdownReportGenerator の多言語化テスト
1. 日本語出力
2. 英語出力
3. 0件時文言
4. 違反あり時文言

### C. テンプレート描画確認
1. 主要ラベルが日本語で出る
2. 主要ラベルが英語で出る

### D. 必要ならエラー表示確認
1. 日本語表示
2. 英語表示

### 17.2 テスト方針
- 主要文言の存在確認を中心とする
- 完全文言一致の過剰固定は避ける
- 回帰防止に必要な範囲で固定する

---

## 18. 実行確認仕様

多言語化実装後は、最低限以下を確認する。

1. `mvn test`
2. `mvn -q -DskipTests package`
3. Web画面で日本語表示確認
4. Web画面で `?lang=en` の英語表示確認
5. Web画面で `?lang=ja` の日本語復帰確認
6. 日本語レポート生成確認
7. 英語レポート生成確認
8. 0件時レポート文言の切替確認

---

## 19. 非機能要件

1. 多言語化により既存の解析速度を大きく悪化させない
2. Render / Docker 公開構成を壊さない
3. CLI実行導線を壊さない
4. Web MVP の単画面運用を壊さない
5. メッセージファイル追加により保守性を向上させる

---

## 20. 今回未着手とするもの

1. AI用Markdown の全面多言語化
2. CLI引数による言語切替
3. ロケールの永続保存
4. 3言語目以降の実装
5. DB駆動型翻訳管理
6. 内部ログの多言語化
7. 監査・管理画面用言語設定

---

## 21. Codex向け実装優先順位

1. WebConfig 追加または更新
2. messages_ja.properties / messages_en.properties 作成
3. Thymeleaf テンプレート置換
4. 言語切替UI追加
5. MarkdownReportGenerator 多言語化
6. エラー表示文言多言語化
7. テスト追加 / 更新
8. 実行確認

---

## 22. 成功条件

以下を満たしたら、本仕様の実装完了とする。

1. デフォルト言語が日本語
2. `?lang=ja` / `?lang=en` で切替可能
3. Web画面主要文言が日本語/英語で出し分けられる
4. qscout-report.md が日本語/英語で出し分けられる
5. 0件時文言も自然に切り替わる
6. 英語利用も維持される
7. 既存CLI/Web動作を壊していない
8. テストで回帰防止されている

---

## 23. 最終結論

Q-Scout for Spring の多言語化は、
「日本語を主利用言語としつつ、英語利用も維持する」
という方針で進めるのが最も合理的である。

実装方式は Spring 標準の MessageSource / SessionLocaleResolver / LocaleChangeInterceptor を採用し、
まずは Web UI と人間用レポートを主対象として多言語化する。

AI用Markdown は今回は英語維持とし、
人間向けUX改善と将来拡張性の両立を図る。

この仕様により、
- 主利用者にとって自然な利用体験
- 英語運用の維持
- 将来の追加言語拡張余地
- 既存CLI/Web資産の保全
を同時に満たすことができる。

以上を、Q-Scout for Spring 多言語化対応仕様の正式版とする。
