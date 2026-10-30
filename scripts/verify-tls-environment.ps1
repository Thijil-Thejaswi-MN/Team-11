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
    7. Security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection)
    8. Backend actuator health and dual-instance load balancing
#>

[CmdletBinding()]
param(
    [Alias("TargetHost", "Host")]
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
    $tls12Out = $null | & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_2 -servername $HostName 2>&1
    $tls12Text = ($tls12Out | Out-String)
    $tls12Success = ($tls12Text -match "Protocol\s*:\s*TLSv1\.2") -or ($tls12Text -match "Cipher is")
    $details12 = if ($tls12Success) { ($tls12Out | Select-String "Protocol|Cipher" | Select-Object -First 2 | Out-String).Trim() } else { "Failed to negotiate TLS 1.2" }
    Record-Test "TLS 1.2 connection accepted" $tls12Success $details12

    # TLS 1.3 Test
    $tls13Out = $null | & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_3 -servername $HostName 2>&1
    $tls13Text = ($tls13Out | Out-String)
    $tls13Success = ($tls13Text -match "Protocol\s*:\s*TLSv1\.3")
    $details13 = if ($tls13Success) { ($tls13Out | Select-String "Protocol|Cipher" | Select-Object -First 2 | Out-String).Trim() } else { "Failed to negotiate TLS 1.3" }
    Record-Test "TLS 1.3 connection accepted" $tls13Success $details13

    # TLS 1.0 Rejection Test
    $tls10Out = $null | & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1 -servername $HostName 2>&1
    $tls10Text = ($tls10Out | Out-String)
    $isConnRefused10 = ($tls10Text -match "Connection refused|connect:errno|calling connect")
    $tls10Accepted = ($tls10Text -match "Protocol\s*:\s*TLSv1(\.0)?\b")
    $tls10Rejected = (-not $isConnRefused10) -and (-not $tls10Accepted) -and ($tls10Text -match "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno|alert number 70")
    $details10 = if ($isConnRefused10) { "Target host/port unreachable" } elseif ($tls10Accepted) { "TLS 1.0 was unexpectedly accepted" } elseif ($tls10Rejected) { "Handshake rejected as expected by Nginx" } else { "Unexpected response: $(($tls10Out | Select-Object -First 1) -join ' ')" }
    Record-Test "TLS 1.0 connection rejected (Legacy disabled)" $tls10Rejected $details10

    # TLS 1.1 Rejection Test
    $tls11Out = $null | & $OpenSSL s_client -connect "${HostName}:${HttpsPort}" -tls1_1 -servername $HostName 2>&1
    $tls11Text = ($tls11Out | Out-String)
    $isConnRefused11 = ($tls11Text -match "Connection refused|connect:errno|calling connect")
    $tls11Accepted = ($tls11Text -match "Protocol\s*:\s*TLSv1\.1\b")
    $tls11Rejected = (-not $isConnRefused11) -and (-not $tls11Accepted) -and ($tls11Text -match "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno|alert number 70")
    $details11 = if ($isConnRefused11) { "Target host/port unreachable" } elseif ($tls11Accepted) { "TLS 1.1 was unexpectedly accepted" } elseif ($tls11Rejected) { "Handshake rejected as expected by Nginx" } else { "Unexpected response: $(($tls11Out | Select-Object -First 1) -join ' ')" }
    Record-Test "TLS 1.1 connection rejected (Legacy disabled)" $tls11Rejected $details11
} else {
    Write-Host "[WARN] OpenSSL not found. Skipping granular protocol negotiation checks." -ForegroundColor Yellow
}

# 3. Test Security Headers & HSTS
Write-Host "`n--- 3. Testing Security Headers & HSTS (INS-SR-001 / SAD §4.4) ---" -ForegroundColor White
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
    Record-Test "X-Frame-Options present" ($xFrame -eq "DENY" -or $xFrame -eq "SAMEORIGIN") "Header: $xFrame"

    $xXss = $resp.Headers["X-XSS-Protection"]
    Record-Test "X-XSS-Protection present" ($xXss -match "1") "Header: $xXss"

    $resp.Close()
} catch {
    $errMsg = "Failed to connect to https://${HostName}:${HttpsPort}/healthz: $($_.Message)"
    Record-Test "HSTS header present" $false $errMsg
    Record-Test "X-Content-Type-Options: nosniff" $false $errMsg
    Record-Test "X-Frame-Options present" $false $errMsg
    Record-Test "X-XSS-Protection present" $false $errMsg
}

# 4. Test Backend Routing & Load Balancing (INS-NF-002 / INS-NF-003)
Write-Host "`n--- 4. Testing Backend Routing & Instance Load Balancing ---" -ForegroundColor White
$apiUrl = "https://${HostName}:${HttpsPort}/api/v1/actuator/health"
$upstreamServers = @()
for ($i = 1; $i -le 6; $i++) {
    try {
        $req = [System.Net.HttpWebRequest]::Create($apiUrl)
        $req.Timeout = 5000
        $resp = $req.GetResponse()
        $upstream = $resp.Headers["X-Upstream-Backend"]
        if ($upstream) { $upstreamServers += $upstream }
        $resp.Close()
    } catch {
        # Loop continues to collect any responsive backends
    }
}

$uniqueUpstreams = $upstreamServers | Select-Object -Unique
$detailsHealth = if ($upstreamServers.Count -gt 0) { "Responses received: $($upstreamServers.Count)" } else { "Could not reach backend health check over HTTPS" }
Record-Test "Actuator Health reachable over HTTPS" ($upstreamServers.Count -gt 0) $detailsHealth

$detailsBalancing = if ($uniqueUpstreams.Count -ge 2) { "Distinct backends observed: $($uniqueUpstreams -join ', ')" } else { "Expected at least 2 distinct backends, observed: $(if ($uniqueUpstreams) { $uniqueUpstreams -join ', ' } else { 'none' })" }
Record-Test "Traffic load-balanced across multiple backends" ($uniqueUpstreams.Count -ge 2) $detailsBalancing

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host " Verification Summary: $passCount Passed, $failCount Failed" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "======================================================================"

if ($failCount -gt 0) {
    exit 1
}
exit 0
