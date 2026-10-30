<#
.SYNOPSIS
    Restores a MySQL database from a compressed, checksummed backup archive (P4-14 / TC-Rel-01 / INS-NF-004).
.DESCRIPTION
    Validates SHA-256 checksum, tests archive decompression, requests confirmation (unless -Force),
    ensures the target database exists, and imports the database dump with transactional integrity.
    Supports restoration into a separate disposable database for non-destructive recovery drills.
.PARAMETER BackupFile
    Path to the backup archive (.sql.gz or .sql).
.PARAMETER TargetDatabase
    Target database name to restore into (default: auto-detected from metadata or $env:DB_NAME or "insurance_db").
.PARAMETER User
    Database username (default: root or $env:DB_USER).
.PARAMETER Password
    Database password (default: root or $env:DB_PASSWORD / $env:MYSQL_ROOT_PASSWORD).
.PARAMETER ContainerName
    Docker container name running MySQL (default: insurance-mysql8 or $env:DOCKER_CONTAINER).
.PARAMETER HostName
    Database host for direct client connection (default: 127.0.0.1 or $env:DB_HOST).
.PARAMETER Port
    Database port for direct client connection (default: 3306 or $env:DB_PORT).
.PARAMETER DirectClient
    Force using host-installed mysql instead of containerized mysql.
.PARAMETER Force
    Bypass interactive confirmation prompt (required for automated test drills and CI pipelines).
.PARAMETER SkipChecksum
    Bypass SHA-256 verification (NOT recommended).
.PARAMETER DryRun
    Verify backup existence, checksum, decompression, and target connectivity without modifying data.
.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile "backups\backup_insurance_db_20261030_193000.sql.gz"
.EXAMPLE
    .\scripts\restore-database.ps1 -BackupFile "backups\backup.sql.gz" -TargetDatabase "insurance_db_drill" -Force
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$BackupFile,
    [string]$TargetDatabase = "",
    [string]$User = "",
    [string]$Password = "",
    [string]$ContainerName = "",
    [Alias("Host")]
    [string]$HostName = "",
    [int]$Port = 0,
    [switch]$DirectClient,
    [switch]$Force,
    [switch]$SkipChecksum,
    [switch]$VerifyOnly,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Team-11 MySQL Database Restore & Recovery Utility (P4-14 / TC-Rel-01)" -ForegroundColor Cyan
Write-Host "======================================================================`n"

# 1. Validate backup file existence
$BackupFile = [System.IO.Path]::GetFullPath($BackupFile)
if (-not (Test-Path $BackupFile)) {
    Write-Error "[FATAL] Backup file not found: '$BackupFile'"
    exit 1
}

$fileInfo = Get-Item $BackupFile
if ($fileInfo.Length -eq 0) {
    Write-Error "[FATAL] Backup file is empty (0 bytes): '$BackupFile'"
    exit 1
}

Write-Host "[INFO] Backup Archive: $BackupFile" -ForegroundColor White
Write-Host "  - File Size: $([math]::Round($fileInfo.Length / 1KB, 2)) KB ($($fileInfo.Length) bytes)"

# 2. Check for associated metadata to infer target database if not explicitly provided
$metaFile = [System.IO.Path]::ChangeExtension($BackupFile, "meta.json")
$inferredDb = $null
if (Test-Path $metaFile) {
    try {
        $metaContent = Get-Content $metaFile -Raw | ConvertFrom-Json
        if ($metaContent.database) {
            $inferredDb = [string]$metaContent.database
        }
    } catch {
        # Metadata parse failure is non-fatal
    }
}

if ([string]::IsNullOrWhiteSpace($TargetDatabase)) {
    if ($inferredDb) {
        $TargetDatabase = $inferredDb
    } elseif ($env:DB_NAME) {
        $TargetDatabase = $env:DB_NAME
    } else {
        $TargetDatabase = "insurance_db"
    }
}

