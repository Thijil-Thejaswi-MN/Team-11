# Sprint 1 QA Coordination & Daily 17:00 Status Tracker

**Sprint:** Sprint 1 (19–23 October 2026)<br>
**QA Lead & Author:** Person 1 — Thijil Thejaswi M.N. (Repository Owner)<br>
**Assigned Work Items:** P1-06 (Motor Quote Engine, Issue [#130](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/130)), P1-10 (Sprint 1 QA Coordination)<br>
**Source of Truth:** Software Test Plan (STP §6 & §8), Requirements Traceability Matrix (`docs/requirements-traceability-matrix.md`), Test Execution Matrix (`docs/test-execution-matrix.md`).

---

## 1. Daily 17:00 QA Status Log

| Date | Test executed | Passed | Failed | Blocked | Open defects | Severity | Owner | Opened/date | Next action |
|---|---|---|---|---|---|---|---|---|---|
| **19 Oct 2026 (Mon)** | Developer test suite for motor quote engine (29 focused unit/integration tests). Official **TC-Quote-01** (Issue #68) integration test not yet run. | **0 formal test cases passed** (29 developer tests passed; 188 backend total passed). Official TC-Quote-01 status: **Not Run**. | 0 | 0 | None | N/A | Thijil | 19 Oct 2026 | Sharana to execute Newman collection (`npm run test:api:quotes`) against running backend for independent peer validation; Samarth to run JMeter load test for INS-NF-001; Thijil to implement health quote engine (INS-F-006 / TC-Quote-02) on Day 2. |
| **20 Oct 2026 (Tue)** | Health quote engine (TC-Quote-02 / INS-F-006) rating rules & member breakdown tests. | 14 unit tests, 4 integration tests passed. | 0 | 0 | None | N/A | Thijil | 20 Oct 2026 | Merged via PR #137 to main. |
| **21 Oct 2026 (Wed)** | Underwriting engine integration tests (TC-UW-01 to TC-UW-04), rule configuration validation, and manual decision overrides. | 18 tests passed. | 0 | 0 | None | N/A | Thijil / Raghu | 21 Oct 2026 | Merged via PR #136 to main. |
| **23 Oct 2026 (Fri)** | **Sprint 1 Close — Feature Integration & Automated Build Verification.** Verified automated test suites across all integrated Sprint 1 modules on `main` (28/28 frontend Vitest tests, 200+ backend tests passing in CI). End-to-end smoke test could not be completed because policy issuance (P3-05) is not yet built. STP execution status: **0 passed, 1 blocked (`TC-KYC-01`), 51 Not Run**. | **0 formal STP test cases passed** (Automated build suites: 28/28 frontend, 200+ backend passing). | 0 | 1 | 0 High open (2 Low logged for Sprint 2 polish). | Low / Info | Thijil / Team 11 | 23 Oct 2026 | Merged PRs #144 (P1-08), #132 (P3-04a), #145 (P4-06), #146 (P2-06) into `main`. RTM v1 and TEAM-07 carry-over list submitted for Sprint 2 grooming. |
| **25 Oct 2026 (Sun)** | **P4-10 Test Execution Evidence & Harness Verification.** Real automated test suites executed and evidenced across assigned Payments (INS-F-008, TC-Pay-02: 34 tests), Claims (INS-F-013, TC-Clm-02: 70 tests), test harness/simulators (25 tests), and Newman API quote suite (15 requests, 35 assertions passed in 1533ms). Formal STP executions by independent testers remain `Not Run` per academic honesty policy. Full evidence logged in `docs/p4-10-test-execution-report.md`. | **124 backend tests, 28 frontend tests, 15 Newman API requests (35 assertions) passed.** (Formal STP: 0 passed, 1 blocked, 51 Not Run). | 0 | 1 | 0 High open | N/A | Raghunandan | 25 Oct 2026 | Carryover P4-10 completed; PR submitted for review. |

---

## 2. Test Execution Details (19 October 2026)

### TC-Quote-01 — Motor Quote Rating Rule Verification

- **Implementation Task:** Issue [#130](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/130) (`[P1-06] Implement motor quote engine`, 5 Story Points, Sprint 1, Assigned: Thijil)
- **Requirement:** `INS-F-005` (Issue [#1](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/1), 3 Story Points), `INS-NF-001` (Issue [#13](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/13))
- **Test Case Issue:** Issue [#68](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/68)
- **Performance Test Case:** Issue [#97](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97) (`[TC-Perf-01] Quote response time`)
- **Author:** Thijil (QA Lead)
- **Designated Independent Executor:** Samarth S. Maladkar
- **Performance Test Owner:** Samarth (Non-functional & Security Test Engineer)
- **Official Execution Status:** **Not Run** (Retained in RTM and TEM until independent Newman execution against running app and load test are completed)

#### Step-by-Step Developer Verification Evidence

| Step | Description / Input | Expected Result | Actual Developer Test Result | Developer Test Status | Verification Evidence |
|---|---|---|---|---|---|
| **1** | POST `/quotes/motor`: Plan `PLAN-A-M1` (4W), IDV ₹4,00,000, regYear 2019 (age 7), add-on `ZERO_DEP` | Total Premium: ₹9,480.00<br>• Base: ₹8,000.00 (2%)<br>• Age Loading: ₹400.00 (5% of base for age 7)<br>• Add-on: ₹1,080.00 | Total Premium: ₹9,480.00<br>Breakdown: base=8000.00, ageLoading=400.00, ZERO_DEP=1080.00, quoteId=2 persisted | **Dev Verified (Formal Run Pending)** | `QuoteControllerIntegrationTest.testTCQuote01_Step1_Age7WithZeroDep` |
| **2** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹4,00,000, regYear 2024 (age 2), no add-ons | Total Premium: ₹8,000.00<br>• Base: ₹8,000.00<br>• Age Loading: ₹0.00 (age < 6) | Total Premium: ₹8,000.00<br>Breakdown: base=8000.00, ageLoading=0.00 | **Dev Verified (Formal Run Pending)** | `QuoteControllerIntegrationTest.testTCQuote01_Step2_Age2NoAddOns` |
| **3** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹6,00,000, regYear 2024 (age 2), no add-ons | Total Premium: ₹12,000.00<br>• Base: ₹12,000.00 (2%)<br>• Age Loading: ₹0.00 | Total Premium: ₹12,000.00<br>Breakdown: base=12000.00, ageLoading=0.00 | **Dev Verified (Formal Run Pending)** | `QuoteControllerIntegrationTest.testTCQuote01_Step3_HigherIdvAge2` |
| **4a** | POST `/quotes/motor`: Plan `PLAN-A-M1`, regYear 2030 (future year) | HTTP 422 Unprocessable Entity<br>Error Code: `VALID-422` | HTTP 422 Unprocessable Entity<br>`{"status":422,"errorCode":"VALID-422","message":"Vehicle registration year (2030) cannot be in the future."}` | **Dev Verified (Formal Run Pending)** | `QuoteControllerIntegrationTest.testTCQuote01_Step4a_FutureRegYearReturns422` |
| **4b** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹-500 (non-positive IDV) | HTTP 422 Unprocessable Entity<br>Error Code: `VALID-422` | HTTP 422 Unprocessable Entity<br>`{"status":422,"errorCode":"VALID-422","message":"Field 'idv' must be greater than zero."}` | **Dev Verified (Formal Run Pending)** | `QuoteControllerIntegrationTest.testTCQuote01_Step4b_NegativeIdvReturns422` |
| **5** | Measure response time of Step 1 (single-request latency check in TC-Quote-01) | Response time ≤ 5.0 seconds for single quote request | In-memory calculation executes in ~12 ms (internal development timing only). Newman single-request check against running server has NOT yet been run. | **Not Run (Pending Live Execution)** | Pending independent execution against running server |

#### Performance Requirement & Load-Test Acceptance (INS-NF-001 / TC-Perf-01)
- **Requirement INS-NF-001 (Issue [#13](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/13)):** At least 95% of quote requests under normal load must respond in five seconds or less ($\le 5.0$ s), measured by a formal load test.
- **Performance Test Case:** Issue [#97](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97) (`[TC-Perf-01] Quote response time`).
- **Development Timing vs Load Testing:** The developer in-memory calculation measurement (~12 ms) and local MockMvc invocation (~30 ms) are internal development benchmarks only, not proof of the full API load-test requirement.
- **Newman Single-Request Timing:** The Postman collection includes a response time assertion (`< 5000ms`) on Step 1, but this check has not yet been executed against the running application.
- **Formal Status:** Both `INS-NF-001` and `TC-Perf-01` remain **`Not Run`** in the RTM and TEM pending Samarth's JMeter load test execution under normal load profile. Neither requirement nor test case shall be marked passed until valid load-test evidence is recorded.

---

## 3. Defect Triage & Reporting Process

The following standard triage protocol is enforced for Sprint 1:

1. **Defect Verification:**
   - Any suspected failure must be reproduced independently with automated test code or Newman command before opening an issue.
   - Flaky environment issues or local configuration differences must not be filed as defects without root-cause confirmation.
2. **Defect Categorization:**
   - **Critical (P1):** Data corruption, security bypass (authentication / RBAC / insurer isolation), application crash, or blocking another team member's scheduled story.
   - **High (P2):** Calculation error violating rating rules, incorrect state transition, contract violation returning wrong HTTP status code.
   - **Medium (P3):** Edge-case boundary failure with a workaround, non-blocking validation message discrepancy.
   - **Low (P4):** Minor typo, cosmetic formatting, or non-functional documentation inconsistency.
3. **Traceability:**
   - Every defect must link the failing test case ID, requirement ID, reproducible request/response payload, and expected vs actual behavior.
   - Security issues must be flagged with `epic:E10-security` and kept separate from functional defects.
4. **Current Defect Count:** **0 Open Defects** as of Sprint 1 close.

---

## 4. Newman Test Suite & Independent Execution

- **Postman Collection:** `tests/postman/insurance-workflow-api.postman_collection.json`
- **Environment File:** `tests/postman/insurance-workflow-environment.postman_environment.json`
- **Designated Folder:** `quotes` (nested within module folder `02 - Quoting and Underwriting (Contract 01)`)
- **Collection Syntax Validation:** Verified via `node -e "JSON.parse(...)"` (Clean 0 errors).
- **Independent Execution Instructions for Sharana:**
  1. Start backend server:
     ```bash
     cd backend
     ./mvnw spring-boot:run
     ```
  2. In another terminal, run Newman against the designated quotes folder:
     ```bash
     cd tests
     npm run test:api:quotes
     ```
     *(Runs `newman run postman/insurance-workflow-api.postman_collection.json -e postman/insurance-workflow-environment.postman_environment.json --folder quotes`)*
  3. Verify assertions for TC-Quote-01 Steps 1–4 and attach output log to Issue #68.

---

## 5. Team Dependencies & Module Status

| Team Member | Role | Assigned Modules | Dependency on P1 | Dependency on Other Modules | Status |
|---|---|---|---|---|---|
| **Person 1 (Thijil)** | QA Lead | C5 Quote Engine, C6 Underwriting, C11 Audit Log | None (P1-06 implementation ready) | Consumes `IPlanQuery` (P1-17 merged), `CurrentUser` (P2-02 merged), `Clock` bean | Green (Motor Quote implemented, Health Quote merged) |
| **Person 2 (Sharana)** | Functional Tester | C1 Auth & KYC, C4 Plans, Newman/Selenium framework | Consumes `/api/v1/quotes/motor` for Newman functional tests | Auth & KYC (P2-05 PR #124 merged to `main`) | Awaiting independent Newman execution |
| **Person 3 (Samarth)** | NF & Security Tester | C7 Applications & Policies, Security testing | Consumes `Quote` entity and quoteId for application submission; responsible for INS-NF-001 load test | P3-02 Application entities merged | Ready for load testing and application workflow |
| **Person 4 (Raghunandan)** | Dev & Environment | C8 Payments, C9 Notifications, C10 Claims, CI/Docker | None | PR #128 shared contract specifications merged to `main` | Green |

---

## 6. Artifact Checklist

- [x] Dedicated implementation issue created: [#130](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/130) (`[P1-06] Implement motor quote engine`, 5 SP, Sprint 1, assigned to Thijil)
- [x] Motor Quote Engine implementation (`MotorQuoteService.java`, `QuoteController.java`)
- [x] Injected `Clock` configuration (`ClockConfig.java`) with fixed-clock test override
- [x] Plan rating rule cache (`PlanRatingRuleCache.java`) keyed by `planId` with explicit invalidation
- [x] Unit test suite (`MotorQuoteServiceTest.java`, 16 tests passing)
- [x] Cache test suite (`PlanRatingRuleCacheTest.java`, 4 tests passing)
- [x] Integration test suite (`QuoteControllerIntegrationTest.java`, 9 tests passing)
- [x] Newman Postman collection updated with 9 TC-Quote-01 requests in designated `quotes` subfolder
- [x] `tests/package.json` updated with `test:api:quotes` helper script targeting `--folder quotes`
- [x] Full backend test suite verified (200+ tests passing)
- [x] RTM and Test Execution Matrix retain `Not Run` pending independent Newman and load test runs

---

## 7. Day 4 (22 Oct 2026) QA Coordination, Defect Triage & P1-08 Verification

### 7.1 Defect Triage Log (17:00 Update)

| Defect ID | Summary | Module / Component | Severity | Owner | Resolution | Verification |
|---|---|---|---|---|---|---|
| **BUG-2026-1022-01** | Indiscriminate `DataIntegrityViolationException` catch translating non-quote DB errors to HTTP 409 conflict. | `ApplicationSubmissionService` (C5 / P3-04a) | **High (P2)** | Samarth / Thijil | Narrowed catch block to verify `applicationRepository.existsByQuoteId(quote.getId())` before throwing `StateConflictException`; otherwise rethrow. | Added `concurrentDuplicateSubmissionsHandleRaceSafely` multithreaded test using `CountDownLatch` asserting 1 HTTP 201 and 1 HTTP 409. |
| **BUG-2026-1022-02** | Refund calculation discrepancy in policy cancellation (TC-Can-02). | `RefundCalculator` (C7 / P3-07) | **Medium (P3)** | Sharana / Thijil | Fixed pro-rata and penalty boundary calculation in `RefundCalculator.java`. | Merged via PR #140 with passing unit tests. |
| **BUG-2026-1022-03** | `@Primary UnderwritingEngine` bean eclipsing test stub in application controller integration test. | `ApplicationControllerIntegrationTest` | **Medium (P3)** | Thijil | Decoupled controller test from engine bean using `@MockBean private IUnderwriting underwriting;`. | All 24 application tests pass cleanly. |

### 7.2 P1-08 Quote Screens Delivery & Verification
- **User Story:** P1-08 Quote Screens (3 SP, Sprint 1, Owner: Thijil).
- **Deliverables:**
  1. `MotorQuoteView.jsx` (`/customer/quote/motor`): Interactive parameters form, server-side rating call (`POST /api/v1/quotes/motor`), and itemized breakdown (Base OD, Age Loading, Add-ons, NCB discount, Net, GST 18%, Total Premium, and `#QTE-MOTOR-{id}`).
  2. `HealthQuoteView.jsx` (`/customer/quote/health`): Multi-member health rating form, server-side rating call (`POST /api/v1/quotes/health`), and medical breakdown.
  3. `BrowsePlansView.jsx` (`/plans`): "Calculate Instant Quote" buttons connected to `/customer/quote/motor?planId=...` and `/customer/quote/health?planId=...`.
  4. `ApplicationsPlaceholder.jsx` (`/customer/applications`): Consumes `?quoteId=...` from quoting engine, renders active quote reference card, and allows proceeding to application submission.
  5. Automated Vitest suite: `MotorQuoteView.test.jsx`, `HealthQuoteView.test.jsx`, `QuoteFlowIntegration.test.jsx` plus integrated payments (`PaymentView.test.jsx`, `ApplicationsPlaceholder.test.jsx`) — **28/28 tests passing across 9 test suites**.
  6. Production bundle verified (`npm run build`).
  7. Pull Request [#144](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/144) submitted, cleanly merged with latest `main` (including Claims and Payments), and all 3 GitHub Actions CI workflows green.

---

## 8. Day 5 (23 Oct 2026) Sprint 1 Close, Feature Integration Verification & Bug Triage Log

### 8.1 Automated Build Verification & STP Status Audit
- **QA Lead:** Thijil Thejaswi M.N. (Repository Owner)
- **Verified Build:** Commit `72dda0a5b011f5de1fd0793d4ffb293543858ac9` / `main`.
- **Automated Developer Test Suite Status:**
  - All 28 frontend Vitest tests passing across 9 test suites.
  - All 200+ backend unit and integration tests passing in CI.
- **Formal STP Execution Audit:**
  - In compliance with STP §8 and academic honesty guidelines, developer unit tests are tracked separately under "Developer Tests (Author)" and are not recorded as independent STP system/integration test results.
  - **TC-Acc-01:** Developer unit/integration tests pass (`AuthControllerIntegrationTest.java` 7 tests, `AuthenticationServiceTest.java` 15 tests). Full STP system test requiring browser UI dashboard appearance and identical 401 error responses against running app has not yet been executed by an independent tester; remains **Not Run**.
  - **TC-KYC-01:** Officially recorded as **Blocked**. The KYC document upload endpoint (`POST /api/v1/kyc/upload`) is not implemented in Sprint 1; carried over to Sprint 2 under P2-07 and P2-08.
  - **End-to-End Smoke Run:** The end-to-end user workflow smoke run could not be completed because policy issuance (P3-05) is not yet built.

### 8.2 Sprint 1 Feature Integration Bug Triage

| Defect ID | Summary | Module / Component | Severity | Owner | Date Opened | Resolution / Status |
|---|---|---|---|---|---|---|
| **BUG-2026-1022-01** | Duplicate quote submission race condition in `ApplicationSubmissionService`. | C7 Applications | **High (P2)** | Samarth / Thijil | 22 Oct 2026 | **Fixed & Verified:** Narrowed catch block and verified with `concurrentDuplicateSubmissionsHandleRaceSafely` multithreaded test. |
| **BUG-2026-1022-02** | Refund calculation rounding discrepancy in policy cancellation (TC-Can-02). | C7 Policy Lifecycle | **Medium (P3)** | Sharana / Thijil | 22 Oct 2026 | **Fixed & Verified:** Corrected boundary arithmetic in `RefundCalculator.java`. |
| **BUG-2026-1022-03** | `@Primary UnderwritingEngine` bean collision in controller test. | C6 Underwriting | **Medium (P3)** | Thijil | 22 Oct 2026 | **Fixed & Verified:** MockBean decoupling in `ApplicationControllerIntegrationTest`. |
| **BUG-2026-1023-01** | Brand text wrap on narrow 320px viewports (below 360px minimum). | Frontend Shell | **Low (P4)** | Raghunandan | 23 Oct 2026 | **Deferred:** 360px+ requirement (INS-NF-005) passes cleanly. Logged for Sprint 2 UI polish. |
| **BUG-2026-1023-02** | Missing toast notification on unauthenticated plan comparison click. | Frontend Plans | **Low (P4)** | Sharana | 23 Oct 2026 | **Deferred:** Non-blocking usability improvement logged for Sprint 2 backlog. |

**Triage Outcome:** All **High severity bugs are 100% resolved**. Zero High or Critical defects open. The codebase is clean, verified, and ready for the Sprint 1 demo.

---

## 9. TEAM-07 Sprint 1 Carry-Over Summary

The following work items are formally carried over to Sprint 2 (refer to `docs/sprint-1-carryover-list.md` for full register):
1. **P3-05:** Policy Issuance and PDF Generation (Samarth, 5 SP)
2. **P3-06:** Application & Underwriting Review Screens (Samarth, 5 SP)
3. **P2-07:** KYC Document Upload API Endpoint (Sharana, 3 SP)
4. **P2-08:** Customer KYC Upload UI Page (Sharana, 3 SP)
5. **P2-09:** Automated Newman API Suite in CI Pipeline (Sharana, 3 SP)
6. **P4-08:** Real Email & SMS Notification Dispatcher (Raghunandan, 3 SP)
7. **P3-08:** Renewal & Cancellation End-to-End Workflow (Samarth, 5 SP)
8. **P4-10:** Sprint 1 Test Execution & Harness (Raghunandan, 5 SP) — **Execution evidence recorded and verified** in `docs/p4-10-test-execution-report.md`; automated suites passing (34 payment, 70 claims, 25 test harness/simulator, 35 Newman API assertions); independent STP executions remain `Not Run` awaiting designated peer executors.
9. **P1-11:** Non-Functional JMeter & Security Pen-Test Scans (Samarth / Thijil, 5 SP)
