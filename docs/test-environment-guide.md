# Test Environment & Infrastructure Guide (P4-11)

**Owner:** Raghunandan Kotary (Person 4 â€” Environment Owner / Developer)
**Applicability:** Team-11 Insurance Workflow System
**Traceability:** INS-SR-001 (Transport Security), INS-NF-002 (Availability 99%), INS-NF-003 (Sizing for 500 Concurrent Users), TC-Sec-01, TC-Load-01, TC-Perf-01, TC-Avail-01.

---

## 1. Architecture Overview

The P4-11 test environment provisions a containerized, production-like deployment topology using Docker Compose, fronted by an Nginx reverse proxy enforcing TLS 1.2+ encryption, HTTP Strict Transport Security (HSTS), and load balancing across two independent Spring Boot backend instances.

```
                              Client (Browser / Newman / JMeter)
                                             |
                                  Port 80 (HTTP) -> 301 Redirect
                                  Port 443 (HTTPS) TLS 1.2 / TLS 1.3
                                             v
                           +-----------------------------------+
                           |        Nginx Reverse Proxy        |
                           |   - TLS Termination (1.2/1.3)     |
                           |   - HSTS Header (31536000s)       |
                           |   - Gateway Health (/healthz)     |
                           +-----------------+-----------------+
                                             |
                     +-----------------------+-----------------------+
                     | Path: /                                       | Path: /api/v1/
                     v                                               v
       +----------------------------+                 +-----------------------------+
       |   Frontend Container       |                 | Backend Cluster (least_conn)|
       |   - Nginx Static Host      |                 +--------------+--------------+
       |   - React SPA bundle       |                                |
       +----------------------------+               +----------------+---------------+
                                                    |                                |
                                                    v                                v
                                     +-----------------------------+  +-----------------------------+
                                     |    backend-1 (Port 8080)    |  |    backend-2 (Port 8080)    |
                                     | - Temurin 17 JRE Runtime    |  | - Temurin 17 JRE Runtime    |
                                     | - HikariCP Pool: <= 25 conn |  | - HikariCP Pool: <= 25 conn |
                                     | - Health: /actuator/health  |  | - Health: /actuator/health  |
                                     +--------------+--------------+  +--------------+--------------+
                                                    |                                |
                                                    +----------------+---------------+
                                                                     |
                                                                     v
                                                      +-----------------------------+
                                                      |     MySQL 8.0 Database      |
                                                      |   - max_connections = 200   |
                                                      |   - Persistent volume       |
                                                      |   - UTF8mb4 character set   |
                                                      +-----------------------------+
```

### Key Topology Characteristics

1. **Nginx Reverse Proxy (`nginx:1.27-alpine`):**
   - **TLS 1.2 & TLS 1.3 Only:** TLS 1.0 and TLS 1.1 are explicitly disabled (`ssl_protocols TLSv1.2 TLSv1.3;`).
   - **Modern Cipher Suites:** Forward-secret ECDHE/RSA/ECDSA ciphers with AEAD encryption (GCM/CHACHA20).
   - **HTTP-to-HTTPS Redirection:** Port 80 unconditionally issues `301 Moved Permanently` to `https://$host$request_uri`.
   - **Security Headers:**
     - `Strict-Transport-Security: max-age=31536000; includeSubDomains` (HSTS).
     - `X-Content-Type-Options: nosniff`.
     - `X-Frame-Options: DENY`.
     - `X-XSS-Protection: 1; mode=block`.
     - `Referrer-Policy: strict-origin-when-cross-origin`.
   - **Upstream Load Balancing:** Directs `/api/v1/` traffic to `backend_cluster` using `least_conn` distribution between `backend-1` and `backend-2`.
   - **Gateway Health Check:** Dedicated `/healthz` endpoint returning gateway status and TLS capabilities without requiring database connectivity.

2. **Dual Backend Application Instances (`backend-1`, `backend-2`):**
   - Packaged as multi-stage Eclipse Temurin 17 JRE container images with unprivileged user `appuser`.
   - Both instances connect to the shared MySQL 8 database with distinct `INSTANCE_ID` metadata (`backend-1` and `backend-2`).
   - Independent Docker health checks continuously poll `http://localhost:8080/api/v1/actuator/health`.
   - Configured with `restart: unless-stopped` to satisfy INS-NF-002 availability requirements.

