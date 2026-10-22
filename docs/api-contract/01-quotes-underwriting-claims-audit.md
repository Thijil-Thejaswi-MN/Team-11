# P1 API Contract — Quotes, Underwriting, Claims and Audit

**Owner:** Person 1 — Thijil (QA Lead / repository owner)  
**Status:** Draft for team sign-off — 15 October 2026  
**Source of truth:** SAD v1.0/v1.1 sections 4.1.1, 4.1.4, 4.3.3, 4.3.5, 4.3.6 and 4.3.7, plus the submitted SRS.  
**Important:** This document defines a proposed cross-module contract. It does not claim that these endpoints or interfaces are already implemented. Any difference from the SAD must be marked **PROPOSED** and recorded in `docs/decisions.md`/the agreed deviations log after the team accepts it.

## 1. Conventions

- Base path: `/api/v1`.
- Transport: JSON over HTTPS, except file uploads which use `multipart/form-data`.
- Authentication: Bearer JWT backed by an active server-side session. Quote and customer endpoints require an authenticated Customer unless the SAD explicitly describes the endpoint as public.
- Roles used below: `C` = Customer, `IA` = Insurer Admin, `UW` = Underwriter, `CO` = Claims Officer, `SA` = System Admin.
- Insurer staff access must be scoped to their own insurer. A Customer can access only their own records.
- Monetary amounts are in Indian rupees. The server calculates premiums; clients must not submit a trusted premium value.
- Use the common API error structure agreed by the backend team. The backend foundation documents `{ timestamp, status, errorCode, message, details[], correlationId }`; confirm exact error codes and field names during contract sign-off.
- Examples use fictional data only. Exact premium breakdown fields and internal rule-data serialization need team confirmation.

## 2. Quote endpoints — INS-F-005 and INS-F-006

### 2.1 `POST /api/v1/quotes/motor`

- **Purpose:** Calculate a motor-insurance quote for a two-wheeler or four-wheeler.
- **Authentication:** Required.
- **Role:** Customer (`C`).
- **Requirement:** `INS-F-005`.
- **Test reference:** Consult the submitted STP/RTM for the authoritative motor-quote test IDs.
- **Content type:** `application/json`.

**Request fields from SAD §4.3.3**

| Field | Type | Required | Notes |
|---|---|---:|---|
| `planId` | integer | Yes | Must identify an active, compatible plan. |
| `vehicleType` | string | Yes | `2W` or `4W`. |
| `make` | string | Yes | Vehicle make. |
| `model` | string | Yes | Vehicle model. |
| `regYear` | integer | Yes | Vehicle registration year; validate against allowed/business limits. |
| `city` | string | Yes | Rating location. |
| `idv` | number | Yes | Insured declared value, subject to validation. |
| `addOns` | string array | Yes | Use an empty array when no add-ons are selected. |

**Example request**

```json
{
  "planId": 11,
  "vehicleType": "4W",
  "make": "Maruti",
  "model": "Swift",
  "regYear": 2019,
  "city": "Belagavi",
  "idv": 400000,
  "addOns": ["ZERO_DEP"]
}
```

**Success — `200 OK`**

```json
{
  "quoteId": 5021,
  "premium": 9480,
  "breakdown": {}
}
```

The SAD provides this illustrative example premium. The `breakdown` contents are calculated by the server using the selected plan's rating rules; confirm its exact fields with the plan/quote owners.

**Errors**

- `401 Unauthorized`: no valid active session.
- `403 Forbidden`: authenticated user is not allowed to use the endpoint.
- `404 Not Found`: plan not found or unavailable, if that is the agreed API behaviour.
- `422 Unprocessable Entity`: invalid vehicle data, IDV, registration year, or incompatible add-on/plan selection.

### 2.2 `POST /api/v1/quotes/health`

- **Purpose:** Calculate a health-insurance quote from the selected plan, member ages, sum insured and pre-existing-condition declaration.
- **Authentication:** Required.
- **Role:** Customer (`C`).
- **Requirement:** `INS-F-006`.
- **Test reference:** Consult the submitted STP/RTM for authoritative health-quote test IDs.
- **Content type:** `application/json`.

**Request fields from SAD §4.3.3**

