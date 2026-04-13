package com.qscout.spring.infrastructure;

import com.qscout.spring.domain.AnalysisResult;
import com.qscout.spring.domain.ProjectContext;
import com.qscout.spring.domain.RuleResult;
import com.qscout.spring.domain.ScoreSummary;
import com.qscout.spring.domain.Severity;
import com.qscout.spring.domain.Violation;
import com.qscout.spring.i18n.MessageSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportGeneratorTest {
    private final MarkdownReportGenerator generator = new MarkdownReportGenerator(MessageSources.create(), new InMemoryRuleExplanationCatalog());

    @TempDir
    Path tempDir;

    @Test
    void generatesJapaneseMarkdownReportWithLinkedRuleGuidance() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path reportPath = generator.generate(analysisResultWithViolationAndCheckedRules(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
            String content = Files.readString(reportPath);

            assertThat(reportPath.getFileName().toString()).isEqualTo("qscout-report.md");
            assertThat(content).contains("# Q-Scout 診断レポート");
            assertThat(content).contains("- 対象: project");
            assertThat(content).contains("- 総合スコア: 88/100");
            assertThat(content).contains("- 重大度内訳: HIGH=0, MEDIUM=1, LOW=1");
            assertThat(content).contains("## このレポートの読み方");
            assertThat(content).contains("## ルール別サマリ");
            assertThat(content).contains("## 違反一覧");
            assertThat(content).contains("## 今回の検査対象ルール");
            assertThat(content).contains("## 改善ヒント");
            assertThat(content).contains("意味: Controller が永続化アクセスを直接扱っており");
            assertThat(content).contains("読み取りのヒント: 単純 read-only 参照は条件付き許容余地がありますが");
            assertThat(content).contains("このルールの読み方: この指摘は一律の絶対悪ではなく");
            assertThat(content).contains("重大度の読み取り: LOW は限定的 read-only の可能性");
            assertThat(content).contains("詳細解説: [詳細解説を見る](/help/rules/controller-to-repository-direct-access?lang=ja)");
            assertThat(content).contains("- 例外握りつぶし (0) [詳細解説を見る](/help/rules/exception-swallowing?lang=ja)");
            assertThat(content).contains("- パッケージ依存違反 (0) [詳細解説を見る](/help/rules/package-dependency-violation?lang=ja)");
            assertThat(content).contains("#### 指摘 1");
            assertThat(content).contains("Controller が Repository を直接参照しています。");
            assertThat(content).contains("フィールドインジェクションの代わりにコンストラクタインジェクションを採用しましょう。");
            assertThat(content).doesNotContain("### 例外握りつぶし (0)");
            assertThat(content).doesNotContain("詳細解説キー:");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void generatesEnglishMarkdownReportWithLinkedRuleGuidance() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Path reportPath = generator.generate(analysisResultWithViolationAndCheckedRules(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("# Q-Scout Report");
            assertThat(content).contains("- Target: project");
            assertThat(content).contains("- Overall Score: 88/100");
            assertThat(content).contains("- Severity Counts: HIGH=0, MEDIUM=1, LOW=1");
            assertThat(content).contains("## How To Read This Report");
            assertThat(content).contains("## Rule Summary");
            assertThat(content).contains("## Violations");
            assertThat(content).contains("## Checked Rules");
            assertThat(content).contains("## Improvement Hints");
            assertThat(content).contains("### Controller To Repository Direct Access (1)");
            assertThat(content).contains("Meaning: Controllers are touching persistence concerns directly");
            assertThat(content).contains("Reading hint: Simple read-only access may be conditionally acceptable");
            assertThat(content).contains("How to read this rule: Treat this finding as a structural warning signal");
            assertThat(content).contains("Detailed explanation: [View detailed explanation](/help/rules/controller-to-repository-direct-access?lang=en)");
            assertThat(content).contains("- Exception Swallowing (0) [View detailed explanation](/help/rules/exception-swallowing?lang=en)");
            assertThat(content).contains("#### Finding 1");
            assertThat(content).contains("Controller reads from repository directly. Prefer service mediation to preserve separation of concerns and future extensibility.");
            assertThat(content).contains("Replace field injection with constructor injection.");
            assertThat(content).doesNotContain("### Exception Swallowing (0)");
            assertThat(content).doesNotContain("Detail key:");
            assertThat(content).doesNotContain("Q-Scout 診断レポート");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void localizesLegacyEnglishViolationMessagesInJapaneseReport() throws IOException {
        AnalysisResult result = analysisResultWithFieldInjectionViolation();
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path reportPath = generator.generate(result, new ScoreSummary(100, 80, 0, 1, 0, 1), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("フィールドインジェクションが検出されました。コンストラクタインジェクションを推奨します。");
            assertThat(content).doesNotContain("Field injection detected. Prefer constructor injection.");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void generatesJapaneseSuccessMessageWhenThereAreNoViolations() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path reportPath = generator.generate(emptyAnalysisResult(), new ScoreSummary(100, 100, 0, 0, 0, 0), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("違反は検出されませんでした。");
            assertThat(content).contains("現時点では大きな設計劣化の兆候は確認されていません。");
            assertThat(content).contains("現時点で直ちに対応すべき改善はありません。");
            assertThat(content).contains("このプロジェクトは現行の Q-Scout チェックをすべて通過しました。");
            assertThat(content).contains("## 改善ヒント");
            assertThat(content).doesNotContain("フィールドインジェクションの代わりにコンストラクタインジェクションを採用しましょう。");
            assertThat(content).doesNotContain("Keep transaction boundaries in the service layer.");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void generatesEnglishSuccessMessageWhenThereAreNoViolations() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Path reportPath = generator.generate(emptyAnalysisResult(), new ScoreSummary(100, 100, 0, 0, 0, 0), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("No violations detected.");
            assertThat(content).contains("Current checks did not find clear signs of major design deterioration at this time.");
            assertThat(content).contains("No immediate improvements required.");
            assertThat(content).contains("The project passed all current Q-Scout checks.");
            assertThat(content).contains("## Improvement Hints");
            assertThat(content).doesNotContain("Replace field injection with constructor injection.");
            assertThat(content).doesNotContain("フィールドインジェクションの代わりにコンストラクタインジェクションを採用しましょう。");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void keepsImprovementHintsForViolationReports() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Path reportPath = generator.generate(analysisResultWithViolationAndCheckedRules(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("## Improvement Hints");
            assertThat(content).contains("Replace field injection with constructor injection.");
            assertThat(content).contains("Keep transaction boundaries in the service layer.");
            assertThat(content).contains("Prefer service mediation over controller-to-repository access.");
            assertThat(content).doesNotContain("No immediate improvements required.");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    private AnalysisResult analysisResultWithViolationAndCheckedRules() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        Violation violationOne = new Violation("R001", "Controller To Repository Direct Access", Severity.MEDIUM, Path.of("SampleController.java"), 12, "message", "12: sampleRepository.findAll();");
        Violation violationTwo = new Violation("R002", "Field Injection", Severity.LOW, Path.of("SampleService.java"), 8, "Field injection detected. Prefer constructor injection.", "8: @Autowired");
        RuleResult ruleResultOne = new RuleResult("R001", "Controller To Repository Direct Access", List.of(violationOne));
        RuleResult ruleResultTwo = new RuleResult("R002", "Field Injection", List.of(violationTwo));
        RuleResult ruleResultThree = new RuleResult("R004", "Exception Swallowing", List.of());
        RuleResult ruleResultFour = new RuleResult("R006", "Package Dependency Violation", List.of());
        return new AnalysisResult(context, List.of(ruleResultOne, ruleResultTwo, ruleResultThree, ruleResultFour), List.of(violationOne, violationTwo));
    }

    private AnalysisResult analysisResultWithFieldInjectionViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        Violation violation = new Violation("R002", "Field Injection", Severity.MEDIUM, Path.of("SampleService.java"), 8, "Field injection detected. Prefer constructor injection.", "8: @Autowired");
        RuleResult ruleResult = new RuleResult("R002", "Field Injection", List.of(violation));
        return new AnalysisResult(context, List.of(ruleResult), List.of(violation));
    }

    private AnalysisResult emptyAnalysisResult() {
        return new AnalysisResult(
                new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of()),
                List.of(),
                List.of()
        );
    }
}
