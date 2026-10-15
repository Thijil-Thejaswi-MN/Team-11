# Functional Test Checklist — Authentication, KYC and Plans

**Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Purpose:** prepare functional, UI and acceptance-test coverage for the authentication, KYC and plan API contract.  
**Source:** SRS, STP Appendix A, and SAD §4.3.1–§4.3.2 and §4.3.6.  
**Current state:** planning checklist only. Test execution fields are intentionally blank. No test is reported as passed.

## Execution rules

- Use synthetic test accounts and fixtures only; never use real passwords, KYC documents, PAN/Aadhaar values, or real customer data.
- Fill **Actual result**, **Result**, and **Evidence** only after executing the test. If the backend or UI is not available, record `Blocked — endpoint not available yet` in the result once the blocker has been verified.
- Use the actual route/commit deployed in the test environment; these endpoint paths are the proposed SAD contract, not proof of implementation.
- The author must not be the only person to verify their own implementation. Confirm the independent executor against the team's Testing & RTM mapping.
- Record a date, tester, commit SHA/build tested, and clear evidence link whenever a test is executed.

---

## FC-01 — Register with valid required details

- **STP reference:** `TC-Acc-01` (confirm exact mapping in STP Appendix A).
- **Requirement:** `INS-F-001`.
- **Preconditions / data:** registration endpoint available; unique synthetic email `customer.test@example.invalid`; valid synthetic name, phone and password.
- **Steps:** 1. Submit `POST /api/v1/auth/register` with all required fields. 2. Inspect the status code and response body. 3. Try to observe the new account through the approved test-only method; do not inspect or expose plaintext password.
- **Expected result:** `201 Created`; response contains `userId`; no password is returned or logged.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-02 — Registration with a missing required field

- **STP reference:** `TC-Acc-01` or the corresponding registration-validation case; confirm in STP.
- **Requirement:** `INS-F-001`.
- **Preconditions / data:** registration endpoint available; omit one required field, e.g. `email`.
- **Steps:** 1. Submit the registration request without the required field. 2. Inspect the response. 3. Confirm no account was created.
- **Expected result:** `400 Bad Request` or the exact agreed validation response; no partially created account; message does not reveal internal details.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-03 — Register with an email that already exists

- **STP reference:** confirm the matching registration case in STP Appendix A.
- **Requirement:** `INS-F-001`.
- **Preconditions / data:** one synthetic account already exists for `customer.test@example.invalid`.
- **Steps:** 1. Submit another registration request with the same email. 2. Inspect the HTTP status and response. 3. Confirm a duplicate account was not created.
- **Expected result:** `409 Conflict`; no duplicate account is created.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-04 — Login with valid credentials

- **STP reference:** `TC-Acc-01` (confirm exact mapping in STP Appendix A).
- **Requirement:** `INS-F-001`.
- **Preconditions / data:** a seeded synthetic user exists and is not locked.
- **Steps:** 1. Submit `POST /api/v1/auth/login` with the valid test email and password. 2. Inspect the status and response fields. 3. Use the returned authentication result only in the authorized test environment.
- **Expected result:** `200 OK`; response contains `accessToken`, `role`, and `expiresIn`; role matches the seeded user; password is not returned.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-05 — Login with invalid credentials

- **STP reference:** confirm the matching invalid-login case in STP Appendix A.
- **Requirement:** `INS-F-001`.
- **Preconditions / data:** use a synthetic email and an incorrect password; do not use real credentials.
- **Steps:** 1. Attempt login with an incorrect password. 2. Attempt login with an unknown synthetic email. 3. Compare both public responses.
- **Expected result:** both failures use `401 Unauthorized` and the same public message so the API does not reveal whether the email exists. No token is issued.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-06 — Account lockout after repeated failed login

- **STP reference:** `TC-Sec-05`.
- **Requirement:** `INS-SR-005`.
- **Preconditions / data:** dedicated synthetic test account; ability to control or observe time according to the test setup.
- **Steps:** 1. Submit five consecutive failed login attempts. 2. Attempt another login during the lockout window. 3. Record the actual lockout duration and time source used.
- **Expected result:** account is locked for 15 minutes after five consecutive failures; subsequent login during the lockout is rejected with `423 Locked`.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-07 — Customer cannot call an admin-only plan endpoint

