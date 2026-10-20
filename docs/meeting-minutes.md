# Team 11 Meeting Minutes — Sprint 0 / Pre-Sprint 1 Alignment

**Project:** Insurance Workflow Automation System  
**Repository:** [Thijil-Thejaswi-MN/Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Date:** 18 October 2026 (Updated following PR #128 review)  
**Forum:** Team Alignment & Contract Sign-off Review  
**Document Status:** Working Baseline & Contract Sign-off Review Record  

---

## 1. Attendees and Role Mapping (STP §8 Baseline)

| Member | STP Role | Primary Responsibilities | Attendance / Status |
|---|---|---|---|
| **Thijil Thejaswi M N** (`Thijil-Thejaswi-MN`) | Person 1 / QA Lead | Repository & Merge Owner, RTM, Quotes & Underwriting (C5/C6) | Reviewer (Submitted PR #128 Review) |
| **Sharana K** (`sharanabasavagouda113-debug`) | Person 2 / Functional Test Engineer | Auth, KYC & Plans (C1-C4), Selenium/Newman Test Framework | Reviewer |
| **Samarth B M** (`Samarth-S-Maladkar`) | Person 3 / Non-Functional & Security Test Engineer | Applications, Policies, Renewals & Cancellations (C7), JMeter & Pen-Testing | Reviewer |
| **Raghunandan Kotary** (`Raghunandan-Kotary`) | Person 4 / Developer & Environment Owner | Build & Test Environments, CI Pipeline, Payments, Notifications, Claims, File Store & Test Clock | Author (Draft v0.2 Prepared) |

---

## 2. Confirmed Working Decisions (from Decisions Register `docs/decisions.md`)

The following decisions have been confirmed or established as working baselines:

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
  2. **Deliberate Failure Demo ([PR #61](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/61)):** Verified that an intentional broken frontend test (`CiFailingDemo.test.js`) failed `Frontend CI`. GitHub blocked merging with `mergeable_state: blocked/behind`. PR #61 was closed unmerged on `2026-10-16T03:45:20Z`.
  3. **Branch Protection Status on `main`:**
     - `enabled`: `true`
     - `enforcement_level`: `"everyone"`
     - Required contexts: `["Validate repository structure", "Frontend CI", "Backend CI"]`
- **Audit Conclusion:** Required CI checks are fully configured and actively blocking broken code for all contributors. No configuration changes are required from the repository owner.

---

## 4. Contract Sign-Off Status & Unresolved Decisions (D-09)

The shared contract specification is tracked under **[Issue #127](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/127)** and submitted in **[PR #128](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/128)**.  
Following the review from Thijil (`CHANGES_REQUESTED` on 17 October 2026), Person 4 prepared **Draft v0.2** resolving all inconsistencies.

### Review Register and Decision Items

| ID | Decision Area | Proposed Option (Draft v0.2) | Required Reviewer(s) | Status |
|---|---|---|---|---|
| **D-09.1** | Payment Result & Error Mapping | Return `PaymentResult` record (`SUCCESS`/`FAILED`) using injected `Clock` factories, throwing `PaymentInfrastructuralException` only on dead network; gateway failure maps to `402 PAY-402`. | Person 3 (Samarth) & Person 1 (Thijil) | **Proposed (Awaiting Sign-off)** |
| **D-09.2** | Payment Idempotency Key Format | Server mints `applicationId + "-" + attemptNo`. Client never passes key. | Person 3 (Samarth) | **Proposed (Awaiting Sign-off)** |
| **D-09.3** | Zero-Refund Cancellation | If `hasPaidClaim` is true, refund amount = Rs 0 and gateway call is skipped. | Person 3 (Samarth) | **Proposed (Awaiting Sign-off)** |
| **D-09.4** | Notification Delivery Pattern | Authoritative 16 events from SAD §4.1.7; outbox table + `@Async` fire-and-forget; 3x retry; non-blocking to business callers. | All 4 Members | **Proposed (Awaiting Sign-off)** |
| **D-09.5** | `hasPaidClaim(policyId)` Semantics | Returns true ONLY if any claim on policy is in status `SETTLED`. An Approved claim does not count as paid for refunds. | Person 3 (Samarth) & Person 1 (Thijil) | **Agreed in PR #128 Review (Pending Final PR Sign-Off)** |
| **D-09.6** | Stale Claim Inactivity Threshold | Transition claims in `Document Verification` with no response to document requests to `Closed - No Response` after 15 days inactivity (SRS §4.7.1, TC-Clm-06). | Person 1 (Thijil) | **Proposed (Awaiting Sign-off)** |
| **D-09.7** | File Storage Primary Key & Keying | SAD Table 3.4b `put(file) returns id, get(id), delete(id)`; database primary key `Long id` for entity linking (`Policy.documentFileId`), physical disk file named via UUID. | Person 3 (Samarth) & Person 2 (Sharana) | **Proposed (Awaiting Sign-off)** |
| **D-09.8** | File Storage Security & Error Mapping | Disallowed types return HTTP 415; files > 5MB return HTTP 413; magic-byte check; AES-256 encryption at rest (STP TC-Sec-07). Virus scan is Proposed. | Person 3 (Samarth) | **Proposed (Awaiting Sign-off)** |
| **D-09.9** | Clock Abstraction & Profile Isolation | Standard `java.time.Clock` bean with `MutableTestClock` preserving zone offsets; mapped at `/test/clock` (effective `/api/v1/test/clock`); fail startup in `prod`. | Person 2 (Sharana) & Person 3 (Samarth) | **Proposed (Awaiting Sign-off)** |

### Questions Awaiting Sign-Off in Meeting
- [ ] **Q1 (Payments):** Does Person 3 (`Samarth-S-Maladkar`) confirm that `refund()` skipping gateway for Rs 0 fulfills all cancellation accounting requirements?
- [ ] **Q2 (Notifications):** Does Person 1 (`Thijil-Thejaswi-MN`) confirm that the 16 events in SAD §4.1.7 are the exact boundary for Sprint 1 testing, with `APPLICATION_SUBMITTED` treated as optional telemetry?
- [ ] **Q3 (Claims):** Does Person 1 (`Thijil-Thejaswi-MN`) approve the aligned `ClaimDecisionRequest` (`decision`, `assessedAmount`, `reason`) matching P1 API Contract §6.2?
- [ ] **Q4 (File Store):** Does Person 2 (`sharanabasavagouda113-debug`) agree that `IFileStore.put/get/delete` returning and taking `Long id` satisfies KYC document intake needs?
- [ ] **Q5 (Clock):** Does Person 2 (`sharanabasavagouda113-debug`) confirm that `POST /api/v1/test/clock` fulfills Newman/Selenium automation needs for testing day 14 vs day 15 stale closure in TC-Clm-06?

---

## 5. P4-05 & P4-06 Component Ownership & Implementation Plan

- **Owner:** Person 4 — Raghunandan Kotary (Developer / Environment Owner)
- **Status:** **BLOCKED** pending explicit approval and merge of PR #128.

### P4-05: Test Infrastructure Scope
1. **Injectable Clock & Test Endpoint:** `MutableTestClock` with zone preservation, `TestClockController` (`POST /api/v1/test/clock`, `GET /api/v1/test/clock`), with `@Profile({"dev", "test"})` and strict `prod` startup failure.
2. **Canonical Test DataLoader:** `TestDataLoader` reading Thijil's canonical fixtures (`users.json`, `insurers.json`, `plans.json`, `policies.json`, `claims.json`) without data fabrication.
3. **Failure Simulators:**
   - Payment Gateway Simulator (`MockPaymentGateway` with timeout, decline, success modes).
   - Messaging Failure Simulator (`MockNotificationSender` with retry and dead-letter modes).
   - PDF Generation Failure Simulator (`MockPdfGenerator` with IO failure and success modes).
4. **Docker Compose with MySQL 8:** `docker-compose.yml` defining `mysql:8.0` container, port `3306`, database `insurance_db`, healthcheck, and persistent volume.

### P4-06: Interface Stubs Scope
1. **Payment Stub (`PaymentServiceStub`):** Returns `PaymentResult.success` using clock-injected factories, mock transaction reference, and idempotency caching.
2. **Notification Stub (`NotificationServiceStub`):** In-memory capture queue and SLF4J logging for verification in tests.
3. **Claims Stub (`ClaimServiceStub`):** `hasPaidClaim(policyId)` returning `false` (or querying seeded fixtures) where only `SETTLED` claims return true.
4. **Local Disk File Storage (`DiskFileStore`):** Local filesystem storage under `./storage/uploads/` implementing `put(file) returns id, get(id), delete(id)` with UUID file keys, path traversal checks, and HTTP 413/415 enforcement.

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
| Re-review and sign off on P4-03 Draft Contract v0.2 ([PR #128](https://github.com/Thijil-Thejaswi-MN/Team-11/pull/128)) | Thijil (P1), Sharana (P2), Samarth (P3) | Before Sprint 1 start (19 Oct 2026) | Pending Team Sign-off |
| Add Issue #127 to GitHub Project #1 Board View | Thijil-Thejaswi-MN (Project Admin) | 18 Oct 2026 | Pending Admin Action |
| Implement P4-05 (Test Infrastructure & Docker Compose) | Raghunandan Kotary (P4) | Post Contract Approval | Blocked on P4-03 Sign-off |
| Implement P4-06 (Interface Stubs) | Raghunandan Kotary (P4) | Post Contract Approval | Blocked on P4-03 Sign-off |
| Sprint 1 Planning Poker & Final Story Point Summation | All 4 Members | 19 Oct 2026 | Scheduled |
