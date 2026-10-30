# Thijil's Automated Test Execution Evidence & Manual Checklist (30 Oct 2026)

**Role:** Person 1 — Thijil Thejaswi M.N. (`Thijil-Thejaswi-MN`), QA Lead  
**Repository:** [Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Execution Environment:** Local Integrated Build (`backend` Spring Boot test profile with H2 / `frontend` React Vite)  
**Commit SHA Tested:** `00add8445c4f0105a20d24c2fc28d3ebd6a32d45` (short: `00add84`)  
**Execution Timestamp:** 30 October 2026, 23:12 IST  
**Report Artifact:** `docs/test-evidence/2026-10-30-thijil-newman-00add84.xml`  

> **Note on Test Policy:** In compliance with STP §8 and team governance rules, raw assertion counts and observed metrics are recorded as executed. **No STP row is marked "Passed"**; the final evaluation, status recording, and sign-off are reserved for Thijil to record independently.

---

## 1. Automated Newman Execution Commands & Metrics (TC-Acc-02 & TC-Aud-01)

### A. Execution Commands
```powershell
# Executed against local test harness at freeze SHA 00add84
cd tests
npx newman run postman/insurance-workflow-api.postman_collection.json \
  -e postman/insurance-workflow-environment.postman_environment.json \
  --folder "01 - Authentication, KYC and Plans (Contract 02)" \
  --folder "03 - Claims and Audit (Contract 01 / SAD)" \
  --reporters cli,junit \
  --reporter-junit-export ../docs/test-evidence/2026-10-30-thijil-newman-00add84.xml
```

### B. Observed Suite Metrics
- **Requests Evaluated:** 9 HTTP requests covering Access Control and Audit Logging
- **Total Assertions Evaluated:** 17 assertions
- **Passed Assertions:** 15
- **Failed Assertions:** 2
- **Execution Time:** ~0.450s (collection subset)

### C. Breakdown by Assigned STP Row

| STP Row ID | Requirement(s) | Scenario Description | Covering Automated Request(s) | Raw Pass / Fail Counts | Observed Findings |
|---|---|---|---|:---:|---|
| **TC-Acc-02** | `INS-F-001`, `INS-SR-003` | Role controls access; staff need an insurer | • `Register Customer (Contract INS-F-001)`<br>• `Login Customer (Contract INS-F-001)`<br>• `Create Plan (Insurer Admin, Contract INS-F-003)`<br>• `Deactivate Plan (Insurer Admin, Contract INS-F-003)` | 7 pass / 0 fail | Customer registration (201) and login (200) enforce credentials and session handling. Insurer Admin creates (201) and deactivates (200) plans with insurer scoping. (Developer tests in `AuthenticationServiceTest.java` and `PlanControllerIntegrationTest.java` passing). |
| **TC-Aud-01** | `INS-F-018`, `INS-SR-008` | Key actions logged; filters work; no UPDATE/DELETE | • `Query Audit Logs - System Admin (200 OK)`<br>• `Query Audit Logs - Customer Access Denied (403 Forbidden)`<br>• `Query Audit Logs - Unauthenticated Access Denied (401 Unauthorized)`<br>• `Query Audit Logs - Filter by Action (200 OK)`<br>• `Query Audit Logs - Invalid Filter Range (422 Unprocessable Entity)` | 8 pass / 2 fail | System Admin queries audit logs successfully (200). Non-admin roles rejected with 403. Filtering by action and range validation (422) functional. Unauthenticated access returned 403 instead of expected 401 due to security filter chain order. No mutating endpoints (PUT/POST/DELETE) exist on `/audit`. |

---

## 2. Manual Inspection Checklist for Thijil

### A. TC-UX-01: Main Screens at 360 px, 768 px, and 1366 px
*Requirement: INS-NF-005 (Responsive Layout across Mobile, Tablet, and Desktop)*

| Screen View | 360 px (Mobile) | 768 px (Tablet) | 1366 px (Desktop) | Notes / Check Status |
|---|---|---|---|---|
| **Login / Register** (`/login`, `/register`) | [ ] No horizontal scroll; inputs full-width | [ ] Centered card layout; inputs aligned | [ ] Standard desktop card layout | _(Thijil to inspect)_ |
| **Customer KYC** (`/kyc`) | [ ] File upload and PAN fields stack vertically | [ ] Two-column grid | [ ] Full desktop form | _(Thijil to inspect)_ |
| **Browse / Compare Plans** (`/plans`, `/compare`) | [ ] Plan cards stack single-column; comparison table scrolls | [ ] 2-column cards grid | [ ] 3-column comparison grid | _(Thijil to inspect)_ |
| **Motor Quote** (`/quotes/motor`) | [ ] Quote form single column; breakdown readable | [ ] Form and breakdown side-by-side or stacked | [ ] Full split-screen calculator | _(Thijil to inspect)_ |
| **Health Quote** (`/quotes/health`) | [ ] Member additions stack cleanly | [ ] Form responsive | [ ] Full split-screen calculator | _(Thijil to inspect)_ |
| **Underwriter Dashboard** (`/underwriter`) | [ ] Responsive table / stacked cards | [ ] Responsive queue table | [ ] Full widescreen data grid | _(Thijil to inspect)_ |
| **Claims Officer Desk** (`/claims-officer`) | [ ] Queue readable; appraisal modal fits | [ ] Queue fits with horizontal scrolling | [ ] Full widescreen queue table | _(Thijil to inspect)_ |
| **System Admin Audit Log** (`/admin/audit`) | [ ] Audit filters collapse; log table scrolls | [ ] Filter bar stacked | [ ] Full widescreen filter & log grid | _(Thijil to inspect)_ |

### B. TC-Ntf-01: All 16 SAD §4.1.7 Notification Events
*Requirement: INS-F-016 (Notification Service & Audit Events)*

| Event # | Event Code (SAD §4.1.7) | Triggering Action / Module | Record Produced? | Message Dispatched? | Notes |
|---|---|---|:---:|:---:|---|
| 1 | `USER_REGISTERED` | Customer signs up via `/auth/register` | [ ] | [ ] | _(Thijil to inspect)_ |
| 2 | `PASSWORD_CHANGED` | User updates password | [ ] | [ ] | _(Thijil to inspect)_ |
| 3 | `KYC_SUBMITTED` | Customer uploads KYC documents | [ ] | [ ] | _(Thijil to inspect)_ |
| 4 | `KYC_VERIFIED` | System/admin marks KYC verified | [ ] | [ ] | _(Thijil to inspect)_ |
| 5 | `KYC_REJECTED` | System/admin rejects KYC | [ ] | [ ] | _(Thijil to inspect)_ |
| 6 | `APPLICATION_SUBMITTED` | Policy application completed | [ ] | [ ] | _(Thijil to inspect)_ |
| 7 | `APPLICATION_REFERRED` | Underwriting rule routes to manual queue | [ ] | [ ] | _(Thijil to inspect)_ |
| 8 | `APPLICATION_APPROVED` | Auto or Underwriter approval | [ ] | [ ] | _(Thijil to inspect)_ |
| 9 | `APPLICATION_REJECTED` | Auto or Underwriter rejection | [ ] | [ ] | _(Thijil to inspect)_ |
| 10 | `PAYMENT_COMPLETED` | Payment gateway succeeds | [ ] | [ ] | _(Thijil to inspect)_ |
| 11 | `POLICY_ISSUED` | Active policy document generated | [ ] | [ ] | _(Thijil to inspect)_ |
| 12 | `POLICY_RENEWED` | Renewal payment processed | [ ] | [ ] | _(Thijil to inspect)_ |
| 13 | `POLICY_CANCELLED` | Cancellation request processed & refund computed | [ ] | [ ] | _(Thijil to inspect)_ |
| 14 | `CLAIM_SUBMITTED` | Claim filed with initial documents | [ ] | [ ] | _(Thijil to inspect)_ |
| 15 | `CLAIM_DOCUMENTS_REQUESTED` | Claims Officer requests further evidence | [ ] | [ ] | _(Thijil to inspect)_ |
| 16 | `CLAIM_SETTLED` | Claim payout disbursed and marked settled | [ ] | [ ] | _(Thijil to inspect)_ |