| Field | Type | Required | Notes |
|---|---|---:|---|
| `planId` | integer | Yes | Must identify an active health plan. |
| `members` | array of objects | Yes | Each member object contains `age`. |
| `members[].age` | integer | Yes | Validate against plan/SAD eligibility rules. |
| `sumInsured` | number | Yes | Must be valid for the plan. |
| `preExisting` | boolean | Yes | Indicates whether a pre-existing condition is declared. |

**Example request**

```json
{
  "planId": 21,
  "members": [{"age": 34}, {"age": 31}],
  "sumInsured": 500000,
  "preExisting": false
}
```

**Success — `200 OK`**

Response shape from SAD §4.3.3: `{ quoteId, premium, breakdown }`. The server supplies the quote ID and calculates the premium and breakdown using the selected plan's rating rules. No fixed sample premium is specified here. Confirm the exact premium-breakdown fields with the team before sign-off.

**Errors**

- `401 Unauthorized`: no valid active session.
- `403 Forbidden`: authenticated user is not allowed to use the endpoint.
- `404 Not Found`: plan not found or unavailable, if that is the agreed API behaviour.
- `422 Unprocessable Entity`: invalid member ages, member list, sum insured, or plan/member combination.

## 3. Underwriting interface — INS-F-010 and INS-F-011

### 3.1 Internal contract: `IUnderwriting.evaluate(application)`

The SAD §4.3.7 defines the internal interface as:

```java
UnderwritingResult evaluate(Application application);
void recordManualDecision(
    long applicationId,
    Decision decision,
    String reason,
    long underwriterId
);
```

The evaluation outcome is one of `APPROVE`, `REFER`, or `REJECT`, with a reason. The result must be deterministic for the same application, policy data, plan rules and clock/state inputs.

**Proposed result shape for sign-off**

```json
{
  "outcome": "REFER",
  "reason": "Missing rule configuration"
}
```

This is an internal interface example, not a public REST response. The Java result type and enum names should match the backend team's chosen naming conventions.

### 3.2 Decision rules from SAD

Apply rules in an order that avoids a lower-severity condition overriding a rejection:

- **Motor:** reject if the vehicle age is greater than 20 years or there is an active policy on the vehicle; otherwise refer if the vehicle age is greater than 15 years; otherwise approve. Confirm the exact field/formula for calculating vehicle age from `regYear` during sign-off.
- **Health:** reject if age is greater than 70; otherwise refer if age is 60 or above or `preExisting` is true; otherwise approve.
- **Missing plan/rule configuration:** return `REFER` with reason `Missing rule configuration`.

**Open clarification:** confirm the vehicle-age calculation and identify the authoritative vehicle-registration field/data source for checking an active policy on a vehicle. Do not silently change the SAD rule; record accepted changes in the deviations log.

### 3.3 Temporary always-approve stub — development/testing only

**PROPOSED integration fallback until the real underwriting implementation is ready.** In a development or test profile only, a stub may return `APPROVE` for a request that reaches the stub, with a clearly identifiable reason such as `TEST_STUB_ALWAYS_APPROVES`.

Required safeguards:

- The stub must never be enabled in production.
- The real implementation must apply the SAD rules above; it must not use the always-approve fallback.
- Tests and demos must disclose when the stub is being used.
- The production profile must fail closed if the real underwriting component is missing, rather than silently falling back to the stub.
- The exact profile/configuration mechanism is an implementation choice to confirm with Person 4.

### 3.4 Underwriter assessment endpoints — INS-F-011

#### 3.4.1 `GET /api/v1/applications/referred`

