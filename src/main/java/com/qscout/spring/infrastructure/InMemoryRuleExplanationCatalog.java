package com.qscout.spring.infrastructure;

import com.qscout.spring.application.RuleExplanationCatalog;
import com.qscout.spring.domain.RuleExplanation;
import com.qscout.spring.domain.RuleReportGuide;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class InMemoryRuleExplanationCatalog implements RuleExplanationCatalog {
    @Override
    public RuleExplanation findByRuleId(String ruleId, Locale locale) {
        return findAll(locale).getOrDefault(ruleId, RuleExplanation.fallback(ruleId, ruleId));
    }

    @Override
    public Map<String, RuleExplanation> findAll(Locale locale) {
        return isJapanese(locale) ? japaneseCatalog() : englishCatalog();
    }

    private boolean isJapanese(Locale locale) {
        return Locale.JAPANESE.getLanguage().equals(locale.getLanguage());
    }

    private Map<String, RuleExplanation> japaneseCatalog() {
        Map<String, RuleExplanation> explanations = new LinkedHashMap<>();
        explanations.put("R001", new RuleExplanation(
                "R001",
                "Controller から Repository への直接アクセス",
                "Controller が永続化アクセスを直接扱っており、層の責務がにじみ始めている可能性を示します。",
                "HTTP 層に検索条件調整、永続化都合、更新判断が混ざると、ユースケースの見通しと保守性が落ちやすくなります。",
                "Controller に read/write 判断、複数 Repository の組み合わせ、将来の監査や認可追加ポイントが入り込みやすくなります。",
                "まずは Service にユースケース単位の入口を置き、Controller からはその入口だけを呼ぶ構造へ寄せるのが基本です。",
                "単純な read-only 参照、教材、PoC、限定的な参照 API では条件付きで許容できる余地があります。",
                "この指摘は一律の絶対悪ではなく、責務漏れが広がる前に確認したい構造上の注意信号として読むのが適切です。",
                "Q-Scout は、Controller に永続化都合が混ざり始めた地点を早めに捉えることで、後から大きく崩れる前に設計修正しやすくしたいと考えています。",
                "rule-explanations/controller-to-repository-direct-access",
                "単純 read-only 参照は条件付き許容余地がありますが、書き込みやユースケース処理が混ざるなら優先的に Service へ寄せるべきです。",
                "This rule checks whether controllers are directly handling persistence concerns. Simple read-only cases may be tolerable, but service mediation is generally preferred.",
                new RuleReportGuide(
                        "LOW は限定的 read-only の可能性、MEDIUM 以上は責務混在の進行を疑う読み方が目安です。",
                        "Controller 側で更新、分岐、複数 Repository の組み合わせ、認可や変換ロジックまで抱えていないかを最初に確認してください。",
                        "ユースケースを Service に移し、Controller は入出力変換と HTTP 応答の責務に寄せるのが基本です。",
                        "単純 read-only・教材・PoC は例外になり得ますが、将来肥大化しそうなら早めに分離しておく方が安全です。"
                )
        ));
        explanations.put("R002", new RuleExplanation(
                "R002",
                "フィールドインジェクション",
                "依存関係がフィールドへ直接注入されており、可視性とテスト容易性を下げやすい状態です。",
                "必須依存がコンストラクタに表れないため、クラスの前提条件やテスト差し替えポイントが見えにくくなります。",
                "",
                "コンストラクタインジェクションへ統一し、依存関係を明示します。",
                "",
                "依存関係の明示性とテストしやすさを見るルールです。",
                "Q-Scout は小さな依存注入の乱れが、設計の見通し悪化につながる点を重視します。",
                "rule-explanations/field-injection",
                "依存関係を明示したいルールです。まずはコンストラクタインジェクションへ寄せてください。",
                "This rule flags field injection because constructor injection keeps required dependencies explicit.",
                new RuleReportGuide(
                        "重大度よりも、依存関係が隠れていること自体に注目してください。",
                        "必須依存が private field に散っていないかを確認してください。",
                        "コンストラクタで必須依存を受け取る形へ整理します。",
                        "フレームワーク都合の特殊ケースを除き、コンストラクタインジェクションが基本です。"
                )
        ));
        explanations.put("R003", new RuleExplanation(
                "R003",
                "トランザクション境界の誤用",
                "トランザクション境界が Controller 側にある、または Service 相当クラスで不足している可能性があります。",
                "トランザクション境界の置き場がぶれると、ユースケース単位の整合性や変更影響が読みにくくなります。",
                "",
                "ユースケース単位で Service 層に境界を寄せるのが基本です。",
                "",
                "境界配置の意図が適切かを確認するルールです。",
                "Q-Scout は更新整合性と責務分離の両方に効くポイントとしてトランザクション境界を重視します。",
                "rule-explanations/transactional-misuse",
                "Controller 直下ではなく、Service のユースケース境界でトランザクションを管理するのが基本です。",
                "This rule checks whether transaction boundaries are placed around service-level use cases rather than controllers.",
                new RuleReportGuide(
                        "Controller 側に付いている場合は優先度高め、Service 側不足なら整合性観点で確認が必要です。",
                        "どのユースケース単位で整合性を守りたいかを確認してください。",
                        "トランザクション開始点を Service の公開メソッド側へ寄せます。",
                        "読み取り専用や単純委譲では例外もありますが、境界の意図は明示したいところです。"
                )
        ));
        explanations.put("R004", new RuleExplanation(
                "R004",
                "例外握りつぶし",
                "例外を捕捉したあと、ログ・再スロー・ラップなしで見えなくしている可能性があります。",
                "障害原因が追えなくなり、異常系の振る舞いが黙って壊れるリスクがあります。",
                "",
                "ログ出力、ドメイン例外への変換、再スローのいずれかで異常を明示します。",
                "",
                "異常系が適切に観測・伝播されるかを確認するルールです。",
                "Q-Scout は障害解析不能なコードを早期に減らすことを重視します。",
                "rule-explanations/exception-swallowing",
                "例外を隠すと原因追跡が難しくなります。ログ・再スロー・ラップのいずれかを検討してください。",
                "This rule flags caught exceptions that disappear without logging, wrapping, or rethrowing.",
                new RuleReportGuide(
                        "HIGH のときは障害解析困難に直結しやすいと読んでください。",
                        "catch 節で例外情報が失われていないかを確認してください。",
                        "最低限ログを残し、必要なら意味のある例外へ変換します。",
                        "フォールバック処理でも、異常を黙殺しない形が望まれます。"
                )
        ));
        explanations.put("R005", new RuleExplanation(
                "R005",
                "テスト不足",
                "主要な本番コードに対応するテストが見当たらない可能性があります。",
                "変更時の回帰検知が弱くなり、安心してリファクタリングしづらくなります。",
                "",
                "振る舞いが重要なクラスから順に、最小限でも対応テストを追加します。",
                "",
                "品質の安全網が足りているかを見るルールです。",
                "Q-Scout は改善サイクルを回しやすくするため、テストの土台を重視します。",
                "rule-explanations/missing-test",
                "すべてを一気に書く必要はありません。変更頻度や重要度の高い箇所からテストを補強してください。",
                "This rule points out production classes that appear to be missing corresponding tests.",
                new RuleReportGuide(
                        "件数が多い場合は、重要クラスから優先順位を付けて読むのが現実的です。",
                        "まず変更頻度と障害影響の高いクラスを確認してください。",
                        "重要ユースケースから単体または結合テストを追加します。",
                        "命名差や統合テスト中心の構成では誤検知余地もあるため、実態確認は必要です。"
                )
        ));
        explanations.put("R006", new RuleExplanation(
                "R006",
                "パッケージ依存違反",
                "意図した層構造をまたぐ依存が発生し、パッケージ境界が崩れている可能性があります。",
                "依存方向が乱れると、変更影響範囲が広がり、責務分離の約束が守りにくくなります。",
                "",
                "依存方向を整理し、上位層から下位層への一方向依存へ戻すのが基本です。",
                "",
                "パッケージ境界の整合性を見るルールです。",
                "Q-Scout は層構造の崩れが他ルールの悪化を呼び込みやすい点を重視します。",
                "rule-explanations/package-dependency-violation",
                "Controller から Repository 直参照や、下位層から上位層依存がないかを確認してください。",
                "This rule checks whether package dependencies cross the intended architectural direction.",
                new RuleReportGuide(
                        "単発でも構造劣化の入口になりやすいため、件数より依存方向に注目してください。",
                        "どの層からどの層へ逆流しているかを確認してください。",
                        "責務の置き場を見直し、依存を下位層または共通層へ寄せます。",
                        "移行途中の一時依存もありますが、恒常化させないことが大切です。"
                )
        ));
        return explanations;
    }

    private Map<String, RuleExplanation> englishCatalog() {
        Map<String, RuleExplanation> explanations = new LinkedHashMap<>();
        explanations.put("R001", new RuleExplanation(
                "R001",
                "Controller To Repository Direct Access",
                "Controllers are touching persistence concerns directly, which may blur layer responsibilities.",
                "When HTTP handlers start carrying query choices, persistence details, or write orchestration, use-case boundaries become harder to maintain.",
                "Controllers tend to accumulate branching, multiple repository calls, and persistence-driven decisions that belong closer to the application use case.",
                "Move the use-case entry point behind a service and keep controllers focused on HTTP input/output concerns.",
                "Simple read-only endpoints, tutorials, PoCs, or narrowly scoped reference APIs can be conditionally acceptable.",
                "Treat this finding as a structural warning signal rather than an absolute rule violation in every situation.",
                "Q-Scout cares because controller-level persistence access often starts small and later grows into a harder-to-refactor responsibility leak.",
                "rule-explanations/controller-to-repository-direct-access",
                "Simple read-only access may be conditionally acceptable, but write paths or richer use-case logic should usually move behind a service layer.",
                "This rule checks whether controllers are directly handling persistence concerns. Some simple read-only cases may be acceptable, but service mediation is generally preferred.",
                new RuleReportGuide(
                        "LOW can indicate a narrow read-only case, while MEDIUM or HIGH usually deserves a closer look for responsibility leakage.",
                        "First check whether the controller is doing writes, branching, combining repositories, or mixing in authorization and business decisions.",
                        "Move the use case behind a service so the controller stays focused on request mapping and response shaping.",
                        "This is not automatically wrong in every project, but it is a strong signal to verify whether the layering still makes sense."
                )
        ));
        explanations.put("R002", new RuleExplanation(
                "R002",
                "Field Injection",
                "Dependencies are injected into fields directly, which makes required collaborators less explicit.",
                "Hidden dependencies reduce readability and make tests or manual construction harder.",
                "",
                "Prefer constructor injection so required collaborators are visible and easier to replace in tests.",
                "",
                "This rule checks whether dependency requirements stay explicit.",
                "Q-Scout cares because small DI shortcuts often weaken maintainability over time.",
                "rule-explanations/field-injection",
                "Use constructor injection so required dependencies are visible at the class boundary.",
                "This rule flags field injection because constructor injection keeps required dependencies explicit.",
                new RuleReportGuide(
                        "Focus less on severity and more on the fact that required dependencies are hidden.",
                        "Check whether important collaborators are only discoverable by reading fields.",
                        "Move required dependencies into the constructor.",
                        "Framework edge cases exist, but constructor injection is the default choice in most Spring code."
                )
        ));
        explanations.put("R003", new RuleExplanation(
                "R003",
                "Transactional Misuse",
                "Transaction boundaries may be placed in the wrong layer or missing where service-level work is coordinated.",
                "Poorly placed transaction boundaries make consistency rules and change impact harder to reason about.",
                "",
                "Keep transaction boundaries around service-level use cases.",
                "",
                "This rule checks whether transaction ownership matches application responsibilities.",
                "Q-Scout cares because transaction placement affects both data integrity and architecture clarity.",
                "rule-explanations/transactional-misuse",
                "Transactions usually belong around service use cases rather than controller entry points.",
                "This rule checks whether transaction boundaries are placed around service-level use cases rather than controllers.",
                new RuleReportGuide(
                        "Controller-level transactions are usually a stronger smell than a missing service boundary.",
                        "Check which use case is supposed to be atomic.",
                        "Move transaction ownership to the service method that represents the use case.",
                        "Read-only flows and thin delegators may be exceptions, but the boundary should still be intentional."
                )
        ));
        explanations.put("R004", new RuleExplanation(
                "R004",
                "Exception Swallowing",
                "An exception may be caught and then hidden without logging, wrapping, or rethrowing.",
                "Silent failures make incidents harder to diagnose and can mask broken behavior.",
                "",
                "Log, wrap, or rethrow exceptions so failures remain visible and actionable.",
                "",
                "This rule checks whether failure paths stay observable.",
                "Q-Scout cares because invisible exceptions quickly turn into costly debugging sessions.",
                "rule-explanations/exception-swallowing",
                "Do not let exceptions disappear silently. Preserve failure information with logging, wrapping, or rethrowing.",
                "This rule flags caught exceptions that disappear without logging, wrapping, or rethrowing.",
                new RuleReportGuide(
                        "Higher severity usually means a bigger risk of losing operational visibility.",
                        "Check whether the catch block preserves any failure context.",
                        "At minimum log the exception, and often rethrow or convert it to a meaningful application exception.",
                        "Fallback behavior is sometimes valid, but silent failure usually is not."
                )
        ));
        explanations.put("R005", new RuleExplanation(
                "R005",
                "Missing Test",
                "Production code appears to be missing a corresponding automated test.",
                "Without tests, regressions are easier to introduce and refactoring becomes riskier.",
                "",
                "Add focused tests starting with the most important or frequently changed behavior.",
                "",
                "This rule checks whether the project has enough safety net around production code.",
                "Q-Scout cares because test coverage supports safe improvement and long-term maintainability.",
                "rule-explanations/missing-test",
                "You do not need every test at once, but important and frequently changed code should gain coverage first.",
                "This rule points out production classes that appear to be missing corresponding tests.",
                new RuleReportGuide(
                        "When counts are high, prioritize by business impact and change frequency.",
                        "Check the most critical or frequently edited classes first.",
                        "Add unit or integration tests around important behavior before chasing every missing file.",
                        "Naming differences or integration-test-heavy projects can create false positives, so confirm the actual test strategy."
                )
        ));
        explanations.put("R006", new RuleExplanation(
                "R006",
                "Package Dependency Violation",
                "Dependencies may be crossing the intended package or layer direction.",
                "When dependency direction drifts, responsibilities spread and architectural boundaries get harder to preserve.",
                "",
                "Restore one-way dependency flow and move shared logic to an appropriate lower or shared layer.",
                "",
                "This rule checks whether package boundaries still support the intended architecture.",
                "Q-Scout cares because dependency drift often cascades into broader design erosion.",
                "rule-explanations/package-dependency-violation",
                "Check whether dependencies are bypassing the intended layering, especially controller-to-repository shortcuts.",
                "This rule checks whether package dependencies cross the intended architectural direction.",
                new RuleReportGuide(
                        "Even a small count can matter because dependency drift spreads quickly.",
                        "Check which dependency direction is being reversed.",
                        "Move responsibilities so dependencies point downward or toward a shared layer again.",
                        "Temporary migration seams can exist, but they should not become the long-term structure."
                )
        ));
        return explanations;
    }
}
