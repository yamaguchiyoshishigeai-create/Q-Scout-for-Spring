# Purpose: clone public Spring samples, run light build checks, execute Q-Scout, and aggregate all outputs under samples.
param()

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$samplesRoot = Join-Path $repoRoot "samples"
$outputRoot = Join-Path $samplesRoot "sample-output"
$resultPath = Join-Path $samplesRoot "CodexExec.result"
$summaryPath = Join-Path $samplesRoot "sample-comparison-summary.md"
$jarPath = Join-Path $repoRoot "target\q-scout-for-spring-0.1.0-SNAPSHOT.jar"
$runCli = Join-Path $repoRoot "run-cli.bat"

$targets = @(
    [pscustomobject]@{ Name = "spring-petclinic"; Url = "https://github.com/spring-projects/spring-petclinic.git"; IntendedUse = "正常系ベースライン"; Tier = "Tier1" },
    [pscustomobject]@{ Name = "spring-petclinic-microservices"; Url = "https://github.com/spring-petclinic/spring-petclinic-microservices.git"; IntendedUse = "分散構成・高難度検査"; Tier = "Tier3" },
    [pscustomobject]@{ Name = "spring-boot-realworld-example-app"; Url = "https://github.com/gothinkster/spring-boot-realworld-example-app.git"; IntendedUse = "実務寄り評価用"; Tier = "Tier1" },
    [pscustomobject]@{ Name = "sample-spring-modulith"; Url = "https://github.com/piomin/sample-spring-modulith.git"; IntendedUse = "モジュール境界・責務分離検査"; Tier = "Tier1" },
    [pscustomobject]@{ Name = "spring-boot-monolith"; Url = "https://github.com/mzubal/spring-boot-monolith.git"; IntendedUse = "構造揺さぶり評価用"; Tier = "Tier1" },
    [pscustomobject]@{ Name = "bookstore"; Url = "https://github.com/sivaprasadreddy/bookstore.git"; IntendedUse = "業務CRUD・JPA系評価用"; Tier = "Tier1" },
    [pscustomobject]@{ Name = "gs-rest-service"; Url = "https://github.com/spring-guides/gs-rest-service.git"; IntendedUse = "最小REST API検査"; Tier = "Tier2" },
    [pscustomobject]@{ Name = "gs-accessing-data-jpa"; Url = "https://github.com/spring-guides/gs-accessing-data-jpa.git"; IntendedUse = "JPA/Repository基本構成検査"; Tier = "Tier2" },
    [pscustomobject]@{ Name = "gs-securing-web"; Url = "https://github.com/spring-guides/gs-securing-web.git"; IntendedUse = "Security構成検査"; Tier = "Tier2" },
    [pscustomobject]@{ Name = "gs-reactive-rest-service"; Url = "https://github.com/spring-guides/gs-reactive-rest-service.git"; IntendedUse = "Reactive/WebFlux検査"; Tier = "Tier2" }
)

function Write-ResultLog {
    param([string]$Status, [string]$Code, [string]$Message)
    Add-Content -LiteralPath $resultPath -Value ("[{0}] {1}" -f $Status, $Code)
    if ($Message) { Add-Content -LiteralPath $resultPath -Value $Message }
    Add-Content -LiteralPath $resultPath -Value ""
}

function Test-CommandResult {
    param([string]$Executable, [string[]]$Arguments, [string]$WorkingDirectory, [string]$LogPath)
    Push-Location $WorkingDirectory
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $Executable @Arguments 2>&1
        $exitCode = $LASTEXITCODE
        $text = ($output | Out-String)
        Set-Content -LiteralPath $LogPath -Value $text
        return [pscustomobject]@{ ExitCode = $exitCode; Output = $text }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
        Pop-Location
    }
}

function Parse-Metric {
    param([string]$Text, [string]$Label)
    $pattern = [regex]::Escape($Label) + ":\s*(.+)"
    $match = [regex]::Match($Text, $pattern)
    if ($match.Success) { return $match.Groups[1].Value.Trim() }
    return ""
}

