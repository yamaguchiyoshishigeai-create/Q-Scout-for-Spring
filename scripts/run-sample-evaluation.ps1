# Purpose: clone public Spring samples, run light build checks, execute Q-Scout, and aggregate logs/results.
param()

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$samplesRoot = Join-Path $repoRoot "samples"
$runsRoot = Join-Path $repoRoot "sample-runs"
$resultPath = Join-Path $repoRoot "CodexExec.result"
$summaryPath = Join-Path $repoRoot "sample-comparison-summary.md"
$jarPath = Join-Path $repoRoot "target\q-scout-for-spring-0.1.0-SNAPSHOT.jar"
$runCli = Join-Path $repoRoot "run-cli.bat"

$samples = @(
    [pscustomobject]@{
        Name = "spring-petclinic"
        Url = "https://github.com/spring-projects/spring-petclinic.git"
        IntendedUse = "正常系ベースライン"
    },
    [pscustomobject]@{
        Name = "bookstore"
        Url = "https://github.com/sivaprasadreddy/bookstore.git"
        IntendedUse = "実務寄り評価用"
    },
    [pscustomobject]@{
        Name = "spring-boot-monolith"
        Url = "https://github.com/mzubal/spring-boot-monolith.git"
        IntendedUse = "構造揺さぶり評価用"
    }
)

function Write-ResultLog {
    param(
        [string]$Status,
        [string]$Code,
        [string]$Message
    )
    Add-Content -LiteralPath $resultPath -Value ("[{0}] {1}" -f $Status, $Code)
    if ($Message) {
        Add-Content -LiteralPath $resultPath -Value $Message
    }
    Add-Content -LiteralPath $resultPath -Value ""
}

function Run-Step {
    param(
        [string]$Command,
        [string]$WorkingDirectory,
        [string]$LogPath
    )

    $output = & powershell -NoProfile -Command $Command 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($output | Out-String)
    Set-Content -LiteralPath $LogPath -Value $text
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = $text
    }
}

function Test-CommandResult {
    param(
        [string]$Executable,
        [string[]]$Arguments,
        [string]$WorkingDirectory,
        [string]$LogPath
    )

    Push-Location $WorkingDirectory
    try {
        $output = & $Executable @Arguments 2>&1
        $exitCode = $LASTEXITCODE
        $text = ($output | Out-String)
        Set-Content -LiteralPath $LogPath -Value $text
        return [pscustomobject]@{
            ExitCode = $exitCode
            Output = $text
        }
    } finally {
        Pop-Location
    }
}

function Parse-Metric {
    param(
        [string]$Text,
        [string]$Label
    )
    $pattern = [regex]::Escape($Label) + ":\s*(.+)"
    $match = [regex]::Match($Text, $pattern)
    if ($match.Success) {
        return $match.Groups[1].Value.Trim()
    }
    return ""
}

function Get-RuleSummary {
    param([string]$ReportPath)

    if (-not (Test-Path -LiteralPath $ReportPath)) {
        return ""
    }

    $lines = Get-Content -LiteralPath $ReportPath
    $hits = @()
    foreach ($line in $lines) {
        if ($line -match '^##\s+(.+)$') {
            $name = $matches[1].Trim()
            if ($name -notin @("Q-Scout Report", "Summary", "Findings", "Recommendations")) {
                $hits += $name
            }
        }
    }
    if ($hits.Count -eq 0) {
        return ""
    }
    return (($hits | Select-Object -Unique | Select-Object -First 5) -join ", ")
}

function Get-Recommendation {
    param(
        [bool]$CloneOk,
        [bool]$CompileOk,
        [bool]$QScoutOk,
        [string]$SampleName
    )

    if ($QScoutOk -and $CompileOk) {
        return "A"
    }
    if ($QScoutOk -or $CloneOk) {
        return "B"
    }
    return "C"
}

function Get-RecommendedUse {
    param(
        [string]$SampleName,
        [bool]$QScoutOk,
        [bool]$CompileOk
    )

    switch ($SampleName) {
        "spring-petclinic" {
            if ($QScoutOk) { return "正常系ベースライン向き" }
            return "正常系ベースライン候補"
        }
        "bookstore" {
            if ($QScoutOk) { return "実務寄り向き" }
            return "実務寄り参考用"
        }
        "spring-boot-monolith" {
            if ($QScoutOk) { return "構造揺さぶり向き" }
            return "構造揺さぶり参考用"
        }
        default {
            if ($CompileOk) { return "誤検知検証向き" }
            return "参考用"
        }
    }
}

New-Item -ItemType Directory -Force -Path $samplesRoot | Out-Null
New-Item -ItemType Directory -Force -Path $runsRoot | Out-Null
Set-Content -LiteralPath $resultPath -Value "# Codex Execution Result`r`n"
Set-Content -LiteralPath $summaryPath -Value "# Sample Comparison Summary`r`n"
Write-ResultLog -Status "OK" -Code "F1_PREPARE_DONE" -Message ("Prepared: {0}, {1}" -f $samplesRoot, $runsRoot)

