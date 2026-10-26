<#
.SYNOPSIS
    Automated Verification Suite for Team-11 TLS, HSTS, and Reverse Proxy Environment (STP TC-Sec-01 / INS-SR-001)
.DESCRIPTION
    Executes live verification against Nginx reverse proxy:
    1. HTTP-to-HTTPS automatic 301 redirection
    2. HTTPS TLS 1.2 handshake negotiation
    3. HTTPS TLS 1.3 handshake negotiation
    4. TLS 1.0 rejection verification
    5. TLS 1.1 rejection verification
    6. Strict-Transport-Security (HSTS) header verification
    7. Security headers (X-Frame-Options, X-Content-Type-Options, X-XSS-Protection)
    8. Backend actuator health and dual-instance load balancing
#>

param(
    [string]$HostName = "localhost",
    [int]$HttpPort = 80,
    [int]$HttpsPort = 443
)

$ErrorActionPreference = "Continue"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Team-11 TLS 1.2+, HSTS & Load Balancing Verification (TC-Sec-01)" -ForegroundColor Cyan
Write-Host " Target: ${HostName} (HTTP: ${HttpPort}, HTTPS: ${HttpsPort})" -ForegroundColor Cyan
Write-Host "======================================================================`n"

# Locate OpenSSL
$OpenSSL = $null
if (Get-Command openssl -ErrorAction SilentlyContinue) {
    $OpenSSL = "openssl"
} elseif (Test-Path "C:\Program Files\Git\usr\bin\openssl.exe") {
    $OpenSSL = "C:\Program Files\Git\usr\bin\openssl.exe"
}

$passCount = 0
$failCount = 0

function Record-Test($name, $passed, $details) {
    if ($passed) {
        Write-Host "[PASS] $name" -ForegroundColor Green
        if ($details) { Write-Host "       $details" -ForegroundColor Gray }
        $script:passCount++
    } else {
        Write-Host "[FAIL] $name" -ForegroundColor Red
        if ($details) { Write-Host "       $details" -ForegroundColor Yellow }
        $script:failCount++
    }
}

# 1. Test HTTP to HTTPS Redirection (Port 80 -> 443)
Write-Host "--- 1. Testing HTTP-to-HTTPS Redirection (INS-SR-001) ---" -ForegroundColor White
try {
    $httpUrl = "http://${HostName}:${HttpPort}/api/v1/actuator/health"
    $req = [System.Net.HttpWebRequest]::Create($httpUrl)
    $req.AllowAutoRedirect = $false
    $req.Timeout = 5000
    $resp = $req.GetResponse()
    $statusCode = [int]$resp.StatusCode
    $location = $resp.Headers["Location"]
    $resp.Close()
    Record-Test "HTTP port $HttpPort redirects to HTTPS" ($statusCode -eq 301 -or $statusCode -eq 308) "Status: $statusCode, Location: $location"
} catch [System.Net.WebException] {
    if ($_.Response) {
        $statusCode = [int]$_.Response.StatusCode
        $location = $_.Response.Headers["Location"]
        Record-Test "HTTP port $HttpPort redirects to HTTPS" ($statusCode -eq 301 -or $statusCode -eq 308) "Status: $statusCode, Location: $location"
    } else {
        Record-Test "HTTP port $HttpPort redirects to HTTPS" $false "Connection refused or timed out: $($_.Message)"
    }
} catch {
    Record-Test "HTTP port $HttpPort redirects to HTTPS" $false "Error: $($_.Message)"
}