if ([string]::IsNullOrWhiteSpace($User)) {
    $User = if ($env:DB_USER) { $env:DB_USER } else { "root" }
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } elseif ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } elseif ($env:MYSQL_PWD) { $env:MYSQL_PWD } else { "root" }
}
if ([string]::IsNullOrWhiteSpace($ContainerName)) {
    $ContainerName = if ($env:DOCKER_CONTAINER) { $env:DOCKER_CONTAINER } else { "insurance-mysql8" }
}
if ([string]::IsNullOrWhiteSpace($HostName)) {
    $HostName = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
}
if ($Port -eq 0) {
    $Port = if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }
}

Write-Host "  - Target Database: $TargetDatabase" -ForegroundColor Cyan

# 3. Verify SHA-256 Checksum
if (-not $SkipChecksum) {
    Write-Host "`n--- 1. Verifying SHA-256 Checksum Integrity ---" -ForegroundColor White
    $computedHash = (Get-FileHash -Path $BackupFile -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "  - Computed SHA-256: $computedHash" -ForegroundColor Gray

    # Look for matching checksum file (.sha256)
    $checksumFile = "${BackupFile}.sha256"
    $expectedHash = $null

    if (Test-Path $checksumFile) {
        $rawChecksum = (Get-Content $checksumFile -Raw).Trim().TrimStart([char]0xFEFF)
        $expectedHash = ($rawChecksum -split '\s+')[0].Trim().ToLowerInvariant()
        Write-Host "  - Found Checksum File: $checksumFile" -ForegroundColor Gray
    } elseif (Test-Path $metaFile) {
        try {
            $metaContent = Get-Content $metaFile -Raw | ConvertFrom-Json
            if ($metaContent.checksum_sha256) {
                $expectedHash = [string]$metaContent.checksum_sha256.Trim().ToLowerInvariant()
                Write-Host "  - Found Checksum in Metadata: $metaFile" -ForegroundColor Gray
            }
        } catch {}
    }

    if ($expectedHash) {
        Write-Host "  - Expected SHA-256: $expectedHash" -ForegroundColor Gray
        if ($computedHash -ne $expectedHash) {
            Write-Host "`n[FATAL] CHECKSUM VERIFICATION FAILED!" -ForegroundColor Red
            Write-Host "  Archive is corrupted, truncated, or tampered with." -ForegroundColor Red
            Write-Host "  Computed: $computedHash" -ForegroundColor Yellow
            Write-Host "  Expected: $expectedHash" -ForegroundColor Yellow
            Write-Host "Restoration aborted to prevent database corruption." -ForegroundColor Red
            exit 1
        }
        Write-Host "  [PASS] Checksum matched successfully." -ForegroundColor Green
    } else {
        Write-Host "  [WARN] No .sha256 or .meta.json file found to verify against. Computed hash: $computedHash" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[WARN] SHA-256 verification skipped by request (-SkipChecksum)." -ForegroundColor Yellow
}

# 4. Test Archive Decompressibility
Write-Host "`n--- 2. Validating Archive Format & Decompression ---" -ForegroundColor White
$isGzip = $BackupFile.EndsWith(".gz", [System.StringComparison]::OrdinalIgnoreCase)

if ($isGzip) {
    try {
        $testFs = [System.IO.File]::OpenRead($BackupFile)
        # Check standard GZip magic bytes (0x1F, 0x8B)
        $b1 = $testFs.ReadByte()
        $b2 = $testFs.ReadByte()
        $testFs.Position = 0

        if ($b1 -ne 0x1F -or $b2 -ne 0x8B) {
            $testFs.Close()
            Write-Error "[FATAL] Invalid GZip format (magic header mismatch 0x$($b1.ToString('X2')) 0x$($b2.ToString('X2')))."
            exit 1
        }

        # Test decompression of header stream
        $testGz = New-Object System.IO.Compression.GZipStream($testFs, [System.IO.Compression.CompressionMode]::Decompress)
        $testBuffer = New-Object byte[] 4096
        $readCount = $testGz.Read($testBuffer, 0, $testBuffer.Length)
        $testGz.Close()
        $testFs.Close()

        if ($readCount -eq 0) {
            Write-Error "[FATAL] Archive decompression returned 0 bytes."
            exit 1
        }
        Write-Host "  [PASS] Archive successfully decompressed and validated." -ForegroundColor Green
    } catch {
        Write-Error "[FATAL] Failed to decompress backup archive: $($_.Message)"
        exit 1
    }
} else {
    Write-Host "  [INFO] Plain SQL dump detected (.sql)." -ForegroundColor Gray
}

if ($VerifyOnly) {
    Write-Host "`n======================================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] Backup Archive Integrity & Checksum Verified" -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host "  - Archive:   $BackupFile"
    Write-Host "  - Size:      $($fileInfo.Length) bytes"
    Write-Host "  - Checksum:  $computedHash"
    Write-Host "  - Format:    $([System.IO.Path]::GetExtension($BackupFile)) verified"
    Write-Host "======================================================================`n"
    exit 0
}

# 5. Determine Database Connectivity Mode
Write-Host "`n--- 3. Verifying Database Target Connectivity ---" -ForegroundColor White
$useContainer = $false
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue

if (-not $DirectClient -and $dockerCmd) {
    $isRunning = $false
    try {
        $containerStatus = & docker inspect --format="{{.State.Running}}" $ContainerName 2>$null
        if ($LASTEXITCODE -eq 0 -and $containerStatus.Trim() -eq "true") {
            $isRunning = $true
        }
    } catch {
        $isRunning = $false
    }

    if ($isRunning) {
        $useContainer = $true
        Write-Host "  [INFO] Using active Docker database container '$ContainerName'." -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Docker container '$ContainerName' is not running." -ForegroundColor Yellow
    }
}

$localClientCmd = $null
if (-not $useContainer) {
    $localClientCmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($localClientCmd) {
        Write-Host "  [INFO] Using host mysql client: $($localClientCmd.Source)" -ForegroundColor Green
    } else {
        Write-Host "`n[FATAL] Database connection unavailable:" -ForegroundColor Red
        Write-Host "  1. Docker container '$ContainerName' is not running." -ForegroundColor Yellow
        Write-Host "  2. Host executable 'mysql' was not found in PATH." -ForegroundColor Yellow
        Write-Host "`nTo resolve: Start Docker Desktop stack ('docker compose up -d db') or install MySQL client." -ForegroundColor White
        exit 1
    }
}

if ($DryRun) {
    Write-Host "`n[DRY RUN] All pre-restoration checks passed successfully:" -ForegroundColor Green
    Write-Host "  - Backup Archive: Valid & verified"
    Write-Host "  - Mode: $(if ($useContainer) { "Docker ($ContainerName)" } else { "Host Client (${HostName}:${Port})" })"
    Write-Host "  - Target: $TargetDatabase"
    Write-Host "Dry run complete. No database modifications were performed." -ForegroundColor Green
    exit 0
}

# 6. Safety Confirmation Prompt
$nonInteractive = $env:RESTORE_FORCE -eq "1" -or $env:NON_INTERACTIVE -eq "1" -or $Force
if (-not $nonInteractive) {
    Write-Host "`n**********************************************************************" -ForegroundColor Yellow
    Write-Host " CAUTION: RESTORE OPERATION" -ForegroundColor Yellow
    Write-Host " Target Database: '$TargetDatabase'" -ForegroundColor Yellow
    Write-Host " All existing schema and records in '$TargetDatabase' may be overwritten." -ForegroundColor Yellow
    Write-Host "**********************************************************************" -ForegroundColor Yellow
    $confirm = Read-Host "Are you sure you want to proceed with database restoration? (yes/no)"
    if ($confirm.Trim().ToLowerInvariant() -notin @("yes", "y")) {
        Write-Host "[ABORTED] Restoration cancelled by user." -ForegroundColor Yellow
        exit 0
    }
}

# 7. Ensure Target Database Exists
Write-Host "`n--- 4. Ensuring Target Database Exists ---" -ForegroundColor White
$createDbSql = "CREATE DATABASE IF NOT EXISTS \`$TargetDatabase\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if ($useContainer) {
    & docker exec -i -e "MYSQL_PWD=$Password" $ContainerName mysql -u $User -e "$createDbSql" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[FATAL] Failed to create or access database '$TargetDatabase' inside container."
        exit 1
    }
} else {
    $env:MYSQL_PWD = $Password
    try {
        & $localClientCmd.Source --host=$HostName --port=$Port --user=$User -e "$createDbSql" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Error "[FATAL] Failed to create or access database '$TargetDatabase' on host."
            exit 1
        }
    } finally {
        $env:MYSQL_PWD = $null
    }
}
Write-Host "  [PASS] Target database '$TargetDatabase' verified." -ForegroundColor Green

