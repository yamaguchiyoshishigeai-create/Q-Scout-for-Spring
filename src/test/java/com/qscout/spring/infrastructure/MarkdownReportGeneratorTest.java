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
    private final MarkdownReportGenerator generator = new MarkdownReportGenerator(MessageSources.create());

    @TempDir
    Path tempDir;

    @Test
    void generatesJapaneseMarkdownReportWithMainSections() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path reportPath = generator.generate(analysisResultWithViolation(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
            String content = Files.readString(reportPath);

            assertThat(reportPath.getFileName().toString()).isEqualTo("qscout-report.md");
            assertThat(content).contains("# Q-Scout 診断レポート");
            assertThat(content).contains("総合スコア: 88/100");
            assertThat(content).contains("重大度内訳");
            assertThat(content).contains("## ルール別サマリ");
            assertThat(content).contains("## 違反一覧");
            assertThat(content).contains("Controller から Repository への直接アクセス");
            assertThat(content).contains("Controller が Repository を直接参照しています。");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void generatesEnglishMarkdownReportWithMainSections() throws IOException {
        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            Path reportPath = generator.generate(analysisResultWithViolation(), new ScoreSummary(100, 88, 0, 1, 1, 2), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("# Q-Scout Report");
            assertThat(content).contains("Overall Score: 88/100");
            assertThat(content).contains("Severity Counts");
            assertThat(content).contains("## Rule Summary");
            assertThat(content).contains("## Violations");
            assertThat(content).contains("Controller To Repository Direct Access");
            assertThat(content).contains("Controller reads from repository directly. Prefer service mediation to preserve separation of concerns and future extensibility.");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    @Test
    void localizesLegacyEnglishViolationMessagesInReport() throws IOException {
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
    void generatesLocalizedSuccessMessageWhenThereAreNoViolations() throws IOException {
        AnalysisResult empty = new AnalysisResult(
                new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of()),
                List.of(),
                List.of()
        );

        Locale previous = LocaleContextHolder.getLocale();
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        try {
            Path reportPath = generator.generate(empty, new ScoreSummary(100, 100, 0, 0, 0, 0), tempDir);
            String content = Files.readString(reportPath);

            assertThat(content).contains("違反は検出されませんでした。");
            assertThat(content).contains("## 改善ヒント");
            assertThat(content).contains("現時点で直ちに対応すべき改善はありません。");
            assertThat(content).contains("このプロジェクトは現行の Q-Scout チェックをすべて通過しました。");
            assertThat(content).doesNotContain("Field injection detected. Prefer constructor injection.");
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    private AnalysisResult analysisResultWithViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        Violation violation = new Violation("R001", "Controller To Repository Direct Access", Severity.MEDIUM, Path.of("SampleController.java"), 12, "message", "12: sampleRepository.findAll();");
        RuleResult ruleResult = new RuleResult("R001", "Controller To Repository Direct Access", List.of(violation));
        return new AnalysisResult(context, List.of(ruleResult), List.of(violation));
    }

    private AnalysisResult analysisResultWithFieldInjectionViolation() {
        ProjectContext context = new ProjectContext(Path.of("project"), Path.of("project/pom.xml"), List.of(), List.of());
        Violation violation = new Violation("R002", "Field Injection", Severity.MEDIUM, Path.of("SampleService.java"), 8, "Field injection detected. Prefer constructor injection.", "8: @Autowired");
        RuleResult ruleResult = new RuleResult("R002", "Field Injection", List.of(violation));
        return new AnalysisResult(context, List.of(ruleResult), List.of(violation));
    }
}
