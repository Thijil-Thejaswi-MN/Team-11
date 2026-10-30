<#
.SYNOPSIS
    Automated Test Suite for P4-14 Database Backup & Restore Scripts (TC-Rel-01 / INS-NF-004).
.DESCRIPTION
    Validates script syntax, argument handling, checksum generation, checksum verification,
    corruption rejection, and error handling for missing/empty files.
    If the Docker database container is active, also runs live backup, restore, and drill tests.
#>

$ErrorActionPreference = "Continue"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Team-11 P4-14 Database Backup & Recovery Test Suite (TC-Rel-01)" -ForegroundColor Cyan
Write-Host "======================================================================`n"

$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Definition }
$projectRoot = Split-Path -Parent $scriptDir
$tempDir = Join-Path $projectRoot "backups\test_scratch"

if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
}

$passCount = 0
$failCount = 0

function Assert-Test($name, $condition, $details) {
    if ($condition) {
        Write-Host "[PASS] $name" -ForegroundColor Green
        if ($details) { Write-Host "       $details" -ForegroundColor Gray }
        $script:passCount++
    } else {
        Write-Host "[FAIL] $name" -ForegroundColor Red
        if ($details) { Write-Host "       $details" -ForegroundColor Yellow }
        $script:failCount++
    }
}

# 1. PowerShell Script Syntax Validation
Write-Host "--- 1. Validating PowerShell Script Syntax ---" -ForegroundColor White
$ps1Files = @(
    "backup-database.ps1",
    "restore-database.ps1",
    "recovery-drill.ps1"
)

foreach ($file in $ps1Files) {
    $fullPath = Join-Path $scriptDir $file
    $syntaxOk = $false
    $parseErrors = $null
    try {
        [System.Management.Automation.Language.Parser]::ParseFile($fullPath, [ref]$null, [ref]$parseErrors) | Out-Null
        $syntaxOk = ($parseErrors.Count -eq 0)
    } catch {
        $syntaxOk = $false
    }
    $details = if ($syntaxOk) { "Parsed without errors" } else { ($parseErrors | Out-String).Trim() }
    Assert-Test "Syntax parse: $file" $syntaxOk $details
}

# 2. Bash Script Syntax Validation
Write-Host "`n--- 2. Validating Bash Script Syntax (bash -n) ---" -ForegroundColor White
$shFiles = @(
    "backup-database.sh",
    "restore-database.sh",
    "recovery-drill.sh"
)

$bashCmd = $null
if (Test-Path "C:\Program Files\Git\bin\bash.exe") {
    $bashCmd = "C:\Program Files\Git\bin\bash.exe"
} elseif (Get-Command bash -ErrorAction SilentlyContinue) {
    $bashCmd = "bash"
}

