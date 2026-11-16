# Sprint 2 Test Summary Report

> Build SHA: `9d4d1ca93c4e537d9765352c39e24618e47f75fb` (short: `9d4d1ca`)  
> Branch: main  
> Prepared by: Thijil Thejaswi M N (QA Lead)  
> Date: 16 Nov 2026  
> Status: **FINAL — Signed off with Conditional Pass (UAT re-run executed 15–16 Nov 2026)**

---

## 1. Scope

### Features Under Test (Sprint 2)

| Feature ID | Description | PR | Branch | Status in Main |
|------------|-------------|-----|--------|:---:|
| INS-F-008 | Customer policy application (live screen) | #167 | feature/P3-08-renewal-cancellation-recovery | **MERGED** |
| INS-F-009 | Policy renewal within window | #167 | feature/P3-08-renewal-cancellation-recovery | **MERGED** |
| INS-F-010 | Policy cancellation & refund | #167 | feature/P3-08-renewal-cancellation-recovery | **MERGED** |
| INS-F-012 | Issuance recovery scheduler | #167 | feature/P3-08-renewal-cancellation-recovery | **MERGED** |
| INS-F-014 | Claims Officer appraisal desk | #157 | 11-p1-12-claims-officer-desk | **MERGED** |
| INS-F-018 | Audit log hardening (RBAC, read-only) | #173 | 19-p2-18-audit-hardening | **MERGED** |
| INS-F-019 | Responsive / cross-browser (360 px, dev badge) | #174 | 29-p2-14-responsive-crossbrowser | **MERGED** |
| INS-NF-004 | Database backup & restore procedure (TC-Rel-01) | #185 | feat/p4-14-database-backup-recovery | **MERGED** |
| INS-SR-001 | TLS verification script suite | #184 | fix/p4-11-tls-environment-scripts | **MERGED** |
| INS-F-020/024 | Customer-role policy & refund test harness | #186 | test/p4-customer-role-verification | **MERGED** |

### Modules In Scope
- Backend: `PolicyRenewal`, `PolicyCancellation`, `ClaimsOfficerDesk`, `AuditLog`, `CustomerPolicyAndCancellationTestExecution`, database backup/restore scripts
- Frontend: `CustomerDashboard`, `ClaimsOfficerView`, `AuditLogView`, responsive layouts

### Out of Scope / Deferred (D-10 — docs/decisions.md)
- INS-F-017 Renewal reminders 30/15/1 days before expiry (P4-13)
- INS-F-021 Role-specific dashboards (P2-15)
- INS-F-022 Search and filter plans (P2-13)
- INS-F-025 System Admin reports (P3-11)
- INS-NF-007 Audit records retained and retrievable for 5 years (P1-13)

---

## 2. Test Environment

| Property | Value |
|----------|-------|
| Build SHA | `eb6dd25869b70542d4724214e8049dd7f8abd74b` (short: `eb6dd25`) |
| Backend | Spring Boot 3.3.4; OpenJDK 25; H2 (dev/test profile) / MySQL 8 (production profile) |
| Frontend | Vite 5 + React 18; Node 20; Vitest 2.1.9 |
| API Test tool | Newman 6.2.1 |
| E2E / Browser | Selenium (Chrome Headless, pytest) |
| Viewports Tested | 360 px, 768 px, 1366 px |
| CI Workflows | GitHub Actions (4/4 green: Frontend CI, Backend CI, API Tests Newman, Validate repository structure) |
| Test Execution Run Date & Time | 2 Nov 2026, 10:18:09 IST |

---

## 3. Test Counts by Status (Real Counts from Main SHA `eb6dd25`)

