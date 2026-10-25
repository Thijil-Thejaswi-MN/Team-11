# P4-10 Test Execution Evidence & Harness Verification Report

**Work Item:** P4-10 — Sprint 1 Test Execution & Harness Verification  
**Task Owner:** Person 4 — Raghunandan Kotary (Developer / Environment Owner)  
**Story Points:** 5 SP  
**Date of Execution:** 24–25 October 2026  
**Source of Truth:** Software Test Plan (STP §6, §8, §12 & Appendix A), SAD §3.4b, §4.1.3, §4.1.5, §4.4, SRS §4.7.1, INS-F-008, INS-F-013, `docs/contracts/p4-contract.md`.  
**Associated GitHub Issues:** [#76 (TC-Pay-02)](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/76), [#87 (TC-Clm-02)](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/87), [#42 (INS-F-008)](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/42), [#43 (INS-F-013)](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/43).  

---

## 1. Executive Summary & Acceptance Criteria

This report delivers the verified test execution evidence and harness verification for **P4-10**, covering the core payments (`C8`) and claims workflow (`C10`) components, test infrastructure harnesses (`PaymentGatewaySimulator`, `MutableTestClock`, `DiskFileStore`, and `TestDataLoader`), frontend customer payment views, and Newman API regression suites.

### P4-10 Acceptance Criteria Audit

| # | Acceptance Criterion | Verification Status | Evidence Section |
|---|---|:---:|---|
| **AC-1** | Real test execution against actual codebase with zero invented timings or fabricated results. | **COMPLETED** | Section 3 (All logs & millisecond timestamps captured from live execution). |
| **AC-2** | Coverage of assigned Payment components (INS-F-008, TC-Pay-02, C8 Payment Adapter, retry, idempotency). | **COMPLETED** | Section 3.1 & 3.2 (34 payment unit, integration, and UI tests passing). |
| **AC-3** | Coverage of assigned Claims components (INS-F-013, TC-Clm-02, C10 Claim State Machine, 30-day rule, stale closure). | **COMPLETED** | Section 3.2 (70 claims state machine, service, and controller tests passing). |
| **AC-4** | Test harness and failure simulator verification (`PaymentGatewaySimulator`, `MutableTestClock`, `DiskFileStore`). | **COMPLETED** | Section 3.2 (25 infrastructure tests passing). |
| **AC-5** | Newman API test suite execution against live running test backend with in-memory H2 database. | **COMPLETED** | Section 3.3 (15 requests, 35 assertions passed in 1533ms). |
| **AC-6** | Explicit classification distinguishing executed automated tests, mocked tests, planned tests, and blocked integration tests. | **COMPLETED** | Section 4 (Categorization & Blockers Matrix). |
| **AC-7** | Traceability updates to TEM and RTM maintaining strict academic honesty policy. | **COMPLETED** | Section 5 (Traceability alignment). |

---

## 2. Test Execution Environment Specifications

All tests were executed against clean working checkouts on the designated test harness environment:

- **Host Operating System:** Microsoft Windows 11 Home (x86_64, Version 10.0.26100)
- **Java Runtime Environment (JRE):** Java(TM) SE Runtime Environment (build 25.0.2+10-LTS-69, javac release 17 target)
- **Build Tool:** Apache Maven 3.9.9 via Maven Wrapper (`mvnw.cmd`)
- **Node.js Runtime:** Node.js v20.x, npm v10.x
- **Frontend Test Framework:** Vitest v2.1.9 with `@testing-library/react` and jsdom environment
- **Backend Test Framework:** JUnit 5 (Jupiter), Mockito 5.11.0, Spring Boot Test 3.3.4 with MockMvc
- **API Test Runner:** Newman v6.2.3 (Postman CLI runner)
- **Database Environment:** In-memory H2 Database (`jdbc:h2:mem:insurance_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`) with Hibernate `create-drop` isolation
- **Active Spring Profiles:** `test`
- **Application Base URL:** `http://localhost:8080` (context path: `/api/v1`)

---

## 3. Real Test Execution Evidence & Logs

### 3.1 Frontend Component & Viewport Tests (Vitest)

- **Execution Command:** `npm test -- --run` (in `frontend/`)
- **Execution Start Time:** `2026-10-24T22:18:10.630944`
- **Execution End Time:** `2026-10-24T22:18:16.404510`
- **Total Duration:** 5.77 seconds (Vitest internal duration: 4.76s)
- **Exit Code:** 0
- **Outcome:** **9 test files passed (9/9), 28 tests passed (28/28), 0 failures**

#### Test File Breakdown

| Test File | Tests Passed | Duration | Coverage Scope |
|---|:---:|:---:|---|
| `src/test/PaymentView.test.jsx` | 8 | 1080 ms | Payment UI: charges only `applicationId`, HTTP 402 retry banner, HTTP 409 conflict, double-click prevention spinner, 404 handling. |
| `src/test/ResponsiveViewport.test.jsx` | 2 | 252 ms | 360px mobile viewport verification (INS-NF-005, TC-UX-01). |
| `src/test/ResponsiveShell.test.jsx` | 3 | 353 ms | Accessible mobile hamburger toggle, navigation drawer collapse. |
| `src/test/apiClient.test.js` | 5 | 18 ms | Axios client interceptors, auth token header attachment, error normalization. |
| `src/test/AppLayout.test.jsx` | 1 | 96 ms | Brand header, children layout container, footer rendering. |
| `src/test/ApplicationsPlaceholder.test.jsx` | 1 | 259 ms | Payment redirection handoff from applications page. |
| `src/test/HealthQuoteView.test.jsx` | 3 | 708 ms | Health quoting form, member selection, server quote calculation. |
| `src/test/MotorQuoteView.test.jsx` | 4 | 757 ms | Motor quoting form, IDV inputs, add-on checkboxes, premium breakdown. |
| `src/test/QuoteFlowIntegration.test.jsx` | 1 | 576 ms | End-to-end plan browse to quote calculation handoff. |

---

### 3.2 Backend Payments, Claims & Test Infrastructure Suites (JUnit 5 / MockMvc)

- **Execution Command:** `mvnw.cmd test -Dtest=PaymentServiceTest,PaymentControllerTest,PaymentGatewaySimulatorTest,ClaimStateMachineTest,ClaimServiceTest,ClaimControllerIntegrationTest,ClaimServiceStubTest,TestClockControllerTest,MutableTestClockTest,DiskFileStoreTest -Dspring.profiles.active=test` (in `backend/`)
- **Execution Start Time:** `2026-10-24T22:19:00.409679`
- **Execution End Time:** `2026-10-24T22:19:33.340724`
- **Total Duration:** 32.93 seconds (Maven build time: 31.538s)
- **Exit Code:** 0
- **Outcome:** **124 tests executed, 124 passed, 0 failures, 0 errors, 0 skipped**

#### Class-by-Class Verification Summary

| Test Class | Category | Tests Run | Failures | Errors | Duration | Key Scenarios Verified |
|---|---|:---:|:---:|:---:|:---:|---|
| `PaymentServiceTest` | Payment Core (C8) | 17 | 0 | 0 | 4.133s | Gateway SUCCESS/DECLINE, 3x timeout retry with backoff, idempotency key atomicity, non-approved 409 conflict, already-paid 409 conflict, null-user 403 rejection. |
| `PaymentControllerTest` | Payment REST API (C8) | 9 | 0 | 0 | 0.604s | POST `/payments` HTTP 200, HTTP 402 with `PAY-402`, HTTP 409 `STATE-409`, HTTP 403 cross-customer, HTTP 401 unauthenticated, GET `/payments/applications/{id}` summary. |
| `PaymentGatewaySimulatorTest` | Harness / Simulator | 7 | 0 | 0 | 0.015s | Simulated SUCCESS, DECLINED, TIMEOUT modes, latency simulation, reset controls. |
| `ClaimStateMachineTest` | Claims Core (C10) | 31 | 0 | 0 | 0.090s | Canonical state machine: `SUBMITTED` -> `DOC_VERIF` -> `ASSESSMENT` -> `PAYMENT_PROC` -> `SETTLED`. Illegal skips throw `StateConflictException` (HTTP 409). Terminal state immutability. |
| `ClaimServiceTest` | Claims Business Logic | 27 | 0 | 0 | 0.861s | 30-day incident window boundary (day 30 ok, day 31 rejected), 15-day stale document auto-closure (`closeStale()`), UC-24 duplicate flagging, settled-only `hasPaidClaim()` (D-09.5), multi-tenant insurer isolation. |
| `ClaimControllerIntegrationTest` | Claims REST API | 12 | 0 | 0 | 18.80s | POST `/claims` 201 Created, GET `/claims/{id}` 200 OK, cross-customer 403, cross-insurer 403, inactive policy 409, 30-day violation 422 `VALID-422`. |
| `ClaimServiceStubTest` | Interface Stubs | 3 | 0 | 0 | 0.011s | Seeded claim retrieval, `hasPaidClaim` behavior. |
| `MutableTestClockTest` | Test Infrastructure | 5 | 0 | 0 | 0.009s | Virtual time advance, set fixed instant, system clock reset, zone offset preservation. |
| `DiskFileStoreTest` | Test Infrastructure | 13 | 0 | 0 | 0.263s | AES-256-GCM encryption at rest, UUID storage keys, 5 MB file size limit (HTTP 413), media type whitelist (HTTP 415), path traversal defense. |

---

### 3.3 Newman API Automated Regression Suite (Postman Runner)

- **Execution Command:** `npx newman run tests/postman/insurance-workflow-api.postman_collection.json -e tests/postman/insurance-workflow-environment.postman_environment.json --folder quotes --reporters cli,json --reporter-json-export tests/reports/p4_10_newman_evidence.json`
- **Execution Start Time:** `2026-10-25T09:42:02.087548`
- **Execution End Time:** `2026-10-25T09:42:59.515734`
- **Newman Test Duration:** 1533 ms (Total including backend cold start & health polling: 57.43 seconds)
- **Exit Code:** 0
- **Outcome:** **15 requests executed, 35 assertions passed, 0 failures (100% pass rate)**
- **Average API Latency:** 20 ms [min: 5ms, max: 154ms, s.d.: 35ms] (Strictly satisfies INS-NF-001 < 5000ms SLA).

#### Executed Newman Requests & Contract Assertions

1. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 1: Age 7 with ZERO_DEP) — 200 OK, Premium: Rs 9,480.00, Response time < 5s.
2. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 2: Age 2 No Add-ons) — 200 OK, Premium: Rs 8,000.00.
3. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 3: Higher IDV 6L) — 200 OK, Premium: Rs 12,000.00.
4. `POST /api/v1/quotes/motor` (TC-Quote-01 Boundary: Age 6) — 200 OK, 5% age loading, Total: Rs 8,400.00.
5. `POST /api/v1/quotes/motor` (TC-Quote-01 Boundary: Age 10) — 200 OK, 5% age loading, Total: Rs 8,400.00.
6. `POST /api/v1/quotes/motor` (TC-Quote-01 Boundary: Age 11) — 200 OK, 0% age loading, Total: Rs 8,000.00.
7. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 4a: Future Registration Year) — 422 Unprocessable Entity, `VALID-422`.
8. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 4b: Negative IDV) — 422 Unprocessable Entity, `VALID-422`.
9. `POST /api/v1/quotes/motor` (TC-Quote-01 Step 4c: Incompatible 2W on 4W Plan) — 422 Unprocessable Entity, `VALID-422`.
10. `POST /api/v1/quotes/health` (TC-Quote-02 Step 1: 1 Member Age 45) — 200 OK, Premium: Rs 4,500.00.
11. `POST /api/v1/quotes/health` (TC-Quote-02 Step 2: 2 Members Ages 30 & 32) — 200 OK, Premium: Rs 6,000.00.
12. `POST /api/v1/quotes/health` (TC-Quote-02 Step 3: 2 Members 10L Coverage) — 200 OK, Premium: Rs 9,600.00.
13. `POST /api/v1/quotes/health` (TC-Quote-02 Step 4: Pre-Existing Condition) — 200 OK, Premium: Rs 7,200.00.
14. `POST /api/v1/quotes/health` (TC-Quote-02 Step 5a: No Members) — 422 Unprocessable Entity, `VALID-422`.
15. `POST /api/v1/quotes/health` (TC-Quote-02 Step 5b: Unsupported Sum Insured 7.5L) — 422 Unprocessable Entity, `VALID-422`.

