# Database Backup & Disaster Recovery Runbook (P4-14 / TC-Rel-01)

**System:** Team-11 Insurance Workflow Automation System  
**Requirement Reference:** `INS-NF-004` (Backup and Recovery)  
**Test Case Reference:** `TC-Rel-01` (Recovery Time & Objective Drill)  
**Author:** Person 4 (DevOps & Environment Engineer)  
**Version:** 1.0  
**Last Updated:** October 2026  

---

## 1. Executive Summary & Recovery Objectives

To guarantee business continuity, data protection, and compliance with architectural requirements, the Team-11 Insurance Workflow Automation System implements a fully automated, cross-platform MySQL database backup and recovery subsystem.

### Service Level Objectives (SLOs)

| Metric | Target Specification | Operational Ceiling | Verification Mechanism |
|:---|:---:|:---:|:---|
| **Recovery Point Objective (RPO)** | $\le$ 24 hours | 24 hours | Daily scheduled snapshot timestamp delta verification |
| **Recovery Time Objective (RTO)** | $\le$ 2 hours | 4 hours | Automated end-to-end recovery drill (`recovery-drill.ps1` / `.sh`) |
| **Backup Compression** | Gzip (`.sql.gz`) | Level 9 / Optimal | Streamed compression reduces storage footprint $>70\%$ |
| **Integrity Verification** | SHA-256 Checksum | Mandatory match | Hash validation before any restore or database modification |
| **Transaction Consistency** | ACID consistent snapshot | `--single-transaction` | Non-blocking snapshot execution on InnoDB tables |

---

## 2. Architecture & File Structure

The backup and recovery subsystem consists of dual-platform scripts (Windows PowerShell 5.1+ and POSIX Bash 4+) designed to work seamlessly both against the Docker Compose test environment (`insurance-mysql8`) and native standalone MySQL client installations.

```
scripts/
├── backup-database.ps1           # Windows PowerShell full backup & checksum utility
├── backup-database.sh            # POSIX Bash full backup & checksum utility
├── restore-database.ps1          # Windows PowerShell verified restore & dry-run utility
├── restore-database.sh           # POSIX Bash verified restore & dry-run utility
├── recovery-drill.ps1            # Windows PowerShell 5-phase TC-Rel-01 automated drill
├── recovery-drill.sh             # POSIX Bash 5-phase TC-Rel-01 automated drill
└── test-backup-recovery-suite.ps1# 12-test automated verification suite

backups/                          # Backup storage directory (gitignored)
└── .gitkeep                      # Git directory anchor
```

### Generated Artifacts per Backup Run

For each backup execution, three synchronized artifacts are produced in the designated backup directory:

1. **Compressed Database Dump (`backup_<db>_<timestamp>.sql.gz`)**: Full database DDL, table structures, constraints, and data streamed into Gzip.
2. **SHA-256 Checksum File (`backup_<db>_<timestamp>.sql.gz.sha256`)**: Standard GNU sha256 format (`<hash>  <filename>`) encoded in UTF-8 without BOM.
3. **Structured Metadata (`backup_<db>_<timestamp>.meta.json`)**: Machine-readable JSON manifest containing archive name, database name, UTC timestamp, file sizes, compression ratio, backup duration, target RPO/RTO, and source mode.

---

## 3. Backup Procedure

### 3.1 Characteristics
- **Transactional Consistency**: Uses `mysqldump --single-transaction --quick --routines --triggers --events` to produce a point-in-time consistent dump without locking tables during production read operations.
- **Direct Streaming Compression**: Output from `mysqldump` is piped directly into Gzip compression in memory without leaving unencrypted or uncompressed plaintext SQL dumps on disk.
- **Fail-Secure Clean Up**: If the backup process fails or is interrupted, partial artifacts are immediately deleted.
- **Dry-Run Validation**: Supports `-DryRun` / `--dry-run` to validate container reachability, credentials, and write permissions without generating dump files.

### 3.2 Backup Execution Commands

#### Windows PowerShell
```powershell
# Standard backup (auto-detects insurance-mysql8 Docker container)
.\scripts\backup-database.ps1

# Custom target database and output directory
.\scripts\backup-database.ps1 -Database "insurance_db" -BackupDir "./backups"

# Dry-run configuration validation
.\scripts\backup-database.ps1 -DryRun

# Direct connection via host MySQL client (non-container mode)
.\scripts\backup-database.ps1 -DirectClient -HostName "127.0.0.1" -Port 3306 -User "root"
```

#### Linux / macOS / Git Bash
```bash
# Ensure execution permissions
chmod +x scripts/backup-database.sh

# Standard backup
./scripts/backup-database.sh

# Custom database and output path
./scripts/backup-database.sh --db insurance_db --out ./backups

# Dry-run validation
./scripts/backup-database.sh --dry-run
```

---

## 4. Restore & Recovery Procedure

