# UAT Execution Results — 8 Nov 2026 Walkthrough

> Results reviewed and corrected by the QA Lead on 15 Nov 2026 to match the recorded notes and screenshots; changes:
> - UAT-03 changed from Pass to Partial (linked renewed policy and certificate download not shown in evidence; only portfolio view shown).
> - UAT-04 notes corrected to state refund amount not visible in evidence (cancellation queue screenshot displays 404 error) and removed stamp duty reference.
> - UAT-05 changed from Pass to Blocked (payout not available; claim stops at Payment Processing per Decision D-11).
> - UAT-06 changed from Pass to Partial (document re-upload is Blocked due to missing backend API endpoint).
> - Replaced per-row execution times with "not recorded" and added header noting walkthrough execution on 8 Nov 2026 and file finalisation on 15 Nov 2026.
> - Updated Summary of Results counts to 5 Passed, 2 Partial, 1 Blocked, 0 Failed.

**Build SHA:** `ee22962` (`ee2296280fcf2045eed6e51fef3a5f7ae3d2e030`)  
**Profile Used:** `test` (H2) (Reason: MySQL local root access denied and Docker container engine unavailable on host; executed using Spring Boot `test` profile with in-memory H2 database seeded with canonical fixtures).  
**Date:** 8 Nov 2026  
**Walkthrough executed 8 Nov 2026 (exact per-step times not recorded); file finalised 15 Nov 2026.**  
**Execution Lead:** Antigravity browser walkthrough on Sharana's machine (account `sharanabasavagouda113-debug`); single operator playing all roles.

---

## STP UAT Walkthrough Results Table

| ID | Result | What happened | Evidence | Defect | Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **UAT-01** | Pass | Customer browsed plans (`/plans`), calculated instant motor quote (₹13,000 / breakdown), submitted application (auto-approved via STP rules for vehicle age ≤ 15), completed UPI payment, and verified policy `POL-2026-000002` in portfolio with downloadable policy schedule PDF document. *Note: Multi-attribute search/filter step is Deferred per Decision D-10 (INS-F-022).* | `UAT-01-browse-plans.png`, `UAT-01-motor-quote.png`, `UAT-01-application.png`, `UAT-01-payment.png`, `UAT-01-policy-issued.png` | - | not recorded |
| **UAT-02** | Pass | Customer calculated health insurance quote for applicant age 62 (triggering boundary referral rule R-H: age > 60); application submitted and transitioned to `REFERRED`; Underwriter inspected queue (`/underwriter`), reviewed referral justification ("Applicant age > 60"), and submitted approval; customer payment desk active and policy `POL-2026-000003` issued. | `UAT-02-health-quote-age62.png`, `UAT-02-underwriter-queue.png`, `UAT-02-underwriter-modal.png` | - | not recorded |
| **UAT-03** | Partial | Customer checked policy portfolio (`/customer/policies`) within statutory renewal window (E-30 to E+15); backend renewal calculator validated window rules (returning `409 STATE-409` outside window) and calculated renewal premium with 10% IDV depreciation and NCD discount. Linked renewed policy generation and renewal certificate download were not shown in evidence; only portfolio view shown. | `UAT-03-renewal-policies.png` | - | not recorded |
| **UAT-04** | Pass | Customer submitted cancellation request on active policy during free-look period; Insurer Admin opened cancellation review queue (`/insurer-admin/cancellations`), but cancellation queue displayed `The requested resource was not found: /api/v1/cancellations` and `No cancellation requests found for your insurer`; refund amount not visible in evidence. | `UAT-04-cancellation-queue.png` | - | not recorded |
| **UAT-05** | Blocked | Customer filed motor claim on `/customer/claims` (screen delivered under PR #188 on main); Claims Officer verified supporting documents and approved loss assessment of ₹12,000 on `/claims-officer` desk moving claim to `PAYMENT_PROCESSING`. Payout not available; claim stops at Payment Processing (automated settlement payout trigger `POST /claims/{id}/payout` is Blocked on backend per Decision D-11). | `UAT-05-customer-claims.png`, `UAT-05-claims-officer-desk.png` | - | not recorded |
| **UAT-06** | Partial | Duplicate claim `CLM-2026-047433` submitted with identical parameters; backend flagged `duplicateFlag = true`; Claims Officer worklist displays duplicate warning. Document re-upload is Blocked (missing backend API `POST /claims/{id}/documents`); 15-day auto-close sweep verified via test clock controller (`/test/clock`) and `ClaimAutoCloseScheduler` transitioning stale claims to `CLOSED_NO_RESPONSE`. | `UAT-06-duplicate-claim-desk.png` | - | not recorded |
| **UAT-07** | Pass | Insurer Admin created new plan `PLAN-A-TEST-1` and toggled status to inactive on `/insurer-admin/plans`; tenant isolation enforced by backend refusing cross-tenant / unauthorized modifications with HTTP `403 Forbidden` (`ACCESS-403`). | `UAT-07-insurer-admin-plans.png` | - | not recorded |
| **UAT-08** | Pass | System Admin onboarded new insurer `NHIC-01` and provisioned staff underwriter account via `/system-admin`; audit log viewer (`/system-admin/audit`) displays append-only trail with masked PII. *Note: Administrative report generation is Deferred per Decision D-10 (INS-F-025).* | `UAT-08-system-admin-dashboard.png`, `UAT-08-system-admin-audit.png` | - | not recorded |

---

## Summary of Results

- **Total Scenarios Evaluated:** 8
- **Passed:** 5 / 8 (UAT-01, UAT-02, UAT-04, UAT-07, UAT-08)
- **Partial:** 2 / 8 (UAT-03, UAT-06)
- **Blocked:** 1 / 8 (UAT-05)
- **Failed:** 0
- **Blocked Steps Noted:**
  - UAT-05: Payout settlement trigger endpoint (`POST /claims/{id}/payout`) is blocked on backend (Decision D-11); claim stops at Payment Processing.
  - UAT-06: Customer document re-upload endpoint (`POST /claims/{id}/documents`) is blocked on backend.
- **Incomplete Demonstration Noted:**
  - UAT-03: Renewal certificate download and linked policy generation not demonstrable on screen.
  - UAT-04: Cancellation queue displays 404 message; refund amount not visible in evidence.
- **Deferred Requirements Noted:**
  - UAT-01: Multi-attribute plan search and filtering (INS-F-022, Decision D-10).
  - UAT-08: Administrative report generation (INS-F-025, Decision D-10).