$rows = New-Object System.Collections.Generic.List[object]
$conclusions = New-Object System.Collections.Generic.List[string]
$completedPhases = @("フェーズ1")
$unfinishedPhases = New-Object System.Collections.Generic.List[string]

foreach ($sample in $samples) {
    $sampleDir = Join-Path $samplesRoot $sample.Name
    $runDir = Join-Path $runsRoot $sample.Name
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
    $ruleSummary = ""
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
        $pomOk = Test-Path -LiteralPath (Join-Path $sampleDir "pom.xml")
        $mainSrcOk = Test-Path -LiteralPath (Join-Path $sampleDir "src\main\java")
        $testSrcOk = Test-Path -LiteralPath (Join-Path $sampleDir "src\test\java")
    }

    if ($cloneOk) {
        $wrapper = $null
        if (Test-Path -LiteralPath (Join-Path $sampleDir "mvnw.cmd")) {
            $wrapper = "mvnw.cmd"
        } elseif (Test-Path -LiteralPath (Join-Path $sampleDir "mvnw")) {
            $wrapper = "mvnw"
        }

        if ($wrapper) {
            $firstTry = Test-CommandResult -Executable (Join-Path $sampleDir $wrapper) -Arguments @("-q", "-DskipTests", "compile") -WorkingDirectory $sampleDir -LogPath $buildLog
            if ($firstTry.ExitCode -eq 0) {
                $compileOk = $true
            } else {
                $secondTry = Test-CommandResult -Executable (Join-Path $sampleDir $wrapper) -Arguments @("compile") -WorkingDirectory $sampleDir -LogPath $buildLog
                if ($secondTry.ExitCode -eq 0) {
                    $compileOk = $true
                } else {
                    $notes.Add("compile failed")
                }
            }
        } else {
            Set-Content -LiteralPath $buildLog -Value "Maven Wrapper not found."
            $notes.Add("maven wrapper missing")
        }

        if ($compileOk) {
            Write-ResultLog -Status "OK" -Code ("F3_BUILD_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand=wrapper compile`r`nsummary=compile succeeded`r`nretry=not required" -f $sample.Name)
        } else {
            $buildSummary = if (Test-Path -LiteralPath $buildLog) { ((Get-Content -LiteralPath $buildLog -Raw).Trim() -replace "`r?`n", " | ") } else { "build step not executed" }
            Write-ResultLog -Status "FAIL" -Code ("F3_BUILD_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=wrapper compile`r`nsummary={1}`r`nretry=yes, depending on repository prerequisites" -f $sample.Name, $buildSummary)
        }
    } else {
        Set-Content -LiteralPath $buildLog -Value "Build skipped because clone failed."
        Write-ResultLog -Status "FAIL" -Code ("F3_BUILD_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=wrapper compile`r`nsummary=skipped because clone failed`r`nretry=yes after clone success" -f $sample.Name)
    }

    if ($cloneOk -and (Test-Path -LiteralPath $jarPath)) {
        $qScout = Test-CommandResult -Executable $runCli -Arguments @($sampleDir, $runDir) -WorkingDirectory $repoRoot -LogPath $qscoutLog
        if ($qScout.ExitCode -eq 0) {
            $qScoutOk = $true
            $score = Parse-Metric -Text $qScout.Output -Label "Final Score"
            $violations = Parse-Metric -Text $qScout.Output -Label "Total Violations"
            $ruleSummary = Get-RuleSummary -ReportPath (Join-Path $runDir "qscout-report.md")
            Write-ResultLog -Status "OK" -Code ("F4_QSCOUT_{0}_DONE" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat {1} {2}`r`nsummary=analysis succeeded`r`nretry=not required" -f $sample.Name, $sampleDir, $runDir)
        } else {
            $notes.Add("qscout failed")
            Write-ResultLog -Status "FAIL" -Code ("F4_QSCOUT_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat {1} {2}`r`nsummary={3}`r`nretry=yes, depending on source structure and runtime dependencies" -f $sample.Name, $sampleDir, $runDir, ($qScout.Output.Trim() -replace "`r?`n", " | "))
        }
    } else {
        Set-Content -LiteralPath $qscoutLog -Value "Q-Scout skipped because clone failed or jar is missing."
        Write-ResultLog -Status "FAIL" -Code ("F4_QSCOUT_{0}_FAILED" -f $sample.Name) -Message ("repo={0}`r`ncommand=run-cli.bat sample output`r`nsummary=skipped because clone failed or jar missing`r`nretry=yes after prerequisites are met" -f $sample.Name)
    }

    $rows.Add([pscustomobject]@{
        Sample = $sample.Name
        Clone = if ($cloneOk) { "OK" } else { "FAIL" }
        Pom = if ($pomOk) { "Yes" } else { "No" }
        Main = if ($mainSrcOk) { "Yes" } else { "No" }
        Test = if ($testSrcOk) { "Yes" } else { "No" }
        Compile = if ($compileOk) { "OK" } else { "FAIL" }
        QScout = if ($qScoutOk) { "OK" } else { "FAIL" }
        Score = if ($score) { $score } else { "-" }
        Violations = if ($violations) { $violations } else { "-" }
        RecommendedUse = Get-RecommendedUse -SampleName $sample.Name -QScoutOk $qScoutOk -CompileOk $compileOk
        Recommendation = Get-Recommendation -CloneOk $cloneOk -CompileOk $compileOk -QScoutOk $qScoutOk -SampleName $sample.Name
        RuleSummary = if ($ruleSummary) { $ruleSummary } else { "-" }
        Notes = if ($notes.Count -gt 0) { $notes -join ", " } else { $sample.IntendedUse }
    })
}

