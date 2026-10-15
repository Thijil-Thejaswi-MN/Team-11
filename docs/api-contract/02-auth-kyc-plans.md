# API Contract Draft: Authentication, KYC, CurrentUser and Plans

**Work item:** P2-01  
**Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Status:** Draft for team/backend sign-off — not evidence that endpoints are implemented  
**Date prepared:** 15 October 2026  
**Source:** Software Architecture Document (SAD) v1.1, sections 4.3.1 (Authentication and KYC), 4.3.2 (Plans), 4.3.6 (Administration, dashboards, reports and audit), plus the applicable SRS requirements.

> This file is a proposal for review. The SAD's endpoint table is the baseline. Items marked **PROPOSED / CONFIRM** are not final until Person 1 (Thijil), Person 4 (Raghunandan), and the team agree. Do not claim any endpoint is implemented just because it is documented here.

## 1. Conventions

- **Base path:** `/api/v1`.
- **Transport and format:** HTTPS. JSON for ordinary request/response bodies; multipart form data for KYC file upload.
- **Authentication:** Public routes are registration, login, and public plan browse/compare. Other routes require an authenticated user with an active server-side session, using the agreed Bearer JWT/session arrangement.
- **Roles:** `CUSTOMER`, `INSURER_ADMIN`, `UNDERWRITER`, `CLAIMS_OFFICER`, `SYSTEM_ADMIN`.
  - SAD shorthand: C = Customer, IA = Insurer Admin, UW = Underwriter, CO = Claims Officer, SA = System Admin.
- **Insurer isolation:** insurer staff may access only records belonging to their own insurer. The server must determine the caller's identity and role; never trust a client-supplied role or insurer ID.
- **Pagination:** list endpoints support `page` and `size`; exact defaults and maximums must be agreed with Person 4.
- **Money:** values are in Indian rupees (INR), following the SAD.
- **Error body:** use the shared backend error-response shape defined by the backend foundation; confirm exact field names and error codes with Person 4. Do not return passwords, tokens, Aadhaar/PAN values, or file contents in error messages or logs.
- **Examples:** all names, email addresses, identifiers, tokens and file names below are synthetic examples. Do not use real credentials or real KYC documents.

## 2. Authentication and registration — INS-F-001

### 2.1 `POST /api/v1/auth/register`

- **Purpose:** create a new user account.
- **Authentication:** not required (public).
- **Allowed caller:** public registration flow.
- **Related requirement:** `INS-F-001`.
- **Request fields:**
  - `name` — required string.
  - `email` — required string; must satisfy agreed email validation.
  - `phone` — required string.
  - `password` — required string; never log or return it.
- **Example request:**

```json
{
  "name": "Test Customer",
  "email": "customer.test@example.invalid",
  "phone": "TEST-PHONE-001",
  "password": "NotARealPassword-123"
}
```

- **Success:** `201 Created` — `{ "userId": 1001 }` (illustrative test ID).
- **Errors from SAD:** `400 Bad Request` for invalid input; `409 Conflict` if the email already exists.
- **Expected tests:** valid details; missing required field; duplicate email; password not returned or logged.
- **Open detail:** minimum password policy and email normalization rules should be confirmed with Person 4/security design; do not invent these as finalized values.

### 2.2 `POST /api/v1/auth/login`

- **Purpose:** authenticate a user and start the server-side session described in the SAD.
- **Authentication:** not required before login.
- **Allowed caller:** public login form.
- **Related requirement:** `INS-F-001`; lockout behavior also relates to `INS-SR-005`.
- **Request fields:** `email` (required string), `password` (required string).
- **Example request:**

```json
{
  "email": "customer.test@example.invalid",
  "password": "NotARealPassword-123"
}
```

- **Success:** `200 OK` — `{ "accessToken": "<test-token>", "role": "CUSTOMER", "expiresIn": 900 }`.
  - `accessToken` is a placeholder, not a real token.
  - `expiresIn` is shown as an illustrative numeric example only. Confirm the actual token/session lifetime with Person 4; the SAD requires a 15-minute inactivity timeout, which is not necessarily the same as the token lifetime.
