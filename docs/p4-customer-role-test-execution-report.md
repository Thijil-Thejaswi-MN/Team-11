# Customer Role Test Execution & Environment Verification Report

**Document Reference:** TEAM-11 / P4-Customer-Role-Execution  
**Task Owner:** Person 4 — Raghunandan Kotary (DevOps & Test Environment Owner)  
**Execution Date:** 30 October 2026, 23:12 IST  
**Environment Baseline Commit SHA:** `00add84` (tracking `origin/main`)  
**Scope of Execution:** Customer Role Workflows for `TC-Pol-02`, `TC-Can-01`, and `TC-Can-02`  
**Explicit Deferral Confirmation:** `TC-Plan-03` (INS-F-022) is Deferred for capacity under Decision `D-10` and was NOT executed.  
**Source of Truth:** Software Test Plan (STP §12), SAD §4.1.6, §4.3.4, SRS §4.6.2, §4.7.1, INS-F-020, INS-F-024, Decision `D-09.10`.  

---

## 1. Executive Summary & Status Overview

In accordance with Sprint 2 testing assignments and Team 11 Academic Honesty & Governance policies, a rigorous verification of the Customer role workflows was conducted against the current `main` codebase.

| Test Case ID | Test Case Title | Req ID | Priority | Verdict | Summary of Findings |
|:---:|:---|:---:|:---:|:---:|:---|
| **TC-Pol-02** | Download policy documents | `INS-F-024`<br>`INS-SR-003` | Medium | **PARTIALLY VERIFIED / PARTIAL BLOCK** | **Step 1 (200 OK & PDF download)** and **Step 3 (403 Forbidden on other customer)** PASSED. **Steps 2 & 4** are BLOCKED by missing `type=renewal` parameter handling in `PolicyController` (owned by Person 3 under P3-09). |
| **TC-Can-01** | Cancellation in the free-look period | `INS-F-020` | High | **BLOCKED** | Backend cancellation endpoint `POST /policies/{id}/cancellation` returns **HTTP 404 Not Found** on `main`. Workflow is pending merge of Person 3's PR #167 (`feature/P3-08-renewal-cancellation-recovery`). |
| **TC-Can-02** | Pro-rata refund, boundaries, claim paid, rejection | `INS-F-020`<br>`INS-SR-003` | High | **UNIT VERIFIED / SYSTEM BLOCKED** | **Steps 1–5 (Refund arithmetic oracles for Days 15, 16, 100, 360, and Claim Paid)** PASSED 100% via `RefundCalculatorTest`. **Steps 6 & 7 (Admin decision & cross-tenant 403)** are BLOCKED by missing cancellation decision endpoints on `main`. |
| **TC-Plan-03** | Search and filter plans | `INS-F-022` | Medium | **DEFERRED** | Deferred for capacity per Decision `D-10`; intentionally excluded from execution. |

---

## 2. Test Environment & Infrastructure Health Status

### 2.1 Host Environment Specifications

- **Operating System:** Microsoft Windows 11 Home (x86_64, Version 10.0.26100)
- **Java Runtime Environment:** Java(TM) SE Runtime Environment (build 25.0.2+10-LTS-69, javac release 17 target)
- **Build Tool:** Apache Maven 3.9.9 via Maven Wrapper (`mvnw.cmd`)
- **Node.js / Vitest Framework:** Node.js v20.x, Vitest v2.1.9, React Testing Library
- **Spring Boot Version:** Spring Boot v3.3.4 (Spring v6.1.13)
- **Active Spring Profile:** `test` (using in-memory H2 MySQL compatibility mode `jdbc:h2:mem:insurance_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE`)

### 2.2 Docker & Container Environment Audit

- **Compose Configuration (`docker-compose.yml`):** Validated via `docker compose config`. Cleanly provisions:
  1. `db`: MySQL 8.0 with `max_connections=200`, persistent volume `insurance-mysql_data`, and health check.
  2. `backend-1` & `backend-2`: Dual Spring Boot backend instances, HikariCP pool sized to 25 connections each, encryption key bootstrap verified, health check polling `/api/v1/actuator/health`.
  3. `frontend`: React SPA production static host with Nginx health check.
  4. `nginx`: TLS reverse proxy (TLS 1.2/1.3 only, HSTS `max-age=31536000`, HTTP->HTTPS 301 redirection).
