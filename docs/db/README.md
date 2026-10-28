# Database Audit Roles & Privilege Hardening

**Work Item:** P2-18 / INS-SR-008 / INS-F-018  
**Author:** Person 2 (Sharana — Functional Test Engineer)  
**Database Target:** MySQL 8.0+  
**Script Path:** [`docs/db/audit-roles.sql`](docs/db/audit-roles.sql)  
**Testing References:** TC-Aud-01, TC-Sec-08  

---

## 1. Architecture Overview

Under **INS-SR-008** and **SAD §4.3.7**, audit trail integrity requires defense-in-depth:
1. **Application Layer:** `AuditLogRepository` extends `Repository` (not `CrudRepository` or `JpaRepository`), with zero delete/update methods. The `AuditLog` entity sets `updatable = false` on every column and provides no setters.
2. **Database Engine Layer (MySQL 8):** Database-level access control roles guarantee that even direct SQL executions or compromised database credentials cannot mutate or delete audit log records.

---

## 2. Role Specifications

| Role Name | Table | Allowed Privileges | Prohibited Operations | Purpose |
|---|---|---|---|---|
| `audit_writer` | `insurance_db.audit_logs` | `INSERT`, `SELECT` | `UPDATE`, `DELETE`, `DROP`, `TRUNCATE` | Assigned to application service user to record new immutable audit entries and verify writes. |
| `audit_reader` | `insurance_db.audit_logs` | `SELECT` | `INSERT`, `UPDATE`, `DELETE`, `DROP`, `TRUNCATE` | Assigned to read-only compliance officers, security auditors, and analytical report generators. |

---

## 3. How Application Users Map to Database Roles

In accordance with team decision **D-03**, the development and automated testing environments continue running their agreed datasource setups without unilateral alteration.

In a production or hardened staging environment:
1. Run `docs/db/audit-roles.sql` as a DBA user (`root` or administrative user).
2. Map the application database user (e.g. `insurance_app`) to `audit_writer`:
   ```sql
   GRANT 'audit_writer'@'%' TO 'insurance_app'@'%';
   SET DEFAULT ROLE 'audit_writer'@'%' TO 'insurance_app'@'%';
   ```
3. Map read-only compliance auditor accounts (e.g. `compliance_auditor`) to `audit_reader`:
   ```sql
   GRANT 'audit_reader'@'%' TO 'compliance_auditor'@'%';
   SET DEFAULT ROLE 'audit_reader'@'%' TO 'compliance_auditor'@'%';
   ```

When the application connects using `insurance_app`, it possesses privilege to execute `INSERT` and `SELECT` on `audit_logs`, but any `UPDATE` or `DELETE` statement is rejected with MySQL error `1142 (42000): UPDATE command denied to user 'insurance_app'@'host' for table 'audit_logs'`.
