# Codex向け実装指示書 v0.1
## 対象プロジェクト
Q-Scout for Spring

## 目的
Spring Boot / Maven / 単一モジュール / Java 17 のプロジェクトに対して、
6ルールの品質診断を行い、以下を出力するCLIツールのMVPを実装する。

- 標準出力：実行サマリー
- 人間用Markdownレポート
- AI投入用Markdown

本MVPでは、AI API連携は実装しない。
AI投入用Markdownを生成し、人間がChatGPT/Geminiへ手動投入する前提とする。

---

## 実装対象の前提

### 要件前提
- CLIツールとして実装
- 対象は Spring Boot / Maven / 単一モジュール / Java 17
- 6ルールで候補検出
- スコアは100点満点の減点方式
- 重大度ごとの減点値
  - HIGH: -10
  - MEDIUM: -5
  - LOW: -2
- 人間用MarkdownとAI投入用Markdownは別ファイルで出力
- フェーズ0ではAI API未使用
- 将来API化しやすい構造を維持する

### 成功条件
- CLIが動作する
- 6ルールを実行できる
- スコアを算出できる
- 人間用Markdownを出力できる
- AI投入用Markdownを出力できる
- 各違反にコード断片を含められる
- サンプル案件で有用な結果が出る

---

## 実装時の最重要方針
1. AI非依存コアを維持する
2. ルール追加しやすい構造にする
3. 過設計を避ける
4. MVP時点では同期・単純構成でよい
5. Spring本体ではなくCLIアプリとして実装する
6. 外部REST APIは実装しない
7. JSON出力は未実装でよい

---

## パッケージ構成
以下の構成でファイルを配置すること。

com.qscout.spring

├─ cli
│  ├─ Main
│  ├─ CliApplication
│  └─ ArgumentParser
│
├─ application
│  ├─ ProjectScanner
│  ├─ RuleEngine
│  ├─ ScoreCalculator
│  ├─ ReportGenerator
│  └─ AiMarkdownGenerator
│
├─ domain
│  ├─ AnalysisRequest
│  ├─ ProjectContext
│  ├─ AnalysisResult
│  ├─ RuleResult
│  ├─ Violation
│  ├─ Severity
│  ├─ ScoreSummary
│  ├─ ReportArtifact
│  └─ ExecutionSummary
│
├─ rule
│  ├─ Rule
│  ├─ ControllerToRepositoryDirectAccessRule
│  ├─ FieldInjectionRule
│  ├─ TransactionalMisuseRule
│  ├─ ExceptionSwallowingRule
│  ├─ MissingTestRule
│  └─ PackageDependencyViolationRule
│
├─ infrastructure
│  ├─ DefaultProjectScanner
│  ├─ DefaultRuleEngine
│  ├─ DefaultScoreCalculator
│  ├─ MarkdownReportGenerator
│  └─ AiMarkdownFileGenerator
│
└─ util
   ├─ FileCollector
   ├─ CodeSnippetExtractor
   └─ MarkdownWriter

---

## API仕様
以下のAPIシグネチャを維持すること。

public interface ProjectScanner {
    ProjectContext scan(AnalysisRequest request);
}

public interface Rule {
    String ruleId();
    String ruleName();
    RuleResult evaluate(ProjectContext projectContext);
}

public interface RuleEngine {
    AnalysisResult analyze(ProjectContext projectContext);
}

public interface ScoreCalculator {
    ScoreSummary calculate(AnalysisResult analysisResult);
}

public interface ReportGenerator {
    Path generate(
        AnalysisResult analysisResult,
        ScoreSummary scoreSummary,
        Path outputDirectory
    );
}

public interface AiMarkdownGenerator {
    Path generate(
        AnalysisResult analysisResult,
        Path outputDirectory
    );
}

public final class CliApplication {
    public ExecutionSummary run(String[] args) {
        // 実装
    }
}

---

## DTO仕様
以下のDTOを実装すること。

public record AnalysisRequest(
    Path projectRootPath,
    Path outputDirectory
) {}

public record ProjectContext(
    Path projectRootPath,
    Path pomXmlPath,
    List<Path> mainJavaFiles,
    List<Path> testJavaFiles
) {}

public record Violation(
    String ruleId,
    String ruleName,
    Severity severity,
    Path filePath,
    Integer lineNumber,
    String message,
    String codeSnippet
) {}

public enum Severity {
    HIGH,
    MEDIUM,
    LOW
}

public record RuleResult(
    String ruleId,
    String ruleName,
    List<Violation> violations
) {}

public record AnalysisResult(
    ProjectContext projectContext,
    List<RuleResult> ruleResults,
    List<Violation> allViolations
) {}

public record ScoreSummary(
    int initialScore,
    int finalScore,
    int highCount,
    int mediumCount,
    int lowCount,
    int totalViolations
) {}

public record ReportArtifact(
    Path humanReportPath,
    Path aiReportPath
) {}

public record ExecutionSummary(
    int finalScore,
    int totalViolations,
    Path humanReportPath,
    Path aiReportPath
) {}

---

## 実装順序
以下の順番で実装すること。

