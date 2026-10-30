-- ==============================================================================
-- Team 11 — Insurance Workflow Automation System
-- Work Item: P2-18 Audit Log Hardening (INS-SR-008, INS-F-018, TC-Aud-01, TC-Sec-08)
-- File: docs/db/audit-roles.sql
-- Compatibility: MySQL 8.0+
-- Description:
--   Creates immutable, append-only database roles for the audit logging subsystem.
--   Enforces database-tier tamper protection such that:
--     1. audit_writer has INSERT and SELECT on audit_logs only (no UPDATE/DELETE).
--     2. audit_reader has SELECT only on audit_logs (no INSERT/UPDATE/DELETE).
--   Note: Does NOT modify the running application datasource configuration (D-03).
-- ==============================================================================

USE `insurance_db`;

-- 1. Create dedicated database roles
CREATE ROLE IF NOT EXISTS 'audit_writer'@'%';
CREATE ROLE IF NOT EXISTS 'audit_reader'@'%';

-- 2. Grant permissions to audit_writer
-- Strictly permit INSERT and SELECT; omit and deny UPDATE, DELETE, DROP, TRUNCATE
GRANT SELECT, INSERT ON `insurance_db`.`audit_logs` TO 'audit_writer'@'%';

-- 3. Grant permissions to audit_reader
-- Strictly permit SELECT only for read-only audit inquiries and reporting
GRANT SELECT ON `insurance_db`.`audit_logs` TO 'audit_reader'@'%';

-- Flush privileges to apply changes immediately
FLUSH PRIVILEGES;

-- ==============================================================================
-- Example User Mapping (Production Blueprint)
-- ==============================================================================
-- Application runtime service account mapping:
-- CREATE USER IF NOT EXISTS 'insurance_app'@'%' IDENTIFIED BY 'StrongAppPassword123!';
-- GRANT 'audit_writer'@'%' TO 'insurance_app'@'%';
-- SET DEFAULT ROLE 'audit_writer'@'%' TO 'insurance_app'@'%';

-- Auditor / compliance reporting read-only account mapping:
-- CREATE USER IF NOT EXISTS 'compliance_auditor'@'%' IDENTIFIED BY 'AuditorPass456!';
-- GRANT 'audit_reader'@'%' TO 'compliance_auditor'@'%';
-- SET DEFAULT ROLE 'audit_reader'@'%' TO 'compliance_auditor'@'%';

-- ==============================================================================
-- Role Verification Queries
-- ==============================================================================
-- SHOW GRANTS FOR 'audit_writer'@'%';
-- SHOW GRANTS FOR 'audit_reader'@'%';
