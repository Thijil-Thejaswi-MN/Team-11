#!/usr/bin/env bash
# ==============================================================================
# Resets the test database to a clean state and seeds volume data
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
SCALE="${1:-standard}"

echo "=== Team-11 Test Database Reset & Volume Seeding Utility ==="
echo "Target Scale: ${SCALE}"

if [[ "${RECREATE_CONTAINERS:-0}" == "1" ]]; then
    echo "[INFO] Recreating database container and volume..."
    docker compose -f "${PROJECT_ROOT}/docker-compose.yml" down -v
    docker compose -f "${PROJECT_ROOT}/docker-compose.yml" up -d db

    echo "[INFO] Waiting for MySQL container to become healthy..."
    until [ "$(docker inspect --format='{{.State.Health.Status}}' insurance-mysql8 2>/dev/null)" == "healthy" ]; do
        sleep 2
    done
fi

if [[ "${SCALE}" != "canonical" ]]; then
    SQL_FILE="${SCRIPT_DIR}/volume-data.sql"
    echo "[INFO] Generating volume SQL dataset (${SCALE})..."
    python3 "${SCRIPT_DIR}/seed-volume-data.py" --scale "${SCALE}" --output "${SQL_FILE}"

    echo "[INFO] Importing volume data into MySQL..."
    docker compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T db mysql -u root -proot insurance_db < "${SQL_FILE}"
    rm -f "${SQL_FILE}"
    echo "[SUCCESS] Volume dataset seeded successfully."
else
    echo "[INFO] Canonical seed data is automatically managed by Spring Boot TestDataLoader upon startup."
fi

echo "=== Database Reset Complete ==="