- **Purpose:** Retrieve the queue of applications awaiting manual underwriter risk review.
- **Authentication:** Required.
- **Roles:** Underwriter (`UW`), Insurer Admin (`IA`), System Admin (`SA`).
- **Access Rule:** Enforces multi-tenant insurer isolation (`INS-SR-004`). Insurer staff only see referred applications for their assigned `insurerId`. System Admin sees all.
- **Privacy & Data Minimization:** Raw application persistence entities (`Application`) and database audit timestamps (`updatedAt`) are never exposed. Free-form `detailsJson` is sanitized server-side to expose only legitimate underwriting risk factors (`regYear`, `vehicleAge`, `vehicleType`, `make`, `model`, `city`, `idv`, `addOns`, `age`, `applicantAge`, `members`, `preExisting`, `hasPreExisting`, `sumInsured`), while strictly withholding unnecessary sensitive customer data (such as `aadhaar`, `pan`, `bankAccount`, card tokens, and KYC document references).
- **Success — `200 OK`:** Array of `ReferredApplicationResponse` objects:
  ```json
  [
    {
      "id": 7001,
      "customerId": 1001,
      "insurerId": 1,
      "planId": 1,
      "quoteId": 5001,
      "productType": "HEALTH",
      "vehicleRegNo": null,
      "premium": 9000.00,
      "detailsJson": "{\"members\":[{\"age\":62}],\"preExisting\":false}",
      "status": "REFERRED",
      "reason": "Oldest member age requires referral: 62 years",
      "createdAt": "2026-10-20T12:00:00Z"
    }
  ]
  ```
- **Open Clarification for Sprint 2:** Confirm whether `detailsJson` should remain a sanitized JSON string or migrate to a strongly typed nested `underwritingDetails` object prior to frontend table binding in P1-11.

#### 3.4.2 `POST /api/v1/applications/{id}/decision`

- **Purpose:** Record manual underwriter decision (`APPROVE` or `REJECT`) for a referred application.
- **Authentication:** Required.
- **Roles:** Underwriter (`UW`), Insurer Admin (`IA`), System Admin (`SA`).
- **Access Rule:** Multi-tenant insurer isolation strictly enforced.
- **Request Body:** `{ "decision": "APPROVE" | "REJECT", "reason": "string" }` (reason mandatory).
- **Success — `200 OK`:** `{ "applicationId": 7001, "status": "APPROVED", "reason": "..." }`.
- **State Machine Rule:** State must transition from `REFERRED` to `APPROVED` or `REJECTED`. Immutable history and audit log are recorded.

## 4. Plan and rating-rule data contract — INS-F-003 dependency

Person 2 owns the plans API. This section specifies the minimum information the quote and underwriting components need to read; it does not redefine Person 2's public endpoints.

### 4.1 Plan fields exposed to dependent services

Based on SAD §4.3.2, the model/query contract needs to provide:

- `planId`
- insurer identity / `insurerId`
- plan `type` (motor two-wheeler, motor four-wheeler, or health; exact enum values to agree)
- `name`
- `coverage`
- `deductible`
- `waitingPeriodMonths` (health-specific where applicable)
- `premiumRules[]`
- `uwRules[]`
- `addOns[]`
- active/deactivated state

The public plan response includes plan ID, insurer, type, coverage, deductible, waiting period and add-ons. The create-plan request in SAD includes type, name, coverage, deductible, waiting period, premium rules, underwriting rules and add-ons. The exact persisted schema and JSON shape of each rule object need confirmation with Person 2; do not invent a final rule schema here.

### 4.2 Internal query dependency

The quote and underwriting components require the plan owner to expose compatible read operations. The SAD/internal dependency notes use:

```java
Plan getPlan(long planId);
RatingRules getRatingRules(long planId);
```

Confirm the exact method names, return types and package/interface ownership with Person 2 and Person 4. The caller must not accept premium or underwriting rules supplied by the client as authoritative.

## 5. Audit log contract — INS-F-018 and INS-SR-008

The SAD's audit view is `GET /api/v1/audit`, restricted to System Admin (`SA`). It supports filters such as user, action, from and to, and returns records containing the user, action, entity, entity ID and timestamp.

### 5.1 Internal write interface

**Proposed internal signature; confirm with the backend owner before implementation:**

```java
void record(long userId, String action, String entity, long entityId);
```

- The timestamp should be assigned server-side, not trusted from a request body.
- Record security-relevant actions such as authentication events, plan changes, underwriting decisions, payments, cancellation actions and claim decisions, in accordance with the SRS/SAD.
- The log is append-only: ordinary application roles must not update or delete an existing entry.
- Audit writes should record the authenticated actor from server-side security context, not a user ID supplied by an untrusted client.
- Confirm how audit failures affect the calling transaction and which exact actions are mandatory with Person 4.

### 5.2 `GET /api/v1/audit`

