# Sprint 2 Test Summary Report

> Build SHA: 00add8445c4f0105a20d24c2fc28d3ebd6a32d45  
> Branch: main  
> Prepared by: Thijil Thejaswi M N (QA Lead)  
> Date: 30 Oct 2026 (code freeze)  
> Status: **DRAFT – result columns to be filled after UAT execution**

---

## 1. Scope

### Features Under Test (Sprint 2)

| Feature ID | Description | PR | Branch |
|------------|-------------|-----|--------|
| INS-F-008 | Customer policy application (live screen) | #167 | feature/P3-08-renewal-cancellation-recovery |
| INS-F-009 | Policy renewal within window | #167 | feature/P3-08-renewal-cancellation-recovery |
| INS-F-010 | Policy cancellation & refund | #167 | feature/P3-08-renewal-cancellation-recovery |
| INS-F-012 | Issuance recovery scheduler | #167 | feature/P3-08-renewal-cancellation-recovery |
| INS-F-014 | Claims Officer appraisal desk | #157 | 11-p1-12-claims-officer-desk |
| INS-F-018 | Audit log hardening (RBAC, read-only) | #173 (merged) | 19-p2-18-audit-hardening |
| INS-F-019 | Responsive / cross-browser (360 px, dev badge) | #174 (merged) | 29-p2-14-responsive-crossbrowser |

### Modules In Scope
- Backend: `PolicyRenewal`, `PolicyCancellation`, `ClaimsOfficerDesk`, `AuditLog`
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
| Build SHA | `00add8445c4f0105a20d24c2fc28d3ebd6a32d45` |
| Backend | Spring Boot 3.x; PostgreSQL 16 (Docker) |
| Frontend | Vite + React; Node 20 |
| API Test tool | Newman (Postman CLI) |
| Browser | Chrome (latest), Firefox (latest) |
| Viewport | 360 px, 768 px, 1280 px |
| CI | GitHub Actions (4 workflows) |
| Test date | 30 Oct 2026 |

---

## 3. Test Counts by Status

> Fill this section after UAT execution is complete.

| Category | Total | Passed | Failed | Blocked | Not Run |
|----------|-------|--------|--------|---------|---------|
| Backend unit/integration (Maven Surefire) | _(to fill)_ | | | | |
| Frontend unit (Vitest) | _(to fill)_ | | | | |
| API / Newman | _(to fill)_ | | | | |
| UAT (manual, UAT-01..08) | 8 | | | | |
| **Total** | | | | | |

### STP Coverage (selected rows)

| STP Row | Description | Status |
|---------|-------------|--------|
| TC-Acc-02 | Account registration & login flow | _(to fill)_ |
| TC-UX-01 | Responsive layout at 360 px | _(to fill)_ |
| TC-Ntf-01 | Email/notification trigger at state change | _(to fill)_ |
| TC-Aud-01 | Audit log — System Admin read-only access | _(to fill)_ |
| TC-Clm-03 | Claims Officer appraisal desk end-to-end | _(to fill)_ |
| TC-Ren-01 | Renewal within window (deferred D-10) | Deferred |
| TC-Rpt-01 | Admin reports (deferred D-10) | Deferred |
| TC-Dash-01 | Customer dashboard live data (deferred D-10) | Deferred |

---

## 4. Non-Functional / Security Results

> To be supplied by respective owners after UAT.

| Area | Test | Result |
|------|------|--------|
| RBAC — Audit log | GET /audit: 403 for non-System-Admin | _(Sharana to verify)_ |
| RBAC — Claims desk | Claims Officer role only | _(Samarth to verify after #157 merge)_ |
| Multi-tenant isolation | Insurer A cannot see Insurer B data | _(Samarth to verify after #167 merge)_ |
| Performance / load (NF-007) | Not tested — deferred D-10 | Deferred |
| Cross-browser (360 px) | No horizontal scroll at 360 px | _(Sharana to verify)_ |

---

## 5. Open Defects at Code Freeze

| GitHub Issue | Severity | Summary | Status |
|-------------|----------|---------|--------|
| _(to fill from open issues)_ | | | |

> Run `gh issue list --label bug --state open` to populate this table.

---

## 6. Deferred Items (D-10)

Per `docs/decisions.md`:

| ID | Feature / Requirement | Reason | Planned Sprint |
|----|----------------------|--------|----------------|
| D-10-F017 | INS-F-017 Quote filter step | Scope / time | Sprint 3 |
| D-10-F021 | INS-F-021 Duplicate claim auto-flag (full) | Scope / time | Sprint 3 |
| D-10-F022 | INS-F-022 Application filter | Scope / time | Sprint 3 |
| D-10-F025 | INS-F-025 System Admin reports | Scope / time | Sprint 3 |
| D-10-NF007 | NF-007 Performance / load test | Environment | Sprint 3 |
| D-11 | Claims ownership split (P1-P3 boundary) | Design | docs/decisions.md |

---

## 7. Conclusion

> **To be completed after UAT execution.**  
> Do not fill until all UAT rows have been executed and evidence recorded.

Overall test result: _(PASS / FAIL / CONDITIONAL PASS — to fill)_

Prepared by: Thijil Thejaswi M N  
Reviewed by: _(to fill)_  
Date: _(to fill after UAT)_