### STEP 1: 骨格作成
- MavenプロジェクトのCLIエントリを作成
- パッケージ構成を作成
- DTOを全て作成
- application配下のインターフェースを作成
- Ruleインターフェースと6ルールクラスの空実装を作成

### STEP 2: CLI入力処理
- Mainを作成
- CliApplicationを作成
- ArgumentParserを作成
- 引数は最低限以下を受け取る
  - 第1引数: 解析対象プロジェクトルート
  - 第2引数: 出力先ディレクトリ
- 引数不備時は使い方を表示して終了する

### STEP 3: プロジェクト走査
- DefaultProjectScannerを実装
- 以下を確認する
  - project root が存在する
  - pom.xml が存在する
  - src/main/java 以下の .java を収集
  - src/test/java 以下の .java を収集
- ProjectContextを返す
- 不正な入力時は明示的な例外メッセージで停止する

### STEP 4: ルールエンジン
- DefaultRuleEngineを実装
- List<Rule> を受け取り順次 evaluate する
- RuleResult一覧と allViolations を統合して AnalysisResult を返す

### STEP 5: 6ルールの最小実装
以下はMVP向けの簡易検出でよい。
誤検知ゼロではなく、候補検出を優先する。

#### 5-1 ControllerToRepositoryDirectAccessRule
- 対象: Controllerクラス
- 判定: Controllerが Repository 型をフィールドまたは依存に持ち、Repositoryメソッドを直接使っている候補を検出
- 重大度: HIGH
- codeSnippet を可能な範囲で付与

#### 5-2 FieldInjectionRule
- 判定: フィールドに @Autowired が付いている箇所を検出
- 重大度: MEDIUM

#### 5-3 TransactionalMisuseRule
- 簡易判定でよい
- 例:
  - Service層相当なのに @Transactional が見当たらない候補
  - Controllerに @Transactional が付いている候補
- 重大度: MEDIUM または HIGH を妥当な方で設定

#### 5-4 ExceptionSwallowingRule
- catch ブロック内でログなし、再throwなし、コメントのみ、空ブロック等を簡易検出
- 重大度: HIGH

#### 5-5 MissingTestRule
- main側クラスに対応する test 側クラスが見つからない候補を検出
- 命名ベースの簡易判定でよい
- 重大度: LOW

#### 5-6 PackageDependencyViolationRule
- controller -> service -> repository の基本レイヤ前提で、
  逆流や不自然な直接参照を簡易検出
- 重大度: MEDIUM

### STEP 6: スコア計算
- DefaultScoreCalculator を実装
- 初期スコア 100
- HIGH 件数 * 10
- MEDIUM 件数 * 5
- LOW 件数 * 2
- 下限 0
- ScoreSummary を返す

### STEP 7: レポート出力
#### 7-1 MarkdownReportGenerator
人間用Markdownとして以下を含める。
- タイトル
- 解析対象
- 総合スコア
- 重大度別内訳
- ルール別違反件数
- 違反詳細
  - ruleName
  - severity
  - filePath
  - lineNumber
  - message
  - codeSnippet
- 簡易改善観点

#### 7-2 AiMarkdownFileGenerator
AI投入用Markdownとして以下を含める。
- Project Analysis Input タイトル
- Project Summary
- Detected Issues
- issueごとの違反詳細
- codeSnippet
- 固定指示文
  - Explain why each issue is problematic
  - Suggest improvements

### STEP 8: 実行サマリー
- ExecutionSummary を返す
- main() で標準出力に以下を表示
  - Final Score
  - Total Violations
  - Human Report Path
  - AI Report Path

---

## 実装上の技術選択
### 優先技術
- Java 17
- Maven
- JavaParser を優先候補としてよい
- ただしMVPでは文字列ベース/簡易解析とJavaParserの併用でもよい
- 過度なフレームワーク導入は不要

### 禁止・非推奨
- Spring Bootアプリとして起動させる構成にしない
- DBや外部サービス依存を入れない
- 非同期化しない
- ルールごとの複雑な独自DTOを増やさない
- 今回JSON出力を作らない

---

## 最低限必要なテスト
以下の観点で最低限のユニットテストまたは動作確認コードを用意すること。

1. 引数解析
2. pom.xml なしでエラーになる
3. フィールドインジェクション検出
4. 例外握りつぶし検出
5. スコア計算
6. レポートファイル生成

---

## サンプルプロジェクト
テストまたは手動確認用に、違反を含む最小サンプルコードを用意してよい。
少なくとも以下が確認できると望ましい。
- ControllerからRepository直接参照
- @Autowired フィールド注入
- 空catch
- テスト欠落

---

## 完了条件
以下をすべて満たしたらMVP実装完了とする。

1. CLI実行できる
2. 指定パスから解析できる
3. 6ルールが一通り動作する
4. ScoreSummary が正しく出る
5. 人間用Markdownが出る
6. AI投入用Markdownが出る
7. コード断片が少なくとも主要違反に付く
8. サンプル案件で結果確認できる

---

## 実装時の注意
- まずは「動く最小構成」を優先する
- ルール精度はMVPでは完璧でなくてよい
- 後で改善しやすいよう、Rule単位の独立性を崩さない
- 人間用MarkdownとAI用Markdownの責務を混在させない
- 将来 Gradle / JSON / API化 を見越しても、今回は作り込まない