#!/usr/bin/env bash
# ==============================================================================
# Generates reproducible, self-signed TLS test certificates for Team-11 Nginx
# Compatible with Linux, WSL, and macOS environments.
# ==============================================================================
set -euo pipefail

# Prevent MSYS/Git Bash from corrupting OpenSSL -subj on Windows
export MSYS2_ARG_CONV_EXCL="${MSYS2_ARG_CONV_EXCL:-}${MSYS2_ARG_CONV_EXCL:+;}/C="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${1:-"${SCRIPT_DIR}/../nginx/certs"}"
DAYS_VALID="${2:-365}"

mkdir -p "${OUTPUT_DIR}"
KEY_PATH="${OUTPUT_DIR}/server.key"
CERT_PATH="${OUTPUT_DIR}/server.crt"

if [[ -f "${KEY_PATH}" && -f "${CERT_PATH}" && "${FORCE:-0}" != "1" ]]; then
    echo "[INFO] TLS certificates already exist in ${OUTPUT_DIR}. Set FORCE=1 to regenerate."
    exit 0
fi

echo "=== Generating Team-11 Test TLS Certificate & Private Key ==="
echo "Output Directory: ${OUTPUT_DIR}"

openssl req -x509 -nodes -days "${DAYS_VALID}" -newkey rsa:2048 \
    -keyout "${KEY_PATH}" \
    -out "${CERT_PATH}" \
    -subj "/C=IN/ST=Karnataka/L=Bengaluru/O=Team-11/OU=Engineering/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:insurance.local,DNS:host.docker.internal,IP:127.0.0.1" \
    -addext "basicConstraints=critical,CA:FALSE" \
    -addext "keyUsage=critical,digitalSignature,keyEncipherment" \
    -addext "extendedKeyUsage=serverAuth"

chmod 600 "${KEY_PATH}"
chmod 644 "${CERT_PATH}"

echo ""
echo "[SUCCESS] TLS test certificate and key generated successfully:"
echo "  - Private Key: ${KEY_PATH}"
echo "  - Certificate: ${CERT_PATH}"
echo ""
echo "SECURITY NOTE: This certificate is strictly for development/test purposes."
echo "Never commit private keys or deploy self-signed certificates to production."