- **Errors from SAD:** `401 Unauthorized` for invalid credentials; `423 Locked` when the account is locked for 15 minutes.
- **Security requirement:** an unknown email and an incorrect password must return the same public `401` message so the response does not reveal whether an account exists. This response wording is a security expectation to confirm with the backend owner.
- **Expected tests:** valid credentials; invalid email; incorrect password; repeated failures and 15-minute lockout; role returned is correct; no password/token is written to logs.

## 3. KYC — INS-F-002

### 3.1 `POST /api/v1/kyc`

- **Purpose:** submit KYC details and document files for the authenticated customer.
- **Authentication:** required.
- **Allowed caller:** `CUSTOMER` only.
- **Related requirement:** `INS-F-002`; secure-file constraints also relate to `INS-SR-007`.
- **Content type:** `multipart/form-data`.
- **Fields:** `panNumber` (required string), `aadhaarNumber` (required string), `files[]` (one or more supported file uploads, exact count rules to confirm with the team).
- **File constraints from the project requirements:** PDF, JPG or PNG; maximum file size 5 MB per the submitted security requirement; the project's simulated scanning step must run before storage.
- **Example form fields:** `panNumber=TEST-PAN-0001`, `aadhaarNumber=TEST-AADHAAR-0001`, `files[]=test-kyc.pdf`. These are placeholders for test fixtures, not real identity details.
- **Success:** `202 Accepted` — `{ "kycStatus": "Pending" }`; the SAD allows the status values `Pending`, `Verified`, or `Rejected`.
- **Errors from SAD:** `400 Bad Request` for invalid input; `413 Payload Too Large` for a file over 5 MB; `415 Unsupported Media Type` for a disallowed file type.
- **Security notes:** do not return the uploaded file contents or full PAN/Aadhaar values in responses; do not log their values. Record only what is needed for traceability.
- **Expected tests:** supported file within size limit; unsupported type; file over 5 MB; missing/invalid details; unauthenticated request rejected; KYC result belongs to the submitting customer.

### 3.2 `GET /api/v1/kyc/status`

- **Purpose:** return the current authenticated customer's KYC status.
- **Authentication:** required.
- **Allowed caller:** `CUSTOMER`; the response must be based on the authenticated identity.
- **Related requirement:** `INS-F-002`.
- **Request:** no body.
- **Success:** `200 OK` — `{ "kycStatus": "Verified" }` (illustrative status; actual status depends on test fixture/service response).
- **Errors from SAD:** `401 Unauthorized` if not logged in. Other authorization/data-isolation response details should be confirmed with Person 4.
- **Expected tests:** status visible to the correct customer; unauthenticated caller rejected; one customer cannot view another customer's KYC information.

> The SAD table specifies `/kyc` and `/kyc/status`. Do not silently rename the upload route to `/kyc/documents`. Any alternate path needs backend/team sign-off.

## 4. Plans and premium rules — INS-F-003, INS-F-004, INS-F-022

### 4.1 `GET /api/v1/plans`

- **Purpose:** browse active insurance plans.
- **Authentication:** not required for public browsing; an authenticated customer may also call it.
- **Allowed caller:** public / `CUSTOMER`.
- **Related requirements:** `INS-F-003`, `INS-F-004`, `INS-F-022`.
- **Query fields:**
  - `type` — optional, e.g. `HEALTH` (confirm accepted values with Person 4).
  - `insurer` — optional insurer filter.
  - `premiumMax` — optional maximum premium.
  - `coverageMin` — optional minimum coverage.
  - `page`, `size` — list pagination fields; default/max values need confirmation.
- **Example:** `GET /api/v1/plans?type=HEALTH&premiumMax=10000&coverageMin=500000&page=1&size=20`.
- **Success:** `200 OK` — `{ "items": [{ "planId": 11, "insurer": "Test Insurer", "type": "HEALTH", "coverage": 500000, "deductible": 0, "waitingPeriod": 30, "addOns": [] }] }`.
- **Errors from SAD:** `400 Bad Request` for an invalid filter.
- **Expected tests:** public browse without login; filter by type/insurer/premium/coverage; inactive plans are not shown; malformed filters rejected.

