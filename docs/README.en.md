# Q-Scout for Spring

Q-Scout for Spring is a Spring Boot / Spring Framework code quality assessment tool that scores design health and Spring best-practice compliance.

This project is built to reduce review inconsistency, expose hidden technical debt earlier, and make Spring-specific quality issues easier to explain and act on. Instead of only listing warnings, it evaluates a Spring project with rule-based checks and generates Markdown outputs that can be used by both humans and AI-assisted workflows.

## Problems Addressed

- Review quality depends too much on the individual reviewer
- Design erosion and technical debt are hard to spot early
- Spring-specific bad practices take time to find manually
- Quality checks increase review effort as projects grow

## What The Tool Does

- Analyzes a Spring project with six quality rules
- Calculates an overall score for code quality and design health
- Produces a human-readable Markdown report
- Produces a separate Markdown file for AI-assisted follow-up
- Delivers value even without any external AI API integration

## Main Outputs

- `qscout-report.md`
- `qscout-ai-input.md`

## Quick Start

Build:

```bash
mvn test
mvn -q -DskipTests package
```

Run the web application:

```bash
java -jar target/q-scout-for-spring-0.1.0-SNAPSHOT.jar
```

Run the CLI analyzer:

```bash
java -Dloader.main=com.qscout.spring.cli.Main -cp target/q-scout-for-spring-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher <project-root> <output-dir>
```

Windows helper scripts are also available from the repository root, including `run-cli.bat` and `run-self-analysis.bat`.

## Full Documentation

The full README is maintained in Japanese at [../README.md](../README.md).
