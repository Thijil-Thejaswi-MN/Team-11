# Security Baseline — Insurance Workflow Automation System

**Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Target Deadline:** 19 October 2026  
**Last Updated:** 16 October 2026  
**Source Documents:** SRS v1.0 (INS-SR requirements), STP v1.0 (Appendix A — Security Test Cases), SAD v1.1 (§4.4 Security Architecture)

> [!IMPORTANT]
> This document tracks the **current security posture** of the Insurance Workflow Automation System against all SRS security requirements. It is a living document updated as implementation and verification progress. No requirement is marked "verified" until an independent test has been executed with evidence.

---

## 1. Scope and Objectives

This security baseline covers:

1. **All SRS security requirements** (INS-SR-001 through INS-SR-008)
2. **Mapping to STP test cases** (TC-Sec-01 through TC-Sec-11)
3. **Current implementation status** per backend/frontend codebase
4. **Verification schedule** aligned with sprint milestones
5. **Risk assessment** for requirements not yet implemented

### Out of Scope

- Penetration testing by external parties (not required for this project phase)
- Production deployment hardening (no production environment exists yet)
- Third-party dependency vulnerability scanning (deferred to CI/CD pipeline maturity)

---

## 2. Security Requirements Traceability Matrix

| Req ID | Requirement Title | STP Test Case(s) | GitHub Issue(s) | Status | Risk |
|--------|------------------|-------------------|-----------------|--------|------|
| INS-SR-001 | HTTPS and TLS 1.2+ | TC-Sec-01 | #103 | 🔲 Not Implemented | Medium — local dev uses HTTP; production will require reverse proxy |
| INS-SR-002 | Salted password hashing | TC-Sec-02 | #104, #22 | ✅ Implemented | Low — BCrypt factor 12 implemented in PasswordService; verified by unit/integration tests |
| INS-SR-003 | Role-based access control and insurer isolation | TC-Sec-03, TC-Dash-01 | #105, #95, #23 | ✅ Implemented | Low — RoleSecurity + CurrentUser + JwtAuthenticationFilter + staff insurer linkage |
| INS-SR-004 | Encryption at rest | TC-Sec-04 | #106 | ⚠️ Partial — AES-256-GCM field encryption implemented for PAN/Aadhaar in DB; file encryption in Sprint 2 | Low (database) / Medium (file store) |
| INS-SR-005 | Account lockout and session timeout | TC-Sec-05, TC-Sec-10 | #107, #112, #24 | ✅ Implemented | Low — 5-attempt lockout (15 min) + 15 min server session idle timeout in UserSession |
| INS-SR-006 | Input validation and injection defence | TC-Sec-06 | #108, #25 | ✅ Implemented | Low — Jakarta Bean Validation on all DTOs + JPA parameterized queries |
| INS-SR-007 | Secure file upload | TC-Sec-07 | #109, #27 | ⚠️ Partial — KycRecord & IKycStatus implemented; file multipart storage in Sprint 2 | Medium |
| INS-SR-008 | Append-only audit log | TC-Sec-08 | #110, #28 | 🔲 Not Implemented | Medium — IAuditLog interface defined on P1-05 branch |

---

## 3. Detailed Security Control Assessment

### 3.1 INS-SR-001 — HTTPS and TLS 1.2+

**SRS Requirement:** All communication between clients and servers MUST use HTTPS with TLS 1.2 or higher.

**Current State:**
- Local development runs on HTTP (`localhost:8080` for backend, `localhost:5173` for frontend)
- No reverse proxy (Nginx) configured in the development environment
- No TLS certificates provisioned

**Implementation Plan:**
- [ ] Configure Nginx reverse proxy with TLS termination (Person 4 — infrastructure)
- [ ] Redirect all HTTP requests to HTTPS
- [ ] Disable TLS 1.0 and TLS 1.1
- [ ] Add HSTS header (`Strict-Transport-Security`)