### 4.2 `GET /api/v1/plans/compare?ids=11,14,19`

- **Purpose:** compare up to three plans side by side.
- **Authentication:** not required for public browsing; an authenticated customer may also call it.
- **Allowed caller:** public / `CUSTOMER`.
- **Related requirement:** `INS-F-004`.
- **Query field:** `ids` — list of plan IDs; maximum three IDs.
- **Success:** `200 OK` — `{ "plans": [ /* same plan fields returned by GET /plans */ ] }`.
- **Errors from SAD:** `400 Bad Request` if more than three IDs are supplied; `404 Not Found` if a plan ID is unknown.
- **Expected tests:** compare one, two and three known plans; reject four IDs; report an unknown plan ID; ensure displayed fields include premium/coverage/deductible/waiting period/add-ons as supported by the plan response.

### 4.3 `POST /api/v1/plans`

- **Purpose:** create an insurance plan and its rating/underwriting rules.
- **Authentication:** required.
- **Allowed caller:** `INSURER_ADMIN` only, scoped to the authenticated user's insurer.
- **Related requirement:** `INS-F-003`.
- **Request fields from SAD:** `type`, `name`, `coverage`, `deductible`, `waitingPeriodMonths`, `premiumRules[]`, `uwRules[]`, `addOns[]`.
- **Example request (illustrative):**

```json
{
  "type": "HEALTH",
  "name": "Test Health Plan",
  "coverage": 500000,
  "deductible": 0,
  "waitingPeriodMonths": 30,
  "premiumRules": [],
  "uwRules": [],
  "addOns": []
}
```

- **Success:** `201 Created` — `{ "planId": 11 }`.
- **Errors from SAD:** `403 Forbidden` if caller is not an Insurer Admin; `422 Unprocessable Entity` for invalid values.
- **Expected tests:** authorized Insurer Admin can create a valid plan; Customer is denied; invalid plan values are rejected; one insurer cannot manage another insurer's plans.

### 4.4 `PUT /api/v1/plans/{id}` and `PATCH /api/v1/plans/{id}`

- **Purpose:** update an insurer's plan or deactivate it.
- **Authentication:** required.
- **Allowed caller:** `INSURER_ADMIN` for the plan belonging to that user's insurer.
- **Related requirement:** `INS-F-003`.
- **Request:** `PUT` sends the agreed full set of plan fields; `PATCH` sends only changed fields. Deactivation example: `{ "active": false }`.
- **Success:** `200 OK` — `{ "plan": { /* updated plan fields */ } }`.
- **Errors from SAD:** `403 Forbidden` for a plan belonging to another insurer; `404 Not Found` if the plan does not exist.
- **Expected tests:** update an authorized plan; deactivate a plan and confirm it no longer appears in public browsing; deny cross-insurer modification; handle unknown ID.
- **Open detail:** exact difference between PUT and PATCH payload validation must be agreed with Person 4.

## 5. System administration — SAD §4.3.6

### 5.1 `POST /api/v1/admin/users`

- **Purpose:** create a staff or user account through System Administration.
- **Authentication:** required.
- **Allowed caller:** `SYSTEM_ADMIN` only.
- **Related requirement:** `INS-F-001`; role-based isolation also relates to `INS-SR-003`.
- **Request schema shown in SAD:** `{ "name", "email", "role" }`.
- **Success:** `201 Created` — `{ "id": 2001 }` (illustrative).
- **Errors from SAD:** `409 Conflict` for duplicate email.
- **Important contract gap:** project rules require staff roles (Insurer Admin, Underwriter, Claims Officer) to be linked to exactly one insurer, and a staff role without an insurer must be rejected with `422`. The SAD's compact request schema does not show how `insurerId` is provided. **Confirm the exact request field/flow with Person 4 before treating it as final.** Do not guess the final schema.
- **Expected tests:** System Admin permitted; non-System-Admin denied; duplicate email rejected; staff account without an insurer rejected with the agreed `422` behavior; staff account is linked to exactly one insurer.

