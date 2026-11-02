# Sharana's Automated Test Execution Evidence (29 Oct 2026 UAT Cycle)

**Role:** Person 2 — Sharana (`sharanabasavagouda113-debug`), Functional Test Engineer  
**Repository:** [Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Execution Environment:** Local Integrated Build (`backend` Spring Boot test profile against H2 / `frontend` React Vite / Chrome Headless)  
**Commit SHA Tested:** `00add8445c4f0105a20d24c2fc28d3ebd6a32d45` (short: `00add84`)  
**Execution Timestamp:** 30 October 2026 (Recorded for 29 October 2026 UAT cycle)  

---

## 1. Automated Test Suite Execution Commands & Timestamps

### A. Newman API Test Suite
- **Command Line:**  
  `cd tests && npm run test:api:all -- --reporters cli,junit --reporter-junit-export ../docs/test-evidence/2026-10-29-newman-00add84.xml`
- **Execution Window (Observed):**
  - Start Time: `2026-10-30T10:36:21+05:30`
  - End Time: `2026-10-30T10:36:50+05:30`
  - Elapsed Time: ~29 seconds (Newman run duration: 1.74s)
- **Report Artifact:** `docs/test-evidence/2026-10-29-newman-00add84.xml`
- **Raw Suite Metrics:**
  - Total HTTP Requests / Suites in Collection: 38
  - Total Assertions Evaluated: 54
  - Total Passed Assertions: 35
  - Total Failed Assertions: 19

### B. Selenium Core E2E Smoke Suite
- **Command Line:**  
  `cd tests && pytest selenium -v --junitxml=../docs/test-evidence/2026-10-29-selenium-00add84.xml`
- **Execution Window (Observed):**
  - Start Time: `2026-10-30T10:38:01+05:30`
  - End Time: `2026-10-30T10:39:02+05:30`
  - Elapsed Time: ~61 seconds (pytest run duration: 34.64s)
- **Report Artifact:** `docs/test-evidence/2026-10-29-selenium-00add84.xml`
- **Raw Suite Metrics:**
  - Total Tests Run: 3
  - Passed: 3
  - Failed: 0
  - Errors: 0
  - Individual Test Cases:
    1. `tests/selenium/test_full_smoke.py::test_full_customer_journey_smoke` -> **PASSED**
    2. `tests/selenium/test_home_smoke.py::test_home_page_smoke` -> **PASSED**
    3. `tests/selenium/test_home_smoke.py::test_navigation_to_browse_plans` -> **PASSED**

---

## 2. Automated Coverage for Sharana's Assigned STP Rows

*Note: In accordance with STP Section 8 and team governance rules, raw pass/fail assertion counts are recorded directly from the automated execution reports. No STP row is marked "Passed"; final evaluation and matrix status recording is reserved for Sharana's independent sign-off.*

| STP Row ID | Requirement(s) | Description | Covering Automated Request(s) / Test(s) | Raw Pass / Fail Assertion Counts | Execution Summary / Observations |
|---|---|---|---|:---:|---|
| **TC-UW-04** | `INS-F-011`, `INS-F-016` | Manual review of referred applications | • Newman: `02 - Quoting and Underwriting / underwriting / Get Referred Applications (Underwriter, Contract INS-F-011)`<br>• Newman: `02 - Quoting and Underwriting / underwriting / Underwriter Decision (Contract INS-F-011)` | 0 pass / 0 fail (0 assertions defined) | Both endpoints executed over HTTP; request definitions in Postman collection contain no post-request test assertions. Note: Developer tests in `UnderwritingEngineIntegrationTest.java` (11 tests) and `UnderwriterDashboardView.test.jsx` (7 tests) pass. |
| **TC-Pay-04** | `INS-F-009`, `INS-F-008` | Policy issuance failure after successful payment | None (No automated Newman request or Selenium script covers fault switch / retryPendingIssuance) | - | **manual - not run** |
| **TC-Ren-02** | `INS-F-019`, `INS-F-009` | Renewal quote, payment and renewed policy | None (No automated Newman request covers `/policies/{id}/renewal-quote` or `/renew`) | - | **manual - not run** |
| **TC-Ren-03** | `INS-F-019` | Renewal window boundaries | None (No automated Newman request covers E-31, E-30, E+15, E+16 window checks) | - | **manual - not run** |
| **TC-Clm-01** | `INS-F-012` | File a claim and eligibility rules | • Newman: `03 - Claims and Audit / File Insurance Claim (Contract INS-F-012)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`POST /claims`); request contains no post-request test assertions. Note: Backend integration tests in `ClaimControllerIntegrationTest.java` (20 tests) pass. |
| **TC-Clm-02** | `INS-F-013` | Claim state machine rejects skipped states | None (No automated Newman request tests state-machine transition restrictions) | - | **manual - not run** (Covered in developer integration suite `ClaimStateMachineTest.java` 31 tests & `ClaimControllerIntegrationTest.java`). |
| **TC-Clm-03** | `INS-F-014`, `INS-F-016` | Verify, assess, decide and settle | • Newman: `03 - Claims and Audit / Assess Claim (Claims Officer, Contract INS-F-014)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`POST /claims/9001/assess`); request contains no post-request test assertions. Note: Component tests in `ClaimsOfficerDashboardView.test.jsx` (12 tests) pass. Settlement payout trigger endpoint pending on backend. |
| **TC-Clm-04** | `INS-F-015`, `INS-SR-003` | Track claim status | • Newman: `03 - Claims and Audit / Track Claim Status (Contract INS-F-015)` | 0 pass / 0 fail (0 assertions defined) | Endpoint executed over HTTP (`GET /claims/9001`); request contains no post-request test assertions. Note: Backend endpoint `GET /claims/{id}` verified in `ClaimControllerIntegrationTest.java`. |
| **TC-Clm-05** | `INS-F-023` | Duplicate claim detection | None (No automated Newman request executes duplicate filing sequence) | - | **manual - not run** (Covered in developer integration test `fileClaim_duplicateDetection_flagsSecondClaimOnly` in `ClaimControllerIntegrationTest.java`). |
| **TC-Clm-06** | `INS-F-014`, `INS-F-016` | Closed - No Response after 15 days | None (No automated Newman request triggers clock progression or scheduler) | - | **manual - not run** (Covered in developer integration test `ClaimAutoCloseSchedulerIntegrationTest.java` 2 tests; document upload step is Blocked). |