$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("# Sample Comparison Summary")
$summaryLines.Add("")
$summaryLines.Add("| Sample | Clone | pom.xml | Main Src | Test Src | Compile | Q-Scout | Score | Violations | Recommended Use | Recommendation |")
$summaryLines.Add("|--------|-------|---------|----------|----------|---------|---------|-------|------------|-----------------|----------------|")
foreach ($row in $rows) {
    $summaryLines.Add(("| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} |" -f
        $row.Sample, $row.Clone, $row.Pom, $row.Main, $row.Test, $row.Compile, $row.QScout, $row.Score, $row.Violations, $row.RecommendedUse, $row.Recommendation))
}
$summaryLines.Add("")

foreach ($row in $rows) {
    $summaryLines.Add(("## {0}" -f $row.Sample))
    $summaryLines.Add(("- clone: {0}" -f $row.Clone))
    $summaryLines.Add(("- source structure: pom.xml={0}, main={1}, test={2}" -f $row.Pom, $row.Main, $row.Test))
    $summaryLines.Add(("- compile: {0}" -f $row.Compile))
    $summaryLines.Add(("- q-scout: {0}" -f $row.QScout))
    $summaryLines.Add(("- final score / violations: {0} / {1}" -f $row.Score, $row.Violations))
    $summaryLines.Add(("- detected rules: {0}" -f $row.RuleSummary))
    $summaryLines.Add(("- notes: {0}" -f $row.Notes))
    $summaryLines.Add("")
}

$adopted = $rows | Where-Object { $_.Recommendation -in @("A", "B") } | Select-Object -First 3
$summaryLines.Add("## Final Conclusion")
$summaryLines.Add("")
$summaryLines.Add("1. 今後の標準サンプルとして採用すべき 2〜3 本")
if ($adopted) {
    foreach ($item in $adopted) {
        $summaryLines.Add(("   - {0}" -f $item.Sample))
    }
} else {
    $summaryLines.Add("   - 現時点では継続採用候補なし")
}
$summaryLines.Add("2. その採用理由")
if ($adopted) {
    foreach ($item in $adopted) {
        $summaryLines.Add(("   - {0}: Clone={1}, Compile={2}, Q-Scout={3} のため評価継続に向く" -f $item.Sample, $item.Clone, $item.Compile, $item.QScout))
    }
} else {
    $summaryLines.Add("   - 取得または解析の安定性が不足しており、追加整備後に再判定が必要")
}
$summaryLines.Add("3. Q-Scout のどの評価用途に向くか")
foreach ($item in $rows) {
    $summaryLines.Add(("   - {0}: {1}" -f $item.Sample, $item.RecommendedUse))
}
$summaryLines.Add("4. 次に追加取得すべきサンプルがあるか")
$summaryLines.Add("   - ある。MVC 構成がより明確な中規模 Spring Boot サンプルと、マルチモジュール構成のサンプルを追加すると比較軸が増える。")
$summaryLines.Add("5. 必要なら「意図的にアンチパターンを含む自作サンプル」を別途作るべきか")
$summaryLines.Add("   - 作るべき。公開サンプルだけでは誤検知・見逃しの境界条件を十分に揺さぶれないため。")
$summaryLines.Add("")

$failedClone = ($rows | Where-Object { $_.Clone -eq "FAIL" }).Count
$failedQScout = ($rows | Where-Object { $_.QScout -eq "FAIL" }).Count
if ($failedClone -gt 0 -or $failedQScout -gt 0) {
    $summaryLines.Add("## Incomplete Work")
    $summaryLines.Add("")
    $summaryLines.Add(("- 完了したフェーズ: フェーズ1, フェーズ2, フェーズ3, フェーズ4, フェーズ5"))
    $summaryLines.Add(("- 未完了フェーズ: なし。ただし一部サンプルは失敗あり"))
    $summaryLines.Add(("- 失敗理由: clone 失敗や解析失敗の詳細は CodexExec.result と各 sample-runs 配下ログを参照"))
    $summaryLines.Add(("- 再開時の開始地点: 失敗したサンプルのフェーズ2またはフェーズ4から再実行"))
    $tempRecommended = $rows | Sort-Object Recommendation, Sample | Select-Object -First 2
    $summaryLines.Add(("- 現時点での暫定推奨サンプル: {0}" -f (($tempRecommended | ForEach-Object { $_.Sample }) -join ", ")))
}

Set-Content -LiteralPath $summaryPath -Value ($summaryLines -join "`r`n")
Write-ResultLog -Status "OK" -Code "F5_SUMMARY_DONE" -Message ("summary={0}" -f $summaryPath)
