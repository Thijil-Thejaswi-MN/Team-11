# P3 Contract Draft (P3-01)

**Owner:** Person 3 | **Status:** Draft v0.3, 14 Oct 2026 | **For:** contract sign-off meeting, 15 Oct
**Source of truth:** SAD v1.0 (3.4, 4.1.2, 4.1.5, 4.1.6, 4.3.3, 4.3.4, 4.3.7, 4.4) and SRS v1.0.
Anything that differs from the SAD is marked **PROPOSED** and must go into `DEVIATIONS.md` if accepted.

## 1. Scope

| Requirement | Title | Sprint | Tests |
| --- | --- | --- | --- |
| INS-F-007 | Submit application | per RTM tab | TC-App-01 |
| INS-F-009 | Payment success to policy issuance | per RTM tab | TC-Pol-01 |
| INS-F-019 | Renewal | per RTM tab | TC-Ren-02, TC-Ren-03 |
| INS-F-020 | Cancellation and refund | per RTM tab | TC-Can-01, TC-Can-02 |
| INS-F-024 | View/download policy documents | per RTM tab | TC-Pol-02 |
| INS-F-025 | Admin reports | per RTM tab | TC-Rpt-01 (endpoint contract is in P2-01, SAD 4.3.6) |

Not in this contract (owned by others): `/quotes/*`, `/applications/referred`, `/applications/{id}/decision` (P1-03); auth, KYC, plans, admin (P2-01); IPaymentService, INotify, IClaimService, IFileStore (P4-03).

## 2. Conventions

- Base path `/api/v1`, JSON over HTTPS, Bearer JWT. Identity comes from `CurrentUser { userId, role, insurerId }` (P2-01); use the test stub until real login exists.
- The `CurrentUser` test stub is for development and automated testing only. It must never be enabled in production: it is active only under the `dev`/`test` profile, and the production profile must fail startup if the stub bean is present.
- Roles: C = Customer, IA = Insurer Admin, UW = Underwriter.
- Amounts in rupees, rounded down to a whole rupee. Dates are ISO `YYYY-MM-DD`.
- Errors use `{ timestamp, status, errorCode, message, details[], correlationId }` with the codes VALID-422, AUTH-401, ACCESS-403, NOTFOUND-404, STATE-409, PAY-402, SYS-500.
- Time-based rules (renewal window, free-look, expiry) must read an injected clock, never `LocalDate.now()` directly, so tests can move time (P4-03 test-clock rule).

## 3. Endpoints

### 3.1 POST /applications (INS-F-007), role C

Request:
```json
{ "quoteId": 5021,
  "details": { "vehicleRegNo": "KA22AB1234", "...": "..." },
  "documentIds": [301, 302] }
```
Success `201` (application Approved or Referred only):
```json
{ "applicationId": 7001, "status": "Approved" }
```
(status is `Approved` or `Referred`. `Rejected` is never returned with `201`.)

Errors:
- `403 ACCESS-403` KYC not verified (nothing is persisted).
- `404 NOTFOUND-404` quote not found or not the caller's (nothing is persisted).
- `422 VALID-422` application Rejected by underwriting (see below).

Rejected response (exact): HTTP `422`, body:
```json
{ "timestamp": "...", "status": 422, "errorCode": "VALID-422",
  "message": "Application rejected",
  "details": [{ "reason": "<underwriting reason>" }],
  "correlationId": "..." }
```

Rules:
- Save the application as **Submitted** (with a history entry) before underwriting runs, then call `IUnderwriting.evaluate(application)` and move to Approved, Referred or Rejected (SAD 4.1.2).
- A Rejected application **is persisted**: it is saved as Submitted first, then moved to Rejected (final state, with history entry and reason) and kept for audit. It is not deleted or rolled back, so returning the `422` must not roll back the transaction. It cannot be paid or resubmitted; the customer starts a new application.
- KYC check: `IKycStatus.isVerified(userId)`.
- Notify: `APPLICATION_APPROVED`, `APPLICATION_REFERRED` or `APPLICATION_REJECTED`.