3. **Database Connection Pool Sizing (SAD Â§5.1, INS-NF-003):**
   - **MySQL 8 Server:** `max_connections=200`.
   - **HikariCP Pool:** `maximum-pool-size: 25` per backend instance (`application-dev.yml`).
   - **Total Pool Demand:** $2 \times 25 = 50$ connections, consuming only 25% of the database capacity and preventing connection exhaustion under 500 concurrent users during JMeter stress testing (TC-Load-01).

4. **Frontend Single-Page Application (`frontend`):**
   - Multi-stage build (Node 20 Alpine builder -> Nginx 1.27 Alpine runtime).
   - Serves pre-compiled production assets with client-side SPA routing support (`try_files $uri $uri/ /index.html;`).

5. **Security Keys & Encryption Management (INS-SR-004, SAD §4.4):**
   - **Environment Variable:** `ENCRYPTION_KEY` mapped to `insurance.security.encryption-key`.
   - **Supported Key Formats:** Both `FieldEncryptionService` and `DiskFileStore` accept 256-bit AES keys in any of three formats:
     1. *64-character Hexadecimal:* (e.g. `openssl rand -hex 32` or dev default `0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef`).
     2. *44-character Base64:* 32-byte Base64-encoded string (e.g. `openssl rand -base64 32`).
     3. *32-byte UTF-8 String:* Exactly 32 ASCII characters (e.g. `dev-aes256-key-team11-workflow!!`).
   - **Fail-Secure Behavior:**
     - In `dev` or `test` profile, omitting `ENCRYPTION_KEY` triggers a clear warning log and safely falls back to a development key.
     - In `prod` or `production` profile, missing, malformed, or insecure keys (including all known dev/test keys) halt application startup with a `CRITICAL SECURITY VIOLATION` exception.
     - Production deployments must inject a high-entropy 256-bit key via `ENCRYPTION_KEY`.

---

## 2. Test Certificate Provisioning

For local development, integration, and security testing, a reproducible self-signed X.509 certificate with Subject Alternative Names (SAN) is generated.

### Automated Generation Scripts

- **Windows PowerShell:** `scripts/generate-test-certs.ps1`
- **Linux / macOS / WSL:** `scripts/generate-test-certs.sh`

### Included Subject Alternative Names (SAN)
- `localhost`
- `127.0.0.1`
- `host.docker.internal`
- `insurance.local`

### Execution

```powershell
# PowerShell
.\scripts\generate-test-certs.ps1
```

```bash
# Bash / Linux / macOS
chmod +x scripts/generate-test-certs.sh
./scripts/generate-test-certs.sh
```

The script outputs `server.key` (2048-bit RSA private key) and `server.crt` (X.509 certificate valid for 365 days) into `nginx/certs/`.

### Certificate Trust & Security Notice