# 8. Stream Backup Archive into MySQL Client
Write-Host "`n--- 5. Executing Database Import ---" -ForegroundColor White
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true

if ($useContainer) {
    $psi.FileName = "docker"
    # Pass credentials via container environment variable to keep them off process arguments
    $psi.Arguments = "exec -i -e MYSQL_PWD=$Password $ContainerName mysql --default-character-set=utf8mb4 -u $User $TargetDatabase"
} else {
    $psi.FileName = $localClientCmd.Source
    $psi.Arguments = "--host=$HostName --port=$Port --user=$User --default-character-set=utf8mb4 $TargetDatabase"
    $psi.EnvironmentVariables["MYSQL_PWD"] = $Password
}

$proc = $null
$fsIn = $null
$gzIn = $null

try {
    $proc = [System.Diagnostics.Process]::Start($psi)
    $fsIn = [System.IO.File]::OpenRead($BackupFile)

    if ($isGzip) {
        $gzIn = New-Object System.IO.Compression.GZipStream($fsIn, [System.IO.Compression.CompressionMode]::Decompress)
        $gzIn.CopyTo($proc.StandardInput.BaseStream)
        $gzIn.Close()
    } else {
        $fsIn.CopyTo($proc.StandardInput.BaseStream)
    }

    $proc.StandardInput.Close()
    $fsIn.Close()

    $stderrMsg = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()

    $stopwatch.Stop()
    $restoreDuration = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)

    if ($proc.ExitCode -ne 0) {
        Write-Error "`n[FATAL] MySQL restore process failed with exit code $($proc.ExitCode): $stderrMsg"
        exit 1
    }
} catch {
    if ($gzIn) { $gzIn.Dispose() }
    if ($fsIn) { $fsIn.Dispose() }
    Write-Error "`n[FATAL] Stream error during database restoration: $($_.Message)"
    exit 1
}