### 3.2 POST /payments (INS-F-009), role C

Request `{ "applicationId": 7001 }`. The server builds the idempotency key `applicationId + "-" + attemptNo`; the client never sends it.

Success `200`: `{ "paymentStatus": "Success", "policyNo": "POL-2026-000123" }` or, if issuing is delayed, `{ "paymentStatus": "Success", "policyStatus": "Issuing" }`.

Errors: `402 PAY-402` payment failed, retry allowed; `409 STATE-409` application not Approved; `403 ACCESS-403` not the owner.

### 3.3 Policy reads (INS-F-024), role C

| Endpoint | Success | Errors |
| --- | --- | --- |
| `GET /policies?status=Active&page=&size=` | `200 { items: [{ policyNo, plan, validTo, status }] }` (own policies only) | none |
| `GET /policies/{id}/document?type=policy\|renewal` | `200` PDF | `403 ACCESS-403` not the owner; `404 NOTFOUND-404` |

### 3.4 Renewal (INS-F-019), role C

| Endpoint | Request | Success | Errors |
| --- | --- | --- | --- |
| `POST /policies/{id}/renewal-quote` | none | `200 { renewalQuoteId, premium, breakdown, renewableUntil }` | `409 STATE-409` outside window |
| `POST /policies/{id}/renew` | `{ renewalQuoteId }` | `200 { newPolicyNo }` | `402 PAY-402`; `409 STATE-409` not renewable |

Rules (SAD 4.1.6): window is 30 days before expiry to 15 days after; plan must still be active; update age (health) or vehicle age and reduced IDV (motor); apply the insurer's current premium rules; 10% no-claim discount on own-damage premium for motor with no claim in the previous term. On renew, charge through `IPaymentService`, then create a new Policy with `previousPolicyId`, a term starting at the old expiry date, and a renewal certificate. Notify `POLICY_RENEWED`.
(Per D-09.5, settled claims with payout > 0 count as claims).

**PROPOSED:** `renewalQuoteId` is stored server-side so `/renew` charges the quoted premium, not a client value.

### 3.5 Cancellation (INS-F-020)

| Endpoint | Role | Request | Success | Errors |
| --- | --- | --- | --- | --- |
| `POST /policies/{id}/cancellation` | C | `{ reason }` | `201 { requestId, status: "Requested" }` | `409 STATE-409` policy not Active |
| `POST /cancellations/{id}/decision` | IA | `{ decision: "Approve" \| "Reject" }` | `200 { status, refundAmount }` | `403 ACCESS-403` other insurer; `409 STATE-409` already decided |

Refund (fee = 5% of premium paid for the current term; test oracle for the ST (Day-16 refund oracle is Rs 10,873 per SRS 4.6.2 round-down; D-09.5 settled-claim rule applies).P):

| Case | Refund |
| --- | --- |
| A claim has been paid on the policy | 0 |
| Within 15-day free-look, no claim | premiumPaid - fee |
| After free-look, no claim | max(0, premiumPaid x unusedDays / totalDays - fee) |

Examples: Rs 12,000, day 10 = **Rs 11,400**. Rs 12,000, 365-day term, day 100 = 12,000 x 265 / 365 = 8,712.33 - 600 = 8,112.33, rounded down = **Rs 8,112**.

"Claim paid" comes from `IClaimService.hasPaidClaim(policyId)` (P4-03). Refund money goes through `IPaymentService.refund` with `kind = REFUND`. Notify `CANCELLATION_REQUESTED`, `CANCELLATION_APPROVED` (with amount), `CANCELLATION_REJECTED`.

## 4. Internal interfaces (my module provides)

```java
// IPolicyQuery  (consumers: Claims Workflow, Underwriting Engine)
Policy  getPolicy(long policyId);                          // in SAD
boolean existsActivePolicyForVehicle(String vehicleRegNo); // PROPOSED, see below

// IRenewalService  (consumer: Scheduler)
List<Policy> findExpiringIn(int days);   // policies whose endDate = today + days
void sendReminder(long policyId);        // RENEWAL_REMINDER at 30, 15, 1 days
void retryPendingIssuance();             // issues policies for Paid applications that have none
```

