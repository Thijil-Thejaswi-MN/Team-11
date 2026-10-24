# Sprint 1 Retrospective Report (TEAM-07)

**Project:** Insurance Workflow Automation System  
**Team:** 11  
**Sprint Window:** 19 October 2026 – 23 October 2026 (Close: 24 October 2026)  
**Facilitator:** Person 1 (Thijil Thejaswi M.N. — Repository Owner & QA Lead)  
**Participants:**
- Thijil Thejaswi M.N. (Person 1 — QA Lead & Quoting/Underwriting Core)
- Sharana K. (Person 2 — Functional Test Engineer, Auth & Plans)
- Samarth S. Maladkar (Person 3 — Security/NF Test Engineer, Applications & Policies)
- Raghunandan Kotary (Person 4 — Developer/Environment, Payments & Claims)

---

## 1. Executive Summary & Sprint 1 Delivery Audit

During Sprint 1, Team 11 established the core full-stack architectural baseline, security boundaries, rating algorithms, and end-to-end user workflows:

### Key Delivered Features & Merged PRs
- **Architecture & Infrastructure:**
  - Standardized three-tier repository (`backend/`, `frontend/`, `docs/`) with automated multi-job GitHub Actions CI (`Backend CI`, `Frontend CI`, `Validate repository structure`).
  - MySQL 8 dev database with Hibernate update/create-drop isolation, injectable test clock (`/api/v1/test/clock`), and disk file store with AES-256-GCM encryption at rest.
- **Authentication & RBAC (P2-05, P2-06):**
  - BCrypt password hashing (work factor $\ge 12$), JWT tokens, and account lockout after 5 failed attempts (HTTP 423 `AUTH-423`).
  - Plan management API with Insurer Admin RBAC, browse and compare UI views.
- **Quoting & Underwriting Engine (P1-06, P1-07, P1-08, P1-18):**
  - Motor quoting with age loadings and zero-dep fees; Health quoting with age-banded family floater matrices.
  - Boundary-checked underwriting rule engine with automatic STP approval and risk referral routing.
  - Interactive Customer Quoting screens for motor and health with application quote handoff.
- **Payment & Claims Core (P4-09, P4-17):**
  - Claims core state machine enforcing strict transition rules with HTTP 409 conflict detection.
  - Secure payment checkout page sending only `applicationId`, with server-minted idempotency keys and exponential backoff retry.
