# Sprint 2 Test Summary Report

> Build SHA: `eb6dd25869b70542d4724214e8049dd7f8abd74b` (short: `eb6dd25`)  
> Branch: main  
> Prepared by: Thijil Thejaswi M N (QA Lead)  
> Date: 2 Nov 2026  
> Status: **DRAFT – result columns to be filled after UAT execution**

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
- INS-F-017: Customer quote-filter step (TC-F-022 deferred)
- INS-F-021: Duplicate-claim auto-flag (UI surface only; logic deferred)
- INS-F-022: Policy application filter (deferred to next sprint)
- INS-F-025: System Admin reports (INS-F-025 deferred)
- NF-007: Performance / load testing

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
| Newman API Assertions | Sharana Newman run (`docs/test-evidence/2026-10-29-newman-00add84.xml`) | 54 (38 requests) | 35 | 19 | 0 | 0 |
| Selenium E2E Smoke | Sharana Selenium run (`docs/test-evidence/2026-10-29-selenium-00add84.xml`) | 3 | 3 | 0 | 0 | 0 |
| STP Independent Test Cases | `docs/test-execution-matrix.md` | 52 | 0 | 0 | 0 | Blocked / Deferred / Not Run |
| UAT Scenarios | `docs/uat-29-oct.md` (UAT-01..UAT-08) | 8 | _(to fill)_ | _(to fill)_ | 0 | _(to fill)_ |

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

*Results recorded by Samarth (Person 3, Security & Non-Functional Lead):*

| Area | Requirement | Scenario / Test Description | Result Reported by NF Executor |
|------|-------------|----------------------------|--------------------------------|
| Transport Security | INS-SR-001 | HTTPS and TLS 1.2+ configuration (TC-Sec-01) | Not reported by the NF/security executor |
| Password Storage | INS-SR-002 | BCrypt work factor >= 12 (TC-Sec-02) | Not reported by the NF/security executor |
| RBAC Matrix | INS-SR-003 | Deny by default, role scoping (TC-Sec-03) | Not reported by the NF/security executor |
| Encryption at Rest | INS-SR-004 | AES-256 for documents and sensitive PII (TC-Sec-04) | Not reported by the NF/security executor |
| Account Lockout | INS-SR-005 | Lockout after 5 failed attempts (TC-Sec-05) | Not reported by the NF/security executor |
| Injection & XSS | INS-SR-006 | OWASP ZAP automated scan (TC-Sec-06) | Not reported by the NF/security executor |
| File Upload Security| INS-SR-007 | Allow-list extensions, <= 5 MB limit (TC-Sec-07) | Not reported by the NF/security executor |
| Audit Append-Only | INS-SR-008 | Append-only audit log, no UPDATE/DELETE (TC-Sec-08) | Not reported by the NF/security executor |
| Insurer Isolation | INS-SR-003 | Multi-tenant insurer data isolation (TC-Sec-09) | Not reported by the NF/security executor |
| Session Timeout | INS-SR-005 | 15-minute inactivity session expiration (TC-Sec-10) | Not reported by the NF/security executor |
| Log Masking | INS-SR-002 | Sensitive data masking in application logs (TC-Sec-11) | Not reported by the NF/security executor |
| Performance / Load | INS-NF-007 | Performance load testing (NF-007) | Deferred (Decision D-10) |

---

## 5. Open Defects at Code Freeze (`type:bug`)

*Source: `gh issue list --label type:bug --state open` on 2 Nov 2026:*

| Issue # | Severity | Summary | Module | Status |
|:---:|:---:|---|---|:---:|
| **#187** | High | [Audit API] Unauthenticated GET /api/v1/audit returns 403 ACCESS-403 instead of 401 AUTH-401 (TC-Aud-01 / TC-Sec-03) | Admin / Audit | OPEN |
| **#179** | Medium | [System Admin] Hardcoded sample audit records in SystemAdminDashboardView.jsx | Admin | OPEN |
| **#178** | Medium | [Customer Policies] Fallback synthetic policies in MyPoliciesView.jsx | Policies | OPEN |
| **#177** | Medium | [Customer Dashboard] Hardcoded portfolio metrics and sample policy in CustomerDashboardView.jsx | Customer | OPEN |
| **#176** | Medium | [Customer Claims] Hardcoded sample claim records in ClaimsPlaceholder.jsx | Claims | OPEN |
| **#175** | Medium | [Customer Applications] Hardcoded sample application records in ApplicationsPlaceholder.jsx | Applications | OPEN |
| **#172** | Low | [Notifications] Audit logging not recorded for notification events | Notifications | OPEN |
| **#171** | Low | [Policies] Audit logging not recorded for policy lifecycle | Policies | OPEN |
| **#170** | Low | [Claims] Audit logging not recorded for claim actions | Claims | OPEN |
| **#169** | Low | [Payment] Audit logging not recorded for payment transactions | Payment | OPEN |

---

## 6. Deferred Items (Decision D-10)

Per `docs/decisions.md`:

| ID | Requirement ID | Feature / Requirement | Reason | Planned Sprint |
|----|----------------|----------------------|--------|:---:|
| D-10-F017 | INS-F-017 | Customer quote filter step (TC-F-022) | Scope / time capacity | Sprint 3 |
| D-10-F021 | INS-F-021 | Duplicate claim auto-flagging engine logic (UI notice implemented) | Scope / time capacity | Sprint 3 |
| D-10-F022 | INS-F-022 | Policy application filtering | Scope / time capacity | Sprint 3 |
| D-10-F025 | INS-F-025 | System Admin reports generation | Scope / time capacity | Sprint 3 |
| D-10-NF007| NF-007 | Performance & stress load testing | Test environment limitations | Sprint 3 |
| D-11 | — | Claims module ownership split between P1 and P4 | Technical architecture | docs/decisions.md |

---

## 7. Conclusion

> **To be completed after UAT execution.**  
> Do not fill until all UAT rows have been executed and evidence recorded.

Overall test result: _(PASS / FAIL / CONDITIONAL PASS — to fill)_

Prepared by: Thijil Thejaswi M N  
Reviewed by: _(to fill)_  
Date: _(to fill after UAT)_