**PROPOSED `existsActivePolicyForVehicle`:** the SAD underwriting rule says motor is rejected if there is an "active policy on the vehicle", but no interface or field identifies the vehicle. Proposal: the motor `details` carries `vehicleRegNo`; the method returns true if any Policy for that registration has status Active or Cancellation Requested and an endDate not in the past; it checks across all insurers (duplicate cover is a customer-level problem, not an insurer-scoped record).

## 5. State tables

**Application**

| From | To | Trigger | Who |
| --- | --- | --- | --- |
| (new) | Submitted | POST /applications | system |
| Submitted | Approved / Referred / Rejected | IUnderwriting.evaluate | system |
| Referred | Approved / Rejected | UW decision | UW |
| Approved | Paid | payment SUCCESS | system |

Rejected and Paid are final. Rejected applications stay persisted (see 3.1). Anything else returns `STATE-409`. Payment is allowed only in Approved.

**Policy**

| From | To | Trigger |
| --- | --- | --- |
| (new) | Active | issuance after payment, or renewal |
| Active | Cancellation Requested | customer requests cancellation |
| Cancellation Requested | Cancelled | IA approves (**PROPOSED**: policy becomes Cancelled at approval) |
| Cancellation Requested | Active | IA rejects |
| Active | Expired | endDate passes |

A renewal creates a new Policy linked by `previousPolicyId`; the old one is not edited.

**Cancellation request**

| From | To | Trigger |
| --- | --- | --- |
| (new) | Requested | customer request |
| Requested | Approved / Rejected | IA decision |
| Approved | Refunded | refund payment SUCCESS (**PROPOSED**: refund 0 goes straight to Refunded with no gateway call) |

## 6. Payment success to policy issuance (how P4's payment triggers my module)

The dependency is one-way: **Quote & Purchase calls the Payment Adapter**, never the reverse (SAD 3.4: the adapter only requires the gateway and the database).

1. `POST /payments` loads the application; if not Approved return `STATE-409`.
2. `attemptNo` = previous attempts + 1; key = `applicationId + "-" + attemptNo`.
3. Call `IPaymentService.charge(applicationId, premium, key)`. The adapter saves the Payment in its **own transaction** and returns `SUCCESS` or `FAILED` (retries 3 times on timeout with the same key).
4. `FAILED`: notify `PAYMENT_FAILED`, return `402 PAY-402`, application stays Approved, no policy.
5. `SUCCESS`: move the application to **Paid** (own transaction), then call `issue(applicationId)`.
6. `issue` is idempotent: look for a Policy with this `applicationId`; if none, create it with a unique policy number, a PDF stored through `IFileStore.put`, status Active and the term dates; notify `POLICY_ISSUED`; write the audit log.
7. If `issue` throws, the application stays Paid, the response is `200 { paymentStatus: "Success", policyStatus: "Issuing" }`, and the Scheduler calls `retryPendingIssuance()` every few minutes until the policy exists. The customer is never charged twice.

**PROPOSED policy number format:** `POL-<year>-<6-digit sequence>`, with a UNIQUE database constraint.

## 7. Interfaces my module uses

`IKycStatus.isVerified`, `IPlanQuery.getPlan / getRatingRules`, `IUnderwriting.evaluate`, `IPaymentService.charge / refund`, `IClaimService.hasPaidClaim`, `INotify.send`, `IFileStore.put / get`, `IAuditLog.record`, `IDataAccess`.

## 8. Open questions for sign-off

- [ ] Add `vehicleRegNo` to the motor application details, and agree the `existsActivePolicyForVehicle` rule above.
- [ ] Policy becomes Cancelled at approval (proposed) or at refund?
- [ ] Refund of Rs 0: skip the gateway (proposed)?
- [ ] Policy number format and PDF library.
- [ ] `/payments` returns `policyNo` directly (proposed) or the UI polls `/policies`?