function Get-RuleSummary {
    param([string]$ReportPath)
    if (-not (Test-Path -LiteralPath $ReportPath)) { return "-" }
    $lines = Get-Content -LiteralPath $ReportPath
    $inRuleSummary = $false
    $hits = @()
    foreach ($line in $lines) {
        if ($line -match '^##\s+Rule Summary') { $inRuleSummary = $true; continue }
        if ($inRuleSummary -and $line -match '^##\s+') { break }
        if ($inRuleSummary -and $line -match '^- (.+):\s*([0-9]+)$') {
            $rule = $matches[1].Trim()
            $count = [int]$matches[2]
            if ($count -gt 0) { $hits += ("{0} ({1})" -f $rule, $count) }
        }
    }
    if ($hits.Count -eq 0) { return "No violations detected" }
    return (($hits | Select-Object -Unique | Select-Object -First 5) -join ", ")
}

function Get-BuildNote {
    param([string]$BuildLogPath)
    if (-not (Test-Path -LiteralPath $BuildLogPath)) { return "" }
    $text = Get-Content -LiteralPath $BuildLogPath -Raw
    if ($text -match 'release 25') { return "compile failed: JDK 25 required" }
    if ($text -match 'JAVA_HOME not found') { return "compile failed: JAVA_HOME not set for wrapper" }
    if ($text -match 'BUILD FAILURE') { return "compile failed: Maven build failure" }
    if ($text -match 'Maven Wrapper not found') { return "compile skipped: Maven Wrapper missing" }
    return ""
}

function Get-EffectiveProjectDir {
    param([string]$SampleDir)
    $candidates = @(
        $SampleDir,
        (Join-Path $SampleDir "complete"),
        (Join-Path $SampleDir "initial")
    )

    foreach ($candidate in $candidates) {
        if ((Test-Path -LiteralPath $candidate) -and (Test-Path -LiteralPath (Join-Path $candidate "src"))) {
            if (
                (Test-Path -LiteralPath (Join-Path $candidate "pom.xml")) -or
                (Test-Path -LiteralPath (Join-Path $candidate "build.gradle")) -or
                (Test-Path -LiteralPath (Join-Path $candidate "build.gradle.kts"))
            ) {
                return $candidate
            }
        }
    }

    return $SampleDir
}

function Test-AnyPath {
    param([string[]]$Paths)
    foreach ($path in $Paths) {
        if (Test-Path -LiteralPath $path) { return $true }
    }
    return $false
}

