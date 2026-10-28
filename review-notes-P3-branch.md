# Peer Review Findings: Branch feature/P3-08-renewal-cancellation-recovery (PR #167)

**Author:** Samarth S Maladkar (`Samarth-S-Maladkar`)  
**Branch:** `feature/P3-08-renewal-cancellation-recovery` $\to$ `main`  
**Reviewer:** Thijil Thejaswi M N (Person 1: QA Lead & Repo Owner)  
**Date:** 28 Oct 2026 (Evening)  
**Status:** Local Review Notes (DO NOT commit, DO NOT post to GitHub; for Thijil's review only)

---

## 1. Local Execution & Build Verification

### Backend Verification: `.\mvnw.cmd -B verify`
* **Result: FAILED (Compilation Errors — 0 tests executed)**
* **Errors:**
  * `ApplicationQueryController.java`: multiple `cannot find symbol` errors referencing `currentUser.role()` and `currentUser.userId()` (lines 81, 88, 89, 91, 92, 94, 108, 115, etc.).
  * `PolicyCancellationController.java`: multiple `cannot find symbol` errors referencing `currentUser.userId()` and `currentUser.role()` (lines 111, 131, 152, 169, 176, 177, 179, 180, 182, 217, 263, 286, 302, 325, etc.).
  * **Root Cause:** `CurrentUser` is a class with JavaBean getters (`getUserId()`, `getRole()`, `getUserRole()`, `getInsurerId()`), not a record with accessor methods `userId()` or `role()`.

### Frontend Verification: `npm test`
* **Result: FAILED (2 test files failed, 12 passed [14 total]; 2 tests failed, 61 passed [63 total])**
* **Failed Tests:**
  1. `src/test/ApplicationsPlaceholder.test.jsx`:
     * Test: `ApplicationsPlaceholder Component (P4-17 Navigation) > navigates to the payment page when Pay Premium button is clicked`
     * Cause: Button text changed from `"Pay Premium"` to `"Pay Now"`.
  2. `src/test/QuoteFlowIntegration.test.jsx`:
     * Test: `End-to-End Quote Flow Integration (P1-08 -> P3-04a) > allows user to browse plans, navigate to quote calculation, see breakdown, and proceed to application with quoteId`
     * Cause: Header / active quote badge text expected by integration test was removed or modified during the live API rewiring.

### Frontend Build: `npm run build`
* **Result: PASSED** (Vite built production bundle in 1.11s; 123 modules transformed).

---

## 2. File Placement & Naming Defects

1. **`PolicyRenewalController.java` is in the Repository Root Directory:**
   * Committed at `Team-11/PolicyRenewalController.java` instead of `backend/src/main/java/com/team11/insurance/policies/PolicyRenewalController.java`.
   * Because of this, it is completely ignored by Maven and never compiled or included in the Spring Boot context.
2. **Typo in Recovery Scheduler Extension:**
   * Committed as `backend/src/main/java/com/team11/insurance/policies/aPolicyIssuanceRecoveryScheduler.jav` (extension `.jav` instead of `.java`, leading `a`).
   * Maven ignores `.jav` files; the `@Scheduled` component was never compiled or registered.
3. **Typo in Unit/Integration Test Extension:**
   * Committed as `backend/src/test/java/com/team11/insurance/policies/aPolicyLifecycleExtensionTest.jav` (extension `.jav` instead of `.java`, leading `a`).
   * No tests from this file were executed during `mvn verify`.
4. **Non-Existent Method Call in `PolicyRenewalController.java`:**
   * Invokes `RenewalQuoteCalculator.isWithinRenewalWindow(policy.getEndDate(), today)`.
   * `RenewalQuoteCalculator` has no method named `isWithinRenewalWindow`; the actual public method is `requireWithinWindow(LocalDate expiryDate, LocalDate today)` which throws `StateConflictException`. Once moved into `src/`, this will cause immediate compilation failure.

---

## 3. STP Compliance & Requirement Verification

### TC-Ren-02 (Renewal Quote, NCD, Linked Policy, Renewal Certificate)
* **Status:** Incomplete / Non-functional due to controller misplacement.
* Logic correctly invokes `RenewalQuoteCalculator.quote(...)` and links `renewed.setPreviousPolicyId(policy.getId())`.
* Dispatches `IFileStore` renewal certificate.

### TC-Ren-03 (Renewal Window Boundaries: E-31 409, E-30 200, E+15 200, E+16 409)
* **Status:** Incomplete / Non-functional.
* Intended logic checks `30` days before to `15` days after expiry, but calls the non-existent `isWithinRenewalWindow` method.

### TC-Can-01 & TC-Can-02 (Cancellation & Refunds: 11,400 / 10,874 / 8,112 / 0, Reject keeps Active, Other Insurer 403)
* **Status:** Incomplete / Security Flaw.
* Rejection correctly preserves `PolicyStatus.ACTIVE`.
* Calls `RefundCalculator.calculateRefund(...)` for statutory pro-rata refunds.
* **Security Defect in Insurer Isolation:** Line in `PolicyCancellationController.java`:
  ```java
  if (!Objects.equals(cancelReq.getInsurerId(), currentUser.userId())) {
      throw new AccessDeniedException("You are not authorized to decide cancellation requests for another insurer.");
  }
  ```
  Compares `cancelReq.getInsurerId()` (an insurer ID) against `currentUser.userId()` (a user ID). It must check `currentUser.getInsurerId()`. Under this code, any Insurer Admin whose User ID does not match the Insurer ID (almost all users) will be falsely rejected with 403.

### TC-Pay-04 (Recovery Job Issues Exactly One Policy)
* **Status:** Non-functional.
* Logic in `aPolicyIssuanceRecoveryScheduler.jav` polls `ApplicationStatus.PAID` applications missing policies and triggers `policyService.issuePolicy(app)`.
* However, file has `.jav` extension and is never compiled or scheduled.

### Date Rules & Injected Clock
* **Status:** PASSED.
* All date/time evaluations in `PolicyCancellationController` and `PolicyRenewalController` inject `Clock` and use `LocalDate.now(clock)` or `Instant.now(clock)`. No unmocked `LocalDate.now()` calls were detected.

---

## 4. UI Completeness & Missing Features

1. **Insurer Admin Cancellation Queue View is MISSING:**
   * No UI screen was implemented for Insurer Admins to view pending cancellation requests or submit `Approve` / `Reject` decisions.
2. **Customer Cancel Policy Button is MISSING:**
   * No button was added to `MyPoliciesView.jsx` allowing a customer to initiate `POST /policies/{id}/cancellation`.
3. **Regression in Existing Frontend Views:**
   * Edits to `ApplicationsPlaceholder.jsx` altered test-critical selectors and button labels (`"Pay Premium"` $\to$ `"Pay Now"`), causing failures in `ApplicationsPlaceholder.test.jsx` and `QuoteFlowIntegration.test.jsx`.

---

## 5. Summary Recommendation for Thijil

**DO NOT MERGE / CHANGES REQUIRED:**
PR #167 cannot be merged into `main` in its current state. The branch fails backend compilation, fails frontend test suites, contains misplaced/misnamed source files, exhibits an insurer isolation bug comparing `insurerId` to `userId`, and lacks the required Insurer Admin cancellation review and customer cancel UI screens.

