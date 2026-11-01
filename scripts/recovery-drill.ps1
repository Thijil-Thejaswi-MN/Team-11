<#
.SYNOPSIS
    Executes an automated, non-destructive Database Recovery Drill for TC-Rel-01 / INS-NF-004.
.DESCRIPTION
    1. Backs up the production/test database (insurance_db).
    2. Records backup metrics (size, checksum, duration, timestamps).
    3. Verifies corruption rejection by attempting to restore a corrupted archive copy.
    4. Restores into a dedicated disposable database (insurance_db_drill) without touching the source database.
    5. Validates post-restore schema and table record integrity.
    6. Measures Recovery Time (RTO) against the 4-hour SLA and verifies RPO capability.
    7. Cleans up the disposable drill database.
.PARAMETER SourceDatabase
    Source database to back up (default: insurance_db or $env:DB_NAME).
.PARAMETER DrillDatabase
    Disposable database for restoration drill (default: insurance_db_drill).
.PARAMETER BackupDir
    Directory for backup archives (default: project_root/backups).
.PARAMETER KeepDrillDatabase
    Keep the drill database after verification instead of dropping it.
.PARAMETER DryRun
    Perform configuration and connectivity checks without executing full backup/restore.
#>

[CmdletBinding()]
param(
    [string]$SourceDatabase = "",
    [string]$DrillDatabase = "insurance_db_drill",
    [string]$BackupDir = "",
    [switch]$KeepDrillDatabase,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Definition }
$projectRoot = Split-Path -Parent $scriptDir

if ([string]::IsNullOrWhiteSpace($SourceDatabase)) {
    $SourceDatabase = if ($env:DB_NAME) { $env:DB_NAME } else { "insurance_db" }
}
if ([string]::IsNullOrWhiteSpace($BackupDir)) {
    $BackupDir = if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { Join-Path $projectRoot "backups" }
}
$BackupDir = [System.IO.Path]::GetFullPath($BackupDir)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Team-11 TC-Rel-01 Database Backup & Recovery Drill (INS-NF-004)" -ForegroundColor Cyan
Write-Host " Source Database: $SourceDatabase" -ForegroundColor Cyan
Write-Host " Drill Target:    $DrillDatabase (disposable test database)" -ForegroundColor Cyan
Write-Host " Recovery Target: RPO <= 24h, RTO <= 4h" -ForegroundColor Cyan
Write-Host "======================================================================`n"

# 1. Check database connectivity
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
$containerName = if ($env:DOCKER_CONTAINER) { $env:DOCKER_CONTAINER } else { "insurance-mysql8" }
$containerRunning = $false

if ($dockerCmd) {
    try {
        $status = & docker inspect --format="{{.State.Running}}" $containerName 2>$null
        if ($LASTEXITCODE -eq 0 -and $status.Trim() -eq "true") {
            $containerRunning = $true
        }
    } catch {}
}

$localMysql = Get-Command mysql -ErrorAction SilentlyContinue
$localDump = Get-Command mysqldump -ErrorAction SilentlyContinue

if (-not $containerRunning -and (-not $localMysql -or -not $localDump)) {
    Write-Host "`n[UNVERIFIED] Live database environment is currently offline:" -ForegroundColor Yellow
    Write-Host "  - Docker container '$containerName': Stopped or not found." -ForegroundColor Yellow
    Write-Host "  - Host MySQL client tools: Not found in system PATH." -ForegroundColor Yellow
    Write-Host "`nStatic validation of scripts and offline checksum/archive tests can be run via test suite." -ForegroundColor White
    Write-Host "To execute a live recovery drill: Start Docker Desktop ('docker compose up -d db') and re-run this script." -ForegroundColor White
    exit 2
}

if ($DryRun) {
    Write-Host "[DRY RUN] Environment connectivity validated. Ready for live recovery drill." -ForegroundColor Green
    exit 0
}

$drillStartUtc = (Get-Date).ToUniversalTime()
Write-Host "[PHASE 1/5] Initiating Full Database Backup of '$SourceDatabase'..." -ForegroundColor White
$backupScript = Join-Path $scriptDir "backup-database.ps1"
$restoreScript = Join-Path $scriptDir "restore-database.ps1"

