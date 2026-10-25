<#
.SYNOPSIS
    Resets the test database to a clean, canonical state and seeds test volume data.
.DESCRIPTION
    One-command database reset for Person 3 performance, security, and integration testing.
    Can reset Docker volume or apply generated volume SQL.
#>

param(
    [ValidateSet("canonical", "small", "standard", "full")]
    [string]$Scale = "standard",
    [switch]$RecreateContainers
)

$ErrorActionPreference = "Stop"

Write-Host "=== Team-11 Test Database Reset & Volume Seeding Utility ===" -ForegroundColor Cyan
Write-Host "Target Scale: $Scale"

$ProjectRoot = Split-Path -Parent $PSScriptRoot

if ($RecreateContainers) {
    Write-Host "[INFO] Recreating database container and volume..." -ForegroundColor Yellow
    docker compose -f (Join-Path $ProjectRoot "docker-compose.yml") down -v
    docker compose -f (Join-Path $ProjectRoot "docker-compose.yml") up -d db

    Write-Host "[INFO] Waiting for MySQL container to become healthy..."
    $retries = 30
    while ($retries -gt 0) {
        $status = docker inspect --format="{{.State.Health.Status}}" insurance-mysql8 2>$null
        if ($status -eq "healthy") {
            Write-Host "[INFO] MySQL container is healthy." -ForegroundColor Green
            break
        }
        Start-Sleep -Seconds 2
        $retries--
    }
}

if ($Scale -ne "canonical") {
    $SqlFile = Join-Path $PSScriptRoot "volume-data.sql"
    Write-Host "[INFO] Generating volume SQL dataset ($Scale)..."
    python (Join-Path $PSScriptRoot "seed-volume-data.py") --scale $Scale --output $SqlFile

    Write-Host "[INFO] Importing volume data into MySQL..."
    Get-Content $SqlFile | docker compose -f (Join-Path $ProjectRoot "docker-compose.yml") exec -T db mysql -u root -proot insurance_db

    Remove-Item $SqlFile -Force -ErrorAction SilentlyContinue
    Write-Host "[SUCCESS] Volume dataset seeded successfully." -ForegroundColor Green
} else {
    Write-Host "[INFO] Canonical seed data is automatically loaded by Spring Boot TestDataLoader upon instance startup." -ForegroundColor Green
}

Write-Host "=== Database Reset Complete ===" -ForegroundColor Cyan