- **STP reference:** `TC-Sec-03` or the corresponding access-control test; confirm exact mapping in STP.
- **Requirements:** `INS-F-003`, `INS-SR-003`.
- **Preconditions / data:** authenticated synthetic `CUSTOMER` account.
- **Steps:** 1. Call `POST /api/v1/plans` with the Customer token and a valid synthetic plan body. 2. Inspect response. 3. Confirm no plan was created.
- **Expected result:** `403 Forbidden`; no plan mutation occurs.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-08 — Insurer Admin can manage only its own plans

- **STP reference:** `TC-Plan-01`; role-isolation tests as mapped in STP.
- **Requirements:** `INS-F-003`, `INS-SR-003`.
- **Preconditions / data:** two synthetic insurers with separate `INSURER_ADMIN` users and plan records.
- **Steps:** 1. Log in as Insurer A's Admin. 2. Create or update a plan belonging to Insurer A. 3. Attempt to update a plan belonging to Insurer B. 4. Capture both responses.
- **Expected result:** permitted operation succeeds as specified; cross-insurer mutation is denied (`403 Forbidden` per SAD); no other insurer's data changes.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-09 — Upload an allowed KYC file within the size limit

- **STP reference:** `TC-KYC-01`.
- **Requirements:** `INS-F-002`, `INS-SR-007`.
- **Preconditions / data:** authenticated synthetic Customer; safe synthetic PDF/JPG/PNG test fixture under 5 MB; simulated KYC service available.
- **Steps:** 1. Submit `POST /api/v1/kyc` as multipart form data with test `panNumber`, `aadhaarNumber`, and `files[]`. 2. Inspect response status and `kycStatus`. 3. Confirm no sensitive values are echoed in the response/logs.
- **Expected result:** `202 Accepted`; status uses one of the SAD values `Pending`, `Verified`, or `Rejected`, according to the configured mock/verification result; upload respects file restrictions.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-10 — Reject an unsupported KYC file type

- **STP reference:** `TC-Sec-07`.
- **Requirements:** `INS-F-002`, `INS-SR-007`.
- **Preconditions / data:** authenticated synthetic Customer; harmless test file with an unsupported extension/type.
- **Steps:** 1. Submit the unsupported file using the KYC upload form. 2. Inspect response and confirm the file is not accepted/stored.
- **Expected result:** `415 Unsupported Media Type`; file is rejected.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-11 — Reject a KYC file larger than 5 MB

- **STP reference:** `TC-Sec-07`.
- **Requirements:** `INS-F-002`, `INS-SR-007`.
- **Preconditions / data:** authenticated synthetic Customer; safe generated test file larger than 5 MB.
- **Steps:** 1. Submit the oversized file. 2. Inspect response. 3. Confirm the file was not accepted or stored.
- **Expected result:** `413 Payload Too Large`; upload is rejected.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-12 — Customer can view their own KYC status, not another user's KYC

- **STP reference:** `TC-KYC-01` plus role/data-isolation tests; confirm exact second reference in STP.
- **Requirements:** `INS-F-002`, `INS-SR-003`.
- **Preconditions / data:** synthetic Customers A and B with distinct KYC records.
- **Steps:** 1. Authenticate as Customer A and request `GET /api/v1/kyc/status`. 2. Confirm the response refers only to A's status. 3. Try any UI/API route that would access B's KYC information, using A's credentials. Do not invent a route; use only the implemented, authorized route under test.
- **Expected result:** A can see only A's status; B's KYC information is not disclosed. The exact denial code (`403` or `404`) for any record-specific route must match the approved contract.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-13 — Public browsing and filtering of plans

- **STP reference:** `TC-Plan-02` and `TC-Plan-03`.
- **Requirements:** `INS-F-004`, `INS-F-022`.
- **Preconditions / data:** multiple synthetic active plans with distinct types, insurers, premiums and coverage; no login for public browse.
- **Steps:** 1. Call `GET /api/v1/plans` without login. 2. Apply one valid filter such as `type=HEALTH&premiumMax=10000`. 3. Check every returned row against the filters.
- **Expected result:** `200 OK`; only eligible active plans matching the requested filters are shown; malformed filter values return the agreed `400` response.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-14 — Compare no more than three plans

