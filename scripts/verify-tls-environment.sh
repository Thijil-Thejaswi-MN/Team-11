#!/usr/bin/env bash
# ==============================================================================
# Automated Verification Suite for Team-11 TLS, HSTS, and Reverse Proxy (TC-Sec-01)
# ==============================================================================
set -euo pipefail

HOST="${1:-localhost}"
HTTP_PORT="${2:-80}"
HTTPS_PORT="${3:-443}"

echo "======================================================================"
echo " Team-11 TLS 1.2+, HSTS & Load Balancing Verification (TC-Sec-01)"
echo " Target: ${HOST} (HTTP: ${HTTP_PORT}, HTTPS: ${HTTPS_PORT})"
echo "======================================================================"

pass_count=0
fail_count=0

record_test() {
    local name="$1"
    local status="$2"
    local details="$3"
    if [ "$status" -eq 0 ]; then
        echo -e "\033[0;32m[PASS]\033[0m ${name}"
        [ -n "$details" ] && echo "       ${details}"
        pass_count=$((pass_count + 1))
    else
        echo -e "\033[0;31m[FAIL]\033[0m ${name}"
        [ -n "$details" ] && echo "       ${details}"
        fail_count=$((fail_count + 1))
    fi
}

# 1. HTTP to HTTPS Redirection
echo ""
echo "--- 1. Testing HTTP-to-HTTPS Redirection (INS-SR-001) ---"
http_resp=$(curl -sI -o /dev/null -w "%{http_code} %{redirect_url}" "http://${HOST}:${HTTP_PORT}/" 2>/dev/null || echo "000 error")
status_code=$(echo "$http_resp" | cut -d' ' -f1)
redirect_url=$(echo "$http_resp" | cut -d' ' -f2)

if [[ "$status_code" =~ ^(301|308)$ && "$redirect_url" =~ ^https:// ]]; then
    record_test "HTTP port ${HTTP_PORT} redirects to HTTPS" 0 "Status: ${status_code}, Target: ${redirect_url}"
else
    record_test "HTTP port ${HTTP_PORT} redirects to HTTPS" 1 "Expected 301/308 redirect, got: ${http_resp}"
fi

# 2. TLS Protocols
echo ""
echo "--- 2. Testing TLS Handshake & Cipher Negotiation (TC-Sec-01) ---"
# TLS 1.2
tls12_out=$(openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_2 -servername "${HOST}" </dev/null 2>&1 || true)
if echo "$tls12_out" | grep -Eq "Protocol\s*:\s*TLSv1\.2|Cipher is"; then
    record_test "TLS 1.2 connection accepted" 0 "Negotiated TLSv1.2"
else
    record_test "TLS 1.2 connection accepted" 1 "Failed to negotiate TLS 1.2"
fi

# TLS 1.3
tls13_out=$(openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_3 -servername "${HOST}" </dev/null 2>&1 || true)
if echo "$tls13_out" | grep -Eq "Protocol\s*:\s*TLSv1\.3"; then
    record_test "TLS 1.3 connection accepted" 0 "Negotiated TLSv1.3"
else
    record_test "TLS 1.3 connection accepted" 1 "Failed to negotiate TLS 1.3"
fi

# TLS 1.0 Rejection
tls10_out=$(openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1 -servername "${HOST}" </dev/null 2>&1 || true)
if echo "$tls10_out" | grep -Eq "Connection refused|connect:errno"; then
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 1 "Target host/port unreachable"
elif echo "$tls10_out" | grep -Eq "Protocol\s*:\s*TLSv1(\.0)?\b"; then
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 1 "TLS 1.0 was unexpectedly accepted"
elif echo "$tls10_out" | grep -Eq "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno|alert number 70"; then
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 0 "Handshake rejected as expected by server"
else
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 1 "Unexpected response: $(echo "$tls10_out" | head -n 1)"
fi

# TLS 1.1 Rejection
tls11_out=$(openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_1 -servername "${HOST}" </dev/null 2>&1 || true)
if echo "$tls11_out" | grep -Eq "Connection refused|connect:errno"; then
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 1 "Target host/port unreachable"
elif echo "$tls11_out" | grep -Eq "Protocol\s*:\s*TLSv1\.1\b"; then
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 1 "TLS 1.1 was unexpectedly accepted"
elif echo "$tls11_out" | grep -Eq "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno|alert number 70"; then
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 0 "Handshake rejected as expected by server"
else
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 1 "Unexpected response: $(echo "$tls11_out" | head -n 1)"
fi

# 3. Security Headers & HSTS
echo ""
echo "--- 3. Testing Security Headers & HSTS (INS-SR-001) ---"
headers=$(curl -k -sI "https://${HOST}:${HTTPS_PORT}/healthz" 2>/dev/null || true)

if echo "$headers" | grep -qi "strict-transport-security:.*max-age="; then
    hsts_val=$(echo "$headers" | grep -i "strict-transport-security:" | tr -d '\r')
    record_test "HSTS header present" 0 "$hsts_val"
else
    record_test "HSTS header present" 1 "Missing Strict-Transport-Security header"
fi

if echo "$headers" | grep -qi "x-content-type-options:.*nosniff"; then
    record_test "X-Content-Type-Options: nosniff" 0 "nosniff present"
else
    record_test "X-Content-Type-Options: nosniff" 1 "Missing nosniff header"
fi

if echo "$headers" | grep -qi "x-frame-options:"; then
    xframe_val=$(echo "$headers" | grep -i "x-frame-options:" | tr -d '\r')
    record_test "X-Frame-Options present" 0 "$xframe_val"
else
    record_test "X-Frame-Options present" 1 "Missing X-Frame-Options header"
fi

if echo "$headers" | grep -qi "x-xss-protection:"; then
    xxss_val=$(echo "$headers" | grep -i "x-xss-protection:" | tr -d '\r')
    record_test "X-XSS-Protection present" 0 "$xxss_val"
else
    record_test "X-XSS-Protection present" 1 "Missing X-XSS-Protection header"
fi

# 4. Backend Health & Load Balancing
echo ""
echo "--- 4. Testing Backend Routing & Instance Load Balancing ---"
servers=()
for i in {1..6}; do
    srv=$(curl -k -sI "https://${HOST}:${HTTPS_PORT}/api/v1/actuator/health" 2>/dev/null | grep -i "x-upstream-backend:" | cut -d' ' -f2- | tr -d '\r' || true)
    [ -n "$srv" ] && servers+=("$srv")
done

if [ "${#servers[@]}" -gt 0 ]; then
    record_test "Actuator Health reachable over HTTPS" 0 "Responses received: ${#servers[@]}"
else
    record_test "Actuator Health reachable over HTTPS" 1 "Could not reach backend health check over HTTPS"
fi

distinct_servers=($(printf "%s\n" "${servers[@]}" | sort -u))
if [ "${#distinct_servers[@]}" -ge 2 ]; then
    record_test "Traffic load-balanced across multiple backends" 0 "Distinct backends observed: ${distinct_servers[*]}"
else
    record_test "Traffic load-balanced across multiple backends" 1 "Expected at least 2 distinct backends, observed: ${distinct_servers[*]:-none}"
fi

echo ""
echo "======================================================================"
echo " Verification Complete: ${pass_count} Passed, ${fail_count} Failed"
echo "======================================================================"

if [ "${fail_count}" -gt 0 ]; then
    exit 1
fi
exit 0
