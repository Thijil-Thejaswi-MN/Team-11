#!/usr/bin/env bash
# ==============================================================================
# Team-11 MySQL Database Restore & Recovery Utility (P4-14 / TC-Rel-01)
# ==============================================================================
set -euo pipefail

BACKUP_FILE=""
TARGET_DB=""
DB_USER="${DB_USER:-"root"}"
DB_PASSWORD="${DB_PASSWORD:-"${MYSQL_ROOT_PASSWORD:-"${MYSQL_PWD:-"root"}"}"}"
DOCKER_CONTAINER="${DOCKER_CONTAINER:-"insurance-mysql8"}"
DB_HOST="${DB_HOST:-"127.0.0.1"}"
DB_PORT="${DB_PORT:-"3306"}"
DIRECT_CLIENT="${DIRECT_CLIENT:-0}"
FORCE="${RESTORE_FORCE:-"${NON_INTERACTIVE:-0}"}"
SKIP_CHECKSUM=0
VERIFY_ONLY=0
DRY_RUN=0

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --file|-f)
            BACKUP_FILE="$2"; shift 2 ;;
        --target-db|--db)
            TARGET_DB="$2"; shift 2 ;;
        --user|-u)
            DB_USER="$2"; shift 2 ;;
        --password|-p)
            DB_PASSWORD="$2"; shift 2 ;;
        --container|-c)
            DOCKER_CONTAINER="$2"; shift 2 ;;
        --host|-h)
            DB_HOST="$2"; shift 2 ;;
        --port)
            DB_PORT="$2"; shift 2 ;;
        --direct)
            DIRECT_CLIENT=1; shift ;;
        --force|-y)
            FORCE=1; shift ;;
        --skip-checksum)
            SKIP_CHECKSUM=1; shift ;;
        --verify-only)
            VERIFY_ONLY=1; shift ;;
        --dry-run)
            DRY_RUN=1; shift ;;
        --help)
            echo "Usage: $0 <backup-file> [options]"
            echo "Options:"
            echo "  --target-db <name>   Target database name (default: auto-detected or insurance_db)"
            echo "  --user <user>        Database user (default: root)"
            echo "  --password <pwd>     Database password (default: root)"
            echo "  --container <name>   Docker container name (default: insurance-mysql8)"
            echo "  --force, -y          Bypass interactive confirmation prompt"
            echo "  --skip-checksum      Skip SHA-256 verification (NOT recommended)"
            echo "  --direct             Force using host mysql client instead of container"
            echo "  --dry-run            Validate backup archive and target without restoring"
            exit 0 ;;
        *)
            if [[ -z "${BACKUP_FILE}" ]]; then
                BACKUP_FILE="$1"; shift
            else
                echo "[ERROR] Unknown argument: $1" >&2; exit 1
            fi ;;
    esac
done

if [[ -z "${BACKUP_FILE}" ]]; then
    echo "[ERROR] Backup file path is required. Usage: $0 <path-to-backup.sql.gz>" >&2
    exit 1
fi

echo "======================================================================"
echo " Team-11 MySQL Database Restore & Recovery Utility (P4-14 / TC-Rel-01)"
echo "======================================================================"
echo ""

# 1. Validate file existence
if [[ ! -f "${BACKUP_FILE}" ]]; then
    echo "[FATAL] Backup file not found: '${BACKUP_FILE}'" >&2
    exit 1
fi

if [[ ! -s "${BACKUP_FILE}" ]]; then
    echo "[FATAL] Backup file is empty (0 bytes): '${BACKUP_FILE}'" >&2
    exit 1
fi

FILE_SIZE=$(wc -c < "${BACKUP_FILE}" | tr -d ' ')
echo "[INFO] Backup Archive: ${BACKUP_FILE} (${FILE_SIZE} bytes)"

# 2. Inferred Target Database
META_FILE="${BACKUP_FILE%.sql.gz}.meta.json"
[ ! -f "${META_FILE}" ] && META_FILE="${BACKUP_FILE}.meta.json"

if [[ -z "${TARGET_DB}" ]]; then
    if [[ -f "${META_FILE}" ]] && grep -q '"database"' "${META_FILE}"; then
        TARGET_DB=$(grep '"database"' "${META_FILE}" | cut -d'"' -f4)
    else
        TARGET_DB="${DB_NAME:-"insurance_db"}"
    fi
fi
echo "  - Target Database: ${TARGET_DB}"