**Verification:** TC-Sec-01 — TLS scan using `testssl.sh` or equivalent; HTTP redirect confirmation

**Risk Level:** Medium — acceptable in local dev; must be enforced before any shared/staging deployment

---

### 3.2 INS-SR-002 — Salted Password Hashing (bcrypt/Argon2)

**SRS Requirement:** Passwords must be stored using salted hashing (bcrypt or Argon2). No plaintext passwords in storage, logs, responses, or error messages.

**Current State:**
- Backend skeleton exists but authentication module is not yet implemented
- API contract (docs/api-contract/02-auth-kyc-plans.md) specifies password handling rules
- Functional test checklist FC-01 through FC-05 prepared but unexecuted

**Implementation Plan:**
- [ ] Implement `BCryptPasswordEncoder` (or Argon2) in Spring Security configuration
- [ ] Ensure `password` field is `@JsonIgnore` and excluded from all response DTOs
- [ ] Add log-scrubbing filter to prevent password leakage in application logs
- [ ] Verify with TC-Sec-02: inspect database for hashes, search logs for plaintext

**Verification:** TC-Sec-02 — Database inspection for bcrypt hashes; log search for password values

**Risk Level:** High — core authentication security; must be correct from first implementation

---

### 3.3 INS-SR-003 — Role-Based Access Control and Insurer Isolation

**SRS Requirement:** The system must enforce role-based access control (RBAC) with deny-by-default. Insurer staff can only access records belonging to their own insurer.