function Get-RelativePathOrSelf {
    param([string]$Path, [string]$Base)
    if ($Path.StartsWith($Base, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $Path.Substring($Base.Length).TrimStart('\')
    }
    return $Path
}

function Get-Recommendation {
    param([bool]$CloneOk, [bool]$CompileOk, [bool]$QScoutOk)
    if ($QScoutOk -and $CompileOk) { return "A" }
    if ($QScoutOk -or $CloneOk) { return "B" }
    return "C"
}

function Get-RecommendedUse {
    param([string]$SampleName, [bool]$QScoutOk, [bool]$CompileOk)
    switch ($SampleName) {
        "spring-petclinic" { if ($QScoutOk) { return "正常系ベースライン向き" }; return "正常系ベースライン候補" }
        "spring-petclinic-microservices" { if ($QScoutOk) { return "分散構成検査向き" }; return "重量級構成の参考用" }
        "spring-boot-realworld-example-app" { if ($QScoutOk) { return "実務寄りAPI評価向き" }; return "実務寄り参考用" }
        "sample-spring-modulith" { if ($QScoutOk) { return "責務分離・モジュール境界評価向き" }; return "モジュール構成参考用" }
        "spring-boot-monolith" { if ($QScoutOk) { return "構造揺さぶり向き" }; return "構造揺さぶり参考用" }
        "bookstore" { if ($QScoutOk) { return "業務CRUD/JPA評価向き" }; return "業務CRUD参考用" }
        "gs-rest-service" { if ($QScoutOk) { return "軽量REST検査向き" }; return "軽量REST参考用" }
        "gs-accessing-data-jpa" { if ($QScoutOk) { return "JPA/Repository基本検査向き" }; return "JPA基本構成参考用" }
        "gs-securing-web" { if ($QScoutOk) { return "Security構成検査向き" }; return "Security構成参考用" }
        "gs-reactive-rest-service" { if ($QScoutOk) { return "Reactive/WebFlux検査向き" }; return "Reactive構成参考用" }
        default { if ($CompileOk) { return "誤検知検証向き" }; return "参考用" }
    }
}

New-Item -ItemType Directory -Force -Path $samplesRoot | Out-Null
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
Set-Content -LiteralPath $resultPath -Value "# Codex Execution Result`r`n"
Set-Content -LiteralPath $summaryPath -Value "# Sample Comparison Summary`r`n"
Write-ResultLog -Status "OK" -Code "EXECUTION_COMMAND" -Message "powershell -ExecutionPolicy Bypass -File .\scripts\run-sample-evaluation-under-samples.ps1"
Write-ResultLog -Status "OK" -Code "F1_PREPARE_DONE" -Message ("Prepared: {0}, {1}" -f $samplesRoot, $outputRoot)

$rows = New-Object System.Collections.Generic.List[object]

foreach ($sample in $targets) {
    $sampleDir = Join-Path $samplesRoot $sample.Name
    $runDir = Join-Path $outputRoot $sample.Name
    $cloneLog = Join-Path $runDir "clone.log"
    $buildLog = Join-Path $runDir "build.log"
    $qscoutLog = Join-Path $runDir "qscout.log"

    New-Item -ItemType Directory -Force -Path $runDir | Out-Null

    $cloneOk = $false
    $pomOk = $false
    $mainSrcOk = $false
    $testSrcOk = $false
    $compileOk = $false
    $qScoutOk = $false
    $score = ""
    $violations = ""
    $ruleSummary = "-"
    $analysisRoot = ""
    $notes = New-Object System.Collections.Generic.List[string]

    if (Test-Path -LiteralPath $sampleDir) {
        $cloneOk = $true
        Set-Content -LiteralPath $cloneLog -Value "Repository directory already exists. Kept as-is."
        Write-ResultLog -Status "OK" -Code ("F2_CLONE_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand=skip existing clone`r`nsummary=already present, left unchanged`r`nretry=not required" -f $sample.Name)
    } else {
        $clone = Test-CommandResult -Executable "git" -Arguments @("clone", $sample.Url, $sampleDir) -WorkingDirectory $repoRoot -LogPath $cloneLog
        if ($clone.ExitCode -eq 0 -and (Test-Path -LiteralPath $sampleDir)) {
            $cloneOk = $true
            Write-ResultLog -Status "OK" -Code ("F2_CLONE_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand=git clone {1} {2}`r`nsummary=clone succeeded`r`nretry=not required" -f $sample.Name, $sample.Url, $sampleDir)
        } else {
            $notes.Add("clone failed")
            Write-ResultLog -Status "FAIL" -Code ("F2_CLONE_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=git clone {1} {2}`r`nsummary={3}`r`nretry=likely yes if network access is available" -f $sample.Name, $sample.Url, $sampleDir, ($clone.Output.Trim() -replace "`r?`n", " | "))
        }
    }

    if ($cloneOk) {
        $effectiveProjectDir = Get-EffectiveProjectDir -SampleDir $sampleDir
        $analysisRoot = Get-RelativePathOrSelf -Path $effectiveProjectDir -Base $repoRoot
        $pomOk = Test-AnyPath -Paths @(
            (Join-Path $effectiveProjectDir "pom.xml"),
            (Join-Path $sampleDir "pom.xml")
        )
        $mainSrcOk = Test-AnyPath -Paths @(
            (Join-Path $effectiveProjectDir "src\main\java"),
            (Join-Path $sampleDir "src\main\java")
        )
        $testSrcOk = Test-AnyPath -Paths @(
            (Join-Path $effectiveProjectDir "src\test\java"),
            (Join-Path $sampleDir "src\test\java")
        )
        if ($effectiveProjectDir -ne $sampleDir) {
            $notes.Add(("analysis root adjusted to {0}" -f $analysisRoot))
        }
    }

    if ($cloneOk) {
        $wrapper = $null
        $buildSystem = ""
        $projectDir = if ($effectiveProjectDir) { $effectiveProjectDir } else { $sampleDir }
        if (Test-Path -LiteralPath (Join-Path $projectDir "mvnw.cmd")) { $wrapper = "mvnw.cmd"; $buildSystem = "maven" }
        elseif (Test-Path -LiteralPath (Join-Path $projectDir "mvnw")) { $wrapper = "mvnw"; $buildSystem = "maven" }
        elseif (Test-Path -LiteralPath (Join-Path $projectDir "gradlew.bat")) { $wrapper = "gradlew.bat"; $buildSystem = "gradle" }
        elseif (Test-Path -LiteralPath (Join-Path $projectDir "gradlew")) { $wrapper = "gradlew"; $buildSystem = "gradle" }

        if ($wrapper) {
            if ($buildSystem -eq "maven") {
                if ($wrapper -eq "mvnw.cmd") {
                    $firstTry = Test-CommandResult -Executable "cmd" -Arguments @("/c", $wrapper, "-q", "-DskipTests", "compile") -WorkingDirectory $projectDir -LogPath $buildLog
                } else {
                    $firstTry = Test-CommandResult -Executable (Join-Path $projectDir $wrapper) -Arguments @("-q", "-DskipTests", "compile") -WorkingDirectory $projectDir -LogPath $buildLog
                }
            } else {
                if ($wrapper -eq "gradlew.bat") {
                    $firstTry = Test-CommandResult -Executable "cmd" -Arguments @("/c", $wrapper, "--quiet", "classes", "-x", "test") -WorkingDirectory $projectDir -LogPath $buildLog
                } else {
                    $firstTry = Test-CommandResult -Executable (Join-Path $projectDir $wrapper) -Arguments @("--quiet", "classes", "-x", "test") -WorkingDirectory $projectDir -LogPath $buildLog
                }
            }
            if ($firstTry.ExitCode -eq 0) {
                $compileOk = $true
            } else {
                if ($buildSystem -eq "maven") {
                    if ($wrapper -eq "mvnw.cmd") {
                        $secondTry = Test-CommandResult -Executable "cmd" -Arguments @("/c", $wrapper, "compile") -WorkingDirectory $projectDir -LogPath $buildLog
                    } else {
                        $secondTry = Test-CommandResult -Executable (Join-Path $projectDir $wrapper) -Arguments @("compile") -WorkingDirectory $projectDir -LogPath $buildLog
                    }
                } else {
                    if ($wrapper -eq "gradlew.bat") {
                        $secondTry = Test-CommandResult -Executable "cmd" -Arguments @("/c", $wrapper, "classes") -WorkingDirectory $projectDir -LogPath $buildLog
                    } else {
                        $secondTry = Test-CommandResult -Executable (Join-Path $projectDir $wrapper) -Arguments @("classes") -WorkingDirectory $projectDir -LogPath $buildLog
                    }
                }
                if ($secondTry.ExitCode -eq 0) { $compileOk = $true } else { $notes.Add("compile failed") }
            }
        } else {
            Set-Content -LiteralPath $buildLog -Value "Build wrapper not found."
            $notes.Add("build wrapper missing")
        }

        if ($compileOk) {
            Write-ResultLog -Status "OK" -Code ("F3_BUILD_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand={1} wrapper compile`r`nsummary=compile succeeded`r`nretry=not required" -f $sample.Name, $buildSystem)
        } else {
            $buildSummary = if (Test-Path -LiteralPath $buildLog) { ((Get-Content -LiteralPath $buildLog -Raw).Trim() -replace "`r?`n", " | ") } else { "build step not executed" }
            $buildNote = Get-BuildNote -BuildLogPath $buildLog
            if ($buildNote) { $notes.Add($buildNote) }
            $buildSystemLabel = if ($buildSystem) { $buildSystem } else { "unknown" }
            Write-ResultLog -Status "FAIL" -Code ("F3_BUILD_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand={1} wrapper compile`r`nsummary={2}`r`nretry=yes, depending on repository prerequisites" -f $sample.Name, $buildSystemLabel, $buildSummary)
        }
    } else {
        Set-Content -LiteralPath $buildLog -Value "Build skipped because clone failed."
        Write-ResultLog -Status "FAIL" -Code ("F3_BUILD_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=wrapper compile`r`nsummary=skipped because clone failed`r`nretry=yes after clone success" -f $sample.Name)
    }

    if ($cloneOk -and (Test-Path -LiteralPath $jarPath)) {
        $projectDir = if ($effectiveProjectDir) { $effectiveProjectDir } else { $sampleDir }
        $qScout = Test-CommandResult -Executable $runCli -Arguments @($projectDir, $runDir) -WorkingDirectory $repoRoot -LogPath $qscoutLog
        if ($qScout.ExitCode -eq 0) {
            $qScoutOk = $true
            $score = Parse-Metric -Text $qScout.Output -Label "Final Score"
            $violations = Parse-Metric -Text $qScout.Output -Label "Total Violations"
            $ruleSummary = Get-RuleSummary -ReportPath (Join-Path $runDir "qscout-report.md")
            Write-ResultLog -Status "OK" -Code ("F4_QSCOUT_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat {1} {2}`r`nsummary=analysis succeeded`r`nretry=not required" -f $sample.Name, $projectDir, $runDir)
        } else {
            $notes.Add("qscout failed")
            Write-ResultLog -Status "FAIL" -Code ("F4_QSCOUT_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat {1} {2}`r`nsummary={3}`r`nretry=yes, depending on source structure and runtime dependencies" -f $sample.Name, $projectDir, $runDir, ($qScout.Output.Trim() -replace "`r?`n", " | "))
        }
    } else {
        Set-Content -LiteralPath $qscoutLog -Value "Q-Scout skipped because clone failed or jar is missing."
        Write-ResultLog -Status "FAIL" -Code ("F4_QSCOUT_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat sample output`r`nsummary=skipped because clone failed or jar missing`r`nretry=yes after prerequisites are met" -f $sample.Name)
    }

    $rows.Add([pscustomobject]@{
        Sample = $sample.Name
        Tier = $sample.Tier
        Clone = if ($cloneOk) { "OK" } else { "FAIL" }
        Pom = if ($pomOk) { "Yes" } else { "No" }
        Main = if ($mainSrcOk) { "Yes" } else { "No" }
        Test = if ($testSrcOk) { "Yes" } else { "No" }
        Compile = if ($compileOk) { "OK" } else { "FAIL" }
        QScout = if ($qScoutOk) { "OK" } else { "FAIL" }
        Score = if ($score) { $score } else { "-" }
        Violations = if ($violations) { $violations } else { "-" }
        RecommendedUse = Get-RecommendedUse -SampleName $sample.Name -QScoutOk $qScoutOk -CompileOk $compileOk
        Recommendation = Get-Recommendation -CloneOk $cloneOk -CompileOk $compileOk -QScoutOk $qScoutOk
        RuleSummary = $ruleSummary
        AnalysisRoot = if ($analysisRoot) { $analysisRoot } else { $sample.Name }
        Notes = if ($notes.Count -gt 0) { $notes -join ", " } else { $sample.IntendedUse }
    })
}

$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("# Sample Comparison Summary")
$summaryLines.Add("")
$summaryLines.Add("## Execution Summary")
$summaryLines.Add("")
$summaryLines.Add("- Command: powershell -ExecutionPolicy Bypass -File .\\scripts\\run-sample-evaluation-under-samples.ps1")
$summaryLines.Add(("- Clone success / fail: {0} / {1}" -f (($rows | Where-Object { $_.Clone -eq "OK" }).Count), (($rows | Where-Object { $_.Clone -eq "FAIL" }).Count)))
$summaryLines.Add(("- Build success / fail: {0} / {1}" -f (($rows | Where-Object { $_.Compile -eq "OK" }).Count), (($rows | Where-Object { $_.Compile -eq "FAIL" }).Count)))
$summaryLines.Add(("- Q-Scout success / fail: {0} / {1}" -f (($rows | Where-Object { $_.QScout -eq "OK" }).Count), (($rows | Where-Object { $_.QScout -eq "FAIL" }).Count)))
$summaryLines.Add("")
$summaryLines.Add("| Sample | Tier | Clone | pom.xml | Main Src | Test Src | Compile | Q-Scout | Score | Violations | Recommended Use | Recommendation |")
$summaryLines.Add("|--------|------|-------|---------|----------|----------|---------|---------|-------|------------|-----------------|----------------|")
foreach ($row in $rows) {
    $summaryLines.Add(("| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} | {11} |" -f $row.Sample, $row.Tier, $row.Clone, $row.Pom, $row.Main, $row.Test, $row.Compile, $row.QScout, $row.Score, $row.Violations, $row.RecommendedUse, $row.Recommendation))
}
$summaryLines.Add("")

foreach ($row in $rows) {
    $summaryLines.Add(("## {0}" -f $row.Sample))
    $summaryLines.Add(("- tier: {0}" -f $row.Tier))
    $summaryLines.Add(("- clone: {0}" -f $row.Clone))
    $summaryLines.Add(("- analysis root: {0}" -f $row.AnalysisRoot))
    $summaryLines.Add(("- source structure: pom.xml={0}, main={1}, test={2}" -f $row.Pom, $row.Main, $row.Test))
    $summaryLines.Add(("- compile: {0}" -f $row.Compile))
    $summaryLines.Add(("- q-scout: {0}" -f $row.QScout))
    $summaryLines.Add(("- final score / violations: {0} / {1}" -f $row.Score, $row.Violations))
    $summaryLines.Add(("- detected rules: {0}" -f $row.RuleSummary))
    $summaryLines.Add(("- notes: {0}" -f $row.Notes))
    $summaryLines.Add("")
}

$coreCandidates = $rows | Where-Object { $_.Recommendation -in @("A", "B") -and $_.Tier -eq "Tier1" } | Select-Object -First 3
$supportCandidates = $rows | Where-Object { $_.Recommendation -in @("A", "B") -and $_.Tier -ne "Tier1" } | Select-Object -First 3
$heavyCandidates = $rows | Where-Object { $_.Tier -eq "Tier3" } | Select-Object -First 2

$summaryLines.Add("## Final Conclusion")
$summaryLines.Add("")
$summaryLines.Add("1. 今後の標準サンプルとして採用すべき 5 本前後")
$adopted = $rows | Where-Object { $_.Recommendation -in @("A", "B") } | Select-Object -First 5
if ($adopted) {
    foreach ($item in $adopted) { $summaryLines.Add(("   - {0}" -f $item.Sample)) }
} else {
    $summaryLines.Add("   - 現時点では継続採用候補なし")
}
$summaryLines.Add("2. 中核採用候補 3 本")
if ($coreCandidates) {
    foreach ($item in $coreCandidates) { $summaryLines.Add(("   - {0}" -f $item.Sample)) }
} else {
    $summaryLines.Add("   - Tier1 の中核候補は再評価が必要")
}
$summaryLines.Add("3. 技術特性確認用の補助候補")
if ($supportCandidates) {
    foreach ($item in $supportCandidates) { $summaryLines.Add(("   - {0}" -f $item.Sample)) }
} else {
    $summaryLines.Add("   - 補助候補は今回の結果から追加選定")
}
$summaryLines.Add("4. 重量級で常時実行には不向きな候補")
if ($heavyCandidates) {
    foreach ($item in $heavyCandidates) { $summaryLines.Add(("   - {0}" -f $item.Sample)) }
} else {
    $summaryLines.Add("   - 今回の一覧には重量級候補なし")
}
$summaryLines.Add("5. 今後、意図的アンチパターンサンプルを別途自作すべきか")
$summaryLines.Add("   - 作るべき。公開サンプルだけでは誤検知・見逃しの境界条件を十分に揺さぶれないため。")
$summaryLines.Add("")

$failedClone = ($rows | Where-Object { $_.Clone -eq "FAIL" }).Count
$failedQScout = ($rows | Where-Object { $_.QScout -eq "FAIL" }).Count
if ($failedClone -gt 0 -or $failedQScout -gt 0) {
    $summaryLines.Add("## Incomplete Work")
    $summaryLines.Add("")
    $summaryLines.Add("- 完了したフェーズ: フェーズ1, フェーズ2, フェーズ3, フェーズ4, フェーズ5")
    $summaryLines.Add("- 未完了フェーズ: なし。ただし一部サンプルは失敗あり")
    $summaryLines.Add("- 失敗理由: clone 失敗や解析失敗の詳細は samples/CodexExec.result と samples/sample-output 配下ログを参照")
    $summaryLines.Add("- 再開時の開始地点: 失敗したサンプルのフェーズ2またはフェーズ4から再実行")
    $tempRecommended = $rows | Sort-Object Recommendation, Sample | Select-Object -First 3
    $summaryLines.Add(("- 現時点での暫定推奨サンプル: {0}" -f (($tempRecommended | ForEach-Object { $_.Sample }) -join ", ")))
}

Set-Content -LiteralPath $summaryPath -Value ($summaryLines -join "`r`n")
Write-ResultLog -Status "OK" -Code "F5_SUMMARY_DONE" -Message ("summary={0}" -f $summaryPath)
