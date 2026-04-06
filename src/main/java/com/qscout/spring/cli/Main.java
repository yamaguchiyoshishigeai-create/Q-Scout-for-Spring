package com.qscout.spring.cli;

import com.qscout.spring.domain.ExecutionSummary;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        CliApplication application = new CliApplication();
        try {
            ExecutionSummary summary = application.run(args);
            System.out.println("Final Score: " + summary.finalScore());
            System.out.println("Total Violations: " + summary.totalViolations());
            System.out.println("Human Report Path: " + summary.humanReportPath());
            System.out.println("AI Report Path: " + summary.aiReportPath());
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        } catch (RuntimeException exception) {
            System.err.println("Analysis failed: " + exception.getMessage());
            System.exit(2);
        }
    }
}
