# Q-Scout for Spring

Q-Scout for Spring is packaged as a Spring Boot web application, while still keeping the original CLI analysis flow available for local execution and self-analysis.

## Build

```bash
mvn test
mvn -q -DskipTests package
```

## Web start

```bash
java -jar target/q-scout-for-spring-0.1.0-SNAPSHOT.jar
```

Browser:

```text
http://localhost:8080/
```

Docker launch is also supported.

```bash
docker build -t qscout-for-spring .
docker run --rm -p 8080:8080 -e PORT=8080 qscout-for-spring
```

## CLI start

The packaged jar keeps the web entry point as default, so CLI execution should use Spring Boot's `PropertiesLauncher` with the CLI main class specified explicitly.

```bash
java -Dloader.main=com.qscout.spring.cli.Main -cp target/q-scout-for-spring-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher <project-root> <output-dir>
```

Example:

```bash
java -Dloader.main=com.qscout.spring.cli.Main -cp target/q-scout-for-spring-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher src/test/resources/sample-project sample-output/cli-check
```

## Helper scripts

Windows users can run the helper scripts from the repository root.

CLI execution:

```bat
run-cli.bat <project-root> <output-dir>
```

Self-analysis of the current repository:

```bat
run-self-analysis.bat
```

Optional custom output directory:

```bat
run-self-analysis.bat sample-output\self-analysis-custom
```

## Output files

CLI execution writes the following files under the specified output directory.

- `qscout-report.md`
- `qscout-ai-input.md`

## Typical workflow

1. `mvn -q -DskipTests package`
2. `run-self-analysis.bat`
3. Open the generated `sample-output\self-analysis\qscout-report.md`
