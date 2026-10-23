# Independent Code Review Findings: PR #132 & PR #131
**Repository:** [Team-11](https://github.com/Thijil-Thejaswi-MN/Team-11)  
**Author:** Samarth S Maladkar  
**Reviewer:** Independent Peer Reviewer / QA Lead (Thijil Thejaswi M N)  
**Date:** 20 October 2026  
**Status:** Review Comments Prepared (No GitHub Review Submitted / Not Merged)

---

## 1. Review Summary & High-Level Posture

| PR | Branch | Feature | Latest Head Commit | Current CI Status | Recommendation |
|---|---|---|---|---|---|
| **#132** | `feature/P3-04a-application-submission` | POST /applications Submission Flow | `41d683b` | ❌ Failing (`Backend CI`) | **Changes Requested** (Blocking race condition, test isolation failure, contract mismatch) |
| **#131** | `feature/P3-07-calculators` | Refund & Renewal-Quote Calculators | `da4f74e` | ✅ Passing (All 3 checks) | **Changes Requested / Advisory** (Boundary enforcement of D-09.5, oracle discrepancy documentation, null edge cases) |

---

## 2. Formal Review for PR #132 (Application Submission Flow)

### A. Confirmed Defects (Blocking Issues)

#### 1. Concurrency Defect: Duplicate-Submission Race Condition & Missing DB Unique Constraint
- **Location:** [`backend/src/main/java/com/team11/insurance/applications/Application.java`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/applications/Application.java) & [`ApplicationSubmissionService.java:253`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/applications/service/ApplicationSubmissionService.java#L253)
- **Finding:**
  The check `if (applicationRepository.existsByQuoteId(quote.getId()))` is executed at the application service level. However, there is no database-level unique constraint on `applications.quote_id` in the JPA entity or table definition (`@Column(name = "quote_id", nullable = false)` lacks `unique = true`, and there is no `@Table(uniqueConstraints = ...)`).
- **Impact:**
  Under concurrent submissions for the same quote (e.g. double-click, network retries, or parallel API clients), two threads will both pass `existsByQuoteId(...)` simultaneously, creating two separate `Application` records with `SUBMITTED` state for the exact same quote, resulting in duplicate policy generation or invalid state transitions.
- **Remediation Required:**
  1. Add `unique = true` to `@Column(name = "quote_id", nullable = false, unique = true)` in `Application.java` (or `@Table(uniqueConstraints = @UniqueConstraint(columnNames = "quote_id")))`.
  2. In `ApplicationSubmissionService`, wrap the persistence or handle `DataIntegrityViolationException` gracefully to return a 409 `StateConflictException` when a concurrent race occurs.

---

#### 2. Test-Isolation Failure & Dependency on PR #135 Fix
- **Location:** GitHub Actions `Backend CI` (`PlanPersistenceTest.queriesByInsurerAndType:67 expected: <2> but was: <7>`)
- **Finding:**
  The GitHub Actions backend check on PR #132 is currently failing. The failure is **not** caused by a code defect in Samarth's submission code; it is caused by test-data leakage from `PlanDataSeeder` on older commits of `main`.
- **Root Cause & Fix:**
  This issue was isolated and resolved on `main` in PR #135 (`e7861f0b9a0bdeff24dc0196773bcfa89d073f8b`).
- **Remediation Required:**
  Samarth must merge `origin/main` or rebase `feature/P3-04a-application-submission` onto the latest `main` once PR #135 is merged. This will immediately resolve the backend CI failure.

---

#### 3. API Contract Discrepancy: Underwriting Rejection Response
- **Location:** [`ApplicationController.java:90-94`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/applications/controller/ApplicationController.java#L90-L94)
- **Finding:**
  When underwriting rejects an application, `ApplicationController` throws:
  ```java
  throw new ValidationException("Application rejected", reason);
  ```
  `ValidationException` produces an error response with `errorCode: "VALID-422"` and `details: ["<reason>"]` (an array of plain strings).
- **Discrepancy:**
  According to `docs/contracts/p3-contract.md §3.1` and the agreed error contract:
  - Error details must follow `{ "field": "...", "reason": "..." }` or the application rejection schema.
  - Furthermore, whether a business rejection should return HTTP 201 with `{ "status": "REJECTED", "rejectionReason": "..." }` or HTTP 422 with a structured rejection payload must be strictly standardized with the frontend client. Currently, the client expecting a standard `SubmitApplicationResponse` JSON on business evaluation will receive a `VALID-422` error structure.
- **Remediation Required:**
  Align rejection handling with `docs/contracts/p3-contract.md §3.1`. If HTTP 422 is retained by design, ensure the response body schema matches the frontend schema expectations.

---

#### 4. Denial-of-Service / Storage Risk: Unbounded `details` Payload
- **Location:** [`ApplicationSubmissionService.java:321-339`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/applications/service/ApplicationSubmissionService.java#L321-L339) (`toJson(request.details())`)
- **Finding:**
  The request payload accepts an unconstrained `Map<String, Object> details`. There is no check on the serialized JSON string length or map size before serializing and persisting to `details_json` (mapped to `TEXT` / `VARCHAR(500)` or `LONGTEXT`).
- **Impact:**
  An adversary or automated script could supply arbitrary nested JSON objects of several megabytes, resulting in database truncation errors or memory exhaustion.
- **Remediation Required:**
  Enforce a maximum serialized byte/character limit (e.g. 4,000 characters) on `detailsJson` and validate that keys conform to alphanumeric identifiers before saving.

---

#### 5. Fault Tolerance: Handling Unexpected Underwriting Failures
- **Location:** [`ApplicationSubmissionService.java:263-282`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/applications/service/ApplicationSubmissionService.java#L263-L282)
- **Finding:**
  The `SUBMITTED` record is persisted in a separate, already-committed transaction prior to calling `underwriting.evaluate(submitted)`. If `underwriting.evaluate()` throws an unhandled runtime exception (e.g., unexpected NPE, database timeout, or external rule evaluation error), the method aborts with HTTP 500.
- **Impact:**
  The application remains permanently stranded in `SUBMITTED` state in the database. Because `existsByQuoteId` checks exist, any customer retry on the same quote fails with HTTP 409 `StateConflictException`, locking the user out indefinitely without an administrative workflow to recover the application.
- **Remediation Required:**
  Wrap `underwriting.evaluate()` in a defensive try-catch. If an unexpected exception occurs, catch it and either:
  a) Roll back or delete the preliminary `SUBMITTED` row so the user can retry, OR
  b) Transition the application status to `REFERRED` or `PENDING_REVIEW` with an internal incident note.

---

### B. Questions Requiring Team or Guide Clarification

1. **Transaction Boundary vs Controller Exception:**
   In `ApplicationController`, throwing `ValidationException` after the application has already been saved to the DB as `REJECTED` in an independent transaction is intentional to return HTTP 422 without rolling back the DB record. Is the team and frontend agreed that an HTTP 422 error response is preferred over an HTTP 200/201 response with `status: "REJECTED"`?
2. **FileStore Verification (P3-04b):**
   The comment acknowledges that document existence in `IFileStore` is deferred to P3-04b. Should a temporary stub or pre-check be introduced to ensure nonexistent IDs are rejected early?

---

## 3. Formal Review for PR #131 (Refund and Renewal-Quote Calculators)

### A. Confirmed Defects & Architecture Invariants

#### 1. Enforcement of Decision D-09.5 at Integration Boundary
- **Location:** [`backend/src/main/java/com/team11/insurance/policies/RefundCalculator.java:145-154`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/policies/RefundCalculator.java#L145-L154)
- **Finding:**
  `RefundCalculator.calculateRefund(...)` takes a boolean parameter `boolean claimPaid`.
- **Architectural Rule (Decision D-09.5):**
  Per confirmed decision D-09.5:
  > `IClaimService.hasPaidClaim(policyId)` returns `true` only when a claim associated with the policy has status `SETTLED`. A claim in status `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, or `PAYMENT_PROCESSING` does NOT count as paid for cancellation-refund eligibility.
- **Remediation Required:**
  1. Add an explicit Javadoc reference in `RefundCalculator.java` referencing Decision D-09.5:
     `@param claimPaid true ONLY if a claim on this policy is in SETTLED status (Decision D-09.5)`.
  2. When wiring `CancellationService` (integration layer), verify that the call to `IClaimService.hasPaidClaim(policyId)` strictly queries `claim.status == ClaimStatus.SETTLED`.

---

#### 2. The ₹10,873 vs ₹10,874 Refund Oracle Discrepancy (TC-Can-02)
- **Location:** [`RefundCalculator.java:172-180`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/policies/RefundCalculator.java#L172-L180) & `RefundCalculatorTest.java`
- **Finding:**
  For a 365-day policy with gross premium ₹12,000 cancelled on Day 16 (unused days = 349) with a 5% cancellation fee (₹600):
  $$\text{Unexpired Premium} = 12,000 \times \frac{349}{365} = 11,473.9726\dots$$
  $$\text{Net Refund} = 11,473.9726\dots - 600 = 10,873.9726\dots$$
  - Under SRS §4.6.2 and SAD §4.1.6, all monetary amounts round **DOWN** (floor) to the whole rupee (`Rupees.ROUNDING = RoundingMode.DOWN`). Applying floor to 10,873.97 yields **₹10,873**.
  - However, the STP specification for TC-Can-02 states expected refund is **₹10,874** because the test author performed premature intermediate rounding: $12000 - (12000 \times 16 / 365) = 12000 - 526.02 = 11473.97 \approx 11474$, minus $600 = 10874$.
- **Evaluation:**
  Samarth's exact integer-numerator arithmetic:
  `numerator = (12000 * 349) - (600 * 365) = 4,188,000 - 219,000 = 3,969,000`
  `3,969,000 / 365 = 10,873` (with `RoundingMode.DOWN`)
  is mathematically superior and strictly complies with the round-down mandate.
- **Action Required:**
  Do **not** change the formula to match the flawed STP intermediate rounding. Instead:
  Record this known 1-rupee difference in `docs/decisions.md` and `docs/test-execution-matrix.md` under TC-Can-02 as an agreed deviation so future test runs do not flag ₹10,873 as a bug.

---

#### 3. Null-Safety & Robustness Edge Cases
- **Location:** [`MotorRatingParameters.java:26`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/policies/MotorRatingParameters.java#L26) & [`RenewalQuoteCalculator.java:223`](file:///c:/Users/thiji/OneDrive/Documents/Projects/SE%20MiniProject/IMPLEMENTATION/Team-11/backend/src/main/java/com/team11/insurance/policies/RenewalQuoteCalculator.java#L223)
- **Findings:**
  1. `MotorRatingParameters` compact constructor calls `addOnPrices = Map.copyOf(addOnPrices)`. If `addOnPrices` is null, it throws an unhandled `NullPointerException` rather than treating it as an empty map or throwing an `IllegalArgumentException`.
  2. In `RenewalQuoteCalculator.requireWithinWindow(LocalDate expiryDate, LocalDate today)`, if either `expiryDate` or `today` is null, an untyped `NullPointerException` occurs.
- **Remediation Required:**
  1. In `MotorRatingParameters`:
     ```java
     this.addOnPrices = addOnPrices == null ? Map.of() : Map.copyOf(addOnPrices);
     ```
  2. In `RenewalQuoteCalculator`:
     Add explicit null checks with descriptive error messages or annotations:
     ```java
     Objects.requireNonNull(expiryDate, "expiryDate must not be null");
     Objects.requireNonNull(today, "today must not be null");
     ```

---

### B. Questions Requiring Team or Guide Clarification

1. **System-Wide Rounding Mode:**
   `Rupees.java` sets `ROUNDING = RoundingMode.DOWN`. For Motor Quote Generation (INS-F-005), ensure that the motor quotation engine applies the exact same floor rounding rule so renewal quotes and initial quotes never produce rounding divergence.
2. **Grace Period Renewal Validity:**
   `RenewalQuoteCalculator` returns `renewableUntil = expiryDate.plusDays(15)`. Does policy coverage remain active during the 15-day grace period, or is coverage suspended until payment is received? (To be documented in Sprint 2).

---

## 4. Summary of Action Items for Branch Author (Samarth)

1. **PR #132:**
   - [ ] Add unique constraint on `applications.quote_id` in `Application.java`.
   - [ ] Catch `DataIntegrityViolationException` to prevent concurrent duplicate submissions.
   - [ ] Merge `origin/main` into `feature/P3-04a-application-submission` to pull in the test isolation fix from PR #135.
   - [ ] Add payload size guardrails to `detailsJson`.
   - [ ] Wrap `underwriting.evaluate()` in try-catch to avoid permanently stranding applications in `SUBMITTED` on runtime faults.
2. **PR #131:**
   - [ ] Add defensive null handling for `addOnPrices` in `MotorRatingParameters`.
   - [ ] Add Javadoc invariant for Decision D-09.5 in `RefundCalculator`.
   - [ ] Co-sign documentation of the ₹10,873 vs ₹10,874 oracle deviation in `docs/decisions.md`.