**Current State:**
- ✅ `CurrentUser` identity contract designed (PR #118, branch `59-p2-02-current-user-stub`)
- ✅ `RoleSecurity` annotation and guard implemented for method-level RBAC
- ✅ `ICurrentUserProvider` interface with `TestCurrentUserProvider` for testing
- ✅ `UserRole` enum: `CUSTOMER`, `INSURER_ADMIN`, `UNDERWRITER`, `CLAIMS_OFFICER`, `SYSTEM_ADMIN`
- ⚠️ `ProductionCurrentUserProvider` is a stub (returns synthetic values) — needs JWT/session integration
- ⚠️ Insurer isolation filter not yet applied to repository queries

**Implementation Plan:**
- [ ] Complete `ProductionCurrentUserProvider` to extract identity from JWT/session
- [ ] Add `@PreAuthorize` or `RoleSecurity` annotations to all controller endpoints
- [ ] Implement insurer-scoped repository queries (WHERE `insurer_id = :currentInsurerId`)
- [ ] Verify with TC-Sec-03: RBAC matrix test for all 5 roles; cross-insurer denial

**Verification:** TC-Sec-03 — Full RBAC matrix; TC-Dash-01 — Role-specific dashboard access

**Risk Level:** High — fundamental authorization layer; partial implementation exists

---

### 3.4 INS-SR-004 — Encryption at Rest

**SRS Requirement:** Sensitive data (PAN, Aadhaar, KYC documents) must be encrypted at rest in the database and file storage.

**Current State:**
- ✅ AES-256-GCM authenticated field encryption implemented in `FieldEncryptionService` with 96-bit random IV, 128-bit authentication tag, and versioned ciphertext (`v1:...`).
- ✅ JPA AttributeConverter (`EncryptedStringConverter`) applied to `KycRecord.panNumber` and `KycRecord.aadhaarNumber`.
- ✅ Database columns store only ciphertext; native SQL inspection confirms no plaintext PAN or Aadhaar in persistent storage.
- ✅ Key management: External configuration via `insurance.security.encryption-key` with fail-secure validation refusing startup in production if missing, malformed, or insecure.
- ✅ Data compatibility and migration plan: Unencrypted legacy data cannot be read without migration (fails fast); data migration procedure documented for legacy database migration prior to cutover.
- ⚠️ Encrypted file storage for KYC document binaries is scheduled for Sprint 2 with `IFileStore` (Person 4 / issue #127).

**Implementation Plan:**
- [x] Implement JPA `@Convert` with AES-256-GCM authenticated encryption for sensitive columns (`panNumber`, `aadhaarNumber`)
- [x] Enforce fail-secure startup in production profile for encryption key configuration
- [x] Verify tamper-evident decryption failure (`AEADBadTagException`) and unique nonces
- [ ] Configure encrypted storage volume or application-level encryption for KYC binary uploads via `IFileStore` (Sprint 2)

**Verification:** TC-Sec-04 — Automated tests in `FieldEncryptionServiceTest` and `KycServiceTest` verifying ciphertext persistence, tampering defense, unique IVs, and production key fail-secure startup.

**Risk Level:** Low for database fields (mitigated by AES-256-GCM); Medium for binary file storage until `IFileStore` lands in Sprint 2.

---

### 3.5 INS-SR-005 — Account Lockout and Session Timeout

**SRS Requirement:** Lock accounts after 5 consecutive failed login attempts for 15 minutes. Sessions must time out after a defined inactivity period.

**Current State:**
- API contract specifies `423 Locked` response for locked accounts
- Functional test checklist FC-06 prepared (5 failed attempts → lockout)
- No backend implementation of lockout counter or session timeout

**Implementation Plan:**
- [ ] Track failed login attempts per user in database (counter + last failure timestamp)
- [ ] Implement lockout check in authentication flow
- [ ] Configure Spring Session or JWT with expiry + refresh mechanism
- [ ] Set server-side session inactivity timeout (SAD recommends 30 minutes)
- [ ] Verify with TC-Sec-05 (lockout) and TC-Sec-10 (session timeout)

**Verification:** TC-Sec-05 — 5 failed attempts → lockout; TC-Sec-10 — Session expires after inactivity

**Risk Level:** Medium — protects against brute-force attacks; standard security measure

---

### 3.6 INS-SR-006 — Input Validation and Injection Defence

**SRS Requirement:** All user inputs must be validated. The system must defend against SQL injection, XSS, and other injection attacks.

**Current State:**
- Spring Boot backend provides basic input binding but no explicit validation annotations
- No `@Valid` / `@Validated` annotations on controller DTOs
- No XSS sanitization filter configured
- JPA/Hibernate parameterised queries provide SQL injection protection by default

**Implementation Plan:**
- [ ] Add Jakarta Bean Validation (`@NotNull`, `@Size`, `@Email`, `@Pattern`) to all request DTOs
- [ ] Configure global exception handler to return safe 400 errors (no stack traces)
- [ ] Add XSS filter or Content-Security-Policy header
- [ ] Verify with TC-Sec-06: injection payloads (`<script>`, `' OR 1=1`, etc.)

**Verification:** TC-Sec-06 — Injection and XSS test cases

**Risk Level:** High — injection attacks are OWASP Top 10; must be addressed early

---

### 3.7 INS-SR-007 — Secure File Upload

**SRS Requirement:** File uploads (KYC documents) must enforce type whitelist (PDF, JPG, PNG), maximum size (5 MB), and content-type validation.

**Current State:**
- API contract specifies multipart upload to `POST /api/v1/kyc`
- Functional test checklist FC-09 (valid upload), FC-10 (invalid type), FC-11 (oversized) prepared
- No backend upload endpoint implemented

**Implementation Plan:**
- [ ] Implement multipart file upload endpoint with type whitelist validation
- [ ] Configure `spring.servlet.multipart.max-file-size=5MB`
- [ ] Validate file content type (not just extension) using magic bytes
- [ ] Store files in a secure location not directly web-accessible
- [ ] Verify with TC-Sec-07: valid upload, unsupported type, oversized file

**Verification:** TC-Sec-07 — File upload security tests

**Risk Level:** Medium — KYC upload is a later sprint feature; design must be secure from start

---

### 3.8 INS-SR-008 — Append-Only Audit Log

**SRS Requirement:** All significant operations must be logged in an append-only audit trail. Audit entries must not be editable or deletable by any user.

**Current State:**
- `IAuditLog` interface defined on P1-05 branch (PR #115, branch `114-quote-uw-audit-stubs`)
- No concrete audit log implementation merged to main
- Functional test checklist FC-16 prepared for audit access restrictions

**Implementation Plan:**
- [ ] Implement `IAuditLog` with JPA entity and repository
- [ ] Ensure no `UPDATE` or `DELETE` operations exist on audit table (no setter methods, no delete repository methods)
- [ ] Add audit interceptor/aspect to capture operations automatically
- [ ] Restrict audit API endpoint to `SYSTEM_ADMIN` role
- [ ] Verify with TC-Sec-08: append-only verification; TC-Aud-01: audit log content

**Verification:** TC-Sec-08 — Append-only and attribution; TC-Aud-01 — Content and access control

**Risk Level:** Medium — audit trail is a regulatory and compliance requirement

---

## 4. Security Testing Schedule

| Date | Activity | Owner | Deliverable |
|------|----------|-------|-------------|
| 15 Oct (Done) | Create TC-Sec-01 through TC-Sec-11 issues | Sharana (P2) | Issues #103–#113 ✅ |
| 16 Oct (Today) | Security baseline document draft | Sharana (P2) | This document ✅ |
| 16 Oct (Today) | RBAC guard code ready for review | Sharana (P2) | PR #118 (awaiting review) |
| 17 Oct | Security baseline review with team | All | Updated baseline |
| 17–18 Oct | Begin password hashing verification (TC-Sec-02) once auth module lands | Sharana (P2) | FC-01 through FC-06 blocked until auth module |
| 18 Oct | Input validation framework (Bean Validation on DTOs) | Person 4 | Unblocks TC-Sec-06 |
| 19 Oct | **Security baseline target** — all controls documented, partial verification | Sharana (P2) | Updated baseline with evidence |
| Sprint 2+ | Execute all TC-Sec test cases as features are implemented | Sharana (P2) | Test execution records |

---

## 5. Blocked Items and Dependencies

| Blocker | Blocks | Owner | Expected Resolution |
|---------|--------|-------|---------------------|
| Auth module not implemented | TC-Sec-02, TC-Sec-05, TC-Sec-10, FC-01 through FC-06 | Person 4 (backend) | Sprint 2 |
| PR #118 awaiting review | RBAC guards on main branch | Team reviewer | ASAP |
| KYC upload and document storage | TC-Sec-07, FC-09 through FC-11, INS-SR-004 (files) | Person 4 (IFileStore / PR #127) | Sprint 2 |
| Audit log implementation | TC-Sec-08, FC-16 | Person 1 (IAuditLog) | Sprint 2 |
| Nginx/TLS not configured | TC-Sec-01 | Person 4 (infra) | Sprint 3 |

---

## 6. Risk Summary

| Risk Level | Count | Requirements |
|-----------|-------|-------------|
| 🔴 High | 3 | INS-SR-002, INS-SR-003, INS-SR-006 |
| 🟡 Medium | 5 | INS-SR-001, INS-SR-004, INS-SR-005, INS-SR-007, INS-SR-008 |
| 🟢 Low | 0 | — |

### Top Risks

1. **Password hashing (INS-SR-002):** No implementation exists. First priority when auth module begins.
2. **RBAC enforcement (INS-SR-003):** Guard framework ready (PR #118) but not applied to endpoints yet.
3. **Input validation (INS-SR-006):** No DTO validation in place. SQL injection mitigated by JPA but XSS and business rule validation missing.

---

## 7. Approval and Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Functional Test Engineer (P2) | Sharana | 16 Oct 2026 | Prepared |
| Backend Lead (P4) | Raghunandan | | |
| Architecture / Contracts (P1) | Thijil | | |
| Data Model (P3) | Samarth | | |

---

*This document will be updated as security controls are implemented and verified. Next update expected: 17 October 2026 after team review.*
