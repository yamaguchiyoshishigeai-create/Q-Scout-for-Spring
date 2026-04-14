# Purpose: create a compact ZIP for Web inspection by excluding VCS/build artifacts and bulky sample-only assets.
param(
    [string]$ProjectPath = "samples/bookstore",
    [string]$OutputZipPath,
    [string[]]$AdditionalExcludePaths = @()
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$repoRoot = Split-Path -Parent $PSScriptRoot
$resolvedProjectPath = if ([System.IO.Path]::IsPathRooted($ProjectPath)) {
    $ProjectPath
} else {
    Join-Path $repoRoot $ProjectPath
}
$resolvedProjectPath = (Resolve-Path -LiteralPath $resolvedProjectPath).Path

if (-not $OutputZipPath) {
    $projectName = Split-Path -Leaf $resolvedProjectPath
    $OutputZipPath = Join-Path $repoRoot ("samples/{0}-inspection.zip" -f $projectName)
}

$resolvedOutputZipPath = if ([System.IO.Path]::IsPathRooted($OutputZipPath)) {
    $OutputZipPath
} else {
    Join-Path $repoRoot $OutputZipPath
}

$outputDirectory = Split-Path -Parent $resolvedOutputZipPath
if ($outputDirectory) {
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
}

$stagingRoot = Join-Path $repoRoot ".tmp\inspection-zips"
$projectName = Split-Path -Leaf $resolvedProjectPath
$stagingPath = Join-Path $stagingRoot $projectName

$topLevelExcludes = @(
    ".git",
    ".github",
    ".idea",
    ".vscode",
    "target",
    "build",
    "node_modules",
    ".gradle",
    ".gemini"
)

$projectRelativeExcludes = @(
    "docker",
    "deployment",
    "src/main/resources/data/books.jsonlines"
)

if ($AdditionalExcludePaths.Count -gt 0) {
    $projectRelativeExcludes += $AdditionalExcludePaths
}

function Remove-PathIfExists {
    param([string]$LiteralPath)

    if (Test-Path -LiteralPath $LiteralPath) {
        Remove-Item -LiteralPath $LiteralPath -Recurse -Force
    }
}

function Remove-DirectoryIfEmpty {
    param([string]$LiteralPath)

    if (-not (Test-Path -LiteralPath $LiteralPath)) {
        return
    }

    $remainingEntries = Get-ChildItem -LiteralPath $LiteralPath -Force -ErrorAction SilentlyContinue
    if ($remainingEntries.Count -eq 0) {
        Remove-Item -LiteralPath $LiteralPath -Force
    }
}

function Get-DirectorySizeBytes {
    param([string]$LiteralPath)

    $sum = (Get-ChildItem -LiteralPath $LiteralPath -Recurse -File -Force -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum
    if ($null -eq $sum) {
        return 0
    }
    return [int64]$sum
}

function Get-ZipEntryName {
    param(
        [string]$RootPath,
        [string]$FilePath
    )

    $rootWithSeparator = $RootPath.TrimEnd('\') + '\'
    $relativePath = $FilePath.Substring($rootWithSeparator.Length)
    return ($relativePath -replace '\\', '/')
}

Remove-PathIfExists -LiteralPath $stagingPath
if (Test-Path -LiteralPath $resolvedOutputZipPath) {
    Remove-Item -LiteralPath $resolvedOutputZipPath -Force
}

New-Item -ItemType Directory -Force -Path $stagingPath | Out-Null

Get-ChildItem -LiteralPath $resolvedProjectPath -Force | Where-Object {
    $_.Name -notin $topLevelExcludes
} | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $stagingPath -Recurse -Force
}

foreach ($relativePath in $projectRelativeExcludes | Select-Object -Unique) {
    $targetPath = Join-Path $stagingPath $relativePath
    Remove-PathIfExists -LiteralPath $targetPath
}

$zipFileStream = [System.IO.File]::Open($resolvedOutputZipPath, [System.IO.FileMode]::Create)
try {
    $zipArchive = New-Object System.IO.Compression.ZipArchive($zipFileStream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        Get-ChildItem -LiteralPath $stagingPath -Recurse -File -Force | ForEach-Object {
            $entryName = Get-ZipEntryName -RootPath $stagingPath -FilePath $_.FullName
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zipArchive, $_.FullName, $entryName, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally {
        $zipArchive.Dispose()
    }
} finally {
    $zipFileStream.Dispose()
}

$zipSizeBytes = (Get-Item -LiteralPath $resolvedOutputZipPath).Length
$sourceSizeBytes = Get-DirectorySizeBytes -LiteralPath $resolvedProjectPath
$stagedSizeBytes = Get-DirectorySizeBytes -LiteralPath $stagingPath

Write-Host ("Created: {0}" -f $resolvedOutputZipPath)
Write-Host ("Source size : {0:N2} MB" -f ($sourceSizeBytes / 1MB))
Write-Host ("Staged size : {0:N2} MB" -f ($stagedSizeBytes / 1MB))
Write-Host ("ZIP size    : {0:N2} MB" -f ($zipSizeBytes / 1MB))
Write-Host "Excluded top-level entries:"
$topLevelExcludes | ForEach-Object { Write-Host ("  - {0}" -f $_) }
Write-Host "Excluded project-relative paths:"
($projectRelativeExcludes | Select-Object -Unique) | ForEach-Object { Write-Host ("  - {0}" -f $_) }

Remove-PathIfExists -LiteralPath $stagingPath
Remove-DirectoryIfEmpty -LiteralPath $stagingRoot
$stagingRootParent = Split-Path -Parent $stagingRoot
if ($stagingRootParent) {
    Remove-DirectoryIfEmpty -LiteralPath $stagingRootParent
}
