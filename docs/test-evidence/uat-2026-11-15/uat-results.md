# UAT Execution Results — 15 Nov 2026 Re-run Walkthrough

**Build SHA:** `9a5c712`  
**Profile Used:** `dev` (Local MySQL 8.0 server on `localhost:3306`, `insurance_db` database, Spring Boot `dev` profile, frontend running on `http://localhost:5173`).  
**Date:** 15 Nov 2026  
**Execution Window:** 19:40 – 20:05 IST (2026-11-15T14:10:00Z – 2026-11-15T14:35:00Z)  
**Execution Lead:** Antigravity browser walkthrough; single operator logging in via the real authentication page (`/login`) for every role with authentic seeded fixture credentials. Dev role switcher was not used.

### Accounts Used Per Role
- **Customer:** `asha@example.test` (User ID 1, KYC verified with encrypted PAN/Aadhaar)
- **Underwriter:** `uw.a@safeshield.test` (User ID 11, Insurer 1 SafeShield)
- **Claims Officer:** `claims.a@safeshield.test` (User ID 12, Insurer 1 SafeShield)
- **Insurer Admin A:** `admin.a@safeshield.test` (User ID 10, Insurer 1 SafeShield)
- **Insurer Admin B:** `admin.b@bharatcare.test` (User ID 20, Insurer 2 BharatCare)
- **System Admin:** `sysadmin@insurance.test` (User ID 99)

---

## STP UAT Walkthrough Results Table