- **Runtime Daemon Limitation:** The Docker Desktop daemon (`npipe:////./pipe/dockerDesktopLinuxEngine`) is currently stopped on the host system.
- **Reporting Honesty:** Per project instructions, full multi-container Docker stack runtime verification is explicitly marked **UNVERIFIED AT CONTAINER LEVEL**, while all in-process Spring Boot integration suites, MockMvc endpoints, repository JPA constraints, and Vitest frontend suites were executed and verified against real code.

---

## 3. Detailed Test Case Execution Evidence

### 3.1 TC-Pol-02: Download Policy Documents

- **Requirement Traceability:** `INS-F-024` (View & download policy documents), `INS-SR-003` (Role-based access control & tenant isolation).
- **Test Objective:** Verify that an authenticated customer can download their official policy document PDF, that unauthorized customers are rejected with 403 Forbidden, and that renewal certificates are properly differentiated.
- **Preconditions:**
  - Policy `POL-2026-601001` (ID: 8) exists and is owned by Customer `CUST-1` (User ID: 601).
  - Customer `CUST-3` (User ID: 603) is an authenticated customer who does NOT own `POL-2026-601001`.
  - Document is generated and stored in `DiskFileStore` under `DocumentCategory.POLICY`.

#### Step-by-Step Execution Results

| Step | Action Performed | Authenticated User Context | Expected Result (STP §12) | Actual Result | HTTP Status | Verdict |
|:---:|:---|:---|:---|:---|:---:|:---:|
| **1** | `GET /policies/{id}/document?type=policy` | `CUST-1` (ID: 601, Role: `CUSTOMER`) | PDF binary returned with `Content-Disposition: inline; filename="..."` | Returned 2,387-byte OpenPDF binary; `Content-Type: application/pdf`; `Content-Disposition: inline; filename="policy-POL-2026-601001.pdf"` | **200 OK** | **PASS** |
| **2** | `GET /policies/{id}/document?type=renewal` (on renewed policy) | `CUST-1` (ID: 601, Role: `CUSTOMER`) | Renewal certificate PDF returned | Serves base policy schedule because `type` query parameter is ignored by controller | **200 OK** (Base PDF) | **PARTIAL** |
| **3** | `GET /policies/{id}/document` | `CUST-3` (ID: 603, Role: `CUSTOMER`) | Request rejected: 403 Forbidden (not owner) | Rejected by `findAndAuthorizePolicy`; Error payload: `{"errorCode":"ACCESS-403","message":"You do not have permission to access policy 7"}` | **403 Forbidden** | **PASS** |
| **4** | `GET /policies/{id}/document?type=renewal` (unrenewed policy) | `CUST-1` (ID: 601, Role: `CUSTOMER`) | 404 Not Found or clear 'not available' message | Returns 200 OK with base policy schedule; `type` query parameter is not bound | **200 OK** (Expected 404) | **DEFECT / BLOCKED** |

#### Captured Log Evidence (from `CustomerPolicyAndCancellationTestExecution.java`)
```
2026-10-30 23:12:09.368 [main] INFO c.t.i.policies.PolicyController - GET /policies/8/document called by user=CurrentUser{userId=601, role='CUSTOMER', insurerId=null}
2026-10-30 23:12:09.412 [main] INFO c.t.i.filestore.DiskFileStore - DiskFileStore: Stored document ID=129 (originalFilename='policy-POL-2026-601001.pdf', category=POLICY, size=2387 bytes, encryptedSize=2415 bytes)
2026-10-30 23:12:09.431 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Pol-02 Step 1] Status=200, ContentType=application/pdf, Disposition=inline; filename="policy-POL-2026-601001.pdf", Size=2387

2026-10-30 23:12:09.293 [main] INFO c.t.i.policies.PolicyController - GET /policies/7/document called by user=CurrentUser{userId=603, role='CUSTOMER', insurerId=null}
2026-10-30 23:12:09.303 [main] WARN c.t.i.c.e.GlobalExceptionHandler - Application business exception [ACCESS-403]: You do not have permission to access policy 7 (correlationId=4d08e6e9-e12e-46e3-9935-f44e2eaf84fa)
2026-10-30 23:12:09.308 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Pol-02 Step 3] Status=403, Response={"timestamp":"2026-10-30T17:42:09.303346100Z","status":403,"errorCode":"ACCESS-403","message":"You do not have permission to access policy 7","details":[],"correlationId":"4d08e6e9-e12e-46e3-9935-f44e2eaf84fa"}
```