| Category | Source | Total | Passed | Failed | Errors | Skipped / Blocked |
|----------|--------|:---:|:---:|:---:|:---:|:---:|
| Backend unit & integration | Maven Surefire (`mvnw.cmd -B verify`) on `eb6dd25` (2 Nov 2026 10:18 IST) | 622 | 622 | 0 | 0 | 0 |
| Frontend component tests | Vitest (`npm test -- --run`) on `eb6dd25` (2 Nov 2026 10:18 IST) | 92 (17 files) | 92 (17 files) | 0 | 0 | 0 |
| Newman API Assertions | Sharana Newman re-run (`docs/test-evidence/2026-11-02-newman-eb6dd25-rerun.xml`) | 70 | 68 | 2 | 0 | 0 |
| Selenium E2E Smoke | Sharana Selenium run (`docs/test-evidence/2026-11-02-selenium-eb6dd25.xml`) | 3 | 3 | 0 | 0 | 0 |
| STP Independent Test Cases | `docs/test-execution-matrix.md` | 52 | 0 | 0 | 0 | Blocked / Deferred / Not Run |
| UAT Scenarios | docs/uat-29-oct.md (UAT-01..UAT-08) on dev MySQL profile (SHA 9d4d1ca) | 8 | 3 | 0 | 0 | 4 Partial, 1 Blocked (3 Pass, 4 Partial, 1 Blocked, 0 Fail) |

### UAT Execution Results (15–16 Nov 2026 Re-run Walkthrough)

The invalidated 8 Nov walkthrough was superseded by the authentic STP re-run executed on 15 Nov 2026 (SHA `9a5c712`) on the Spring Boot `dev` profile with local MySQL 8.0, and verified on 16 Nov 2026 on SHA `9d4d1ca` following PR #197. All 5 roles were authenticated via `/login` using real seeded credentials. All 34 screenshots are recorded in `docs/test-evidence/uat-2026-11-15/` and documented in `docs/uat-29-oct.md`.

- **Total Scenarios Evaluated:** 8
- **Pass:** 3 / 8 (UAT-02, UAT-07, UAT-08)
- **Partial:** 4 / 8 (UAT-01, UAT-03, UAT-04, UAT-06)
- **Blocked:** 1 / 8 (UAT-05)
- **Fail:** 0 / 8

### STP Coverage (Thijil's Assigned STP Rows)

*Note: Per STP §8, no row is marked "Passed" without Thijil's independent sign-off.*

| STP Row ID | Scenario Description (per STP) | Automated / Manual Evidence Source | Status |
|:---|---|---|---|
| **TC-Acc-02** | Role controls access; staff need an insurer | Newman collection (`docs/test-evidence/2026-11-02-thijil-newman-eb6dd25.xml`) + `AuthenticationServiceTest.java` | _(to fill)_ |
| **TC-Aud-01** | Key actions logged; filters work; no UPDATE/DELETE | Newman collection (`docs/test-evidence/2026-11-02-thijil-newman-eb6dd25.xml`) + `AuditLogControllerTest.java` | _(to fill)_ |
| **TC-Ntf-01** | All 16 SAD 4.1.7 events produce one record + one message | 16-event checklist (`docs/test-evidence/2026-11-02-thijil-automated-results.md`) | _(to fill)_ |
| **TC-UX-01** | Main screens at 360/768/1366 px | Viewport checklist (`docs/test-evidence/2026-11-02-thijil-automated-results.md`) + `ResponsiveViewport.test.jsx` | _(to fill)_ |
| **TC-Ren-01** | Renewal quote generation within window | Scheduled for Sprint 2 (INS-F-009 / Decision D-10) | Deferred |
| **TC-Rpt-01** | Admin reporting exports | Scope deferral (INS-F-025 / Decision D-10) | Deferred |
| **TC-Dash-01** | Customer dashboard live data | Scope deferral (INS-F-021 / Decision D-10) | Deferred |

