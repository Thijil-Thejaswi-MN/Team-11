# Sharana's Automated Test Execution & Triage Evidence (2 Nov 2026 Post-Merge Cycle)

**Role:** Person 2 — Sharana (`sharanabasavagouda113-debug`), Functional Test Engineer  
**Repository:** [Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Execution Environment:** Local Integrated Build (`backend` Spring Boot test profile against H2 / `frontend` React Vite / Chrome Headless)  
**Commit SHA Tested:** `eb6dd25869b70542d4724214e8049dd7f8abd74b` (short: `eb6dd25`)  
**Date of Record:** 2 November 2026  

---

## 1. Automated Test Suite Execution Commands & Timestamps

### A. Initial Newman API Test Suite
- **Command Line:**  
  `cd tests && npm run test:api:all -- --reporters cli,junit --reporter-junit-export ../docs/test-evidence/2026-11-02-newman-eb6dd25.xml`
- **Execution Window (Observed):**
  - Start Time: `2026-11-02T13:15:31+05:30`
  - End Time: `2026-11-02T13:15:39+05:30`
- **Report Artifact:** `docs/test-evidence/2026-11-02-newman-eb6dd25.xml`
- **Raw Suite Metrics (Initial Run):**
  - Total HTTP Requests / Suites: 38
  - Total Assertions Evaluated: 68
  - Total Passed Assertions: 49
  - Total Failed Assertions: 19

### B. Selenium Core E2E Smoke Suite
- **Command Line:**  
  `cd tests && pytest selenium -v --junitxml=../docs/test-evidence/2026-11-02-selenium-eb6dd25.xml`
- **Execution Window (Observed):**
  - Start Time: `2026-11-02T13:16:18+05:30`
  - End Time: `2026-11-02T13:16:36+05:30`
  - Elapsed Time: 17.43s
- **Report Artifact:** `docs/test-evidence/2026-11-02-selenium-eb6dd25.xml`
- **Raw Suite Metrics:**
  - Total Tests Run: 3
  - Passed: 3
  - Failed: 0
  - Errors: 0
  - Individual Test Results:
    1. `tests/selenium/test_full_smoke.py::test_full_customer_journey_smoke` -> **PASSED** (all 8 customer journey steps)
    2. `tests/selenium/test_home_smoke.py::test_home_page_smoke` -> **PASSED**
    3. `tests/selenium/test_home_smoke.py::test_navigation_to_browse_plans` -> **PASSED**

### C. Newman API Rerun (Post-Triage of Test Setup / Outdated Requests)
- **Command Line:**  
  `cd tests && npm run test:api:all -- --reporters cli,junit --reporter-junit-export ../docs/test-evidence/2026-11-02-newman-eb6dd25-rerun.xml`
- **Execution Window (Observed):**
  - Start Time: `2026-11-02T13:27:24+05:30`
  - End Time: `2026-11-02T13:27:32+05:30`
  - Elapsed Time: 5.1s
- **Report Artifact:** `docs/test-evidence/2026-11-02-newman-eb6dd25-rerun.xml`
- **Raw Suite Metrics (Rerun):**
  - Total HTTP Requests / Suites: 38
  - Total Assertions Evaluated: 70
  - Total Passed Assertions: 68
  - Total Failed Assertions: 2 (Both from known application defect #187)

---

## 2. Newman Failure Triage Table (Initial Run on `eb6dd25`)

| # | Request Name | Supporting STP Case | Expected (Status / Body) | Actual (Status / Body) | Category | Root Cause & Resolution |
|---|---|---|---|---|---|---|
| 1 | `01 - Authentication, KYC and Plans / Actuator Health Check (Real Endpoint)` | `TC-Avail-01` | Status 200 OK, `jsonData.status === "UP"` | Status 404 Not Found (HTML 404 response) | **(c) OUTDATED REQUEST** | The Spring Boot backend uses servlet context-path `/api/v1`. The request URL was `{{baseUrl}}/actuator/health` instead of `{{baseUrl}}/{{apiPrefix}}/actuator/health`. Fixed in `tests/postman` collection URL. |
| 2 | `01 - Authentication, KYC and Plans / Actuator Health Check (Real Endpoint)` | `TC-Avail-01` | `jsonData.status === "UP"` | Parsing HTML error page | **(c) OUTDATED REQUEST** | Same as above. Fixed with URL update. |
| 3 | `01 - Authentication, KYC and Plans / kyc / Submit KYC Documents - Verified PAN (Contract INS-F-002)` | `TC-KYC-01`, `TC-Sec-07` | Status 200 OK or 202 Accepted, `kycStatus: "Verified"` | Status 422 Unprocessable Entity (`At least one KYC document file is required.`) | **(b) TEST SETUP** | The form-data key `files[]` had `type: "file"` with `src: []`. Newman failed to load the missing file source and omitted file parts from the multipart body. Fixed by providing synthetic test fixture `tests/postman/sample-kyc.pdf` and updating `src`. |
| 4 | `01 - Authentication, KYC and Plans / kyc / Submit KYC Documents - Rejected PAN ZZZZZ0000Z (Contract INS-F-002)` | `TC-KYC-01`, `TC-Sec-07` | Status 200 OK or 202 Accepted, `kycStatus: "Rejected"` | Status 422 Unprocessable Entity (`At least one KYC document file is required.`) | **(b) TEST SETUP** | Missing file source in Postman collection. Fixed by linking `tests/postman/sample-kyc.pdf`. |
| 5-6 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Step 1: Motor Quote - Age 7 with ZERO_DEP (PLAN-A-M1)` | `TC-Quote-01`, `TC-Perf-01` | Status 200 OK, premium Rs 9480.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Test step `Deactivate Plan (Insurer Admin)` directly preceding quotes was sending `PATCH /plans/1` with `{active: false}`, deactivating seed plan `PLAN-A-M1` instead of using the newly created plan `{{lastCreatedPlanId}}`. Fixed in Postman collection to target `{{lastCreatedPlanId}}`. |
| 7-8 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Step 2: Motor Quote - Age 2 No Add-ons (PLAN-A-M1)` | `TC-Quote-01` | Status 200 OK, premium Rs 8000.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation in preceding step. Fixed by targeting `{{lastCreatedPlanId}}`. |
| 9-10 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Step 3: Motor Quote - Higher IDV 6L Age 2 (PLAN-A-M1)` | `TC-Quote-01` | Status 200 OK, premium Rs 12000.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation. Fixed. |
| 11-12 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Boundary: Motor Quote - Age 6 Boundary (5% Loading)` | `TC-Quote-01` | Status 200 OK, premium Rs 8400.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation. Fixed. |
| 13-14 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Boundary: Motor Quote - Age 10 Boundary (5% Loading)` | `TC-Quote-01` | Status 200 OK, premium Rs 8400.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation. Fixed. |
| 15-16 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Boundary: Motor Quote - Age 11 Boundary (0% Loading)` | `TC-Quote-01` | Status 200 OK, premium Rs 8000.00 | Status 422 Unprocessable Entity (`Selected plan 'PLAN-A-M1' is inactive`) | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation. Fixed. |
| 17 | `02 - Quoting and Underwriting / quotes / TC-Quote-01 Step 4a: Motor Quote - Future Registration Year (Expect 422)` | `TC-Quote-01` | Status 422, message includes `"future"` | Status 422, message `"Selected plan 'PLAN-A-M1' is inactive"` | **(b) TEST SETUP** | Cascading failure from Plan 1 deactivation; inactive plan error masked registration year validation. Fixed. |
| 18-19 | `03 - Claims and Audit / audit / Query Audit Logs - Unauthenticated Access Denied (401 Unauthorized)` | `TC-Aud-01`, `TC-Sec-03` | Status 401 Unauthorized, `errorCode: "AUTH-401"` | Status 403 Forbidden, `errorCode: "ACCESS-403"` | **(a) APPLICATION DEFECT** | Request sends no Authorization header. Under `dev`/`test` profile, `TestCurrentUserProvider` falls back to `SEEDED_CUSTOMER`. `AuditLogController` evaluates role security first and returns 403 `ACCESS-403` instead of challenging unauthenticated access with 401 `AUTH-401`. Logged as Defect **Issue #187** (Severity: High, Module: Admin / Audit). Not altered in collection per governance rules. |

---

## 3. GitHub Issues for Application Defects

- **Issue #187:** `[Audit API] Unauthenticated GET /api/v1/audit returns 403 ACCESS-403 instead of 401 AUTH-401 (TC-Aud-01 / TC-Sec-03)`
  - **Status:** OPEN
  - **Labels:** `type:bug`, `severity:high`
  - **Module:** `admin` / `AuditLogController` / `TestCurrentUserProvider`
  - **Impact:** Unauthenticated requests receive HTTP 403 `ACCESS-403` rather than HTTP 401 `AUTH-401`.

---

## 4. Automated Coverage & Actual Results for Sharana's Assigned STP Rows

*Note: In accordance with STP Section 8 and team governance rules, raw pass/fail assertion counts are recorded directly from the automated execution reports. No STP row is marked "Passed"; final evaluation and matrix status recording is reserved for Sharana's independent sign-off.*

| STP Row ID | Requirement(s) | Description | Covering Automated Request(s) / Test(s) | Raw Pass / Fail Assertion Counts (Rerun) | Execution Summary / Observations |
|---|---|---|---|:---:|---|
| **TC-UW-04** | `INS-F-011`, `INS-F-016` | Manual review of referred applications | • Newman: `02 - Quoting and Underwriting / underwriting / Get Referred Applications (Underwriter, Contract INS-F-011)`<br>• Newman: `02 - Quoting and Underwriting / underwriting / Underwriter Decision (Contract INS-F-011)` | 0 pass / 0 fail (0 assertions defined) | Both endpoints executed over HTTP; request definitions in Postman collection contain no post-request test assertions. Integration tests in `UnderwritingEngineIntegrationTest.java` (11 tests) and `UnderwriterDashboardView.test.jsx` pass. |
| **TC-Pay-04** | `INS-F-009`, `INS-F-008` | Policy issuance failure after successful payment | None (No automated Newman request or Selenium script covers fault switch / retryPendingIssuance) | - | **manual - not run by automation** |
| **TC-Ren-02** | `INS-F-019`, `INS-F-009` | Renewal quote, payment and renewed policy | None (No automated Newman request covers `/policies/{id}/renewal-quote` or `/renew`) | - | **manual - not run by automation** (Covered in developer backend suite `PolicyRenewalIntegrationTest.java` 12 tests merged on 1 Nov). |
| **TC-Ren-03** | `INS-F-019` | Renewal window boundaries | None (No automated Newman request covers E-31, E-30, E+15, E+16 window checks) | - | **manual - not run by automation** (Covered in `PolicyRenewalIntegrationTest.java`). |
| **TC-Clm-01** | `INS-F-012` | File a claim and eligibility rules | • Newman: `03 - Claims and Audit / File Insurance Claim (Contract INS-F-012)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`POST /claims`); request contains no post-request test assertions. Backend integration tests in `ClaimControllerIntegrationTest.java` (20 tests) pass. |
| **TC-Clm-02** | `INS-F-013` | Claim state machine rejects skipped states | None (No automated Newman request tests state-machine transition restrictions) | - | **manual - not run by automation** (Covered in developer integration suite `ClaimStateMachineTest.java` 31 tests & `ClaimControllerIntegrationTest.java`). |
| **TC-Clm-03** | `INS-F-014`, `INS-F-016` | Verify, assess, decide and settle | • Newman: `03 - Claims and Audit / Assess Claim (Claims Officer, Contract INS-F-014)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`POST /claims/9001/assess`); request contains no post-request test assertions. Component tests in `ClaimsOfficerDashboardView.test.jsx` (12 tests) pass. |
| **TC-Clm-04** | `INS-F-015`, `INS-SR-003` | Track claim status | • Newman: `03 - Claims and Audit / Track Claim Status (Contract INS-F-015)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`GET /claims/9001`); request contains no post-request test assertions. Backend endpoint `GET /claims/{id}` verified. |
| **TC-Clm-05** | `INS-F-023` | Duplicate claim detection | None (No automated Newman request executes duplicate filing sequence) | - | **manual - not run by automation** (Covered in developer integration test `fileClaim_duplicateDetection_flagsSecondClaimOnly` in `ClaimControllerIntegrationTest.java`). |
| **TC-Clm-06** | `INS-F-014`, `INS-F-016` | Closed - No Response after 15 days | None (No automated Newman request triggers clock progression or scheduler) | - | **manual - not run by automation** (Covered in developer integration test `ClaimAutoCloseSchedulerIntegrationTest.java` 2 tests; document upload step is Blocked). |

---

## 5. Evidence Files Generated & Preserved

- `docs/test-evidence/2026-10-29-newman-00add84.xml` (Preserved 29 Oct evidence)
- `docs/test-evidence/2026-10-29-selenium-00add84.xml` (Preserved 29 Oct evidence)
- `docs/test-evidence/2026-10-29-sharana-automated-results.md` (Preserved 29 Oct evidence)
- `docs/test-evidence/2026-11-02-newman-eb6dd25.xml` (New initial Newman run evidence)
- `docs/test-evidence/2026-11-02-selenium-eb6dd25.xml` (New Selenium smoke run evidence)
- `docs/test-evidence/2026-11-02-newman-eb6dd25-rerun.xml` (New post-triage Newman rerun evidence)