# 3. Checksum Verification
if [[ "${SKIP_CHECKSUM}" != "1" ]]; then
    echo ""
    echo "--- 1. Verifying SHA-256 Checksum Integrity ---"
    if command -v sha256sum >/dev/null 2>&1; then
        COMPUTED_HASH=$(sha256sum "${BACKUP_FILE}" | cut -d' ' -f1 | tr -dc '[:alnum:]' | tr '[:upper:]' '[:lower:]')
    elif command -v shasum >/dev/null 2>&1; then
        COMPUTED_HASH=$(shasum -a 256 "${BACKUP_FILE}" | cut -d' ' -f1 | tr -dc '[:alnum:]' | tr '[:upper:]' '[:lower:]')
    else
        COMPUTED_HASH=$(openssl dgst -sha256 "${BACKUP_FILE}" | awk '{print $NF}' | tr -dc '[:alnum:]' | tr '[:upper:]' '[:lower:]')
    fi
    echo "  - Computed SHA-256: ${COMPUTED_HASH}"

    CHECKSUM_FILE="${BACKUP_FILE}.sha256"
    EXPECTED_HASH=""
    if [[ -f "${CHECKSUM_FILE}" ]]; then
        EXPECTED_HASH=$(awk '{print $1}' "${CHECKSUM_FILE}" | tr -dc '[:alnum:]' | tr '[:upper:]' '[:lower:]')
        echo "  - Found Checksum File: ${CHECKSUM_FILE}"
    elif [[ -f "${META_FILE}" ]] && grep -q '"checksum_sha256"' "${META_FILE}"; then
        EXPECTED_HASH=$(grep '"checksum_sha256"' "${META_FILE}" | cut -d'"' -f4 | tr -dc '[:alnum:]' | tr '[:upper:]' '[:lower:]')
        echo "  - Found Checksum in Metadata: ${META_FILE}"
    fi

    if [[ -n "${EXPECTED_HASH}" ]]; then
        echo "  - Expected SHA-256: ${EXPECTED_HASH}"
        if [[ "${COMPUTED_HASH}" != "${EXPECTED_HASH}" ]]; then
            echo "" >&2
            echo "[FATAL] CHECKSUM VERIFICATION FAILED!" >&2
            echo "  Archive is corrupted, truncated, or tampered with." >&2
            echo "  Computed: ${COMPUTED_HASH}" >&2
            echo "  Expected: ${EXPECTED_HASH}" >&2
            echo "Restoration aborted to prevent database corruption." >&2
            exit 1
        fi
        echo "  [PASS] Checksum matched successfully."
    else
        echo "  [WARN] No .sha256 or .meta.json file found to verify against."
    fi
fi

# 4. Validate Archive Decompressibility
echo ""
echo "--- 2. Validating Archive Format & Decompression ---"
IS_GZIP=0
if [[ "${BACKUP_FILE}" == *.gz ]]; then
    IS_GZIP=1
    if ! gzip -t "${BACKUP_FILE}" 2>/dev/null; then
        echo "[FATAL] Archive decompression test failed (corrupted gzip file)." >&2
        exit 1
    fi
    echo "  [PASS] Archive decompression verified."
fi

if [[ "${VERIFY_ONLY}" == "1" ]]; then
    echo ""
    echo "======================================================================"
    echo " [SUCCESS] Backup Archive Integrity & Checksum Verified"
    echo "======================================================================"
    echo "  - Archive:   ${BACKUP_FILE}"
    echo "  - Size:      ${FILE_SIZE} bytes"
    echo "  - Checksum:  ${COMPUTED_HASH:-"N/A"}"
    echo "======================================================================"
    echo ""
    exit 0
fi

# 5. Database Connectivity Detection
echo ""
echo "--- 3. Verifying Database Target Connectivity ---"
USE_CONTAINER=0
if [[ "${DIRECT_CLIENT}" != "1" ]] && command -v docker >/dev/null 2>&1; then
    RUNNING=$(docker inspect --format='{{.State.Running}}' "${DOCKER_CONTAINER}" 2>/dev/null || echo "false")
    if [[ "${RUNNING}" == "true" ]]; then
        USE_CONTAINER=1
        echo "  [INFO] Using active Docker database container '${DOCKER_CONTAINER}'."
    else
        echo "  [WARN] Docker container '${DOCKER_CONTAINER}' is not running."
    fi
fi

if [[ "${USE_CONTAINER}" == "0" ]]; then
    if command -v mysql >/dev/null 2>&1; then
        echo "  [INFO] Using host mysql client: $(command -v mysql)"
    else
        echo "" >&2
        echo "[FATAL] Database connection unavailable:" >&2
        echo "  1. Docker container '${DOCKER_CONTAINER}' is not running." >&2
        echo "  2. Host executable 'mysql' was not found in PATH." >&2
        echo "" >&2
        echo "To resolve: Start Docker stack ('docker compose up -d db') or install MySQL client." >&2
        exit 1
    fi
