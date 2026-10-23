# Sprint 1 Carry-Over List (TEAM-07)

**Sprint:** Sprint 1 Close (23 October 2026)  
**Deliverable:** TEAM-07 Sprint 1 Closure & Sprint 2 Backlog Transition  
**Author & QA Lead:** Person 1 (Thijil Thejaswi M.N. — Repository Owner)  
**Status:** Approved for Sprint 2 Backlog Grooming  

---

## 1. Executive Summary

At the conclusion of Sprint 1 (23 Oct 2026), Team 11 has merged and verified the core architectural skeleton, security baselines, quoting engines (Motor & Health), rule-based underwriting engine, plan management, application submission, payments adapter, file storage with AES-256-GCM encryption, claims core state machine, and customer quote screens.

The following items represent planned work items that were intentionally deferred or not merged during Sprint 1. They are formally catalogued below as the **Sprint 1 Carry-Over List** to be scheduled for execution during Sprint 2 (26–30 October 2026).

---

## 2. Itemized Carry-Over Register

| Item ID | Work Item Title | Owner | Story Points | Dependent Modules / Contracts | Impacted Test Cases | Sprint 2 Target Date |
|---|---|---|:---:|---|---|---|
| **P3-05** | Policy Issuance and PDF Generation | Samarth (Person 3) | 5 SP | C7 Policy Issuance, C12 File Store | [TC-Pay-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/75), [TC-Pol-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/79), [TC-Pay-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/78) | Day 1 (26 Oct) |
| **P3-06** | Application & Underwriting Review Screens | Samarth (Person 3) | 5 SP | C6 Underwriting, C7 Application | [TC-UW-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/74), [TC-App-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/70) | Day 2 (27 Oct) |
| **P2-07** | KYC Document Upload API Endpoint | Sharana (Person 2) | 3 SP | C2 KYC Service, C12 File Store | [TC-KYC-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/64), [TC-Sec-07](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/109) | Day 1 (26 Oct) |
| **P2-08** | Customer KYC Upload UI Page | Sharana (Person 2) | 3 SP | C2 KYC Service, Frontend Shell | [TC-KYC-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/64) | Day 2 (27 Oct) |
| **P2-09** | Automated Newman API Suite in CI | Sharana (Person 2) | 3 SP | CI Pipeline, Newman Postman collection | Automated regression in GitHub Actions | Day 3 (28 Oct) |
| **P4-08** | Real Email & SMS Notification Dispatcher | Raghunandan (Person 4) | 3 SP | C9 Notifications, SMTP / SMS Gateway | [TC-Ntf-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/92), [TC-Ren-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/81) | Day 2 (27 Oct) |
| **P3-08** | Renewal & Cancellation End-to-End Workflow | Samarth (Person 3) | 5 SP | C7 Policy Lifecycle, C8 Payments | [TC-Ren-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/82), [TC-Can-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/84) | Day 3 (28 Oct) |
| **P4-10** | Sprint 1 Test Execution & Harness | Raghunandan (Person 4) | 5 SP | C8 Payments, C10 Claims, Test Fixtures | [TC-Pay-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/76), [TC-Clm-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/87) | Day 1 (26 Oct) |
| **P1-11** | Non-Functional JMeter & Security Scans | Samarth (Person 3) / Thijil (Person 1) | 5 SP | C5 Quote, C8 Payments, OWASP ZAP | [TC-Perf-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97), [TC-Load-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/98), [TC-Sec-06](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/108) | Day 4 (29 Oct) |

---

## 3. Detailed Scope & Justification

### 3.1 P3-05 — Policy Issuance & PDF Generation (Samarth)
- **Scope:** Upon successful payment settlement (`PaymentStatus.SUCCESS`), automatically transition approved application to active policy (`POL-{YEAR}-{SEQ}`), generate official PDF schedule with digital watermark via `DiskFileStore`, and return document link.
- **Carry-over Justification:** Dependent on payment adapter completion (P4-17 merged late Day 4). Document formatting and PDF library selection (iText 7 / OpenPDF) slated for dedicated Sprint 2 focus.

### 3.2 P2-07 & P2-08 — KYC Document Intake (Sharana)
- **Scope:** Implement `POST /api/v1/kyc/upload` supporting multipart PDF/JPG/PNG uploads (bounded to 5 MB), storing encrypted metadata in `DiskFileStore`, and creating the customer-facing `/customer/kyc` upload portal.
- **Carry-over Justification:** Explains why **TC-KYC-01** is officially marked **Blocked** in RTM v1 and TEM.

### 3.3 P2-09 — Automated Newman Collection in CI (Sharana)
- **Scope:** Introduce an automated GitHub Actions step in `.github/workflows/ci.yml` that boots the backend test container and executes `newman run postman/insurance-workflow-api.postman_collection.json`.
- **Carry-over Justification:** Newman tests currently run locally via developer commands (`npm run test:api:quotes`). Full CI containerization scheduled for Sprint 2.

### 3.4 P4-08 — Production Notification Dispatcher (Raghunandan)
- **Scope:** Replace development `NotificationServiceStub` with active Spring Mail (`JavaMailSender`) and mock SMS gateway (Twilio / SMS simulator) for real asynchronous notification dispatch.
- **Carry-over Justification:** In-memory stub (`NotificationServiceStub`) verified 100% thread-safe and profile-isolated in Sprint 1; real SMTP infrastructure scheduled for Sprint 2.

### 3.5 P4-10 — Sprint 1 Test Execution & Harness (Raghunandan)
- **Scope:** Complete independent execution of assigned Sprint 1 integration and system test cases for payments and claims (TC-Pay-02, TC-Clm-02) against the live running application, including test fixture environment verification.
- **Carry-over Justification:** Payments and claims components were merged into `main` at the conclusion of Sprint 1; live independent execution is scheduled as the first activity of Sprint 2.

---

## 4. Impact on Sprint 2 Capacity & Velocity
- **Total Carry-Over Points:** 37 Story Points.
- **Estimated Sprint 2 Velocity:** ~65–70 Story Points (adjusted from D-05 planning capacity heuristic).
- **Feasibility:** High. Core entity persistence, security filters, rating engines, payment state machines, and claims logic are already active on `main`, enabling Sprint 2 to focus on PDF rendering, UI wire-up, and automated load/pen testing.