### 5.2 `POST /api/v1/admin/insurers`

- **Purpose:** create an insurer record.
- **Authentication:** required.
- **Allowed caller:** `SYSTEM_ADMIN` only.
- **Related requirement:** administration/role setup; confirm the exact requirement ID if this operation is represented by a specific SRS row.
- **Request:** `{ "name": "Test Insurer" }`.
- **Success:** `201 Created` — `{ "id": 3001 }` (illustrative).
- **Errors from SAD:** `409 Conflict` is specifically documented for duplicate email on user creation; duplicate-insurer behavior is not specified in the SAD table and needs confirmation.
- **Expected tests:** only System Admin can create an insurer; required name validation; duplicate behavior as agreed.

## 6. CurrentUser context and development/test stub (P2-02 follow-up)

The API contract for authenticated code uses the following server-created identity context:

```json
{
  "userId": 1001,
  "role": "CUSTOMER",
  "insurerId": null
}
```

- This is an **internal server-side context**, not a request body the client can choose.
- The backend derives it from the validated authentication/session and stored user/insurer mapping. A client must not be able to spoof `userId`, `role`, or `insurerId` by sending these fields in a request.
- Role constants expected by the project: `CUSTOMER`, `INSURER_ADMIN`, `UNDERWRITER`, `CLAIMS_OFFICER`, `SYSTEM_ADMIN`.
- The planned development/test stub should use a seeded test identity and be enabled only under explicit `dev`/`test` profiles. It must never be enabled in production. Confirm the exact fail-fast configuration with Person 4 during implementation on 15 October.
- This section documents the interface expectation; it does **not** mean Sharana has implemented the stub in this documentation task.

## 7. Internal interfaces consumed by other modules

These are Java service interfaces, not HTTP endpoints:

```java
boolean IKycStatus.isVerified(long userId);
Plan IPlanQuery.getPlan(long planId);
RatingRules IPlanQuery.getRatingRules(long planId);
```

The precise Java package, return types and exception model must match the shared code and be agreed with Person 4. These signatures express the intended capability only.

## 8. Items to confirm at contract sign-off

- Confirm the role values and exact `CurrentUser` construction with Person 4.
- Confirm login/session expiry and lockout behavior; SAD specifies a 15-minute inactivity timeout and 15-minute lockout after five consecutive failed attempts.
- Confirm how `insurerId` is supplied/validated when System Admin creates insurer staff, while preserving the rule that staff belong to exactly one insurer.
- Confirm default/max pagination values for list endpoints.
- Confirm the error response body and error-code names with the backend skeleton owner.
- The SAD table does not define `/auth/logout`, `/auth/me`, or `/admin/kyc/pending`. Do not treat those routes as agreed SAD endpoints; add them only after explicit team/backend approval.
- Confirm how invalid or duplicate insurer creation is handled.
- Confirm PUT vs PATCH semantics for plan updates.

## 9. Traceability summary

| Requirement | Contract coverage | Known STP references |
|---|---|---|
| `INS-F-001` | Registration, login, role returned; System Admin staff creation | `TC-Acc-01`, `TC-Acc-02` |
| `INS-F-002` | KYC submission and own KYC status | `TC-KYC-01` |
| `INS-F-003` | Create/update/deactivate plans | `TC-Plan-01` |
| `INS-F-004` | Public plan browse and compare | `TC-Plan-02` |
| `INS-F-022` | Plan filters | `TC-Plan-03` |
| `INS-SR-002` | Password handling/no plaintext or sensitive logging | `TC-Sec-02` |
| `INS-SR-003` | Role/insurer isolation | `TC-Sec-03` and related access-control tests |
| `INS-SR-005` | Lockout/session timeout | `TC-Sec-05` |
| `INS-SR-007` | KYC file restrictions | `TC-Sec-07` |
| `INS-F-018`, `INS-SR-008` | Audit access/immutability is referenced for checklist coordination, but audit implementation contract is owned/confirmed with the relevant module owners | `TC-Aud-01`, `TC-Sec-08` |