# 2. Test TLS 1.2 and TLS 1.3 Negotiation
Write-Host "`n--- 2. Testing TLS Handshake & Cipher Negotiation (TC-Sec-01) ---" -ForegroundColor White
if ($OpenSSL) {
    # TLS 1.2 Test
    $tls12Out = & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_2 -servername $HostName </dev/null 2>&1
    $tls12Success = ($tls12Out -match "Protocol\s*:\s*TLSv1\.2") -or ($tls12Out -match "Cipher is")
    Record-Test "TLS 1.2 connection accepted" $tls12Success ($tls12Out | Select-String "Protocol|Cipher" | Select-Object -First 2 | Out-String).Trim()

    # TLS 1.3 Test
    $tls13Out = & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_3 -servername $HostName </dev/null 2>&1
    $tls13Success = ($tls13Out -match "Protocol\s*:\s*TLSv1\.3")
    Record-Test "TLS 1.3 connection accepted" $tls13Success ($tls13Out | Select-String "Protocol|Cipher" | Select-Object -First 2 | Out-String).Trim()

    # TLS 1.0 Rejection Test
    $tls10Out = & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1 -servername $HostName </dev/null 2>&1
    $tls10Rejected = ($tls10Out -match "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno")
    Record-Test "TLS 1.0 connection rejected (Legacy disabled)" $tls10Rejected "Handshake rejected as expected by Nginx"

    # TLS 1.1 Rejection Test
    $tls11Out = & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_1 -servername $HostName </dev/null 2>&1
    $tls11Rejected = ($tls11Out -match "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno")
    Record-Test "TLS 1.1 connection rejected (Legacy disabled)" $tls11Rejected "Handshake rejected as expected by Nginx"
} else {
    Write-Host "[WARN] OpenSSL not found. Skipping granular protocol negotiation checks." -ForegroundColor Yellow
}

# 3. Test Security Headers & HSTS
Write-Host "`n--- 3. Testing Security Headers & HSTS (INS-SR-001 / SAD Â§4.4) ---" -ForegroundColor White
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
try {
    $httpsUrl = "https://${HostName}:${HttpsPort}/healthz"
    $req = [System.Net.HttpWebRequest]::Create($httpsUrl)
    $req.Timeout = 5000
    $resp = $req.GetResponse()

    $hsts = $resp.Headers["Strict-Transport-Security"]
    $hasHsts = $hsts -and ($hsts -match "max-age=\d+")
    Record-Test "HSTS header present" $hasHsts "Strict-Transport-Security: $hsts"

    $contentType = $resp.Headers["X-Content-Type-Options"]
    Record-Test "X-Content-Type-Options: nosniff" ($contentType -eq "nosniff") "Header: $contentType"

    $xFrame = $resp.Headers["X-Frame-Options"]
    Record-Test "X-Frame-Options: DENY" ($xFrame -eq "DENY" -or $xFrame -eq "SAMEORIGIN") "Header: $xFrame"

    $xXss = $resp.Headers["X-XSS-Protection"]
    Record-Test "X-XSS-Protection present" ($xXss -match "1") "Header: $xXss"

    $resp.Close()
} catch {
    Record-Test "HTTPS Security Headers" $false "Failed to connect to https://${HostName}:${HttpsPort}/healthz: $($_.Message)"
}

# 4. Test Backend Routing & Load Balancing (INS-NF-002 / INS-NF-003)
Write-Host "`n--- 4. Testing Backend Routing & Instance Load Balancing ---" -ForegroundColor White
try {
    $apiUrl = "https://${HostName}:${HttpsPort}/api/v1/actuator/health"
    $upstreamServers = @()
    for ($i = 1; $i -le 6; $i++) {
        $req = [System.Net.HttpWebRequest]::Create($apiUrl)
        $req.Timeout = 5000
        $resp = $req.GetResponse()
        $upstream = $resp.Headers["X-Upstream-Backend"]
        if ($upstream) { $upstreamServers += $upstream }
        $resp.Close()
    }

    $uniqueUpstreams = $upstreamServers | Select-Object -Unique
    Record-Test "Actuator Health reachable over HTTPS" ($upstreamServers.Count -gt 0) "Responses received: $($upstreamServers.Count)"
    Record-Test "Traffic load-balanced across multiple backends" ($uniqueUpstreams.Count -ge 2) "Distinct backends observed: $($uniqueUpstreams -join ', ')"
} catch {
    Record-Test "Backend Routing & Load Balancing" $false "Error connecting to API backend: $($_.Message)"
}

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host " Verification Summary: $passCount Passed, $failCount Failed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "======================================================================"
