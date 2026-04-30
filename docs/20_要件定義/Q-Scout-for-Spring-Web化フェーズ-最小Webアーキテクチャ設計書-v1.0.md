# Q-Scout for Spring
## Web化フェーズ 最小Webアーキテクチャ設計書 v1.0

> 本書は、Web 化フェーズ時点の最小構成を整理した補助設計資料である。
> 現行の正式な基本設計は `docs/30_基本設計/基本設計.md` を参照すること。
> 本書は、移行経緯や最小構成の意図を補助的に確認する用途で用いる。
> 実装方式に踏み込む記述は、現行実装と矛盾する場合、現行実装および `docs/30_基本設計/基本設計.md` を優先する。

---

## 1. 文書概要

本書は、Q-Scout for Spring の Web化フェーズにおける最小Webアーキテクチャを定義するものである。

前提：
- CLI資産を最大限再利用する
- Web MVP要件を満たす最小構成
- Render環境対応
- Docker前提

---

## 2. 設計結論

### 提案名
共通解析サービス抽出型Webアーキテクチャ

### 優先度
最優先

### 方針
- CLIとWebの入口分離
- 解析処理は共通化
- Web層は薄く構築
- 同期処理
- ステートレス構成

---

## 3. 全体構成

```text
[Browser]
  ↓
[Web UI]
  ↓
[Web Controller]
  ↓
[WebAnalysisService]
  ├ UploadValidationService
  ├ ZipExtractionService
  ├ TempWorkspaceService
  ├ RequestAccessTokenService
  ├ DownloadArtifactService
  ├ SharedAnalysisService
  └ MarkdownPreviewRenderer
  ↓
[既存解析コア]
  ↓
[レポート生成]
  ↓
[結果表示 / 署名付き preview / 署名付き download]
```

---

## 4. バックエンド構成

- Spring Boot Webアプリ
- 単一コンテナ
- ステートレス

コンポーネント：

Controller
- WebPageController
- WebPreviewController
- WebDownloadController

Service
- WebAnalysisService
- UploadValidationService
- ZipExtractionService
- TempWorkspaceService
- RequestAccessTokenService
- DownloadArtifactService
- MarkdownPreviewRenderer

共通
- SharedAnalysisService

既存流用
- ProjectScanner
- RuleEngine
- ScoreCalculator
- ReportGenerator
- AiMarkdownGenerator

---

## 5. フロント構成

提案名：単画面サーバレンダリングUI

構成：
- アップロード
- 実行
- 状態表示
- 結果表示
- preview
- ダウンロード
- エラー表示

非採用：
- SPA
- 非同期UI
- 認証
- 履歴

---

## 6. アップロード設計

- multipart/form-data
- フィールド：projectZip
- zipのみ
- 最大20MB

バリデーション：
- 未指定
- 拡張子不正
- サイズ超過
- ZIPとして読み取れない
- ZIPエントリ数、パス、階層深度、単一ファイルサイズ、展開後合計サイズ、圧縮率等の安全制約違反

---

## 7. 一時ディレクトリ

パス：

```text
/tmp/qscout/{requestId}/
  upload.zip
  extracted/
  output/
```

責務：
- requestId生成
- 作成
- 成果物保持期限判定
- cleanup

保持方針：
- 成功時は preview / download のため15分保持する
- 期限切れ時は成果物期限切れとして扱い、再解析を促す
- エラー時は即時 cleanup する

---

## 8. zip解凍

提案名：安全解凍方式

要件：
- Zip Slip防止
- 外部コマンド禁止
- 展開先固定
- 異常時エラー
- 危険なZIPパス、過大ファイル、過大展開、過剰エントリ数、高圧縮率、深すぎる階層の拒否
- `.git`、`.github`、`target`、`build`、`node_modules`、`.idea`、`.vscode` 等の解析不要・危険化しやすいディレクトリの自動除外

ZIP安全制約：
- アップロードサイズ上限：20MB
- 展開後合計サイズ上限：100MB
- エントリ数上限：5,000
- 単一エントリサイズ上限：10MB
- 階層深度上限：20
- 圧縮率上限：100

---

## 9. ルート判定

- extracted直下優先
- `pom.xml` 必須
- extracted直下に `pom.xml` が存在する場合は extracted直下を project root とする
- extracted直下に単一トップディレクトリがあり、その配下に `pom.xml` が存在する場合は当該ディレクトリを project root とする
- 複数候補は、マルチモジュールまたは不明瞭な構成としてエラー扱いとする

---

## 10. 解析呼び出し

変更：

```text
CLI:
Main
 → CliApplication
   → SharedAnalysisService

Web:
Controller
 → WebAnalysisService
   → SharedAnalysisService
```

---

## 11. SharedAnalysisService

責務：
- AnalysisRequest受領
- scan
- analyze
- score
- report生成
- ai md生成
- ExecutionSummary返却

---

## 12. 成果物保存

出力：
- qscout-report.md
- qscout-ai-input.md

保存：

```text
/tmp/qscout/{requestId}/output/
```

成果物は永続保存せず、requestId 単位の一時ワークスペースに保存する。

---

## 13. preview / download

内部エンドポイント：
- `/preview/{requestId}/{fileKey}`
- `/download/{requestId}/{fileKey}`

fileKey：
- `human`: qscout-report.md
- `ai`: qscout-ai-input.md

利用者向けURL：
- `expires` と `token` を含む署名付きURLとして生成する
- 署名、有効期限、成果物保持状態を検証する
- 署名検証に失敗した場合は 403 を返す
- 期限切れの場合は 410 を返す
- 対象成果物が存在しない場合は 404 を返す

責務：
- 署名検証
- 保持期限検証
- パス解決
- preview / download レスポンス生成

---

## 14. クリーンアップ

提案名：遅延削除

- 成功時は即削除しない
- 成果物保持期間は15分
- 保持期限切れ時は成果物期限切れ扱いとし、再解析を促す
- エラー時は即削除

---

## 15. エラー設計

分類：
- 入力
- 構造
- 実行
- タイムアウト
- 期限切れ
- 署名不正
- 成果物なし

方針：
- 原因明示
- ユーザー理解可能

---

## 16. パッケージ構成

com.qscout.spring

```text
- cli
- web
  - controller
  - service
  - dto
  - exception
- application
  - SharedAnalysisService
- domain
- rule
- infrastructure
- config
- i18n
```

---

## 17. 非採用

- CLI直接呼び出し
- 解析再実装
- 非同期ジョブ

---

## 18. 確定事項

1. Spring Boot Web採用
2. 単画面UI
3. zipアップロード
4. /tmp運用
5. 共通解析サービス
6. ファイル出力
7. 署名付き preview / download URL
8. 15分保持後の遅延削除
9. CLI資産維持

---

## 19. 最終結論

本構成は

- CLI資産を活かす
- Web MVPを満たす
- Render制約に適合
- 最小コストで実装可能

という条件をすべて満たす最適構成である。

---
