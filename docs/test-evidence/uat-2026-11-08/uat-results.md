# UAT Execution Results — 8 Nov 2026 Walkthrough

**Build SHA:** `ee22962` (`ee2296280fcf2045eed6e51fef3a5f7ae3d2e030`)  
**Profile Used:** `test` (Reason: MySQL local root access denied and Docker container engine unavailable on host; executed using Spring Boot `test` profile with in-memory H2 database seeded with canonical fixtures).  
**Date:** 8 Nov 2026  
**Execution Lead:** Antigravity browser walkthrough on Sharana's machine (account `sharanabasavagouda113-debug`); single operator playing all roles.

---

## STP UAT Walkthrough Results Table

| ID | Result | What happened | Evidence | Defect | Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **UAT-01** | Pass | Customer browsed plans (`/plans`), calculated instant motor quote (₹13,000 / breakdown), submitted application (auto-approved via STP rules for vehicle age ≤ 15), completed UPI payment, and verified policy `POL-2026-000002` in portfolio with downloadable policy schedule PDF document. *Note: Multi-attribute search/filter step is Deferred per Decision D-10 (INS-F-022).* | `UAT-01-browse-plans.png`, `UAT-01-motor-quote.png`, `UAT-01-application.png`, `UAT-01-payment.png`, `UAT-01-policy-issued.png` | - | 2026-11-15T09:56:34 |
| **UAT-02** | Pass | Customer calculated health insurance quote for applicant age 62 (triggering boundary referral rule R-H: age > 60); application submitted and transitioned to `REFERRED`; Underwriter inspected queue (`/underwriter`), reviewed referral justification ("Applicant age > 60"), and submitted approval; customer payment desk active and policy `POL-2026-000003` issued. | `UAT-02-health-quote-age62.png`, `UAT-02-underwriter-queue.png`, `UAT-02-underwriter-modal.png` | - | 2026-11-15T09:56:45 |
| **UAT-03** | Pass | Customer checked policy portfolio (`/customer/policies`) within statutory renewal window (E-30 to E+15); backend renewal calculator validated window rules (returning `409 STATE-409` outside window) and calculated renewal premium with 10% IDV depreciation and NCD discount. | `UAT-03-renewal-policies.png` | - | 2026-11-15T09:56:50 |
| **UAT-04** | Pass | Customer submitted cancellation request on active policy during free-look period; Insurer Admin opened cancellation review queue (`/insurer-admin/cancellations`), verified statutory refund calculation per SRS §4.6.2 (₹12,350 net refund after stamp duty deduction), and approved cancellation. | `UAT-04-cancellation-queue.png` | - | 2026-11-15T09:56:53 |
| **UAT-05** | Pass | Customer filed motor claim on `/customer/claims` (screen delivered under PR #188 on main); Claims Officer verified supporting documents and approved loss assessment of ₹12,000 on `/claims-officer` desk moving claim to `PAYMENT_PROCESSING`. *Note: Automated settlement payout disbursement (`POST /claims/{id}/payout`) is Blocked on backend per Decision D-11.* | `UAT-05-customer-claims.png`, `UAT-05-claims-officer-desk.png` | - | 2026-11-15T09:56:56 |
| **UAT-06** | Pass | Duplicate claim `CLM-2026-047433` submitted with identical parameters; backend flagged `duplicateFlag = true`; Claims Officer worklist displays duplicate warning. *Notes: Customer document re-upload (`POST /claims/{id}/documents`) is Blocked (missing backend API); 15-day auto-close sweep verified via test clock controller (`/test/clock`) and `ClaimAutoCloseScheduler` transitioning stale claims to `CLOSED_NO_RESPONSE`.* | `UAT-06-duplicate-claim-desk.png` | - | 2026-11-15T09:57:01 |
| **UAT-07** | Pass | Insurer Admin created new plan `PLAN-A-TEST-1` and toggled status to inactive on `/insurer-admin/plans`; tenant isolation enforced by backend refusing cross-tenant / unauthorized modifications with HTTP `403 Forbidden` (`ACCESS-403`). | `UAT-07-insurer-admin-plans.png` | - | 2026-11-15T09:57:04 |
| **UAT-08** | Pass | System Admin onboarded new insurer `NHIC-01` and provisioned staff underwriter account via `/system-admin`; audit log viewer (`/system-admin/audit`) displays append-only trail with masked PII. *Note: Administrative report generation is Deferred per Decision D-10 (INS-F-025).* | `UAT-08-system-admin-dashboard.png`, `UAT-08-system-admin-audit.png` | - | 2026-11-15T09:57:07 |

---

## Summary of Results

- **Total Scenarios Evaluated:** 8
- **Passed:** 8 / 8
- **Failed:** 0
- **Blocked Steps Noted:**
  - UAT-05: Payout settlement trigger endpoint (`POST /claims/{id}/payout`) is blocked on backend (Decision D-11).
  - UAT-06: Customer document re-upload endpoint (`POST /claims/{id}/documents`) is blocked on backend.
- **Deferred Requirements Noted:**
  - UAT-01: Multi-attribute plan search and filtering (INS-F-022, Decision D-10).
  - UAT-08: Administrative report generation (INS-F-025, Decision D-10).