---

## 4. Test Classification, Simulation Boundary & Blocker Analysis

In strict conformance with academic and QA honesty policies, all test scenarios in Team 11 are classified into four mutually exclusive categories:

```
+---------------------------------------------------------------------------------------------------+
|                                TEAM 11 TEST CLASSIFICATION MATRIX                                 |
+---------------------------------------------------------------------------------------------------+
|  1. EXECUTED AUTOMATED TESTS   |  2. MOCKED / SIMULATED TESTS    |  3. BLOCKED INTEGRATION TESTS  |
|  - 28 Frontend Vitest tests    |  - PaymentGatewaySimulator     |  - TC-Pay-01 (Policy PDF)      |
|  - 124 Backend JUnit 5 tests   |    (DECLINED / TIMEOUT modes)  |  - Full Postman Collection     |
|  - 15 Newman API requests      |  - MutableTestClock             |    (Missing JWT auth scripts,  |
|    (35 passing assertions)     |    (Virtual time advance)       |    /actuator/health context)   |
+--------------------------------+---------------------------------+--------------------------------+
|  4. PLANNED INDEPENDENT STP SYSTEM TESTS (Awaiting Peer Execution in Sprint 1/2)                   |
|  - TC-Pay-02 (System-level failed payment, retry & wrong state by Samarth against live build)      |
|  - TC-Clm-02 (System-level skipped states rejection by Sharana against live build)                 |
+---------------------------------------------------------------------------------------------------+
```

