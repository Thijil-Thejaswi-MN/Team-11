# UAT Execution Results — 8 Nov 2026 Walkthrough

> The 8 Nov walkthrough is invalid: it ran under the dev role switcher as a customer, before the fake-data fix (#191); staff steps returned 403/404 and customer screens showed sample data. Superseded by the re-run on <date>.

**Build SHA:** `ee22962` (`ee2296280fcf2045eed6e51fef3a5f7ae3d2e030`)  
**Profile Used:** `test` (H2)  
**Date:** 8 Nov 2026 (walkthrough executed; invalidated and corrected 15 Nov 2026)  
**Execution Operator:** Antigravity browser walkthrough on Sharana's machine (`sharanabasavagouda113-debug`); ran entirely under dev role switcher as `Customer (Asha)`.

---

## STP UAT Walkthrough Results Table

| ID | Result | What the evidence shows | Evidence | Defect | Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **UAT-01** | Not demonstrated | Executed entirely under dev role switcher as Customer (Asha) before fake-data fix #191. `UAT-01-browse-plans.png` displays hardcoded samplePlans; `UAT-01-motor-quote.png` displays client-side simulation fallback quote (₹14,080 incl. 18% GST); `UAT-01-application.png` displays static sample applications APP-2026-9021 / APP-2026-7640 with hardcoded premium ₹8,450; `UAT-01-payment.png` shows ACCESS-403 ("Access denied: customers may only access their own records") and "Unable to Load Application / Payment Unavailable"; `UAT-01-policy-issued.png` displays fallback demo policies POL-2026-4W-0091 and POL-2025-HL-0012. Expected purchase, real API rating, and policy issuance were not demonstrated. | `UAT-01-browse-plans.png`, `UAT-01-motor-quote.png`, `UAT-01-application.png`, `UAT-01-payment.png`, `UAT-01-policy-issued.png` | #175, #177, #178, #190 | not recorded |
| **UAT-02** | Not demonstrated | Executed under dev role switcher as Customer (Asha). `UAT-02-health-quote-age62.png` shows input age 62 with "No Quote Generated Yet" (quote calculation never triggered); `UAT-02-underwriter-queue.png` and `UAT-02-underwriter-modal.png` show ACCESS-403 ("Access denied: role 'CUSTOMER' is not authorized for this operation") and "No Referred Applications" because underwriter desk was accessed with customer role credentials. Referral decisioning and policy issuance were not demonstrated. | `UAT-02-health-quote-age62.png`, `UAT-02-underwriter-queue.png`, `UAT-02-underwriter-modal.png` | - | not recorded |
| **UAT-03** | Not demonstrated | `UAT-03-renewal-policies.png` only shows the customer policy portfolio displaying static demo policies POL-2026-4W-0091 and POL-2025-HL-0012. No renewal action was executed, no linked renewed policy was generated, and no renewal certificate was downloaded. Expected renewal flow was not demonstrated. | `UAT-03-renewal-policies.png` | #178 | not recorded |
| **UAT-04** | Not demonstrated | `UAT-04-cancellation-queue.png` shows NOTFOUND-404 ("The requested resource was not found: /api/v1/cancellations") and "No cancellation requests found for your insurer" when navigating to `/insurer-admin/cancellations` under customer role. No cancellation approval occurred, and refund amount was not visible in evidence. Expected cancellation approval and refund calculation were not demonstrated. | `UAT-04-cancellation-queue.png` | #195 | not recorded |
| **UAT-05** | Not demonstrated | `UAT-05-customer-claims.png` shows empty customer desk ("No Claims on File / No filed claims found"); `UAT-05-claims-officer-desk.png` shows ACCESS-403 ("Access denied: role 'CUSTOMER' is not authorized for this operation") and empty queue because claims officer desk was opened under customer role. Furthermore, automated payout settlement trigger `POST /claims/{id}/payout` is blocked on backend per Decision D-11. Expected claim lifecycle to Settled was not demonstrated. | `UAT-05-customer-claims.png`, `UAT-05-claims-officer-desk.png` | - | not recorded |
| **UAT-06** | Not demonstrated | `UAT-06-duplicate-claim-desk.png` shows ACCESS-403 ("Access denied: role 'CUSTOMER' is not authorized for this operation") and empty queue (0 active, 0 flags) under customer role; duplicate warning banner on claims desk was not demonstrated. Customer document re-upload is blocked due to missing backend API `POST /claims/{id}/documents`. Expected duplicate claim review and 15-day auto-close workflow were not demonstrated. | `UAT-06-duplicate-claim-desk.png` | - | not recorded |
| **UAT-07** | Not demonstrated | `UAT-07-insurer-admin-plans.png` shows only the initial plan catalog table (SafeShield plans). No plan creation (PLAN-A-TEST-1), no plan deactivation, and no cross-tenant isolation test or 403 Forbidden response were shown in evidence. Expected plan management and tenant isolation were not demonstrated. | `UAT-07-insurer-admin-plans.png` | - | not recorded |
| **UAT-08** | Not demonstrated | `UAT-08-system-admin-dashboard.png` and `UAT-08-system-admin-audit.png` are identical screenshots showing blank onboarding / provisioning input forms and a static hardcoded preview line (`DECIDE_APPLICATION [APP-2026-9021: APPROVED]`). No insurer or user was onboarded, no audit log query was executed, and admin report generation is deferred under Decision D-10 (INS-F-025). Expected governance workflow was not demonstrated. | `UAT-08-system-admin-dashboard.png`, `UAT-08-system-admin-audit.png` | #179 | not recorded |

---

## Summary of Results

- **Total Scenarios Evaluated:** 8
- **Demonstrated:** 0 / 8 (re-run pending)
- **Not Demonstrated:** 8 / 8
- **Conclusion:** The 8 Nov walkthrough is invalid: it ran under the dev role switcher as a customer, before the fake-data fix (#191); staff steps returned 403/404 and customer screens showed sample data. Superseded by the re-run on <date>.