> [!WARNING]
> **Test Certificates Only â€” Do Not Commit Secrets**
> - The files `nginx/certs/*.key` and `nginx/certs/*.crt` are explicitly excluded from version control via `.gitignore`.
> - These certificates are self-signed for automated test environments. Browsers and HTTP clients will report an untrusted certificate authority unless imported into the OS trust store or bypassed using `-k` / `--insecure`.
> - In staging and production environments, replace `server.key` and `server.crt` with certificates issued by a publicly trusted Certificate Authority (e.g., Let's Encrypt or corporate CA).

---

## 3. Environment Lifecycle

### Prerequisites
- Docker Engine 24.0+ and Docker Compose v2.20+
- OpenSSL (optional, if regenerating certificates)
- Python 3.9+ (optional, if generating synthetic test data volume)

### Step-by-Step Startup

1. **Generate Certificates:**
   ```powershell
   .\scripts\generate-test-certs.ps1
   ```

2. **Build and Launch All Services:**
   ```bash
   docker compose up -d --build
   ```

3. **Verify Container Status:**
   ```bash
   docker compose ps
   ```
   All 5 services (`db`, `backend-1`, `backend-2`, `frontend`, `nginx`) should report `Up` with healthy status.

4. **View Service Logs:**
   ```bash
   docker compose logs -f nginx
   docker compose logs -f backend-1 backend-2
   ```

### Graceful Teardown
```bash
# Stop and remove containers and networks
docker compose down

# To also clear the database volume (clean reset)
docker compose down -v
```

---

## 4. Health and Verification Testing

### Automated Verification Test Suite

Run the end-to-end verification script:

```powershell
# Windows PowerShell
.\scripts\verify-tls-environment.ps1 -TargetHost "localhost"
```

```bash
# Bash
chmod +x scripts/verify-tls-environment.sh
./scripts/verify-tls-environment.sh localhost
```

The test suite validates:
1. HTTP (port 80) redirects to HTTPS (port 443) with status code `301 Moved Permanently`.
2. HTTPS connects via TLS 1.2 (`ECDHE-RSA-AES256-GCM-SHA384` or equivalent).
3. HTTPS connects via TLS 1.3 (`TLS_AES_256_GCM_SHA384` or equivalent).
4. Deprecated protocol TLS 1.0 connection is actively refused.
5. Deprecated protocol TLS 1.1 connection is actively refused.
6. HSTS response header `Strict-Transport-Security: max-age=31536000; includeSubDomains` is present.
7. Gateway health endpoint `/healthz` returns HTTP 200 and valid JSON.
8. Backend health endpoint `/api/v1/actuator/health` returns HTTP 200 with database status `UP`.
9. Frontend SPA index page is served over HTTPS.

### Manual Verification Commands

```bash
# 1. Verify HTTP to HTTPS redirect
curl -I http://localhost/

# 2. Verify HSTS and security headers
curl -k -I https://localhost/healthz

# 3. Verify TLS 1.2 handshake
openssl s_client -connect localhost:443 -tls1_2 -brief

# 4. Verify TLS 1.3 handshake
openssl s_client -connect localhost:443 -tls1_3 -brief

# 5. Verify TLS 1.0 rejection (must fail)
openssl s_client -connect localhost:443 -tls1 -brief

# 6. Verify TLS 1.1 rejection (must fail)
openssl s_client -connect localhost:443 -tls1_1 -brief

# 7. Check backend cluster health through proxy
curl -k -s https://localhost/api/v1/actuator/health | jq .
```

---

## 5. High-Volume Test Data Generation & Reset

To satisfy Person 3's requirements for **TC-Perf-01**, **TC-Load-01** (500 concurrent virtual users), and **TC-Rel-01** (database backup and restore drill), a repeatable synthetic data loader and one-command reset script are provided.

### Synthetic Data Specifications
- **500 KYC-verified Customer Users:** Usernames `loadtest_user_0001` to `loadtest_user_0500`, all provisioned with standard test password `Password123!` (hashed via BCrypt work factor 12) and verified KYC records.
- **10,000 Insurance Policies / Plans:** Distributed across registered insurers (Care Health, Star Health, ICICI Lombard) with active status.
- **5,000 Claims:** Distributed across policies with varied statuses (`SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `SETTLED`).

### One-Command Reset

```powershell
# Windows PowerShell
.\scripts\reset-test-database.ps1 -GenerateVolumeData
```

```bash
# Bash
chmod +x scripts/reset-test-database.sh
./scripts/reset-test-database.sh --volume
```

This procedure:
1. Re-initializes the database schema using Flyway / JPA migrations.
2. Generates bulk SQL inserts via `scripts/seed-volume-data.py`.
3. Loads the volume dataset directly into the active MySQL container.
4. Restarts backend instances to reset connection pools and application state.

---

## 6. Route & Port Reference

| Port | Service | Protocol | Description |
|:---:|:---|:---:|:---|
| `80` | `nginx` | HTTP | Redirects all traffic to HTTPS (301) |
| `443` | `nginx` | HTTPS | Primary TLS ingress (TLS 1.2 / 1.3, HSTS) |
| `3306` | `db` | MySQL | Database (bound to localhost for developer inspection) |
| Internal `8080` | `backend-1` | HTTP | Backend instance 1 (private Docker network) |
| Internal `8080` | `backend-2` | HTTP | Backend instance 2 (private Docker network) |
| Internal `80` | `frontend` | HTTP | Frontend static server (private Docker network) |

### Ingress Path Routing

| Path | Destination Upstream | Description |
|:---|:---|:---|
| `/` | `frontend_upstream:80` | React Single Page Application bundle |
| `/api/v1/` | `backend_cluster:8080` | Load-balanced Spring Boot REST API |
| `/api/v1/actuator/health` | `backend_cluster:8080` | Spring Boot Actuator health endpoint |
| `/healthz` | Nginx internal | Reverse proxy gateway health probe |
