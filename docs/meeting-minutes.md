# Team 11 Meeting Minutes — Sprint 0 / Pre-Sprint 1 Alignment

**Project:** Insurance Workflow Automation System  
**Repository:** [Thijil-Thejaswi-MN/Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Date:** 17 October 2026  
**Forum:** Team Alignment & Contract Sign-off Preparation Meeting  
**Document Status:** Working Baseline & Contract Sign-off Review Template  

---

## 1. Attendees and Role Mapping (STP §8 Baseline)

| Member | STP Role | Primary Responsibilities | Attendance |
|---|---|---|---|
| **Thijil Thejaswi M N** (`Thijil-Thejaswi-MN`) | Person 1 / QA Lead | Repository & Merge Owner, RTM, Quotes & Underwriting (C5/C6) | Present / Host |
| **Sharana K** (`sharana-k`) | Person 2 / Functional Test Engineer | Auth, KYC & Plans (C1-C4), Selenium/Newman Test Framework | Present |
| **Samarth B M** (`samarth-b-m`) | Person 3 / Non-Functional & Security Test Engineer | Applications, Policies, Renewals & Cancellations (C7), JMeter & Pen-Testing | Present |
| **Raghunandan Kotary** (`Raghunandan-Kotary`) | Person 4 / Developer & Environment Owner | Build & Test Environments, CI Pipeline, Payments, Notifications, Claims, File Store & Test Clock | Present |

---

## 2. Confirmed Working Decisions (from Decisions Register `docs/decisions.md`)

The following decisions have been reviewed and confirmed or established as provisional baselines:

1. **D-01 (Repository & Branch Protection):** Thijil is merge owner. Every PR requires at least one independent review and passing CI checks. Branch protection on `main` is active.
2. **D-02 (Frontend Framework):** React 18 with Vite, React Router 6, Bootstrap 5.3, and Vitest.
3. **D-03 (Backend Framework):** Java 17 with Spring Boot 3.3.4, Maven, Spring Data JPA, and JUnit 5 / Mockito.
4. **D-04 (Testing Role Division):** Preserves STP §8 role definitions; developers write unit tests for their own components, with independent test verification across members.
5. **D-05 (Planning Capacity):** Provisional assumption of 5 story-point-equivalent units per person-day (100 total capacity for Sprint 1 week, 19–23 Oct 2026).
6. **D-06 (Repository Structure):** Standard layout `backend/`, `frontend/`, `docs/`, `.github/`. Deliverable 1 documents (`SRS.docx`, `SAD.docx`, `STP.docx`) preserved at root.
7. **D-07 (Project Tracking):** GitHub Issues and GitHub Project #1 are the sole sources of truth; Jira is superseded.
8. **D-08 (`tier-b` Convention):** Applied only to medium-priority items explicitly deferrable to Sprint 2.

---

## 3. CI Merge Protection & Branch Enforcement Audit

- **Audit Objective:** Verify that required CI checks on `main` are configured and demonstrably prevent merging broken code.
- **Evidence Verified:**
  1. **P4-04 Normal Pipeline ([PR #60](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/60)):** Merged into `main` by `Thijil-Thejaswi-MN`. GitHub Actions workflow `.github/workflows/ci.yml` successfully runs `Validate repository structure`, `Frontend CI`, and `Backend CI`.
  2. **Deliberate Failure Demo ([PR #61](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/61)):** Verified that a broken frontend test (`CiFailingDemo.test.js`) failed `Frontend CI`. GitHub blocked merging with `mergeable_state: blocked/behind`. PR #61 was closed unmerged on `2026-10-16T03:45:20Z`.
  3. **Branch Protection Status on `main`:**
     - `enabled`: `true`
     - `enforcement_level`: `"everyone"`
     - Required contexts: `["Validate repository structure", "Frontend CI", "Backend CI"]`
- **Audit Conclusion:** Required CI checks are fully configured and actively blocking broken code for all contributors. No configuration changes are required from the repository owner.

---

## 4. Contract Sign-Off Status & Unresolved Decisions (D-09)

The draft specification for shared interfaces, storage, and test clock was prepared by Person 4 and submitted under **[PR #128](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/128)** (tracked in **[Issue #127](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/127)**).

### Review Register and Decision Items

| ID | Decision Area | Proposed Option | Required Reviewer(s) | Status |
|---|---|---|---|---|
| **D-09.1** | Payment Result & Error Mapping | Return `PaymentResult` record (`SUCCESS`/`FAILED`), throwing `PaymentInfrastructuralException` only on dead network/gateway. | Person 3 (Samarth) & Person 1 (Thijil) | **Pending Sign-off** |
| **D-09.2** | Payment Idempotency Key Format | Server mints `applicationId + "-" + attemptNo`. Client never passes key. | Person 3 (Samarth) | **Pending Sign-off** |
| **D-09.3** | Zero-Refund Cancellation | If `hasPaidClaim` is true, refund amount = Rs 0 and gateway call is skipped. | Person 3 (Samarth) | **Pending Sign-off** |
| **D-09.4** | Notification Delivery Pattern | Persist outbox table + `@Async` fire-and-forget; 3x retry; non-blocking to business callers. | All 4 Members | **Pending Sign-off** |
| **D-09.5** | `hasPaidClaim(policyId)` Semantics | Returns true if any claim on policy is in status `APPROVED` or `SETTLED`. | Person 3 (Samarth) & Person 1 (Thijil) | **Pending Sign-off** |
| **D-09.6** | Stale Claim Inactivity Threshold | Transition claims in `DOCUMENTS_REQUIRED` to `CLOSED_STALE` after 30 days inactivity. | Person 1 (Thijil) | **Pending Sign-off** |
| **D-09.7** | File Storage Primary Key & Keying | Database primary key `Long id` for entity linking (`Policy.documentFileId`), physical disk file named via UUID. | Person 3 (Samarth) & Person 2 (Sharana) | **Pending Sign-off** |
| **D-09.8** | File Storage Security Controls | Max 5 MB, magic-byte inspection (PDF/JPG/PNG), canonical path traversal check, AES-256 encryption at rest. | Person 3 (Samarth) | **Pending Sign-off** |
| **D-09.9** | Clock Abstraction & Profile Isolation | Standard `java.time.Clock` bean with `MutableTestClock` in `dev`/`test` and `POST /api/v1/test/clock`; fail startup in `prod`. | Person 2 (Sharana) & Person 3 (Samarth) | **Pending Sign-off** |

### Questions Awaiting Sign-Off in Meeting
- [ ] **Q1 (Payments):** Does Person 3 agree with `PaymentResult` and the `applicationId-attemptNo` idempotency key format?
- [ ] **Q2 (Payments):** Does Person 3 confirm skipping gateway calls for Rs 0 cancellation refunds?
- [ ] **Q3 (Notifications):** Is the `NotificationType` enum list accepted as complete for Sprint 1?
- [ ] **Q4 (Claims):** Does Person 1 accept the `Claims Officer` decision payload format (`decision`, `assessedAmount`, `rejectionReason`)?
- [ ] **Q5 (Claims):** Should `hasPaidClaim(policyId)` treat `APPROVED` claims (prior to payout) as paid for policy cancellation refunds, or strictly `SETTLED`?
- [ ] **Q6 (File Store):** Does the team approve `Long id` as the document primary key for compatibility with `Policy.documentFileId`?
- [ ] **Q7 (Clock):** Do Person 2 and Person 3 agree on `POST /api/v1/test/clock` payload actions (`ADVANCE`, `SET`, `RESET`) for Newman and Selenium tests?

---

## 5. P4-05 & P4-06 Component Ownership & Implementation Plan

- **Owner:** Person 4 — Raghunandan Kotary (Developer / Environment Owner)
- **Status:** **BLOCKED** pending explicit approval and merge of PR #128.

### P4-05: Test Infrastructure Scope
1. **Injectable Clock & Test Endpoint:** `MutableTestClock`, `TestClockController` (`POST /api/v1/test/clock`, `GET /api/v1/test/clock`), with `@Profile({"dev", "test"})` and strict `prod` startup failure.
2. **Canonical Test DataLoader:** `TestDataLoader` reading Thijil's canonical fixtures (`users.json`, `insurers.json`, `plans.json`, `policies.json`, `claims.json`) without data fabrication.
3. **Failure Simulators:**
   - Payment Gateway Simulator (`MockPaymentGateway` with timeout, decline, success modes).
   - Messaging Failure Simulator (`MockNotificationSender` with retry and dead-letter modes).
   - PDF Generation Failure Simulator (`MockPdfGenerator` with IO failure and success modes).
4. **Docker Compose with MySQL 8:** `docker-compose.yml` defining `mysql:8.0` container, port `3306`, database `insurance_db`, healthcheck, and persistent volume.

### P4-06: Interface Stubs Scope
1. **Payment Stub (`PaymentServiceStub`):** Returns `PaymentResult.success` with mock transaction reference and idempotency caching.
2. **Notification Stub (`NotificationServiceStub`):** In-memory capture queue and SLF4J logging for verification in tests.
3. **Claims Stub (`ClaimServiceStub`):** `hasPaidClaim(policyId)` returning `false` (or querying seeded fixtures) to unblock policy cancellation tests.
4. **Local Disk File Storage (`DiskFileStore`):** Local filesystem storage under `./storage/uploads/` with UUID file keys, path traversal checks, and 5 MB / magic-byte enforcement.

---

## 6. Environment Resources and Availability

| Resource | Purpose | Configuration / Endpoint | Status |
|---|---|---|---|
| **Vite Dev Server** | Frontend Development & Review | `http://localhost:5173` | Active & verified |
| **Spring Boot Backend** | Backend API Server | `http://localhost:8080/api/v1` | Active & verified |
| **H2 In-Memory DB** | Unit & Integration Tests | `jdbc:h2:mem:insurance_testdb;MODE=MySQL` | Active in `test` profile |
| **MySQL 8.0 (Docker)** | Local Development DB | `localhost:3306/insurance_db` via Docker Compose | Planned in P4-05 |
| **GitHub Actions CI** | CI Pipeline & Pull Request Checks | Ubuntu-latest, Node 20, Java 17 | Active & passing |
| **Postman / Newman** | API Regression Testing | Collections in `tests/newman/` | Framework merged (PR #120) |
| **Selenium WebDriver** | UI End-to-End Testing | Headless Chrome/Firefox | Framework merged (PR #120) |

---

## 7. Action Items and Next Steps

| Action Item | Assignee | Target Date | Status |
|---|---|---|---|
| Review and sign off on P4-03 Draft Contract ([PR #128](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/128)) | Thijil (P1), Sharana (P2), Samarth (P3) | Before Sprint 1 start (19 Oct 2026) | Pending Team Sign-off |
| Add Issue #127 to GitHub Project #1 Board View | Thijil-Thejaswi-MN (Project Admin) | 18 Oct 2026 | Pending Admin Action |
| Implement P4-05 (Test Infrastructure & Docker Compose) | Raghunandan Kotary (P4) | Post Contract Approval | Blocked on P4-03 Sign-off |
| Implement P4-06 (Interface Stubs) | Raghunandan Kotary (P4) | Post Contract Approval | Blocked on P4-03 Sign-off |
| Sprint 1 Planning Poker & Final Story Point Summation | All 4 Members | 19 Oct 2026 | Scheduled |
