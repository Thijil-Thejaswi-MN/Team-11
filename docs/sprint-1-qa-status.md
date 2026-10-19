# Sprint 1 QA Coordination & Daily 17:00 Status Tracker

**Sprint:** Sprint 1 (19–23 October 2026)<br>
**QA Lead & Author:** Person 1 — Thijil Thejaswi M.N. (Repository Owner)<br>
**Assigned Work Items:** P1-06 (Motor Quote Engine, Issue [#130](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/130)), P1-10 (Sprint 1 QA Coordination)<br>
**Source of Truth:** Software Test Plan (STP §6 & §8), Requirements Traceability Matrix (`docs/requirements-traceability-matrix.md`), Test Execution Matrix (`docs/test-execution-matrix.md`).

---

## 1. Daily 17:00 QA Status Log

| Date | Test executed | Passed | Failed | Blocked | Open defects | Severity | Owner | Opened/date | Next action |
|---|---|---|---|---|---|---|---|---|---|
| **19 Oct 2026 (Mon)** | **TC-Quote-01** (Motor quote rating rule - Issue #68). Developer test suite (`MotorQuoteServiceTest`, `PlanRatingRuleCacheTest`, `QuoteControllerIntegrationTest` - 29 automated tests). | Developer suite: 29/29 passed (188 backend tests overall passed). Official TC-Quote-01 status: **Not Run** (independent Newman execution by Sharana and load testing by Samarth pending). | 0 | 0 | None | N/A | Thijil | 19 Oct 2026 | Sharana to execute Newman collection (`npm run test:api:quotes`) against running backend for independent peer validation; Samarth to run JMeter load test for INS-NF-001; Thijil to implement health quote engine (INS-F-006 / TC-Quote-02) on Day 2. |
| **20 Oct 2026 (Tue)** | *Pending execution* | | | | | | | | |
| **21 Oct 2026 (Wed)** | *Pending execution* | | | | | | | | |
| **22 Oct 2026 (Thu)** | *Pending execution* | | | | | | | | |
| **23 Oct 2026 (Fri)** | *Pending execution* | | | | | | | | |

---

## 2. Test Execution Details (19 October 2026)

### TC-Quote-01 — Motor Quote Rating Rule Verification

- **Implementation Task:** Issue [#130](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/130) (`[P1-06] Implement motor quote engine`, 5 Story Points, Sprint 1, Assigned: Thijil)
- **Requirement:** `INS-F-005` (Issue [#1](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/1), 3 Story Points), `INS-NF-001` (Issue [#13](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/13))
- **Test Case Issue:** Issue [#68](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/68)
- **Performance Test Case:** Issue [#97](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97) (`[TC-Perf-01] Quote response time`)
- **Author:** Thijil (QA Lead)
- **Designated Independent Executor:** Sharana (Functional Test Engineer)
- **Performance Test Owner:** Samarth (Non-functional & Security Test Engineer)
- **Official Execution Status:** **Not Run** (Retained in RTM and TEM until independent Newman execution against running app and load test are completed)

#### Step-by-Step Developer Verification Evidence

| Step | Description / Input | Expected Result | Actual Developer Test Result | Status | Verification Evidence |
|---|---|---|---|---|---|
| **1** | POST `/quotes/motor`: Plan `PLAN-A-M1` (4W), IDV ₹4,00,000, regYear 2019 (age 7), add-on `ZERO_DEP` | Total Premium: ₹9,480.00<br>• Base: ₹8,000.00 (2%)<br>• Age Loading: ₹400.00 (5% of base for age 7)<br>• Add-on: ₹1,080.00 | Total Premium: ₹9,480.00<br>Breakdown: base=8000.00, ageLoading=400.00, ZERO_DEP=1080.00, quoteId=2 persisted | **Dev Verified** | `QuoteControllerIntegrationTest.testTCQuote01_Step1_Age7WithZeroDep` |
| **2** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹4,00,000, regYear 2024 (age 2), no add-ons | Total Premium: ₹8,000.00<br>• Base: ₹8,000.00<br>• Age Loading: ₹0.00 (age < 6) | Total Premium: ₹8,000.00<br>Breakdown: base=8000.00, ageLoading=0.00 | **Dev Verified** | `QuoteControllerIntegrationTest.testTCQuote01_Step2_Age2NoAddOns` |
| **3** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹6,00,000, regYear 2024 (age 2), no add-ons | Total Premium: ₹12,000.00<br>• Base: ₹12,000.00 (2%)<br>• Age Loading: ₹0.00 | Total Premium: ₹12,000.00<br>Breakdown: base=12000.00, ageLoading=0.00 | **Dev Verified** | `QuoteControllerIntegrationTest.testTCQuote01_Step3_HigherIdvAge2` |
| **4a** | POST `/quotes/motor`: Plan `PLAN-A-M1`, regYear 2030 (future year) | HTTP 422 Unprocessable Entity<br>Error Code: `VALID-422` | HTTP 422 Unprocessable Entity<br>`{"status":422,"errorCode":"VALID-422","message":"Vehicle registration year (2030) cannot be in the future."}` | **Dev Verified** | `QuoteControllerIntegrationTest.testTCQuote01_Step4a_FutureRegYearReturns422` |
| **4b** | POST `/quotes/motor`: Plan `PLAN-A-M1`, IDV ₹-500 (non-positive IDV) | HTTP 422 Unprocessable Entity<br>Error Code: `VALID-422` | HTTP 422 Unprocessable Entity<br>`{"status":422,"errorCode":"VALID-422","message":"Field 'idv' must be greater than zero."}` | **Dev Verified** | `QuoteControllerIntegrationTest.testTCQuote01_Step4b_NegativeIdvReturns422` |
| **5** | Response time under normal load (INS-NF-001, SRS §5) | At least 95% of quote requests under normal load must respond in $\le 5.0$ seconds in a load test. | Developer micro-measurement is ~12 ms in memory. Full API load-test acceptance is **Pending** execution by Samarth. | **Pending Load Test** | To be executed by Samarth via JMeter load test plan (`TC-Perf-01` / `TC-Load-01`) |

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
4. **Current Defect Count:** **0 Open Defects** as of 19 October 2026 17:00.

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
| **Person 1 (Thijil)** | QA Lead | C5 Quote Engine, C6 Underwriting, C11 Audit Log | None (P1-06 implementation ready) | Consumes `IPlanQuery` (P1-17 merged), `CurrentUser` (P2-02 merged), `Clock` bean | Green (Motor Quote implemented, Health Quote next) |
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
- [x] Full backend test suite verified (188/188 tests passing)
- [x] RTM and Test Execution Matrix retain `Not Run` pending independent Newman and load test runs