#### Frontend UI Verification
- Component: `frontend/src/views/customer/MyPoliciesView.jsx`
- Automated Suite: `frontend/src/test/MyPoliciesView.test.jsx` (3/3 tests pass)
- Verified Behaviors:
  - Renders policy portfolio table showing policy number, plan name, dates, premium, and status badge.
  - Clicking `Download PDF` triggers `apiClient.get('/policies/{id}/document', { responseType: 'blob' })` and initiates browser file download.
  - Displays statutory IRDAI guidance box explaining the 15-day free-look period.

---

### 3.2 TC-Can-01: Cancellation in the Free-Look Period

- **Requirement Traceability:** `INS-F-020` (Policy cancellation and refund).
- **Test Objective:** Verify that an active policy cancelled within the 15-day statutory free-look period transitions to `CANCELLATION_REQUESTED`, is approved by Insurer Admin with ₹11,400 refund (₹12,000 premium less ₹600 administrative fee), and issues `CANCELLATION_APPROVED` notification.
- **Preconditions:** Active policy, annual term, gross premium ₹12,000, zero settled claims.
- **Assigned Feature Item:** P3-08 / P3-10 (Policy Cancellation Workflow, owned by Person 3 — Samarth).

#### Step-by-Step Execution Results

| Step | Action Performed | Authenticated User Context | Expected Result (STP §12) | Actual Result | HTTP Status | Verdict |
|:---:|:---|:---|:---|:---|:---:|:---:|
| **1** | `POST /policies/{id}/cancellation` with `{"reason":"Customer changed mind"}` | `CUST-1` (ID: 601, Role: `CUSTOMER`) | Policy status -> `CANCELLATION_REQUESTED`; Cancellation request created | Endpoint does NOT exist on `main` branch. Spring returns `404 Not Found` | **404 Not Found** | **BLOCKED** |
| **2** | `POST /cancellations/{id}/decision` with `{"action":"APPROVE"}` | `A-ADM` (Insurer Admin) | Cancellation approved; refund record created; policy status -> `CANCELLED` | Endpoint does NOT exist on `main` branch. Spring returns `404 Not Found` | **404 Not Found** | **BLOCKED** |
| **3** | Check refund amount and policy status | `CUST-1` / Database | Refund = ₹11,400 recorded; `CANCELLATION_APPROVED` sent | Cannot be verified because Steps 1 and 2 could not be executed | N/A | **BLOCKED** |

#### Captured Log Evidence
```
2026-10-30 23:12:08.429 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-01 Step 1] POST /policies/5/cancellation returned HTTP 404
```

#### Blocker Analysis & Cross-Team Dependency
- **Responsible Component:** `PolicyCancellationController.java` (C7 Policy Lifecycle).
- **Responsible Teammate:** Person 3 (Samarth).
- **Status in Repository:** Person 3 has developed `PolicyCancellationController` on branch `feature/P3-08-renewal-cancellation-recovery` in PR #167. However, PR #167 currently has failing CI checks (`Backend CI`, `Frontend CI`, `API Tests (Newman)` all failing) and remains open and unmerged.
- **Adherence to Governance:** Per Team 11 Governance rules, Person 4 does NOT modify another developer's active branch or force-merge unapproved work. `TC-Can-01` is therefore accurately classified as **BLOCKED** awaiting completion of P3-08/P3-10 by Person 3.

---

### 3.3 TC-Can-02: Pro-Rata Refund, Boundaries, Claim Paid, Rejection

- **Requirement Traceability:** `INS-F-020` (Policy cancellation and refund), `INS-SR-003` (Insurer tenant isolation).
- **Test Objective:** Validate pro-rata cancellation refund arithmetic across term boundaries, enforce zero refund when claims are paid or pro-rata formula yields negative amount, and verify rejection and tenant isolation.
- **Test Fixture:** Gross premium ₹12,000, 365-day term (1 Jan 2026 to 31 Dec 2026), 5% statutory administrative fee = ₹600.

#### Step-by-Step Execution Results