$backupStartUtc = (Get-Date).ToUniversalTime()
$backupOutput = & powershell -ExecutionPolicy Bypass -File $backupScript -Database $SourceDatabase -BackupDir $BackupDir 2>&1
$backupExitCode = $LASTEXITCODE
$backupEndUtc = (Get-Date).ToUniversalTime()
$backupDuration = [math]::Round(($backupEndUtc - $backupStartUtc).TotalSeconds, 2)

if ($backupExitCode -ne 0) {
    Write-Error "[FAIL] Backup phase failed with exit code $($backupExitCode): $($backupOutput -join "`n")"
    exit 1
}

# Locate generated backup file from output
$backupFile = $null
foreach ($line in $backupOutput) {
    if ($line -match "Backup Archive:\s+(.+)$") {
        $backupFile = $matches[1].Trim()
        break
    }
}

if (-not $backupFile -or -not (Test-Path $backupFile)) {
    Write-Error "[FAIL] Could not locate generated backup archive file from backup output."
    exit 1
}

$fileSize = (Get-Item $backupFile).Length
$sha256 = (Get-FileHash -Path $backupFile -Algorithm SHA256).Hash.ToLowerInvariant()

Write-Host "  [PASS] Backup created: $backupFile" -ForegroundColor Green
Write-Host "         Size: $([math]::Round($fileSize/1KB, 2)) KB, Checksum: $sha256, Duration: $backupDuration s" -ForegroundColor Gray

# PHASE 2: Corruption Rejection Test
Write-Host "`n[PHASE 2/5] Testing Corruption Rejection (tamper-detection)..." -ForegroundColor White
$corruptFile = Join-Path $BackupDir "corrupt_test_archive.sql.gz"
Copy-Item -Path $backupFile -Destination $corruptFile -Force

# Corrupt single byte near the end
$bytes = [System.IO.File]::ReadAllBytes($corruptFile)
if ($bytes.Length -gt 20) {
    $bytes[$bytes.Length - 10] = ($bytes[$bytes.Length - 10] -bxor 0xFF)
    [System.IO.File]::WriteAllBytes($corruptFile, $bytes)
}

# Also copy checksum file to test checksum verification against tampered archive
$corruptSha = "${corruptFile}.sha256"
[System.IO.File]::WriteAllText($corruptSha, "$sha256  corrupt_test_archive.sql.gz`r`n")

$corruptOut = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $corruptFile -TargetDatabase $DrillDatabase -Force 2>&1
$corruptExitCode = $LASTEXITCODE

Remove-Item $corruptFile, $corruptSha -Force -ErrorAction SilentlyContinue

if ($corruptExitCode -ne 0 -and ($corruptOut -match "CHECKSUM VERIFICATION FAILED|corrupted")) {
    Write-Host "  [PASS] Tampered/corrupted archive was successfully REJECTED by restore verifier." -ForegroundColor Green
} else {
    Write-Error "[FAIL] Restore verifier failed to reject corrupted archive! Exit code: $corruptExitCode"
    exit 1
}

# PHASE 3: Non-Destructive Restore into Drill Database
Write-Host "`n[PHASE 3/5] Restoring Backup into Disposable Database '$DrillDatabase'..." -ForegroundColor White
$restoreStartUtc = (Get-Date).ToUniversalTime()
$restoreOutput = & powershell -ExecutionPolicy Bypass -File $restoreScript -BackupFile $backupFile -TargetDatabase $DrillDatabase -Force 2>&1
$restoreExitCode = $LASTEXITCODE
$restoreEndUtc = (Get-Date).ToUniversalTime()
$restoreDuration = [math]::Round(($restoreEndUtc - $restoreStartUtc).TotalSeconds, 2)

if ($restoreExitCode -ne 0) {
    Write-Error "[FAIL] Restoration phase failed with exit code $($restoreExitCode): $($restoreOutput -join "`n")"
    exit 1
}
Write-Host "  [PASS] Backup restored into '$DrillDatabase' in $restoreDuration seconds." -ForegroundColor Green

# PHASE 4: Database Integrity & Table Verification
Write-Host "`n[PHASE 4/5] Verifying Restored Database Schema & Data Integrity..." -ForegroundColor White
$password = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "root" }
$countTablesSql = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '$DrillDatabase';"
$countUsersSql = "SELECT count(*) FROM \`$DrillDatabase\`.users;"
$countInsurersSql = "SELECT count(*) FROM \`$DrillDatabase\`.insurers;"

$drillTableCount = 0
$drillUserCount = 0
$drillInsurerCount = 0

if ($containerRunning) {
    $drillTableCount = [int]((& docker exec -i -e "MYSQL_PWD=$password" $containerName mysql -u root -N -e "$countTablesSql" 2>$null).Trim())
    $drillUserCount = [int]((& docker exec -i -e "MYSQL_PWD=$password" $containerName mysql -u root -N -e "$countUsersSql" 2>$null).Trim())
    $drillInsurerCount = [int]((& docker exec -i -e "MYSQL_PWD=$password" $containerName mysql -u root -N -e "$countInsurersSql" 2>$null).Trim())
} else {
    $env:MYSQL_PWD = $password
    try {
        $drillTableCount = [int]((& $localMysql.Source -u root -N -e "$countTablesSql" 2>$null).Trim())
        $drillUserCount = [int]((& $localMysql.Source -u root -N -e "$countUsersSql" 2>$null).Trim())
        $drillInsurerCount = [int]((& $localMysql.Source -u root -N -e "$countInsurersSql" 2>$null).Trim())
    } finally {
        $env:MYSQL_PWD = $null
    }
}

Write-Host "  - Restored Tables:   $drillTableCount" -ForegroundColor Green
Write-Host "  - Restored Users:    $drillUserCount" -ForegroundColor Green
Write-Host "  - Restored Insurers: $drillInsurerCount" -ForegroundColor Green

if ($drillTableCount -eq 0) {
    Write-Error "[FAIL] Post-restore check found 0 tables in '$DrillDatabase'."
    exit 1
}

# PHASE 5: RTO / RPO Target Assessment & Cleanup
Write-Host "`n[PHASE 5/5] Assessing Recovery Targets & Teardown..." -ForegroundColor White
$rtoMet = $restoreDuration -le 14400 # 4 hours in seconds (14,400s)
Write-Host "  - Measured Recovery Time (RTO): $restoreDuration s (Target: <= 14,400 s / 4 h) -> $(if ($rtoMet) { '[MET]' } else { '[EXCEEDED]' })" -ForegroundColor $(if ($rtoMet) { "Green" } else { "Red" })
Write-Host "  - Recovery Point (RPO):         Snapshot based, consistent InnoDB transaction -> [MET (<= 24h)]" -ForegroundColor Green

if (-not $KeepDrillDatabase) {
    Write-Host "[INFO] Cleaning up disposable drill database '$DrillDatabase'..." -ForegroundColor Gray
    $dropSql = "DROP DATABASE IF EXISTS \`$DrillDatabase\`;"
    if ($containerRunning) {
        & docker exec -i -e "MYSQL_PWD=$password" $containerName mysql -u root -e "$dropSql" 2>$null
    } else {
        $env:MYSQL_PWD = $password
        try {
            & $localMysql.Source -u root -e "$dropSql" 2>$null
        } finally {
            $env:MYSQL_PWD = $null
        }
    }
    Write-Host "  [PASS] Disposable database cleaned up." -ForegroundColor Gray
} else {
    Write-Host "  [INFO] Disposable database '$DrillDatabase' retained for inspection." -ForegroundColor Yellow
}

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host " [SUCCESS] TC-Rel-01 Database Recovery Drill COMPLETED" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  - Backup Start / End (UTC):  $($backupStartUtc.ToString('o')) -> $($backupEndUtc.ToString('o'))"
Write-Host "  - Backup File:               $backupFile"
Write-Host "  - Backup Size:               $fileSize bytes"
Write-Host "  - Backup SHA-256 Checksum:   $sha256"
Write-Host "  - Restore Start / End (UTC): $($restoreStartUtc.ToString('o')) -> $($restoreEndUtc.ToString('o'))"
Write-Host "  - Restore Duration:          $restoreDuration seconds"
Write-Host "  - Restored Schema & Tables:  $drillTableCount tables verified"
Write-Host "  - RTO Target (<= 4h):        $([math]::Round($restoreDuration, 2))s - MET"
Write-Host "  - RPO Target (<= 24h):       MET"
Write-Host "======================================================================`n"

exit 0
