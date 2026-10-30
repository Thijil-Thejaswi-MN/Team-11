#!/usr/bin/env bash
# ==============================================================================
# Team-11 MySQL Database Backup Utility (P4-14 / TC-Rel-01 / INS-NF-004)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"

# Configuration defaults
BACKUP_DIR="${BACKUP_DIR:-"${PROJECT_ROOT}/backups"}"
DB_NAME="${DB_NAME:-"insurance_db"}"
DB_USER="${DB_USER:-"root"}"
DB_PASSWORD="${DB_PASSWORD:-"${MYSQL_ROOT_PASSWORD:-"${MYSQL_PWD:-"root"}"}"}"
DOCKER_CONTAINER="${DOCKER_CONTAINER:-"insurance-mysql8"}"
DB_HOST="${DB_HOST:-"127.0.0.1"}"
DB_PORT="${DB_PORT:-"3306"}"
DIRECT_CLIENT="${DIRECT_CLIENT:-0}"
DRY_RUN="${DRY_RUN:-0}"

# Parse command line flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        --dir)
            BACKUP_DIR="$2"; shift 2 ;;
        --db)
            DB_NAME="$2"; shift 2 ;;
        --user)
            DB_USER="$2"; shift 2 ;;
        --password)
            DB_PASSWORD="$2"; shift 2 ;;
        --container)
            DOCKER_CONTAINER="$2"; shift 2 ;;
        --host)
            DB_HOST="$2"; shift 2 ;;
        --port)
            DB_PORT="$2"; shift 2 ;;
        --direct)
            DIRECT_CLIENT=1; shift ;;
        --dry-run)
            DRY_RUN=1; shift ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --dir <path>         Destination directory for backups (default: backups)"
            echo "  --db <name>          Database name to back up (default: insurance_db)"
            echo "  --user <user>        Database user (default: root)"
            echo "  --password <pwd>     Database password (default: root)"
            echo "  --container <name>   Docker container name (default: insurance-mysql8)"
            echo "  --host <host>        Direct host (default: 127.0.0.1)"
            echo "  --port <port>        Direct port (default: 3306)"
            echo "  --direct             Force using host mysqldump instead of container"
            echo "  --dry-run            Validate configuration without dumping"
            exit 0 ;;
        *)
            echo "[ERROR] Unknown option: $1" >&2; exit 1 ;;
    esac
done

echo "======================================================================"
echo " Team-11 MySQL Database Backup Utility (P4-14 / TC-Rel-01)"
echo " Target Database: ${DB_NAME}"
echo " Backup Directory: ${BACKUP_DIR}"
echo "======================================================================"
echo ""

# 1. Validate destination directory
mkdir -p "${BACKUP_DIR}"

# 2. Detect execution mode
USE_CONTAINER=0
if [[ "${DIRECT_CLIENT}" != "1" ]] && command -v docker >/dev/null 2>&1; then
    RUNNING=$(docker inspect --format='{{.State.Running}}' "${DOCKER_CONTAINER}" 2>/dev/null || echo "false")
    if [[ "${RUNNING}" == "true" ]]; then
        USE_CONTAINER=1
        echo "[INFO] Detected active Docker database container '${DOCKER_CONTAINER}'."
    else
        echo "[WARN] Docker container '${DOCKER_CONTAINER}' is not running."
    fi
fi

if [[ "${USE_CONTAINER}" == "0" ]]; then
    if command -v mysqldump >/dev/null 2>&1; then
        echo "[INFO] Using host mysqldump client: $(command -v mysqldump)"
    else
        echo "" >&2
        echo "[FATAL] Database connection unavailable:" >&2
        echo "  1. Docker container '${DOCKER_CONTAINER}' is not running." >&2
        echo "  2. Host executable 'mysqldump' was not found in PATH." >&2
        echo "" >&2
        echo "To resolve: Start Docker stack ('docker compose up -d db') or install MySQL client." >&2
        exit 1
    fi
fi

# 3. Filename & Metadata Setup
TIMESTAMP_UTC=$(date -u +"%Y%m%d_%H%M%S")
ISO_TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
BASE_NAME="backup_${DB_NAME}_${TIMESTAMP_UTC}"
BACKUP_FILE="${BACKUP_DIR}/${BASE_NAME}.sql.gz"
CHECKSUM_FILE="${BACKUP_DIR}/${BASE_NAME}.sql.gz.sha256"
META_FILE="${BACKUP_DIR}/${BASE_NAME}.meta.json"

