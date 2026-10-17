# Penetration-test checklist (P3-03)

**Scope:** the application only, in the test environment, with synthetic data (STP section 3). No external targets.
**Status of every item: Not Run.** Fill in the Result column with the actual outcome, the date and a link to the evidence or defect.

Roles: C = Customer, IA = Insurer Admin, UW = Underwriter, SA = System Admin. Insurers: A and B (STP test data).

## 1. Token tampering (TC-Sec-03, TC-Sec-10; INS-SR-003, INS-SR-005)

| # | Attack | Expected | Result |
| --- | --- | --- | --- |
| 1.1 | Call a protected endpoint with no token | 401 | Not Run |
| 1.2 | Change one character of the token (signature) | 401 | Not Run |
| 1.3 | Edit the token payload (role C to SA, change userId or insurerId) and resend | 401, never access | Not Run |
| 1.4 | If the token is a JWT: set `alg` to `none`, or strip the signature | 401 | Not Run |
| 1.5 | Use an expired token, and a token after logout | 401 | Not Run |
| 1.6 | Use a C token on IA, UW and SA endpoints (every endpoint in SAD 4.3) | 403 ACCESS-403 | Not Run |
| 1.7 | Idle 16 minutes, then call | 401; session also ends at 8 hours (needs test clock) | Not Run |

## 2. insurerId tampering and IDOR (TC-Sec-09, TC-Sec-03; INS-SR-003)

| # | Attack | Expected | Result |
| --- | --- | --- | --- |
| 2.1 | As A-UW, A-CO, A-ADM list referred applications, claims, cancellations, dashboard | Only insurer A records, counts correct | Not Run |
| 2.2 | As insurer A staff, request insurer B records directly by id (GET, and POST decision) | 403 ACCESS-403 | Not Run |
| 2.3 | Add `insurerId=B` as a query parameter or body field | Ignored; scope comes from the session | Not Run |
| 2.4 | As customer 1, `GET /policies/{id}/document` for customer 2's policy | 403 ACCESS-403 (contract p3-contract.md 3.3) | Not Run |
| 2.5 | As customer 1, read, pay or cancel customer 2's application or policy by id | 403 or 404, no data leaked | Not Run |
| 2.6 | `POST /cancellations/{id}/decision` as the other insurer's IA | 403 ACCESS-403 | Not Run |
| 2.7 | Check the audit log | Each violation recorded | Not Run |

## 3. Double payment and payment abuse (TC-Pay-01 to TC-Pay-03; contract p3-contract.md section 6)

| # | Attack | Expected | Result |
| --- | --- | --- | --- |
| 3.1 | Send `POST /payments` twice at the same moment for one application (parallel requests) | One charge, one policy | Not Run |
| 3.2 | Pay an application that is already Paid | 409 STATE-409 | Not Run |
| 3.3 | Pay an application that is Referred or Rejected | 409 STATE-409 | Not Run |
| 3.4 | Gateway set to TIMEOUT, then retry | Not charged twice (same idempotency key logic) | Not Run |
| 3.5 | Send an extra idempotency key, amount or premium in the body | Ignored; server decides amount and key | Not Run |
| 3.6 | Cancel and refund the same policy twice, or refund after a paid claim | One refund at most; 0 after a paid claim | Not Run |
| 3.7 | Force issuance failure after a successful payment (PDF-failure simulator), then check the retry | One policy, customer not charged again | Not Run |

## 4. Replay (TC-Sec-10; TC-Pay-03)

| # | Attack | Expected | Result |
| --- | --- | --- | --- |
| 4.1 | Capture `POST /payments` and replay it later | No second charge or policy | Not Run |
| 4.2 | Replay `POST /applications` and `POST /policies/{id}/cancellation` | No duplicate side effects, or a clean 409 | Not Run |
| 4.3 | Replay `POST /cancellations/{id}/decision` (Approve) | 409 STATE-409 on the second call | Not Run |
| 4.4 | Replay a request with a token from a logged-out or expired session | 401 | Not Run |
| 4.5 | Replay a login request body after lockout | 423 even with correct password (TC-Sec-05) | Not Run |

## 5. Other STP security cases to schedule

TC-Sec-01 (TLS), 02 (password hashes), 04 (encryption at rest), 05 (lockout), 06 (injection and XSS, ZAP baseline), 07 (file upload), 08 (audit append-only), 11 (no sensitive data in logs).

## Known risks seen in code review (to confirm by testing, not yet proven)

- TC-Sec-08: the audit repository currently exposes delete methods (PR #115 review). Expect a possible failure until it is narrowed.
- Fixtures contain `hasPaidClaim` and policy fields that do not match the Policy entity (PR #117 review); check the loader before relying on refund tests.