### Detailed Breakdown of Boundaries & Blockers

1. **Category 1: Genuine Executed Automated Tests (Passing):**
   - 28 frontend unit/component tests (`PaymentView.test.jsx`, `ResponsiveViewport.test.jsx`, etc.).
   - 124 backend unit and MockMvc integration tests (`PaymentServiceTest`, `ClaimServiceTest`, etc.).
   - 35 Newman API contract assertions across quoting endpoints.
   - All assertions ran against live code and pass 100%.

2. **Category 2: Mocked / Simulated Tests:**
   - External payment gateway network failures are simulated using `PaymentGatewaySimulator` (in-memory simulator supporting deterministic `DECLINED`, `TIMEOUT`, and `SUCCESS` states). Live external banking gateways (Razorpay/Stripe) are not connected.
   - Time-dependent tests (such as 15-day stale claim auto-closure in `ClaimServiceTest`) use `MutableTestClock` / `Clock.fixed()` to deterministically test day 14 vs. day 15 boundaries without sleeping real days.

3. **Category 3: Blocked Integration Tests & Dependencies:**
   - **TC-Pay-01 (End-to-End Policy Issuance after Payment):** Blocked because policy document generation (OpenPDF schedule rendering, P3-05) was carried over to Sprint 2.
   - **Full Postman Collection Run:** Blocked from 100% pass in CI due to:
     - URL mismatch on Actuator Health Check (`{{baseUrl}}/actuator/health` 404 vs backend `/api/v1/actuator/health` 200).
     - Static placeholder tokens in environment file (`<insurer-admin-bearer-token-placeholder>`) returning HTTP 403 Forbidden on plan management requests.
     - Specification-only endpoints in underwriting and claims folders lacking test assertions.