- **Purpose:** View/search audit records.
- **Authentication:** Required.
- **Role:** System Admin (`SA`) only, per SAD §4.3.6.
- **Query fields:** `user`, `action`, `from`, `to` (plus `page` and `size` for list pagination).
- **Success — `200 OK`:** `{ "items": [{ "user": "...", "action": "...", "entity": "...", "entityId": 123, "at": "2026-10-15T10:00:00Z" }] }`.
- **Errors:** `401` unauthenticated; `403` not a System Admin; `400` invalid filters.

## 6. Claims endpoints — INS-F-014 dependency / cross-module contract

The Claims Workflow implementation belongs to the claims module owner. These endpoint definitions are included so the verification and decision calls agree across modules; coordinate the final interface with Person 4.

### 6.1 `POST /api/v1/claims/{id}/verify`

- **Purpose:** Verify claim documentation and advance the claim workflow.
- **Authentication:** Required.
- **Role:** Claims Officer (`CO`).
- **Request body:**

```json
{
  "documentsComplete": true,
  "missing": []
}
```

- **Success — `200 OK`:** `{ "status": "Assessment", "duplicateFlag": false }` (illustrative values; actual status depends on the SAD workflow and verification outcome).
- **Errors:** `401` unauthenticated; `403` another insurer's claim / insufficient role; `409` claim is in the wrong state; `404` claim not found, if agreed by the implementation.
- **Rules:** enforce insurer scoping and valid state transitions; record the acting user in the audit log.

### 6.2 `POST /api/v1/claims/{id}/decision`

- **Purpose:** Approve or reject a claim after assessment.
- **Authentication:** Required.
- **Role:** Claims Officer (`CO`).
- **Request body:**

```json
{
  "decision": "Approve",
  "assessedAmount": 12000,
  "reason": "Assessment completed"
}
```

- `decision` is required and must be `Approve` or `Reject`.
- `assessedAmount` is required for the relevant approval flow; the team must confirm validation rules and whether it is omitted for rejection.
- `reason` is required by the SAD for decisions.
- **Success — `200 OK`:** `{ "status": "Payment Processing" }` for approval or `{ "status": "Rejected" }` for rejection, as described by SAD §4.3.5.
- **Errors:** `401` unauthenticated; `403` another insurer / insufficient role; `409` claim is not in `Assessment`; `422` missing reason or invalid values; `404` claim not found, if agreed by implementation.
- **Rules:** enforce insurer scoping and legal state transitions; record the acting user and decision in the append-only audit log.

## 7. Dependencies and ownership

| Area | Owner / dependency | Contract requirement |
|---|---|---|
| Plans and premium rules | Person 2 | Confirm plan read interface and rating-rule model. |
| Underwriting and quote logic | Person 1 | Implement the real business rules in a later implementation task; this document does not mean they are implemented. |
| Current user / authentication | Person 2 and Person 4 | Identity and role must come from server-side session/security context. Any test-only stub must remain unavailable in production. |
| Claims workflow | Person 4 / claims module owner | Confirm claim states, role scope, and exact request/response contracts. |
| Audit persistence and security | Coordinate with Person 4 | Confirm signature, write failure semantics and mandatory audit events. |

## 8. Open questions for sign-off

- [ ] Confirm the exact `breakdown` schema for motor and health quote responses.
- [ ] Confirm quote validation errors and whether unknown/inactive plans return `404` or `422`.
- [ ] Confirm the intended motor driver-age rule interpretation and the authoritative vehicle registration field for active-policy checks.
- [ ] Confirm the `RatingRules` and `UnderwritingResult` types and the owners of their interfaces.
- [ ] Confirm the dev/test-only stub configuration and prove that production cannot enable it.
- [ ] Confirm the audit method signature, mandatory event list, timestamp field and failure/transaction semantics.
- [ ] Confirm the claim verification success states and whether `assessedAmount` is required for rejection.
- [ ] Record any accepted divergence from the SAD in `docs/decisions.md` or the team's agreed deviations log.

## 9. Review and approval

- **Person 1 (author):** Thijil
- **Independent reviewer:** Sharana (requested review)
- **Backend / cross-module review:** Raghunandan
- **Team contract sign-off:** pending

No implementation or runtime tests are claimed by this document. After team review, update this contract to reflect the agreed decisions and link it from the relevant GitHub issues and RTM.
