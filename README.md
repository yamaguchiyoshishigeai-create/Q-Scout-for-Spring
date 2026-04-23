# Q-Scout for Spring

この README は日本語版の正本です。英語要約版は [docs/README.en.md](docs/README.en.md)、`docs` 配下の正本 / 履歴資料の整理方針は [docs/README.md](docs/README.md) を参照してください。

Spring Boot / Spring Framework 向けのコード品質診断ツールとして、設計健全性と Spring ベストプラクティス準拠度をスコア付きで可視化するプロジェクトです。

Q-Scout for Spring は、Spring アプリケーションのコードレビュー品質の属人化や、技術負債の見えにくさを減らすために設計した品質診断ツールです。  
一般的な静的解析のように警告を並べるだけではなく、Spring プロジェクトで起こりやすい設計上の劣化や実装アンチパターンを、ルールベースで定量評価し、改善の出発点を分かりやすい Markdown レポートとして出力します。

現在は Spring Boot Web アプリケーションとして動作しつつ、もともとの CLI 分析フローも維持しており、ローカル実行・自己分析・将来的な Web 展開の両方を見据えた構成になっています。

## 解決する課題

- コードレビューの観点が担当者ごとにぶれ、品質判断が属人化しやすい
- 技術負債や設計劣化が蓄積しても、早い段階で気づきにくい
- Spring の流儀から外れた実装をレビューで毎回手作業で拾う必要がある
- 品質確認に時間がかかり、レビュー工数が増えやすい
- 問題点は見つかっても、チームで説明・共有しやすい形に整理されにくい

## このツールでできること

- Spring プロジェクトを解析し、6 つのルールに基づく品質診断を行う
- コード品質・設計健全性・Spring ベストプラクティス準拠度を総合スコアとして可視化する
- 人間がレビューしやすい Markdown レポートを生成する
- AI に相談しやすい Markdown 入力を別ファイルとして生成する
- AI API 連携がなくても、診断とレポート出力だけで価値が成立する

## 主な対象ユーザー

- Spring Boot / Spring Framework を使って開発するアプリケーション開発者
- 設計レビューや品質基準の整備を担うテックリード / アーキテクト
- 品質可視化や改善優先度の判断を行いたい品質管理責任者
- チームのレビュー効率や設計健全性を高めたい開発マネージャー

## 技術的な見どころ

- AI 非依存のコア診断ロジックを中心に据え、外部 AI 連携なしでも完結する設計
- Spring 特有の観点をルールベースで評価し、スコアリングと説明可能性を両立
- 人間向けレポートと AI 向け入力を分離し、用途の異なるアウトプットを整理
- CLI 由来資産を基に分析基盤を発展させつつ、現行の主要利用導線を Web アプリケーションとし、CLI 実行も継続して維持できる構成
- MVP として成立する最小機能を保ちながら、今後の Web 化や AI 活用に広げやすい構造

## 差別化ポイント

- 汎用的な Lint ではなく、Spring 設計品質に焦点を当てている
- 単なる警告列挙ではなく、なぜ問題なのかを説明しやすいスコアリング型の診断を行う
- 人間のレビュー支援と AI への入力接続を最初から分けて設計している
- 教育用途やレビュー観点の共有にも使いやすく、チーム標準化に向いている
- AI 依存ツールではなく、AI で価値を増幅できる品質診断基盤として成立している

## ビルド

Unix 系:

```bash
./mvnw test
./mvnw -q -DskipTests package
```

Windows:

```bat
mvnw.cmd test
mvnw.cmd -q -DskipTests package
```

グローバルな Maven が導入済みであれば、従来どおり `mvn test` / `mvn -q -DskipTests package` でも実行できます。

`./mvnw test` / `mvnw.cmd test` には、既定の日本語ロケールに対する i18n 回帰確認、`?lang=ja` / `?lang=en` の切り替え、ローカライズされた人間向けレポート、違反ゼロ時のレポート文言、英語固定の AI 向け Markdown 契約に関する検証が含まれます。

## Web 起動

```bash
java -jar target/q-scout-for-spring-0.1.0-SNAPSHOT.jar
```

ブラウザ:

```text
http://localhost:8080/
```

Docker での起動にも対応しています。

```bash
docker build -t qscout-for-spring .
docker run --rm -p 8080:8080 -e PORT=8080 qscout-for-spring
```

## CLI 起動

パッケージ済み jar は Web エントリポイントを既定とするため、CLI 実行時は Spring Boot の `PropertiesLauncher` を使い、CLI 用の main クラスを明示してください。

```bash
java -Dloader.main=com.qscout.spring.cli.Main -cp target/q-scout-for-spring-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher <project-root> <output-dir>
```

実行例:

```bash
java -Dloader.main=com.qscout.spring.cli.Main -cp target/q-scout-for-spring-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher samples/sample-project samples/sample-output/cli-check
```

## 補助スクリプト

Windows 環境では、リポジトリ直下から補助スクリプトを実行できます。

CLI 実行:

```bat
run-cli.bat <project-root> <output-dir>
```

現在のリポジトリを自己分析する場合:

```bat
run-self-analysis.bat
```

`samples\` 配下に成果物を集約するサンプル評価:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-sample-evaluation-under-samples.ps1
```

Web 検査用に軽量 ZIP を作る場合:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-inspection-zip.ps1
```

repo root 自己検証として `-ProjectPath '.'` を指定した場合は、既定で `samples` を除外します。明示的に含めたい場合は `-IncludeSamples` を付けてください。

出力先を任意のディレクトリに変更する場合:

```bat
run-self-analysis.bat samples\sample-output\self-analysis-custom
```

## 出力ファイル

CLI 実行では、指定した出力先ディレクトリに以下のファイルを生成します。

- `qscout-report.md`
- `qscout-ai-input.md`

## 基本的な利用フロー

1. `./mvnw -q -DskipTests package`
   Windows では `mvnw.cmd -q -DskipTests package`
2. `run-self-analysis.bat`
3. 生成された `samples\sample-output\self-analysis\qscout-report.md` を確認する