if [[ "${DRY_RUN}" == "1" ]]; then
    echo ""
    echo "[DRY RUN] Configuration validated successfully."
    echo "  - Target: ${DB_NAME}"
    echo "  - Mode: $([ "${USE_CONTAINER}" == "1" ] && echo "Docker (${DOCKER_CONTAINER})" || echo "Host Client (${DB_HOST}:${DB_PORT})")"
    echo "  - Output: ${BACKUP_FILE}"
    exit 0
fi

echo "[INFO] Starting database backup..."
echo "  - Timestamp (UTC): ${ISO_TIMESTAMP}"
echo "  - Destination Archive: ${BACKUP_FILE}"

# Cleanup on failure
cleanup_on_error() {
    rm -f "${BACKUP_FILE}" "${CHECKSUM_FILE}" "${META_FILE}"
}
trap cleanup_on_error ERR

START_TIME=$(date +%s)

# 4. Stream mysqldump into gzip
DUMP_FLAGS="--single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF --add-drop-table"

if [[ "${USE_CONTAINER}" == "1" ]]; then
    docker exec -e MYSQL_PWD="${DB_PASSWORD}" "${DOCKER_CONTAINER}" mysqldump ${DUMP_FLAGS} -u "${DB_USER}" "${DB_NAME}" | gzip -9 > "${BACKUP_FILE}"
else
    MYSQL_PWD="${DB_PASSWORD}" mysqldump --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" ${DUMP_FLAGS} "${DB_NAME}" | gzip -9 > "${BACKUP_FILE}"
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
[ "${DURATION}" -eq 0 ] && DURATION=1

# Validate generated file
if [[ ! -s "${BACKUP_FILE}" ]]; then
    echo "[FATAL] Backup file was not created or is empty." >&2
    rm -f "${BACKUP_FILE}"
    exit 1
fi

FILE_SIZE=$(wc -c < "${BACKUP_FILE}" | tr -d ' ')

# 5. SHA-256 Checksum Generation
echo "[INFO] Generating SHA-256 integrity checksum..."
FILE_NAME_ONLY="$(basename "${BACKUP_FILE}")"
if command -v sha256sum >/dev/null 2>&1; then
    SHA256_HASH=$(sha256sum "${BACKUP_FILE}" | cut -d' ' -f1)
elif command -v shasum >/dev/null 2>&1; then
    SHA256_HASH=$(shasum -a 256 "${BACKUP_FILE}" | cut -d' ' -f1)
else
    # Fallback to openssl
    SHA256_HASH=$(openssl dgst -sha256 "${BACKUP_FILE}" | awk '{print $NF}')
fi

echo "${SHA256_HASH}  ${FILE_NAME_ONLY}" > "${CHECKSUM_FILE}"

# 6. Metadata JSON Generation
cat <<EOF > "${META_FILE}"
{
  "backup_file": "${FILE_NAME_ONLY}",
  "checksum_sha256": "${SHA256_HASH}",
  "database": "${DB_NAME}",
  "timestamp_utc": "${ISO_TIMESTAMP}",
  "file_size_bytes": ${FILE_SIZE},
  "duration_seconds": ${DURATION},
  "compression": "gzip",
  "format": "mysqldump",
  "schema_version": "1.0",
  "target_rpo": "24h",
  "target_rto": "4h",
  "source_mode": "$([ "${USE_CONTAINER}" == "1" ] && echo "docker:${DOCKER_CONTAINER}" || echo "direct:${DB_HOST}:${DB_PORT}")"
}
EOF

trap - ERR

echo ""
echo "======================================================================"
echo " [SUCCESS] MySQL Database Backup Completed Successfully"
echo "======================================================================"
echo "  - Backup Archive:   ${BACKUP_FILE}"
echo "  - Compressed Size:  ${FILE_SIZE} bytes"
echo "  - SHA-256 Checksum: ${SHA256_HASH}"
echo "  - Checksum File:    ${CHECKSUM_FILE}"
echo "  - Metadata File:    ${META_FILE}"
echo "  - Duration:         ${DURATION} second(s)"
echo "======================================================================"
echo ""
exit 0