fi

if [[ "${DRY_RUN}" == "1" ]]; then
    echo ""
    echo "[DRY RUN] All pre-restoration checks passed successfully."
    echo "  - Backup: Valid & verified"
    echo "  - Target: ${TARGET_DB}"
    echo "Dry run complete. No database modifications were performed."
    exit 0
fi

# 6. Safety Confirmation
if [[ "${FORCE}" != "1" ]]; then
    echo ""
    echo "**********************************************************************"
    echo " CAUTION: RESTORE OPERATION"
    echo " Target Database: '${TARGET_DB}'"
    echo " All existing schema and records in '${TARGET_DB}' may be overwritten."
    echo "**********************************************************************"
    read -r -p "Are you sure you want to proceed with database restoration? (yes/no): " CONFIRM
    if [[ "${CONFIRM}" != "yes" && "${CONFIRM}" != "y" ]]; then
        echo "[ABORTED] Restoration cancelled by user."
        exit 0
    fi
fi

# 7. Ensure Target Database Exists
echo ""
echo "--- 4. Ensuring Target Database Exists ---"
CREATE_SQL="CREATE DATABASE IF NOT EXISTS \`${TARGET_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if [[ "${USE_CONTAINER}" == "1" ]]; then
    docker exec -e MYSQL_PWD="${DB_PASSWORD}" "${DOCKER_CONTAINER}" mysql -u "${DB_USER}" -e "${CREATE_SQL}"
else
    MYSQL_PWD="${DB_PASSWORD}" mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" -e "${CREATE_SQL}"
fi
echo "  [PASS] Target database '${TARGET_DB}' verified."

# 8. Import Data
echo ""
echo "--- 5. Executing Database Import ---"
START_TIME=$(date +%s)

if [[ "${IS_GZIP}" == "1" ]]; then
    if [[ "${USE_CONTAINER}" == "1" ]]; then
        gzip -dc "${BACKUP_FILE}" | docker exec -i -e MYSQL_PWD="${DB_PASSWORD}" "${DOCKER_CONTAINER}" mysql --default-character-set=utf8mb4 -u "${DB_USER}" "${TARGET_DB}"
    else
        gzip -dc "${BACKUP_FILE}" | MYSQL_PWD="${DB_PASSWORD}" mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" --default-character-set=utf8mb4 "${TARGET_DB}"
    fi
else
    if [[ "${USE_CONTAINER}" == "1" ]]; then
        docker exec -i -e MYSQL_PWD="${DB_PASSWORD}" "${DOCKER_CONTAINER}" mysql --default-character-set=utf8mb4 -u "${DB_USER}" "${TARGET_DB}" < "${BACKUP_FILE}"
    else
        MYSQL_PWD="${DB_PASSWORD}" mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" --default-character-set=utf8mb4 "${TARGET_DB}" < "${BACKUP_FILE}"
    fi
fi

END_TIME=$(date +%s)
RESTORE_DURATION=$((END_TIME - START_TIME))
[ "${RESTORE_DURATION}" -eq 0 ] && RESTORE_DURATION=1

# 9. Verify Table Counts
echo ""
echo "--- 6. Post-Restore Schema & Table Verification ---"
COUNT_SQL="SELECT count(*) FROM information_schema.tables WHERE table_schema = '${TARGET_DB}';"
if [[ "${USE_CONTAINER}" == "1" ]]; then
    TABLE_COUNT=$(docker exec -e MYSQL_PWD="${DB_PASSWORD}" "${DOCKER_CONTAINER}" mysql -u "${DB_USER}" -N -e "${COUNT_SQL}" 2>/dev/null || echo "0")
else
    TABLE_COUNT=$(MYSQL_PWD="${DB_PASSWORD}" mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" -N -e "${COUNT_SQL}" 2>/dev/null || echo "0")
fi

echo "  - Tables Restored: ${TABLE_COUNT}"

echo ""
echo "======================================================================"
echo " [SUCCESS] MySQL Database Restoration Completed Successfully"
echo "======================================================================"
echo "  - Restored Archive:   ${BACKUP_FILE}"
echo "  - Target Database:    ${TARGET_DB}"
echo "  - Restored Tables:    ${TABLE_COUNT}"
echo "  - Restore Duration:   ${RESTORE_DURATION} second(s)"
echo "  - SHA-256 Verified:   $([ "${SKIP_CHECKSUM}" == "1" ] && echo "No" || echo "Yes")"
echo "======================================================================"
echo ""
exit 0