4. **Category 4: Planned Independent System Tests:**
   - In accordance with STP §8, developer tests written by the author do not replace independent peer testing.
   - **TC-Pay-02** remains officially `Not Run` for independent STP system verification by Samarth S. Maladkar.
   - **TC-Clm-02** remains officially `Not Run` for independent STP integration verification by Sharana.

---

## 5. Traceability Matrix Alignment (TEM & RTM Updates)

### 5.1 Updates Applied to `docs/test-execution-matrix.md`

- **TC-Pay-02:**
  - Status: Retained as `Not Run` (independent execution by Samarth pending).
  - Developer Tests (Author): Updated to record `PaymentServiceTest.java` (17 tests), `PaymentControllerTest.java` (9 tests), and `PaymentView.test.jsx` (8 tests) — **34 developer tests passing**. Cross-referenced to `docs/p4-10-test-execution-report.md`.
- **TC-Clm-02:**
  - Status: Retained as `Not Run` (independent execution by Sharana pending).
  - Developer Tests (Author): Updated to record `ClaimStateMachineTest.java` (31 tests), `ClaimServiceTest.java` (27 tests), and `ClaimControllerIntegrationTest.java` (12 tests) — **70 developer tests passing**. Cross-referenced to `docs/p4-10-test-execution-report.md`.

### 5.2 Updates Applied to `docs/requirements-traceability-matrix.md`

- **INS-F-008 (Payment processing & retry):**
  - Updated Developer Build Evidence to document 34 verified unit, integration, and UI tests passing across `PaymentServiceTest`, `PaymentControllerTest`, and `PaymentView.test.jsx`.
- **INS-F-013 (Claim state machine):**
  - Updated Developer Build Evidence to document 70 verified tests passing across `ClaimStateMachineTest`, `ClaimServiceTest`, and `ClaimControllerIntegrationTest`.

---

## 6. Verification Checklist & Sign-Off

- [x] All 28 frontend Vitest tests executed and passed (0 failures).
- [x] All 124 backend Payments, Claims, and Test Infrastructure tests executed and passed (0 failures).
- [x] All 15 Newman requests (35 assertions) executed and passed against running test backend.
- [x] Real start times, end times, and durations captured without fabricated data.
- [x] Simulation boundaries and unmerged integration dependencies clearly isolated.
- [x] Traceability documents updated in compliance with team academic honesty policy.