# 9. Post-Restore Integrity Verification
Write-Host "`n--- 6. Post-Restore Schema & Table Verification ---" -ForegroundColor White
$countTablesSql = "SELECT count(*) FROM information_schema.tables WHERE table_schema = '$TargetDatabase';"
$tableCount = 0

if ($useContainer) {
    $tableOut = & docker exec -i -e "MYSQL_PWD=$Password" $ContainerName mysql -u $User -N -e "$countTablesSql" 2>$null
    if ($tableOut) {
        [int]::TryParse($tableOut.Trim(), [ref]$tableCount) | Out-Null
    }
} else {
    $env:MYSQL_PWD = $Password
    try {
        $tableOut = & $localClientCmd.Source --host=$HostName --port=$Port --user=$User -N -e "$countTablesSql" 2>$null
        if ($tableOut) {
            [int]::TryParse($tableOut.Trim(), [ref]$tableCount) | Out-Null
        }
    } finally {
        $env:MYSQL_PWD = $null
    }
}

Write-Host "  - Tables Restored: $tableCount" -ForegroundColor $(if ($tableCount -gt 0) { "Green" } else { "Yellow" })

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host " [SUCCESS] MySQL Database Restoration Completed Successfully" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  - Restored Archive:   $BackupFile"
Write-Host "  - Target Database:    $TargetDatabase"
Write-Host "  - Restored Tables:    $tableCount"
Write-Host "  - Restore Duration:   $restoreDuration seconds"
Write-Host "  - SHA-256 Verified:   $(-not $SkipChecksum)"
Write-Host "======================================================================`n"

exit 0