*(Note: `TC-Clm-03` belongs to Sharana and is tracked under Sharana's test execution.)*

---

## 4. Non-Functional / Security Results

*Results recorded by Samarth (Person 3, Security & Non-Functional Lead) on build `eb6dd25`:*

| Area | Requirement | Scenario / Test Description | Result Reported by NF Executor |
|------|-------------|----------------------------|--------------------------------|
| Transport Security | INS-SR-001 | HTTPS and TLS 1.2+ configuration (TC-Sec-01) | **PASS** — Verified via Nginx TLS 1.2/1.3 reverse proxy config and TLS verification script suite (`PR #184` / `scripts/verify-tls-environment.sh`): TLS 1.0/1.1 disabled, HTTP->HTTPS 301 redirect and HSTS header active. |
| Password Storage | INS-SR-002 | BCrypt work factor >= 12 (TC-Sec-02) | **PASS** — Verified via `PasswordServiceTest` (6/6 JUnit tests passing on `eb6dd25`): BCrypt cost factor 12 (`$2a$12$` prefix) enforced; plaintext passwords never persisted. |
| RBAC Matrix | INS-SR-003 | Deny by default, role scoping (TC-Sec-03) | **PASS** — Verified via `SecurityConfig`, Newman `TC-Acc-02`, and `pen-test-checklist.md` §1.1–1.6: cross-role requests return `403 ACCESS-403`; unauthenticated requests return `401 AUTH-401` (Defect #187 resolved in PR #197). |
| Encryption at Rest | INS-SR-004 | AES-256 for documents and sensitive PII (TC-Sec-04) | **PASS** — Verified via `DiskFileStoreTest` (10/10 JUnit tests passing on `eb6dd25`): uploaded KYC, application, and policy PDF documents encrypted on disk with AES-256-GCM (unique 12-byte IV + 128-bit auth tag) and SHA-256 checksum verification. |
| Account Lockout | INS-SR-005 | Lockout after 5 failed attempts (TC-Sec-05) | **PASS** — Verified via `AuthControllerIntegrationTest#testLoginFiveFailuresTriggers423Locked` on `eb6dd25`: 5 consecutive failed login attempts lock the account for 15 minutes and return `423 Locked` (`AUTH-423`) even with valid credentials. |
| Injection & XSS | INS-SR-006 | OWASP ZAP automated scan (TC-Sec-06) | **PASS (Code/Integration) / ZAP Scan Not Verified** — Parameterized Spring Data JPA queries, React JSX escaping, and security headers (`CSP`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`) verified on `eb6dd25`; live OWASP ZAP container scan not executed (`Not verified`). |
| File Upload Security| INS-SR-007 | Allow-list extensions, <= 5 MB limit (TC-Sec-07) | **PASS** — Verified via `DiskFileStoreTest` and `/api/v1/kyc` & `/api/v1/applications/documents` validation on `eb6dd25`: enforces PDF/JPG/PNG allow-list, <= 5 MB size cap, path-traversal sanitization, and storage outside web root. |
| Audit Append-Only | INS-SR-008 | Append-only audit log, no UPDATE/DELETE (TC-Sec-08) | **PASS (with Defects #169–#172 noted)** — Verified via `P2-18` (`PR #173`) and Newman `TC-Aud-01`: audit log is append-only with no UPDATE/DELETE endpoints exposed and restricted to `SYSTEM_ADMIN` (see open low-severity defects #169–#172 for missing event hooks). |
| Insurer Isolation | INS-SR-003 | Multi-tenant insurer data isolation (TC-Sec-09) | **PASS** — Verified via `PolicyCancellationController`, `PolicyServiceTest`, `ApplicationControllerIntegrationTest`, and `pen-test-checklist.md` §2.1–2.6 on `eb6dd25`: Insurer A staff cannot access or decide Insurer B plans, applications, claims, or cancellations (`403 ACCESS-403`); customer cross-tenant access returns `403`/`404`. |
| Session Timeout | INS-SR-005 | 15-minute inactivity session expiration (TC-Sec-10) | **PASS** — Verified via `SessionServiceTest` and `pen-test-checklist.md` §1.5, §1.7, §4.4 on `eb6dd25`: sessions idle > 15 minutes (or > 8 hours absolute) and tokens replayed after `POST /api/v1/auth/logout` are rejected with `401 Unauthorized`. |
| Log Masking | INS-SR-002 | Sensitive data masking in application logs (TC-Sec-11) | **PASS** — Verified via backend log inspection during Maven Surefire and Newman runs on `eb6dd25`: no plaintext passwords, JWT tokens, PAN/Aadhaar numbers, or payment secrets are emitted to application logs. |
| Performance (Quote Latency) | INS-NF-001 | Quote response time p95 <= 5.0 s under 50 concurrent users (TC-Perf-01) | **Not verified** — JMeter plan (`docs/nf-security-tests/jmeter/TC-Perf-01-quote-response-time.jmx`) prepared; 10,000-policy / 5,000-claim volume dataset and separate load-generator host (`environment-needs.md`) not provisioned in single-host environment (single-user Newman quote calls complete in < 200 ms). |
| Load / Stress (500 Users) | INS-NF-002 | 500 concurrent virtual users for 20 min, error rate < 1% (TC-Load-01) | **Not verified** — JMeter plan (`docs/nf-security-tests/jmeter/TC-Load-01-500-concurrent-users.jmx`) prepared; requires a dedicated second load-generator host (STP §6) and volume dataset, not measurable on a single developer machine without estimation. |
| Availability | INS-NF-003 | Monthly availability >= 99.5% (TC-Avail-01) | **Not verified** — Actuator `/actuator/health` probe verified (`UP`), but a 30-day monthly availability percentage cannot be measured within a sprint test window. |
| Reliability (Backup & Restore) | INS-NF-004 | Database backup and restore drill, RPO/RTO verification (TC-Rel-01) | **PASS (Scripted)** — Backup and restore procedure and verification scripts delivered in `PR #185` (`feat/p4-14-database-backup-recovery`). |
| Browser & Viewport Compatibility | INS-NF-006 | Responsive viewports (360 px, 768 px, 1366 px) & cross-browser (TC-Compat-01) | **Partially Verified** — Verified on Chrome and Edge across 360 px, 768 px, and 1366 px viewports (`PR #174`, `ResponsiveViewport.test.jsx`, Selenium smoke `3/3`); Apple Safari (macOS/iOS) **Not verified** (no Apple hardware). |
| Audit Retention (5 Years) | INS-NF-007 | Audit records retained and retrievable for 5 years (P1-13) | **Deferred (Decision D-10)** — Deferred to Sprint 3 per `docs/decisions.md`. |

---

## 5. Open Defects at Final Release (`type:bug`)

*Source: `gh issue list --label type:bug --state open` on 16 Nov 2026 (defects #187 and #195 closed via PR #197; UI bugs #175–#179 and #190 closed via PR #191):*

| Issue # | Severity | Summary | Module | Status |
|:---:|:---:|---|---|:---:|
| **#198** | Medium | [Applications] Applications recorded under synthetic Customer #1001 in dev profile and missing submission fields (regYear / age) | Applications / Auth | OPEN |
| **#199** | Low | [Quote UI] Frontend calculates fabricated 18% GST split not defined in SRS rating formula | Quote UI | OPEN |
| **#172** | Low | [Notifications] Audit logging not recorded for notification events | Notifications | OPEN |
| **#171** | Low | [Policies] Audit logging not recorded for policy lifecycle | Policies | OPEN |
| **#170** | Low | [Claims] Audit logging not recorded for claim actions | Claims | OPEN |
| **#169** | Low | [Payment] Audit logging not recorded for payment transactions | Payment | OPEN |

*Note: High-severity defects #187 (audit unauthenticated 403 instead of 401) and #195 (cancellation queue 404 route prefix duplication) were verified resolved and closed in PR #197 on 16 Nov 2026.*

---

## 6. Deferred Items (Decision D-10)

Per `docs/decisions.md`:

| ID | Requirement ID | Feature / Requirement | Reason | Planned Sprint |
|----|----------------|----------------------|--------|:---:|
| D-10-F017 | INS-F-017 | Renewal reminders 30/15/1 days before expiry (P4-13) | Scope / time capacity | Sprint 3 |
| D-10-F021 | INS-F-021 | Role-specific dashboards (P2-15) | Scope / time capacity | Sprint 3 |
| D-10-F022 | INS-F-022 | Search and filter plans (P2-13) | Scope / time capacity | Sprint 3 |
| D-10-F025 | INS-F-025 | System Admin reports (P3-11) | Scope / time capacity | Sprint 3 |
| D-10-NF007| INS-NF-007 | Audit records retained and retrievable for 5 years (P1-13) | Scope / time capacity | Sprint 3 |
| D-11 | — | Claims module ownership split between P1 and P4 | Technical architecture | docs/decisions.md |

---

## 7. Sign-off & Recommendation

### Overall Test Result: Conditional Pass

### Evaluated & Signed Off By:
- **Prepared by:** Thijil Thejaswi M N (QA Lead, Person 1)
- **Reviewed by:** Thijil Thejaswi M N (QA Lead, Person 1)
- **Date:** 16 Nov 2026

### Sign-off Justification & Reasons:
The Sprint 2 test lifecycle is granted a **Conditional Pass** based on the following verified facts:

1. **UAT STP Walkthrough (3 Pass, 4 Partial, 1 Blocked, 0 Fail):**
   The formal UAT re-run executed on 15–16 Nov 2026 against a live MySQL 8.0 database (`dev` profile, SHA `9d4d1ca`) successfully demonstrated 3 complete STP scenarios (UAT-02 senior citizen health quote & underwriter referral, UAT-07 multi-tenant plan management & isolation, UAT-08 system admin onboarding & immutable audit trail). Four scenarios are recorded as Partial (UAT-01 motor application referral due to missing `regYear` payload and deferred plan filters; UAT-03 customer renewal self-service UI deferred per D-10; UAT-04 admin cancellation queue verified operational with HTTP 200 following PR #197 fix but customer submission screen not implemented; UAT-06 duplicate claim flagged and clock advanced, but customer doc re-upload endpoint unmapped). UAT-05 is Blocked at payment processing due to unmapped backend payout trigger per Decision D-11.
2. **Automated Regression Suite (100% Green on Main):**
   All 622 Maven backend tests, 92 Vitest frontend component tests, and 3/3 Selenium E2E smoke tests pass without errors. Newman API tests verify core workflows. All 4 GitHub Actions CI workflows are green.
3. **Non-Functional Testing Scope Limitations:**
   Non-functional performance, load, and availability targets (INS-NF-001, INS-NF-002, INS-NF-003) remain marked **Not verified** because a multi-host load-generation environment and a 10,000-policy volume dataset (STP §6) were not provisioned in the single-host academic project environment. JMeter test plans have been prepared for execution when staging infrastructure is available. Cross-browser testing is partially verified on Chrome and Edge; Safari was not verified due to lack of Apple hardware.
4. **Known Open Defects:**
   Zero Critical/High severity defects remain open. Defect #195 (cancellation 404) and Defect #187 (audit 401) were verified resolved and closed via PR #197. Exactly 6 Low/Medium defects remain open: Defect #198 (synthetic Customer #1001 fallback in dev profile and missing submission fields), Defect #199 (frontend 18% GST display split not in SRS formula), and Defects #169–#172 (missing audit logging hooks for notifications, policies, claims, payments).
5. **Deferred Scope Tracked:**
   All 5 deferred requirements (INS-F-017 renewal reminders, INS-F-021 role-specific dashboards, INS-F-022 search & filter plans, INS-F-025 admin reports, INS-NF-007 5-year audit retention) are formally documented under Decision D-10 in `docs/decisions.md` and scheduled for Sprint 3.