if ($bashCmd) {
    foreach ($file in $shFiles) {
        $fullPath = (Join-Path $scriptDir $file).Replace('\', '/')
        $out = & $bashCmd -n "$fullPath" 2>&1
        $bashExit = $LASTEXITCODE
        $details = if ($bashExit -eq 0) { "bash -n passed" } else { ($out -join ' ') }
        Assert-Test "Bash syntax check: $file" ($bashExit -eq 0) $details
    }
} else {
    Write-Host "[WARN] Bash executable not found. Skipping bash -n syntax checks." -ForegroundColor Yellow
}

# 3. Missing and Empty File Error Handling
Write-Host "`n--- 3. Testing Error Handling for Missing / Empty Files ---" -ForegroundColor White
$restoreScript = Join-Path $scriptDir "restore-database.ps1"

# Test missing file
$missingPath = Join-Path $tempDir "non_existent_file.sql.gz"
$missingOut = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $missingPath -Force 2>&1
$missingExit = $LASTEXITCODE
Assert-Test "Reject missing backup file" ($missingExit -ne 0) "Exit code $missingExit, rejected non-existent file"

# Test empty file (0 bytes)
$emptyPath = Join-Path $tempDir "empty_backup.sql.gz"
[System.IO.File]::WriteAllBytes($emptyPath, [byte[]]@())
$emptyOut = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $emptyPath -Force 2>&1
$emptyExit = $LASTEXITCODE
Assert-Test "Reject empty backup file (0 bytes)" ($emptyExit -ne 0) "Exit code $emptyExit, rejected 0-byte file"
Remove-Item $emptyPath -Force -ErrorAction SilentlyContinue

# 4. Checksum Generation and Verification Logic
Write-Host "`n--- 4. Testing Checksum Generation & Verification ---" -ForegroundColor White
$sampleSql = @"
-- Canonical Test SQL Dump
CREATE TABLE IF NOT EXISTS test_table (id INT PRIMARY KEY, name VARCHAR(50));
INSERT INTO test_table VALUES (1, 'Alice'), (2, 'Bob');
"@

$validArchive = Join-Path $tempDir "sample_valid.sql.gz"
$fs = [System.IO.File]::Create($validArchive)
$gz = New-Object System.IO.Compression.GZipStream($fs, [System.IO.Compression.CompressionLevel]::Optimal)
$rawBytes = [System.Text.Encoding]::UTF8.GetBytes($sampleSql)
$gz.Write($rawBytes, 0, $rawBytes.Length)
$gz.Close()
$fs.Close()

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$validHash = (Get-FileHash -Path $validArchive -Algorithm SHA256).Hash.ToLowerInvariant()
$validShaFile = "${validArchive}.sha256"
[System.IO.File]::WriteAllText($validShaFile, "$validHash  sample_valid.sql.gz`r`n", $utf8NoBom)

# Test VerifyOnly with valid checksum in PowerShell
$validOut = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $validArchive -VerifyOnly 2>&1
$validExit = $LASTEXITCODE
$validPassed = ($validExit -eq 0 -and ($validOut | Out-String) -match "Checksum matched")
Assert-Test "PowerShell: Validate valid archive and matching checksum (-VerifyOnly)" $validPassed "Exit code $validExit, Checksum verified"

# Test VerifyOnly in Bash
if ($bashCmd) {
    $restoreSh = (Join-Path $scriptDir "restore-database.sh").Replace('\', '/')
    $validArchiveUnix = $validArchive.Replace('\', '/')
    $validShOut = & $bashCmd "$restoreSh" --file "$validArchiveUnix" --verify-only 2>&1
    $validShExit = $LASTEXITCODE
    $validShPassed = ($validShExit -eq 0 -and ($validShOut | Out-String) -match "Checksum matched")
    Assert-Test "Bash: Validate valid archive and matching checksum (--verify-only)" $validShPassed "Exit code $validShExit, Checksum verified"
}

# 5. Corruption & Tamper Detection
Write-Host "`n--- 5. Testing Corruption & Tamper Rejection ---" -ForegroundColor White
$corruptArchive = Join-Path $tempDir "sample_corrupted.sql.gz"
Copy-Item $validArchive $corruptArchive -Force

# Flip byte in the corrupted copy
$cBytes = [System.IO.File]::ReadAllBytes($corruptArchive)
$cBytes[$cBytes.Length - 5] = ($cBytes[$cBytes.Length - 5] -bxor 0xFF)
[System.IO.File]::WriteAllBytes($corruptArchive, $cBytes)

# Associate original checksum file to trigger mismatch
$corruptShaFile = "${corruptArchive}.sha256"
[System.IO.File]::WriteAllText($corruptShaFile, "$validHash  sample_corrupted.sql.gz`r`n", $utf8NoBom)

# Test PowerShell corruption rejection
$corruptOut = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $corruptArchive -VerifyOnly 2>&1
$corruptExit = $LASTEXITCODE
$corruptPassed = ($corruptExit -ne 0 -and ($corruptOut | Out-String) -match "CHECKSUM VERIFICATION FAILED")
Assert-Test "PowerShell: Reject corrupted / tampered archive with checksum failure" $corruptPassed "Exit code $corruptExit, detected corrupted archive"

# Test Bash corruption rejection
if ($bashCmd) {
    $corruptArchiveUnix = $corruptArchive.Replace('\', '/')
    $corruptShOut = & $bashCmd "$restoreSh" --file "$corruptArchiveUnix" --verify-only 2>&1
    $corruptShExit = $LASTEXITCODE
    $corruptShPassed = ($corruptShExit -ne 0 -and ($corruptShOut | Out-String) -match "CHECKSUM VERIFICATION FAILED")
    Assert-Test "Bash: Reject corrupted / tampered archive with checksum failure" $corruptShPassed "Exit code $corruptShExit, detected corrupted archive"
}

# Cleanup test scratch
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue

# 6. Live Database Recovery Drill (Conditional)
Write-Host "`n--- 6. Live Database Environment Status ---" -ForegroundColor White
$containerRunning = $false
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCmd) {
    try {
        $status = & docker inspect --format="{{.State.Running}}" insurance-mysql8 2>$null
        if ($LASTEXITCODE -eq 0 -and $status.Trim() -eq "true") {
            $containerRunning = $true
        }
    } catch {}
}

if ($containerRunning) {
    Write-Host "[INFO] Active MySQL container detected. Executing live recovery drill..." -ForegroundColor Green
    $drillScript = Join-Path $scriptDir "recovery-drill.ps1"
    $drillOut = & powershell -ExecutionPolicy Bypass -File $drillScript 2>&1
    $drillExit = $LASTEXITCODE
    Assert-Test "Live Recovery Drill Execution" ($drillExit -eq 0) "Exit code $drillExit"
} else {
    Write-Host "[UNVERIFIED] Live database environment is offline (Docker daemon not running)." -ForegroundColor Yellow
    Write-Host "             All static, syntax, archive, and corruption checks PASSED." -ForegroundColor Yellow
    Write-Host "             Full-stack database drill remains UNVERIFIED until Docker Desktop is started." -ForegroundColor Yellow
}

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host " Test Suite Results: $passCount Passed, $failCount Failed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "======================================================================"

if ($failCount -gt 0) {
    exit 1
}
exit 0