| ID | Result | What happened | Evidence | Defect | Time |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **UAT-01** | Partial | Customer browsed plans (`/plans`), calculated instant motor quote on SafeShield Comprehensive Car (IDV ₹5,00,000, 4-yr vehicle, ₹10,000 base + 18% GST = ₹11,800 payable), and submitted application APP-3. Application transitioned to `REFERRED` rather than STP auto-approval because the frontend application details payload omitted regYear (`extractVehicleAge` returned null in `MotorUnderwritingEvaluator`). Underwriter (`uw.a@safeshield.test`) inspected queue at `/underwriter` and approved with justification ("Risk profile acceptable under standard motor underwriting guidelines"); Customer returned to Applications desk, navigated to Payment, paid ₹11,800, and verified policy `POL-2026-000002` Active with download link in `/customer/policies`. *Note: Plan filter deferred per Decision D-10.* | `UAT-01-step1-customer.png`, `UAT-01-step2-customer.png`, `UAT-01-step3-customer.png`, `UAT-01-step4-customer.png`, `UAT-01-step5-customer.png`, `UAT-01-step6-underwriter.png`, `UAT-01-step7-underwriter.png`, `UAT-01-step8-customer.png`, `UAT-01-step9-customer.png` | Auto-approval not triggered (missing regYear in UI payload); D-10 | 2026-11-15T19:59:00 |
| **UAT-02** | Pass | Customer generated health insurance quote for age 62 on SafeShield Critical Care (`/customer/quote/health`); application APP-8 submitted and transitioned to `REFERRED` (triggering boundary referral rule for primary age ≥ 60); Underwriter (`uw.a@safeshield.test`) inspected queue (`/underwriter`), reviewed referral, and approved with justification ("Senior citizen health risk evaluation acceptable under Plan #3 parameters"); Customer navigated to payment, paid ₹10,000, and verified active health policy in `/customer/policies`. | `UAT-02-step1-customer.png`, `UAT-02-step2-customer.png`, `UAT-02-step3-underwriter.png`, `UAT-02-step4-underwriter.png`, `UAT-02-step5-customer.png`, `UAT-02-step6-customer.png`, `UAT-02-step7-customer.png` | - | 2026-11-15T20:04:00 |
| **UAT-03** | Partial | Customer inspected policy portfolio (`/customer/policies`) displaying 4 active policies (`POL-2026-000002`, `POL-M-1`, `POL-M-2`, `POL-H-1`) and opened policy details modal. However, customer self-service renewal UI triggers and automated renewal certificate download were deferred per Decision D-10 (INS-F-009 / INS-F-017 planned for Sprint 3); no renewal button is wired in the UI portfolio view. | `UAT-03-step1-customer.png`, `UAT-03-step2-customer.png` | Deferred (Decision D-10) | 2026-11-15T20:00:15 |
| **UAT-04** | Fail | Customer portfolio (`/customer/policies`) has no self-service cancellation request UI form. On Insurer Admin portal, navigating to `/insurer-admin/cancellations` displays HTTP 404 "The requested resource was not found: /api/v1/cancellations" because backend controller maps to `/api/v1/api/v1/cancellations` due to duplicate base path prefix (Defect #195). | `UAT-04-step1-customer.png`, `UAT-04-step2-insurer-admin.png` | #195 | 2026-11-15T20:00:45 |
| **UAT-05** | Blocked | Customer filed motor claim on active policy `POL-M-1` (`/customer/claims`) with incident date 2026-11-10, claimed amount ₹15,000, and incident description. Claim was created with stage `DOCUMENT_VERIFICATION`. Claims Officer (`claims.a@safeshield.test`) opened queue on `/claims-officer`, completed document verification (moving claim to `ASSESSMENT`), and approved loss assessment of ₹12,000, advancing claim to `PAYMENT_PROCESSING`. Automated settlement payout disbursement (`POST /claims/{id}/payout`) is unmapped on backend per Decision D-11 (claims module split), blocking the claim from reaching `SETTLED`. | `UAT-05-step1-customer.png`, `UAT-05-step2-customer.png`, `UAT-05-step3-claims-officer.png`, `UAT-05-step4-claims-officer.png`, `UAT-05-step5-claims-officer.png` | Decision D-11 | 2026-11-15T20:01:25 |
| **UAT-06** | Partial | Customer filed a duplicate claim on policy `POL-M-1` with identical incident date (`2026-11-10`) and amount (₹15,000); backend set `duplicateFlag = true`; Claims Officer worklist (`/claims-officer`) displayed `⚠️ Possible Duplicate` badge. Test clock advanced by 15 days via `POST /api/v1/test/clock`. Customer document re-upload endpoint (`POST /claims/{id}/documents`) is Blocked (missing backend API, Deviation D-CLM-06-UPLOAD); auto-close job runs on daily 02:00 AM cron (`ClaimAutoCloseScheduler`). | `UAT-06-step1-customer.png`, `UAT-06-step2-claims-officer.png`, `UAT-06-step3-claims-officer.png` | D-CLM-06-UPLOAD | 2026-11-15T20:01:50 |
| **UAT-07** | Pass | Insurer Admin A (`admin.a@safeshield.test`) created new motor plan 'SafeShield Premier Motor Protect' (Coverage ₹8,00,000, Base ₹9,500) and toggled its active status to Deactivated on `/insurer-admin/plans`. Logged in as Insurer Admin B (`admin.b@bharatcare.test`): multi-tenant isolation strictly verified; Insurer A plans were completely hidden and inaccessible to Insurer B. | `UAT-07-step1-insurer-a.png`, `UAT-07-step2-insurer-a.png`, `UAT-07-step3-insurer-b.png` | - | 2026-11-15T20:02:10 |
| **UAT-08** | Pass | System Admin (`sysadmin@insurance.test`) onboarded new insurer 'National Reliance General' and provisioned staff account 'Karan Sharma' (UNDERWRITER, SafeShield) via `/system-admin`; searched append-only audit trail (`/system-admin/audit`) displaying live immutable system events. *Note: Administrative report generation is Deferred per Decision D-10 (INS-F-025).* | `UAT-08-step1-sysadmin.png`, `UAT-08-step2-sysadmin.png`, `UAT-08-step3-sysadmin.png` | Deferred (Decision D-10) | 2026-11-15T20:02:25 |

---

## Summary of Results

- **Total Scenarios Evaluated:** 8
- **Pass:** 3 / 8 (UAT-02, UAT-07, UAT-08)
- **Partial:** 3 / 8 (UAT-01, UAT-03, UAT-06)
- **Blocked:** 1 / 8 (UAT-05)
- **Fail:** 1 / 8 (UAT-04)
- **Defects & Blockers:**
  - **Defect #195:** `GET /cancellations` returns HTTP 404 on Insurer Admin cancellation queue (blocks UAT-04).
  - **Decision D-11:** Automated settlement payout trigger endpoint unmapped on backend (blocks UAT-05 from reaching `SETTLED`).
  - **Deviation D-CLM-06-UPLOAD:** Customer document re-upload endpoint (`POST /claims/{id}/documents`) is unmapped on backend (affects UAT-06).
  - **Auto-Approval Payload:** Frontend application details payload omits `regYear`, causing `MotorUnderwritingEvaluator` to refer motor applications instead of STP auto-approving (affects UAT-01).
- **Deferred Requirements Noted:**
  - UAT-01: Multi-attribute plan search and filtering (INS-F-022, Decision D-10).
  - UAT-03: Customer self-service renewal workflow and renewal certificate download (INS-F-009 / INS-F-017, Decision D-10).
  - UAT-08: Administrative report generation (INS-F-025, Decision D-10).