---

## 3. Supplementary Automated Executions (General Collection)

### A. Quoting Regression (Newman)
- **TC-Quote-01 (Motor):** 7 requests executed, 3 assertions passed, 17 assertions failed.  
  *Observation:* Requests failed with HTTP 422 because test payload specifies plan code `'PLAN-A-M1'` which is marked inactive in the test fixture state, triggering `Selected plan 'PLAN-A-M1' is inactive`.
- **TC-Quote-02 (Health):** 6 requests executed, 14 assertions passed, 0 failures (100% pass rate).

### B. Audit Log API (Newman)
- **5 requests executed:**
  - `Query Audit Logs - System Admin (200 OK)`: 2 passed, 0 failed.
  - `Query Audit Logs - Customer Access Denied (403 Forbidden)`: 2 passed, 0 failed.
  - `Query Audit Logs - Unauthenticated Access Denied`: 2 failed (expected 401, got 403 due to security filter order).
  - `Query Audit Logs - Filter by Action (200 OK)`: 2 passed, 0 failed.
  - `Query Audit Logs - Invalid Filter Range (422 Unprocessable Entity)`: 2 passed, 0 failed.

---

## 4. Evidence Files Generated
- `docs/test-evidence/2026-10-29-newman-00add84.xml`
- `docs/test-evidence/2026-10-29-selenium-00add84.xml`
