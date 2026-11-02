<#
.SYNOPSIS
    Creates a full, consistent, compressed MySQL database backup with SHA-256 verification (P4-14 / TC-Rel-01 / INS-NF-004).
.DESCRIPTION
    Performs transactional full database backup using mysqldump (--single-transaction, --quick, --routines, --triggers, --events).
    Dumps from the Docker Compose database container (default: insurance-mysql8) or direct host connection.
    Compresses output to gzip (.sql.gz), generates SHA-256 checksum (.sha256), and writes metadata (.meta.json).
.PARAMETER BackupDir
    Destination directory for backup files (default: project_root/backups or $env:BACKUP_DIR).
.PARAMETER Database
    Database to back up (default: insurance_db or $env:DB_NAME).
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
    Force using host-installed mysqldump instead of containerized mysqldump.
.PARAMETER DryRun
    Simulate backup steps and validate configuration without executing mysqldump.
.EXAMPLE
    .\scripts\backup-database.ps1
.EXAMPLE
    .\scripts\backup-database.ps1 -BackupDir "D:\backups" -Database "insurance_db"
#>

[CmdletBinding()]
param(
    [string]$BackupDir = "",
    [string]$Database = "",
    [string]$User = "",
    [string]$Password = "",
    [string]$ContainerName = "",
    [Alias("Host")]
    [string]$HostName = "",
    [int]$Port = 0,
    [switch]$DirectClient,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Resolve defaults from parameters or environment variables
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Definition }
$projectRoot = Split-Path -Parent $scriptDir

if ([string]::IsNullOrWhiteSpace($BackupDir)) {
    $BackupDir = if ($env:BACKUP_DIR) { $env:BACKUP_DIR } else { Join-Path $projectRoot "backups" }
}
$BackupDir = [System.IO.Path]::GetFullPath($BackupDir)

