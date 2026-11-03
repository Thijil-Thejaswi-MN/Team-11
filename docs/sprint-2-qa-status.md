# Sprint 2 QA Coordination & Daily Status Log (P1-14)

**Project:** Insurance Workflow Automation System  
**Repository:** [Thijil-Thejaswi-MN/Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**QA Lead & Repository Owner:** Thijil Thejaswi M N (Person 1)  
**Sprint Period:** 26 October 2026 – 30 October 2026  
**Document Status:** Active Sprint 2 Quality Record  

---

## 1. Quality Governance & Merge Policy (STP §8 & Decision D-01)

As established in Team 11 Governance and Decision `D-01`:
1. **Zero Self-Merge Policy:** No pull request may be merged by its author. Every PR requires at least one independent review and approval by another teammate before merge.
2. **Zero `--admin` Bypass:** Administrative force-pushes or PR merges bypassing branch protection checks (`--admin`) are strictly forbidden.
3. **CI Gate Enforcement:** All GitHub Actions CI workflows (`Validate repository structure`, `Frontend CI`, `Backend CI`, `API Tests (Newman)`) must be green before merge.
4. **Academic Honesty in STP Test Execution:** Automated unit/integration tests written by developers are logged under "Automated Developer Test Suites". Formal STP test case records (`test-execution-matrix.md`) remain `Not Run` or `Blocked` until genuinely executed against running integrated builds by the designated independent reviewer with exact git commit SHA and execution timestamp. Developer unit tests are not STP executions.

---

## 2. Sprint 2 Scope Decision Baseline (26–27 Oct 2026 / D-10 & D-11)

- **Sprint 1 Actual Delivery:** 60 Story Points delivered and verified out of 83 committed SP (72.3% completion).
- **Sprint 1 Carry-Over:** 18 Story Points carried over to Sprint 2 (`P2-07` 5 SP, `P2-08` 3 SP, `P2-17` 2 SP, `P3-06` 3 SP, `P4-08` 5 SP; P4-08 has since merged in PR #150).
- **Sprint 2 Target Velocity:** Calibrated to ~60 SP based on demonstrated Sprint 1 performance (plan 71 SP + 18 SP carry-over = 89 SP against ~60 SP capacity).
- **Scope Decision & Deferrals (Decision D-10):** Dropped deferrable items from bottom of deferral order, except INS-F-023 which is kept because it is already implemented:
  - **Deferred (17 SP total, marked "D" in RTM):**
    - `INS-F-025` System Admin reports (`P3-11`, 5 SP)
    - `INS-F-021` Role-specific dashboards (`P2-15`, 5 SP)
    - `INS-F-017` Renewal reminders (`P4-13`, 3 SP)
    - `INS-NF-007` 5-year retention (`P1-13`, 2 SP)
    - `INS-F-022` Plan search/filter (`P2-13`, 2 SP)
  - **Kept in Scope:**
    - `INS-F-023` Duplicate claim flag (`P1-20`, 3 SP; already implemented at filing, remaining work is verification and Claims Officer banner)
    - `INS-F-024` View/download policy documents (part of renewal `P3-09`)
- **Claims Responsibility Split (Decision D-11):**
  - Agreed between Raghunandan (P4) and Thijil (P1), not at a meeting.
  - Raghunandan Kotary (P4): Claims backend (entities, `ClaimService` with `payout(...)` and `closeStale()`, `ClaimController` mapping `POST /claims`, `GET /claims`, `GET /claims/{id}`, `GET /claims/policy/{policyId}`, `POST /claims/{id}/verify`, `POST /claims/{id}/decision`).
  - Thijil Thejaswi M N (P1): Claims Officer screens (`P1-12`), claim auto-close scheduled job (`P1-19`), and STP independent verification (`TC-Clm-01` through `TC-Clm-04`, `TC-Clm-06`).

---

## 3. Day 1 (26 Oct 2026) 17:00 Daily Status & Bug Triage

### 3.1 Daily Progress Summary
- **Thijil (P1):**
  - Formulated Sprint 2 Scope Decision (D-10) without a team meeting, based on Sprint 1 actual delivery (60/83 SP, 18 SP carry-over) and shared with the team for objection.
  - Recorded Decisions D-10 and D-11 in `docs/decisions.md` and updated `docs/requirements-traceability-matrix.md`.
  - Completed **P1-11** (Underwriter Queue Screen): Replaced placeholder with full dashboard, live referral rule triggers, Approve/Reject modal with mandatory reason, and error banners for 422, 409, and 403.
  - Wired backend `UnderwritingEngine` with `INotify` notification dispatch (`APPLICATION_APPROVED` / `APPLICATION_REJECTED`) and `IAuditLog` audit entries.
  - Added unit test coverage for manual decision notification and audit logging in `UnderwritingEngineIntegrationTest.java`.
  - Wrote automated frontend tests in `UnderwriterDashboardView.test.jsx`. Full frontend suite passing (38/38 tests across 11 files). Backend underwriting integration suite passing.
  - Started **P1-12** (Claims Officer Screens): Built stage queue view and loss appraisal modal in `ClaimsOfficerDashboardView.jsx`.
- **Sharana (P2):** Carrying forward P2-07 (KYC upload endpoint) and P2-08 (KYC upload screen).
- **Samarth (P3):** In progress on P3-05 (Policy Issuance & PDF Generation) and P3-08 (Cancellation & Renewal).
- **Raghunandan (P4):** Backend claims PR merged; notification dispatcher PR #150 merged into `main`.

---

## 4. Day 2 (27 Oct 2026) 17:00 Daily Status & Quality Audit

### 4.1 GitHub Pull Request Register (Verifiable from GitHub API)

#### Merged Pull Requests (Sprint 2 to Date)
| PR # | Title | Branch | Author | Merged At | CI Status |
|---|---|---|---|---|---|
| **#154** | `fix(crypto): implement 64-character hexadecimal key decoding for encryption services` | `fix/p4-11-encryption-key-bootstrap` | Raghunandan | 2026-10-25T07:13Z | All Checks Pass |
| **#153** | `feat(infra): configure Nginx TLS 1.2+, HSTS, two app instances and test volume harness (P4-11)` | `feat/p4-11-tls-environment` | Raghunandan | 2026-10-25T06:17Z | All Checks Pass |
| **#152** | `docs(qa): record P4-10 test execution evidence and harness verification report` | `feat/p4-10-test-execution-evidence` | Raghunandan | 2026-10-25T04:55Z | All Checks Pass |
| **#151** | `ci(newman): add api-tests job with health check and report artifact (P2-09)` | `feat/p2-09-newman-api-ci` | Sharana | 2026-10-24T14:22Z | All Checks Pass |
| **#150** | `feat(notifications): implement real asynchronous notification service (P4-08)` | `feat/p4-08-notification-service` | Raghunandan | 2026-10-24T04:43Z | All Checks Pass |
| **#149** | `feat(policies): P3-05 Policy Issuance & P3-06 Customer Policy Management` | `feature/P3-05-policy-issuance` | Samarth | 2026-10-23T11:52Z | All Checks Pass |

#### Active Open Pull Requests (Awaiting Peer Review)
| PR # | Title | Branch | Author | Opened At | CI Status | Review Status |
|---|---|---|---|---|---|---|
| **#155** | `feat(p1-11, p1-12, team-08): Sprint 2 Planning, Underwriter Queue Screen, and Claims Ownership Split` | `feat/sprint-2-planning-and-p1-11-underwriter-desk` | Thijil (P1) | 2026-10-26T05:58Z | **PASS (4/4)** | Awaiting non-author review (Samarth / Sharana) |
| **#156** | `feat(security): implement P4-12 encryption at rest and sensitive-data log masking` | `feat/p4-12-encryption-masking` | Raghunandan (P4) | 2026-10-26T19:27Z | **PASS (4/4)** | Awaiting review from Thijil |
| **#157** | `feat(p1-12): Claims Officer appraisal desk and verification (INS-F-014, TC-Clm-03)` | `11-p1-12-claims-officer-desk` | Thijil (P1) | 2026-10-27T04:11Z | **PASS (4/4)** | Open without requested reviewer; awaiting peer review |
| **#158** | `feat(p1-19): claim auto-close daily scheduled job (TC-Clm-06)` | `91-p1-19-claim-auto-close` | Thijil (P1) | 2026-10-27T04:16Z | **PASS (4/4)** | Open without requested reviewer; awaiting peer review |

---

### 4.2 Automated Developer Test Suites Execution

*Note: These are automated developer regression suites validating code correctness prior to PR submission. They are not formal STP test case executions.*

| Component | Scope | Tests Run | Pass | Fail | Execution Time | Build Status |
|---|---|---|---|---|---|---|
| **Backend Full Test Suite** (`mvnw -B verify`) | Unit, SpringBootTest, MockMvc, JPA repositories, Outbox dispatch | 567 | 567 | 0 | 43.6s | **SUCCESS** |
| **Claim Controller REST Integration** | Verify (complete/missing), Decide (approve/reject/422/409/403) | 19 | 19 | 0 | 9.3s | **SUCCESS** |
| **Claim Auto-Close Scheduler Integration** | Clock-driven virtual progression (day 0, day 14, day 16, idempotency) | 2 | 2 | 0 | 10.8s | **SUCCESS** |
| **Frontend Component & Integration** (`npm test`) | 11 test suites (Vitest + React Testing Library) | 43 | 43 | 0 | 4.7s | **SUCCESS** |
| **Claims Officer Desk Tests** | Queue default filter, duplicate banner, verify flow, decision flow, 422/409/403 | 12 | 12 | 0 | 0.8s | **SUCCESS** |
| **Frontend Production Build** (`npm run build`) | Vite bundle compilation, asset hashing, CSS extraction | - | - | - | 1.1s | **SUCCESS** |

---

### 4.3 Formal STP System & Integration Test Case Register (27 Oct 2026)

*Strict Governance Rule: An STP test case is marked Passed ONLY when executed against a running integrated deployment by its designated independent executor with verifiable logs, timestamp, and commit SHA. Everything else remains `Not Run` or `Blocked`.*

| Test Case ID | Requirement | Assigned Executor | Status | Verifiable Reason / Execution Evidence |
|---|---|---|---|---|
| **TC-UW-04** | INS-F-011 | Sharana K (Person 2) | **Not Run** | Implementation delivered on PR #155 (`feat/sprint-2-planning-and-p1-11-underwriter-desk`). Awaiting deployment and formal execution by Sharana. |
| **TC-Clm-03** | INS-F-014, INS-F-016 | Sharana K (Person 2) | **Not Run** | Frontend screens and REST verification/decision endpoints delivered in PR #157 (`11-p1-12-claims-officer-desk`). Step 3 (settlement payout trigger) is awaiting backend endpoint. Execution pending by Sharana. |
| **TC-Clm-06** | INS-F-014, INS-F-016 | Sharana K (Person 2) | **Blocked / Not Run** | Auto-close scheduled job and Clock progression delivered in PR #158 (`91-p1-19-claim-auto-close`). Steps 1 and 3 (customer document re-upload on day 14 and 409 conflict on closed claim) are **Blocked** on missing backend document upload endpoint. |
| **TC-Sec-04** | INS-SR-004 | Thijil Thejaswi M N (Person 1) | **Not Run** | Implemented by Raghunandan in PR #156. Verification by QA Lead scheduled for review. |
| **TC-Sec-11** | INS-SR-006 | Thijil Thejaswi M N (Person 1) | **Not Run** | Implemented by Raghunandan in PR #156. Verification by QA Lead scheduled for review. |
| **TC-Ren-01** | INS-F-017 | - | **Deferred** | Deferred for capacity per Sprint 2 scope decision D-10. |
| **TC-Plan-03**| INS-F-022 | - | **Deferred** | Deferred for capacity per Sprint 2 scope decision D-10. |
| **TC-Aud-02** | INS-NF-007 | - | **Deferred** | Deferred for capacity per Sprint 2 scope decision D-10. |
| **TC-Aud-01** | INS-F-018 | Thijil Thejaswi M N (Person 1) | **Not Run** | Audit logging infrastructure in place; formal system test execution pending Sprint 2 integration. |
| **All Other Sprint 2 STPs** | Various | Assigned members | **Not Run** | Test cases not yet executed by their assigned executors. Developer unit tests are not STP executions. |

---

### 4.4 Blockers & Requests to Backend Claims Owner (Person 4: Raghunandan)

1. **Settlement Payout Trigger Endpoint (`TC-Clm-03` Step 3):**
   - **Problem:** `ClaimService.payout(Long claimId, CurrentUser currentUser)` exists in the service layer, but `ClaimController` maps no payout trigger endpoint (e.g. `POST /claims/{id}/payout` or `POST /claims/{id}/settle`).
   - **Action Needed:** Person 4 to expose an endpoint on `ClaimController` so Claims Officers can trigger or view automated disbursement.
   - **Current UI Handling:** Desk displays an informational notice that payout disbursement is pending backend mapping.
2. **Claim Document Upload Endpoint (`TC-Clm-06` Steps 1 & 3):**
   - **Problem:** There is currently no claim document-upload endpoint on the backend (e.g. `POST /claims/{id}/documents`).
   - **Impact:** Blocks full STP verification of `TC-Clm-06` (upload on day 14 resumes verification; upload on closed claim returns 409).
   - **Action Needed:** Person 4 to implement claim document upload endpoint.

---

### 4.5 Peer Review & Merge Governance Reminder

- **PR #155** (Tasks A & B): Open for independent review. Author (Thijil) will NOT self-merge or bypass branch protection.
- **PR #157** (Task C): Open without requested reviewer for team review.
- **PR #158** (Task D): Open without requested reviewer for team review.
- **PR #156** (Security): Open for QA Lead review.

---

## 5. Day 3 (28 Oct 2026) QA & PR Status

### 5.1 Merged PRs on 28 Oct 2026
- No PRs were merged on 28 Oct 2026. (Previous merges on 27 Oct: PR #155, PR #158, PR #166, PR #165).

### 5.2 Open PRs (as of 28 Oct 2026 End-of-Day)
- **PR #157:** feat(p1-12): Claims Officer appraisal desk and verification (INS-F-014, TC-Clm-03) — Author: Thijil (Thijil-Thejaswi-MN), State: Open, Review Decision: CHANGES_REQUESTED (awaiting peer re-approval following CI fix).
- **PR #167:** feat(policies): P3-06 live application screen, P3-09 renewal, P3-10 cancellation, and P3-12 issuance recovery — Author: Samarth (Samarth-S-Maladkar), State: Open, Review Decision: REVIEW_REQUIRED.
- **PR #173:** #19 P2-18 Audit log hardening — Author: Sharana (sharanabasavagouda113-debug), State: Open, Review Decision: REVIEW_REQUIRED.
- **PR #174:** #29 P2-14 Responsive and cross-browser fixes — Author: Sharana (sharanabasavagouda113-debug), State: Open, Review Decision: REVIEW_REQUIRED.

### 5.3 CI Status of main
- Last commit on main on 28 Oct was 697ec0e (Merge PR #166). All 4 CI pipeline checks (Validate repository structure, Frontend CI, Backend CI, API Tests (Newman)) were green (SUCCESS).

### 5.4 STP Test Executions Reported
- No independent STP executions reported on 28 Oct.

---

## 6. Day 4 (29 Oct 2026 — UAT Day) QA & PR Status

### 6.1 Merged PRs on 29 Oct 2026
- **PR #180:** Remove hardcoded metrics from Insurer Admin dashboard (Decision D-10)
  - Author: Thijil Thejaswi M N (Thijil-Thejaswi-MN)
  - Approved by: Samarth S Maladkar (Samarth-S-Maladkar)
  - Merged at: 2026-10-29 17:23:00 UTC (22:52:59 IST) by Samarth (Samarth-S-Maladkar)
  - CI Status: All 4 checks SUCCESS

*(Subsequent early 30 Oct merges: PR #173 merged at 02:05:52 UTC by Thijil; PR #174 merged at 02:54:57 UTC by Thijil).*

### 6.2 Open PRs (Status following Day 4 Review)
- **PR #167:** feat(policies): P3-06 live application screen, P3-09 renewal, P3-10 cancellation, and P3-12 issuance recovery
  - Author: Samarth S Maladkar (Samarth-S-Maladkar)
  - Status: Open (Unmerged)
  - QA Finding: Misplaced controller/view files at root directory, .jav test extensions bypassing Maven build, and insurer isolation check bug. Held unmerged pending author remediation.
- **PR #157:** feat(p1-12): Claims Officer appraisal desk and verification (INS-F-014, TC-Clm-03)
  - Author: Thijil Thejaswi M N (Thijil-Thejaswi-MN)
  - Status: Open. CI green (4/4 checks pass). Awaiting peer re-review approval to satisfy zero self-merge governance.

### 6.3 CI Status of main
- All workflow runs on main following PR #180, #173, and #174 completed with SUCCESS. Current HEAD commit 00add8445c4f0105a20d24c2fc28d3ebd6a32d45.

### 6.4 STP / UAT Executions Reported
- UAT preparation completed (docs/uat-29-oct.md) covering scenarios UAT-01 through UAT-08 across assigned team roles.
- No independent formal STP pass records filed on 29 Oct pending UAT run against integrated deployment.