- **STP reference:** `TC-Plan-02`.
- **Requirement:** `INS-F-004`.
- **Preconditions / data:** at least four known synthetic plan IDs.
- **Steps:** 1. Request comparison for one, two, and three known plan IDs. 2. Request comparison with four IDs. 3. Request comparison with an unknown ID.
- **Expected result:** one to three known plans returns `200 OK`; more than three IDs returns `400 Bad Request`; an unknown plan ID returns `404 Not Found`; comparison contains the documented fields.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-15 — System Admin creates users and insurer records with correct restrictions

- **STP reference:** confirm the corresponding admin/role tests in STP; the endpoint details come from SAD §4.3.6.
- **Requirements:** `INS-F-001`, `INS-SR-003`.
- **Preconditions / data:** dedicated synthetic `SYSTEM_ADMIN`, non-admin user, and synthetic insurer records.
- **Steps:** 1. Call `POST /api/v1/admin/users` as System Admin with agreed fields. 2. Try the same operation as a non-admin. 3. Attempt duplicate-email creation. 4. Confirm the agreed behavior for staff roles without an insurer. 5. Call `POST /api/v1/admin/insurers` as System Admin.
- **Expected result:** only System Admin can create admin users/insurers; user success is `201` with `{id}`; duplicate email returns `409`; a staff role lacking required insurer linkage returns `422` once request schema is confirmed with Person 4.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-16 — Audit records are restricted and append-only

- **STP reference:** `TC-Aud-01`, `TC-Sec-08`.
- **Requirements:** `INS-F-018`, `INS-SR-008`.
- **Preconditions / data:** audit entries exist from synthetic test actions; authorized System Admin and ordinary user test accounts are available; coordinate with the audit module owner.
- **Steps:** 1. Open the approved audit view/API as System Admin. 2. Try as an ordinary user. 3. Attempt to edit/delete an audit entry through any exposed UI/API route (do not assume such routes exist if not implemented).
- **Expected result:** audit data is visible only to authorized roles; ordinary user is denied (`403` per SAD `/audit`); entries cannot be edited/deleted.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-17 — Sensitive information is not exposed in UI errors or logs

- **STP reference:** `TC-Sec-02`, `TC-Sec-06`, and relevant privacy/security cases; confirm exact mapping in STP.
- **Requirements:** `INS-SR-002`, `INS-SR-006`.
- **Preconditions / data:** test environment logs accessible to authorized team only; dedicated synthetic account and test inputs.
- **Steps:** 1. Trigger a safe validation error using synthetic values. 2. Inspect the user-visible message and authorized test logs. 3. Search for the test password/token and any full test PAN/Aadhaar values. 4. Confirm errors do not reveal stack traces or secrets.
- **Expected result:** passwords/tokens and sensitive KYC values do not appear in responses or logs; error messages do not expose internals; record exact observed output and evidence without copying secrets into the issue.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

## FC-18 — Responsive screen and browser smoke checks (when UI build is available)

- **STP reference:** `TC-UX-01`, `TC-Compat-01`.
- **Requirements:** `INS-NF-005`, `INS-NF-006`.
- **Preconditions / data:** working frontend build and test account; browsers/devices available.
- **Steps:** 1. Open registration, login, KYC upload and plan-browse screens at 360 px viewport width. 2. Check labels, validation messages and horizontal scrolling. 3. Run the agreed smoke paths on the available supported versions of Chrome, Firefox, Edge and Safari; note any browser/device unavailable.
- **Expected result:** main screens remain usable at 360 px without unwanted horizontal scrolling; labels/errors are clear; compatibility claims are limited to browsers actually tested.
- **Actual result:** 
- **Result (Pass/Fail/Blocked):** 
- **Tester/date/commit:** 
- **Evidence:** 

---

## Execution register

For each execution, add a dated record with: test/checklist ID, tester, date/time, environment/build or commit SHA, actual result, Pass/Fail/Blocked, evidence link, and linked bug issue if applicable. Keep this planning checklist truthful: do not fill a result or attach an evidence link until the test has genuinely been run.