- **Policy Issuance & Customer Portal (P3-05, P3-06 — PR #149):**
  - OpenPDF statutory policy schedule generation with IRDAI-compliant terms (15-day Free-Look, pro-rata refund schedule).
  - Idempotent policy numbering (`POL-YYYY-XXXXX`), automated payment-to-issuance pipeline, and `/customer/policies` document download view.
  - 5-stage interactive lifecycle timeline on Customer Applications.

---

## 2. Velocity & Sprint Metrics

| Metric | Target / Baseline | Actual Sprint 1 Outcome | Analysis |
|---|:---:|:---:|---|
| **Nominal Planning Capacity (D-05)** | 100 SP (4 members $\times$ 5 days $\times$ 5 SP) | ~72 SP Completed / Active | Sized responsibly; non-essential scope intentionally deferred to Sprint 2. |
| **CI Pipeline Health** | 100% Green on `main` | 100% Passing | All PRs required green status across Backend CI, Frontend CI, and Structure checks. |
| **Backend Unit & Integration Tests** | High Coverage | 220+ Automated Tests Passing | JUnit 5 test suites cover auth, quoting, underwriting, claims, payments, policies, and file store. |
| **Frontend Tests** | Complete Component Coverage | 31/31 Vitest Tests Passing | 10 test suites covering navigation, forms, viewport responsiveness, payment, and policy views. |

---

## 3. What Went Well (Successes)

1. **Robust Shared Contracts (D-09):** Defining clean Java interfaces (`IPaymentService`, `IClaimService`, `IFileStore`, `Clock`) early prevented integration gridlock between teammates working on parallel branches.
2. **Deterministic Test Infrastructure:** The implementation of `PaymentGatewaySimulator` (supporting success, timeout, and decline modes) and `MutableTestClock` made idempotency and timeout recovery tests 100% reproducible without flaky third-party network calls.
3. **Responsive Design Compliance (INS-NF-005):** Verified accessibility and responsive layout down to 360px mobile viewports without horizontal scrolling across all core screens.
4. **Resilient Team Collaboration:** When Samarth was taken unwell, Thijil stepped in to provide technical assistance, ensuring P3-05 policy issuance and P3-06 screens were built, tested, and opened as PR #149 without blocking the team's sprint milestone.

---

## 4. What Didn't Go Well & Lessons Learned

### A. Process Deviation: Admin Override on PR #147
- **Issue:** PR #147 was prematurely merged to `main` using repository admin privileges without an independent approving review, violating the team's documented working rule **D-01**.
- **Root Cause:** Rushing to close documentation deliverables before the sprint cutoff.
- **Correction:** Documented as an explicit process deviation in `docs/decisions.md` (D-01). Submitted correcting PR #148 (`fix/qa-rtm-tem-honest-correction`) for normal peer review with admin override strictly prohibited. Reconfirmed that branch protection must remain active for all members including the repository owner (`enforce_admins: true`).

### B. Conflation of Developer Tests with Independent STP Execution
- **Issue:** Early draft of the Test Execution Matrix marked 18 tests as "Passed" based on developer build tests run in CI, rather than genuine execution by assigned independent STP testers.
- **Correction:** Reverted all unexecuted tests to `Not Run` in PR #148 (retaining `TC-KYC-01` as `Blocked`). Created a separate dedicated column for **Developer Tests (Author)** so build verification is transparently distinguished from formal STP execution.

### C. Incomplete Dependencies Blocking Tests
- **Issue:** `TC-KYC-01` and `TC-Sec-07` could not be executed end-to-end because the KYC document upload endpoint (`POST /api/v1/kyc/upload`) was scheduled under P2-07 for Sprint 2.
- **Lesson:** Accurately marking dependencies as `Blocked` in the RTM/TEM is an expected and healthy part of agile sprint governance, rather than trying to force a passing status prematurely.

---

## 5. Person 1 (Thijil) Sprint 2 Backlog & Roadmap

The following tasks are assigned to Person 1 (QA Lead & Quoting/Underwriting Core) for Sprint 2 (26–30 October 2026):

| Item ID | Deliverable Title | Target Date | Description & Interfaces |
|---|---|:---:|---|
| **P1-11** | Underwriter Queue Screen & Manual Review | Day 1 (26 Oct) | Web desk for Underwriters to review `REFERRED` applications, examine risk profile, approve or reject with justification, and update application status. |
| **P1-12** | Claims Officer Desk & Settlement Screen | Day 2 (27 Oct) | Claims assessment portal: view filed claims, inspect incident documents, calculate approved payout amount, and transition to `SETTLED` or `REJECTED`. |
| **P1-19** | Auto-Close Stale Claims Scheduled Job | Day 3 (28 Oct) | Spring `@Scheduled` cron job checking for claims in `Document Verification` inactive for > 15 days, auto-closing them as `Closed - No Response` (SRS §4.7.1, TC-Clm-06). |
| **P1-20** | Duplicate Claim Detection Engine | Day 3 (28 Oct) | Algorithm detecting duplicate claims on same policy and incident date within 48 hours (TC-Clm-05). |
| **P1-13** | Audit Log Retention & Archival Job | Day 4 (29 Oct) | *Deferrable tier-b item:* Archival policy for audit entries older than statutory retention period. |
| **P1-14 / P1-16** | QA Coordination, Final RTM & Test Summary Report | Day 5 (30 Oct) | Final STP test execution sign-off, consolidated Test Summary Report (TSR), and Deliverable 2 submission package. |

---

## 6. Action Items for Sprint 2 Kickoff

1. [ ] **Merge PR #148 via Standard Peer Review:** Ensure Sharana or Raghunandan approves PR #148 without admin bypass to restore the corrected TEM and RTM baseline to `main`.
2. [ ] **Review & Merge PR #149 (P3-05 / P3-06):** Complete peer review of Samarth's policy issuance and customer portfolio PR.
3. [ ] **Formalize Flyway Migrations (D-03):** Introduce `org.flywaydb:flyway-core` and versioned SQL migration scripts before Sprint 2 database changes.
4. [ ] **Sprint 1 Video Demo:** Record the complete customer journey (Quote $\rightarrow$ Application $\rightarrow$ Payment $\rightarrow$ Policy Certificate PDF Download) using synthetic, privacy-sanitized demo data.