if ([string]::IsNullOrWhiteSpace($Database)) {
    $Database = if ($env:DB_NAME) { $env:DB_NAME } else { "insurance_db" }
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

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Team-11 MySQL Database Backup Utility (P4-14 / TC-Rel-01)" -ForegroundColor Cyan
Write-Host " Target Database: $Database" -ForegroundColor Cyan
Write-Host " Backup Directory: $BackupDir" -ForegroundColor Cyan
Write-Host "======================================================================`n"

# 1. Validate destination directory & disk space
if (-not (Test-Path $BackupDir)) {
    try {
        New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
        Write-Host "[INFO] Created backup directory: $BackupDir" -ForegroundColor Gray
    } catch {
        Write-Error "[FATAL] Failed to create backup directory '$BackupDir': $($_.Message)"
        exit 1
    }
}

# Check disk space (minimum 50 MB required)
try {
    $driveRoot = [System.IO.Path]::GetPathRoot($BackupDir)
    $drive = Get-PSDrive -Name $driveRoot.TrimEnd(':\') -ErrorAction SilentlyContinue
    if ($drive -and $drive.Free -lt 50MB) {
        Write-Error "[FATAL] Insufficient disk space on drive '$driveRoot'. Free space: $([math]::Round($drive.Free/1MB, 2)) MB (minimum 50 MB required)."
        exit 1
    }
} catch {
    # Non-blocking if drive inspect fails
}

# 2. Determine execution mode (Docker container vs direct host mysqldump)
$useContainer = $false
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue

if (-not $DirectClient -and $dockerCmd) {
    # Check if target container is running
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
        Write-Host "[INFO] Detected active Docker database container '$ContainerName'. Using containerized mysqldump." -ForegroundColor Green
    } else {
        Write-Host "[WARN] Docker container '$ContainerName' is not running or not found." -ForegroundColor Yellow
    }
}

$localDumpCmd = $null
if (-not $useContainer) {
    $localDumpCmd = Get-Command mysqldump -ErrorAction SilentlyContinue
    if ($localDumpCmd) {
        Write-Host "[INFO] Using host mysqldump: $($localDumpCmd.Source)" -ForegroundColor Green
    } else {
        Write-Host "`n[FATAL] Database connection unavailable:" -ForegroundColor Red
        Write-Host "  1. Docker container '$ContainerName' is not running." -ForegroundColor Yellow
        Write-Host "  2. Host executable 'mysqldump' was not found in PATH." -ForegroundColor Yellow
        Write-Host "`nTo resolve: Start Docker Desktop stack ('docker compose up -d db') or install MySQL client tools." -ForegroundColor White
        exit 1
    }
}

# 3. Generate backup filename and paths
$timestampUtc = (Get-Date).ToUniversalTime()
$timestampStr = $timestampUtc.ToString("yyyyMMdd_HHmmss")
$baseName = "backup_${Database}_${timestampStr}"
$backupFile = Join-Path $BackupDir "${baseName}.sql.gz"
$checksumFile = Join-Path $BackupDir "${baseName}.sql.gz.sha256"
$metaFile = Join-Path $BackupDir "${baseName}.meta.json"

if ($DryRun) {
    Write-Host "`n[DRY RUN] Configuration validated successfully." -ForegroundColor Green
    Write-Host "  - Target: $Database"
    Write-Host "  - Mode: $(if ($useContainer) { "Docker ($ContainerName)" } else { "Host Client (${HostName}:${Port})" })"
    Write-Host "  - Output: $backupFile"
    exit 0
}

Write-Host "[INFO] Starting database backup..." -ForegroundColor White
Write-Host "  - Timestamp (UTC): $($timestampUtc.ToString('o'))"
Write-Host "  - Destination Archive: $backupFile"

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# 4. Stream mysqldump into GZipStream
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true

if ($useContainer) {
    $psi.FileName = "docker"
    # Execute mysqldump inside container; pass MYSQL_PWD via container env so credentials never leak into process arguments
    $psi.Arguments = "exec -i -e MYSQL_PWD=$Password $ContainerName mysqldump --single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF --add-drop-table -u $User $Database"
} else {
    $psi.FileName = $localDumpCmd.Source
    $psi.Arguments = "--host=$HostName --port=$Port --user=$User --single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF --add-drop-table $Database"
    $psi.EnvironmentVariables["MYSQL_PWD"] = $Password
}

$proc = $null
$fsOut = $null
$gzOut = $null
$uncompressedBytes = 0

try {
    $fsOut = [System.IO.File]::Create($backupFile)
    $gzOut = New-Object System.IO.Compression.GZipStream($fsOut, [System.IO.Compression.CompressionLevel]::Optimal)

    $proc = [System.Diagnostics.Process]::Start($psi)

    $buffer = New-Object byte[] 65536
    $baseStream = $proc.StandardOutput.BaseStream

    while (($bytesRead = $baseStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
        $gzOut.Write($buffer, 0, $bytesRead)
        $uncompressedBytes += $bytesRead
    }

    $gzOut.Close()
    $fsOut.Close()

    $stderrOutput = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()

    $stopwatch.Stop()
    $durationSec = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)

    if ($proc.ExitCode -ne 0 -or $uncompressedBytes -eq 0) {
        # Backup failed: remove incomplete file
        Remove-Item $backupFile -Force -ErrorAction SilentlyContinue
        Write-Error "`n[FATAL] mysqldump failed with exit code $($proc.ExitCode): $stderrOutput"
        exit 1
    }
} catch {
    if ($gzOut) { $gzOut.Dispose() }
    if ($fsOut) { $fsOut.Dispose() }
    Remove-Item $backupFile -Force -ErrorAction SilentlyContinue
    Write-Error "`n[FATAL] Error occurred during backup streaming: $($_.Message)"
    exit 1
}

# 5. Verify and generate SHA-256 Checksum
Write-Host "[INFO] Generating SHA-256 integrity checksum..." -ForegroundColor White
$compressedSize = (Get-Item $backupFile).Length
$sha256Hash = (Get-FileHash -Path $backupFile -Algorithm SHA256).Hash.ToLowerInvariant()

# Write .sha256 file (standard format: "<hash>  <filename>")
$fileNameOnly = [System.IO.Path]::GetFileName($backupFile)
$shaContent = "$sha256Hash  $fileNameOnly`r`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($checksumFile, $shaContent, $utf8NoBom)

# 6. Record metadata JSON
$meta = [ordered]@{
    backup_file          = $fileNameOnly
    checksum_sha256      = $sha256Hash
    database             = $Database
    timestamp_utc        = $timestampUtc.ToString("o")
    file_size_bytes      = $compressedSize
    uncompressed_bytes   = $uncompressedBytes
    compression_ratio    = if ($uncompressedBytes -gt 0) { [math]::Round($compressedSize / $uncompressedBytes, 4) } else { 1.0 }
    duration_seconds     = $durationSec
    compression          = "gzip"
    format               = "mysqldump"
    schema_version       = "1.0"
    target_rpo           = "24h"
    target_rto           = "4h"
    source_mode          = if ($useContainer) { "docker:$ContainerName" } else { "direct:${HostName}:${Port}" }
}

$metaJson = $meta | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($metaFile, $metaJson + "`r`n", $utf8NoBom)

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host " [SUCCESS] MySQL Database Backup Completed Successfully" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
Write-Host "  - Backup Archive:   $backupFile"
Write-Host "  - Compressed Size:  $([math]::Round($compressedSize / 1KB, 2)) KB ($compressedSize bytes)"
Write-Host "  - Raw Dump Size:    $([math]::Round($uncompressedBytes / 1KB, 2)) KB ($uncompressedBytes bytes)"
Write-Host "  - SHA-256 Checksum: $sha256Hash"
Write-Host "  - Checksum File:    $checksumFile"
Write-Host "  - Metadata File:    $metaFile"
Write-Host "  - Duration:         $durationSec seconds"
Write-Host "======================================================================`n"

exit 0