| Step | Scenario / Step Description | Formula / Rule | Expected Result (STP / SAD §4.1.6) | Actual Result (from `RefundCalculator`) | Verdict |
|:---:|:---|:---|:---:|:---:|:---:|
| **1** | Cancel on Day 15 (Free-look boundary) | $12,000 - 600$ | ₹11,400.00 | **₹11,400.00** | **PASS** |
| **2** | Cancel on Day 16 (Pro-rata boundary) | $12,000 \times \frac{349}{365} - 600 = 11473.97 - 600$ | ₹10,873.00 (per D-09.10) | **₹10,873.00** | **PASS** |
| **3** | Cancel on Day 100 | $12,000 \times \frac{265}{365} - 600 = 8712.32 - 600$ | ₹8,112.00 | **₹8,112.00** | **PASS** |
| **4** | Cancel on Day 360 | $12,000 \times \frac{5}{365} - 600 < 0 \rightarrow 0$ | ₹0.00 | **₹0.00** | **PASS** |
| **5** | Cancel policy with claim already paid | Rule: Claim settled $\rightarrow$ ₹0 refund | ₹0.00 | **₹0.00** | **PASS** |
| **6** | Insurer Admin rejects cancellation | Policy remains `ACTIVE`; `CANCELLATION_REJECTED` sent | Policy stays Active | **BLOCKED** (Endpoint missing on `main`) | **BLOCKED** |
| **7** | Insurer B tries to decide request for Insurer A | Tenant isolation rule (INS-SR-003) | HTTP 403 Forbidden | **BLOCKED** (Endpoint missing on `main`) | **BLOCKED** |

#### Captured Log Evidence (Steps 1–5 Oracle Verification)
```
2026-10-30 23:12:07.975 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-02 Step 1] Day 15 Refund: Expected=11400, Actual=11400
2026-10-30 23:12:07.978 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-02 Step 2] Day 16 Refund: Expected=10873, Actual=10873
2026-10-30 23:12:07.979 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-02 Step 3] Day 100 Refund: Expected=8112, Actual=8112
2026-10-30 23:12:07.980 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-02 Step 4] Day 360 Refund: Expected=0, Actual=0
2026-10-30 23:12:07.980 [main] INFO c.t.i.p.CustomerPolicyAndCancellationTestExecution - [TC-Can-02 Step 5] Claim Paid Refund: Expected=0, Actual=0
```

---

## 4. Environment & Integration Defect Log

| Defect ID | Severity | Component | Summary | Root Cause | Owner | Current Status |
|:---|:---:|:---|:---|:---|:---:|:---:|
| **DEF-C7-001** | **High** | Backend (`policies`) | Missing Customer Cancellation Endpoint `POST /policies/{id}/cancellation` | Cancellation controller and service layer not yet merged into `origin/main` | Person 3 (Samarth) | **Open** (Implemented on branch `feature/P3-08-renewal-cancellation-recovery` in PR #167, awaiting CI fix & peer review) |
| **DEF-C7-002** | **High** | Backend (`policies`) | Missing Admin Cancellation Decision Endpoint `POST /cancellations/{id}/decision` | Cancellation decision workflow not yet merged into `origin/main` | Person 3 (Samarth) | **Open** (In PR #167) |
| **DEF-C7-003** | **Medium** | Backend (`policies`) | `PolicyController.getPolicyDocument` ignores `?type=renewal` parameter | `PolicyController` only binds `/policies/{id}/document` without query parameter routing; returns base schedule instead of 404 on unrenewed policy | Person 3 (Samarth) | **Open** (P3-09 scope) |

---

## 5. Traceability Updates

### 5.1 Requirements Traceability Matrix (`docs/requirements-traceability-matrix.md`)
- `INS-F-020` (Cancel policy & refund): Status remains `Not Run` for formal acceptance; developer build evidence updated with `CustomerPolicyAndCancellationTestExecution.java` and `RefundCalculatorTest.java` (12 tests pass). Noted that REST cancellation endpoints are pending PR #167 merge.
- `INS-F-024` (View & download policy documents): Status remains `Not Run` for formal acceptance; developer evidence confirms `PolicyControllerIntegrationTest` (7 tests) and `CustomerPolicyAndCancellationTestExecution` (Step 1 & 3 verified). Renewal document download noted as pending P3-09.
- `INS-F-022` / `TC-Plan-03`: Preserved as **Deferred (D)** under Decision `D-10`.

### 5.2 Test Execution Matrix (`docs/test-execution-matrix.md`)
- `TC-Pol-02`: Updated with execution evidence from 30 Oct 2026 by Person 4. Step 1 (download own PDF) and Step 3 (other customer 403) verified. Steps 2 & 4 partial/blocked on P3-09.
- `TC-Can-01`: Updated with execution evidence from 30 Oct 2026 by Person 4. Marked `Blocked` on backend cancellation endpoints.
- `TC-Can-02`: Updated with execution evidence from 30 Oct 2026 by Person 4. Steps 1–5 unit verified via `RefundCalculatorTest`; Steps 6 & 7 blocked on backend cancellation endpoints.
- `TC-Plan-03`: Preserved as **Deferred**.