### 4.1 Safety Safeguards
1. **File Sanity Checks**: Validates backup archive file existence and verifies that the archive size is non-zero.
2. **Pre-Restoration SHA-256 Verification**: Computes the SHA-256 hash of the archive and compares it against `.sha256` file or `.meta.json`. If a mismatch is detected, execution aborts with a fatal exit code before contacting the database.
3. **Decompression Integrity Test**: Pre-tests Gzip stream integrity (`gzip -t` / .NET `GZipStream`) to detect truncated or corrupted files.
4. **Interactive Confirmation**: Prompts the operator with `Are you sure you want to proceed with database restoration? (yes/no)` to prevent accidental overwrites. Bypassed only when `-Force` / `--force` / `RESTORE_FORCE=1` is explicitly provided.
5. **Disposable Drill Database Support**: Operators can restore into a disposable target database (e.g., `insurance_db_drill`) to verify disaster recovery capabilities without destroying active testing data.
6. **Offline Archive Validation**: The `-VerifyOnly` / `--verify-only` flag runs all file, checksum, and decompression tests without requiring an active database daemon or network connection.

### 4.2 Restore Execution Commands

#### Windows PowerShell
```powershell
# Offline integrity and checksum verification only (no DB connection required)
.\scripts\restore-database.ps1 -BackupFile "backups/backup_insurance_db_20261030_120000.sql.gz" -VerifyOnly

# Interactive restore to main test database
.\scripts\restore-database.ps1 -BackupFile "backups/backup_insurance_db_20261030_120000.sql.gz"

# Non-destructive recovery to disposable drill database (bypassing confirmation)
.\scripts\restore-database.ps1 -BackupFile "backups/backup_insurance_db_20261030_120000.sql.gz" -TargetDatabase "insurance_db_drill" -Force

# Pre-flight check with dry run
.\scripts\restore-database.ps1 -BackupFile "backups/backup_insurance_db_20261030_120000.sql.gz" -DryRun
```

#### Linux / macOS / Git Bash
```bash
# Ensure execution permissions
chmod +x scripts/restore-database.sh

# Offline integrity and checksum verification
./scripts/restore-database.sh backups/backup_insurance_db_20261030_120000.sql.gz --verify-only

# Interactive restore
./scripts/restore-database.sh backups/backup_insurance_db_20261030_120000.sql.gz

# Non-destructive restore to disposable drill database
./scripts/restore-database.sh backups/backup_insurance_db_20261030_120000.sql.gz --target-db insurance_db_drill -y
```

---

## 5. Automated Recovery Drill (TC-Rel-01)

The recovery drill orchestrates a complete 5-phase non-destructive disaster recovery exercise designed to satisfy test case **TC-Rel-01**:

```
[Phase 1] Full Database Backup
    └── Capture live snapshot of insurance_db -> .sql.gz + .sha256 + .meta.json
[Phase 2] Corruption & Tamper Rejection Test
    └── Deliberately alter 1 byte in a scratch archive and assert restore tool ABORTS
[Phase 3] Non-Destructive Disposable Database Restore
    └── Restore verified archive into insurance_db_drill
[Phase 4] Schema & Data Integrity Verification
    └── Query row counts across users, insurers, policies, and claims
[Phase 5] SLA Assessment & Audit Summary
    └── Calculate actual RTO / RPO and compare against targets (RPO <= 24h, RTO <= 4h)
```

### Running the Drill

```powershell
# Windows PowerShell
.\scripts\recovery-drill.ps1 -SourceDatabase "insurance_db" -DrillDatabase "insurance_db_drill"
```

```bash
# POSIX Bash
./scripts/recovery-drill.sh --source-db insurance_db --drill-db insurance_db_drill
```

---

## 6. Automated Test Suite

A standalone test suite (`scripts/test-backup-recovery-suite.ps1`) verifies all scripts without modifying production data or requiring a running database:

1. **PowerShell Syntax Parsing**: Validates syntax of `backup-database.ps1`, `restore-database.ps1`, and `recovery-drill.ps1`.
2. **Bash Syntax Verification**: Validates `bash -n` on `backup-database.sh`, `restore-database.sh`, and `recovery-drill.sh`.
3. **Error Handling**: Asserts that non-existent and 0-byte archives are rejected with exit code 1.
4. **Integrity Verification**: Asserts that valid archives match SHA-256 checksums in both PowerShell (`-VerifyOnly`) and Bash (`--verify-only`).
5. **Corruption Rejection**: Bit-flips an archive byte and asserts that both PowerShell and Bash abort with exit code 1 and checksum failure notices.
6. **Environment Reporting**: Reports Docker daemon status and marks live recovery drill status accurately.

### Running the Test Suite

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-backup-recovery-suite.ps1
```

Expected result: `12 Passed, 0 Failed`.

---

## 7. Security & Operational Best Practices

1. **Credential Isolation**: Passwords are supplied via environment variables (`DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PWD`) and never logged or printed in output.
2. **Repository Exclusion**: All backup archives (`*.sql.gz`), checksums (`*.sha256`), metadata (`*.meta.json`), and generated SQL datasets are explicitly ignored in `.gitignore`.
3. **Disaster Recovery Drills**: Scheduled quarterly to run `recovery-drill.ps1` against staging environments to ensure ongoing compliance with `INS-NF-004`.
4. **Backup Storage Rotation**: In production deployments, backups should be replicated to a remote write-once-read-many (WORM) storage bucket with a 30-day retention policy.
