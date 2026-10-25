<#
.SYNOPSIS
    Generates reproducible, self-signed TLS test certificates for the Team-11 Nginx reverse proxy.
.DESCRIPTION
    Creates a 2048-bit RSA private key and self-signed X.509 v3 certificate with Subject Alternative Names (SAN):
    - DNS: localhost, insurance.local, host.docker.internal
    - IP: 127.0.0.1
    Enforces TLS Web Server Authentication Extended Key Usage (EKU) for compatibility with modern browsers and curl/Newman.

    TRUST & SECURITY LIMITATIONS:
    - This certificate is strictly for development, integration, and security testing (STP Â§TC-Sec-01).
    - It is self-signed and NOT trusted by public Certificate Authorities (CAs).
    - When testing via curl, use -k / --insecure or pass --cacert.
    - Private keys (*.key) are excluded from Git via .gitignore and MUST NEVER be committed.
#>

[CmdletBinding()]
param(
    [string]$OutputDir = "",
    [int]$DaysValid = 365,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Definition }
    $OutputDir = Join-Path (Split-Path -Parent $scriptDir) "nginx\certs"
}

$OutputDir = [System.IO.Path]::GetFullPath($OutputDir)
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$KeyPath = Join-Path $OutputDir "server.key"
$CertPath = Join-Path $OutputDir "server.crt"

if ((Test-Path $KeyPath) -and (Test-Path $CertPath) -and (-not $Force)) {
    Write-Host "[INFO] TLS certificates already exist in $OutputDir. Use -Force to regenerate." -ForegroundColor Yellow
    exit 0
}

Write-Host "=== Generating Team-11 Test TLS Certificate & Private Key ===" -ForegroundColor Cyan
Write-Host "Output Directory: $OutputDir"

# Locate OpenSSL (Git for Windows, system PATH, or fallback)
$OpenSSL = $null
if (Get-Command openssl -ErrorAction SilentlyContinue) {
    $OpenSSL = "openssl"
} elseif (Test-Path "C:\Program Files\Git\usr\bin\openssl.exe") {
    $OpenSSL = "C:\Program Files\Git\usr\bin\openssl.exe"
}

if ($OpenSSL) {
    Write-Host "[INFO] Using OpenSSL binary: $OpenSSL" -ForegroundColor Green

    $Subj = "/C=IN/ST=Karnataka/L=Bengaluru/O=Team-11/OU=Engineering/CN=localhost"
    $San = "subjectAltName=DNS:localhost,DNS:insurance.local,DNS:host.docker.internal,IP:127.0.0.1"

    & $OpenSSL req -x509 -nodes -days $DaysValid -newkey rsa:2048 `
        -keyout $KeyPath `
        -out $CertPath `
        -subj $Subj `
        -addext $San `
        -addext "basicConstraints=critical,CA:FALSE" `
        -addext "keyUsage=critical,digitalSignature,keyEncipherment" `
        -addext "extendedKeyUsage=serverAuth"

    if ($LASTEXITCODE -ne 0) {
        Write-Error "OpenSSL certificate generation failed."
    }
} else {
    Write-Host "[WARN] OpenSSL not found. Falling back to PowerShell New-SelfSignedCertificate..." -ForegroundColor Yellow

    $Cert = New-SelfSignedCertificate `
        -DnsName "localhost", "insurance.local", "host.docker.internal" `
        -CertStoreLocation "Cert:\CurrentUser\My" `
        -KeyExportPolicy Exportable `
        -KeySpec KeyExchange `
        -KeyLength 2048 `
        -KeyAlgorithm RSA `
        -HashAlgorithm SHA256 `
        -NotAfter (Get-Date).AddDays($DaysValid) `
        -Type Custom `
        -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.1")

    # Export certificate (PEM format)
    $CertBytes = $Cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert)
    $CertBase64 = [System.Convert]::ToBase64String($CertBytes, [System.Base64FormattingOptions]::InsertLineBreaks)
    $CertPem = "-----BEGIN CERTIFICATE-----`r`n$CertBase64`r`n-----END CERTIFICATE-----`r`n"
    [System.IO.File]::WriteAllText($CertPath, $CertPem)

    Write-Host "[INFO] Self-signed certificate exported to $CertPath"
}

Write-Host "`n[SUCCESS] TLS test certificate and key generated successfully:" -ForegroundColor Green
Write-Host "  - Private Key: $KeyPath"
Write-Host "  - Certificate: $CertPath"
Write-Host "`nSECURITY REMINDER: This is a self-signed development/test certificate." -ForegroundColor Yellow
Write-Host "Never deploy self-signed certificates or private keys to production environments." -ForegroundColor Yellow
