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
        ((pass_count++))
    else
        echo -e "\033[0;31m[FAIL]\033[0m ${name}"
        [ -n "$details" ] && echo "       ${details}"
        ((fail_count++))
    fi
}

# 1. HTTP to HTTPS Redirection
echo ""
echo "--- 1. Testing HTTP-to-HTTPS Redirection (INS-SR-001) ---"
http_resp=$(curl -sI -o /dev/null -w "%{http_code} %{redirect_url}" "http://${HOST}:${HTTP_PORT}/" || echo "000 error")
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
if openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_2 -servername "${HOST}" </dev/null 2>&1 | grep -q "Protocol  : TLSv1.2"; then
    record_test "TLS 1.2 connection accepted" 0 "Negotiated TLSv1.2"
else
    record_test "TLS 1.2 connection accepted" 1 "Failed to negotiate TLS 1.2"
fi

# TLS 1.3
if openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_3 -servername "${HOST}" </dev/null 2>&1 | grep -q "Protocol  : TLSv1.3"; then
    record_test "TLS 1.3 connection accepted" 0 "Negotiated TLSv1.3"
else
    record_test "TLS 1.3 connection accepted" 1 "Failed to negotiate TLS 1.3"
fi

# TLS 1.0 Rejection
if openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1 -servername "${HOST}" </dev/null 2>&1 | grep -Eq "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno"; then
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 0 "Rejected by server"
else
    record_test "TLS 1.0 connection rejected (Legacy disabled)" 1 "TLS 1.0 was unexpectedly accepted"
fi

# TLS 1.1 Rejection
if openssl s_client -connect "${HOST}:${HTTPS_PORT}" -tls1_1 -servername "${HOST}" </dev/null 2>&1 | grep -Eq "handshake failure|no protocols available|wrong version number|alert protocol version|write:errno"; then
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 0 "Rejected by server"
else
    record_test "TLS 1.1 connection rejected (Legacy disabled)" 1 "TLS 1.1 was unexpectedly accepted"
fi

# 3. Security Headers & HSTS
echo ""
echo "--- 3. Testing Security Headers & HSTS (INS-SR-001) ---"
headers=$(curl -k -sI "https://${HOST}:${HTTPS_PORT}/healthz")

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

# 4. Backend Health & Load Balancing
echo ""
echo "--- 4. Testing Backend Routing & Instance Load Balancing ---"
servers=()
for i in {1..6}; do
    srv=$(curl -k -sI "https://${HOST}:${HTTPS_PORT}/api/v1/actuator/health" | grep -i "x-upstream-backend:" | cut -d' ' -f2 | tr -d '\r' || true)
    [ -n "$srv" ] && servers+=("$srv")
done

distinct_servers=($(printf "%s\n" "${servers[@]}" | sort -u))
if [ "${#distinct_servers[@]}" -ge 2 ]; then
    record_test "Traffic load-balanced across multiple backends" 0 "Backends observed: ${distinct_servers[*]}"
elif [ "${#distinct_servers[@]}" -eq 1 ]; then
    record_test "Backend reachable over HTTPS (Single instance observed)" 0 "Backend: ${distinct_servers[0]}"
else
    record_test "Backend reachable over HTTPS" 1 "Could not reach backend health check over HTTPS"
fi

echo ""
echo "======================================================================"
echo " Verification Complete: ${pass_count} Passed, ${fail_count} Failed"
echo "======================================================================"
