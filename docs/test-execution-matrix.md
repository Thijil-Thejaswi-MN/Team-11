# Team 11 — Test Execution Matrix

**Project:** Insurance Workflow Automation System  
**Deliverable:** P1-04 Test Management Setup (STP §8, §12 & Appendix A)  
**QA Lead:** Person 1 (Thijil)  
**Execution Status Policy:** All execution outcomes remain strictly `Not Run` until tests are genuinely executed during Sprint 1 / Sprint 2.

---

## Independent Test Execution Register (52 Test Cases)

| Case ID | Scenario | Test Level | Priority | Sprint | Author | Independent Executor | GitHub Issue | Execution Status |
| :--- | :--- | :--- | :---: | :---: | :--- | :--- | :---: | :---: |
| **TC-Acc-01** | Register and log in | System | High | Sprint 1 | Sharana | Thijil Thejaswi M.N. | [#57](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/57) | Not Run |
| **TC-Acc-02** | Role controls access; staff need an insurer | System | High | Sprint 2 | Sharana | Samarth S. Maladkar | [#63](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/63) | Not Run |
| **TC-KYC-01** | KYC verification and blocking | System | High | Sprint 1 | Sharana | Thijil Thejaswi M.N. | [#64](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/64) | Not Run |
| **TC-Plan-01** | Insurer Admin manages plans | System | High | Sprint 1 | Sharana | Raghunandan Kotary | [#65](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/65) | Not Run |
| **TC-Plan-02** | Browse and compare plans | System | High | Sprint 1 | Sharana | Samarth S. Maladkar | [#66](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/66) | Not Run |
| **TC-Plan-03** | Search and filter plans | Integration | Medium | Sprint 2 | Sharana | Thijil Thejaswi M.N. | [#67](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/67) | Not Run |
| **TC-Quote-01** | Motor quote follows the rating rule | Integration | High | Sprint 1 | Thijil | Samarth S. Maladkar | [#68](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/68) | Not Run |
| **TC-Quote-02** | Health quote follows the rating rule | Integration | High | Sprint 1 | Thijil | Sharana | [#69](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/69) | Not Run |
| **TC-App-01** | Submit an application | System | High | Sprint 1 | Samarth | Thijil Thejaswi M.N. | [#70](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/70) | Not Run |
| **TC-UW-01** | Motor underwriting rules (boundary values) | Unit | High | Sprint 1 | Thijil | Raghunandan Kotary | [#71](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/71) | Not Run |
| **TC-UW-02** | Health underwriting rules (boundary values & precedence) | Unit | High | Sprint 1 | Thijil | Samarth S. Maladkar | [#72](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/72) | Not Run |
| **TC-UW-03** | Missing rule configuration | Integration | Medium | Sprint 1 | Thijil | Sharana | [#73](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/73) | Not Run |
| **TC-UW-04** | Manual review of referred applications | System | High | Sprint 2 | Thijil | Samarth S. Maladkar | [#74](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/74) | Not Run |
| **TC-Pay-01** | Successful payment issues the policy | System | High | Sprint 1 | Samarth | Thijil Thejaswi M.N. | [#75](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/75) | Not Run |
| **TC-Pay-02** | Failed payment, retry and wrong state | System | High | Sprint 1 | Samarth | Raghunandan Kotary | [#76](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/76) | Not Run |
| **TC-Pay-03** | Idempotency: no double charge | Integration | High | Sprint 1 | Samarth | Thijil Thejaswi M.N. | [#77](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/77) | Not Run |
| **TC-Pay-04** | Policy issuance failure after successful payment | Integration | High | Sprint 2 | Samarth | Sharana | [#78](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/78) | Not Run |
| **TC-Pol-01** | Policy number and document content | System | High | Sprint 1 | Samarth | Sharana | [#79](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/79) | Not Run |
| **TC-Pol-02** | Download policy documents | System | Medium | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#80](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/80) | Not Run |
| **TC-Ren-01** | Renewal reminders | Integration | Medium | Sprint 2 | Samarth | Raghunandan Kotary | [#81](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/81) | Not Run |
| **TC-Ren-02** | Renewal quote, payment and renewed policy | System | High | Sprint 2 | Samarth | Sharana | [#82](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/82) | Not Run |
| **TC-Ren-03** | Renewal window boundaries | Integration | Medium | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#83](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/83) | Not Run |
| **TC-Can-01** | Cancellation in the free-look period | System | High | Sprint 2 | Samarth | Raghunandan Kotary | [#84](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/84) | Not Run |
| **TC-Can-02** | Pro-rata refund, boundaries, claim paid, rejection | System | High | Sprint 2 | Samarth | Sharana | [#85](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/85) | Not Run |
| **TC-Clm-01** | File a claim and eligibility rules | System | High | Sprint 2 | Thijil | Samarth S. Maladkar | [#86](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/86) | Not Run |
| **TC-Clm-02** | Claim state machine rejects skipped states | Integration | High | Sprint 2 | Thijil | Sharana | [#87](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/87) | Not Run |
| **TC-Clm-03** | Verify, assess, decide and settle | System | High | Sprint 2 | Thijil | Raghunandan Kotary | [#88](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/88) | Not Run |
| **TC-Clm-04** | Track claim status | System | Medium | Sprint 2 | Thijil | Samarth S. Maladkar | [#89](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/89) | Not Run |
| **TC-Clm-05** | Duplicate claim detection | Integration | Medium | Sprint 2 | Thijil | Sharana | [#90](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/90) | Not Run |
| **TC-Clm-06** | Closed - No Response after 15 days | Integration | Medium | Sprint 2 | Thijil | Raghunandan Kotary | [#91](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/91) | Not Run |
| **TC-Ntf-01** | Every status change sends a notification | System | High | Sprint 2 | Raghunandan | Thijil Thejaswi M.N. | [#92](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/92) | Not Run |
| **TC-Aud-01** | Audit log content and access | System | Medium | Sprint 2 | Thijil | Samarth S. Maladkar | [#93](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/93) | Not Run |
| **TC-Aud-02** | Record retention | Integration | Medium | Sprint 2 | Thijil | Raghunandan Kotary | [#94](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/94) | Not Run |
| **TC-Dash-01** | Role-specific dashboards | System | Medium | Sprint 2 | Sharana | Thijil Thejaswi M.N. | [#95](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/95) | Not Run |
| **TC-Rpt-01** | Admin reports reconcile | System | Medium | Sprint 2 | Samarth | Sharana | [#96](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/96) | Not Run |
| **TC-Perf-01** | Quote response time | System | High | Sprint 2 | Samarth | Raghunandan Kotary | [#97](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97) | Not Run |
| **TC-Load-01** | 500 concurrent users | System | High | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#98](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/98) | Not Run |
| **TC-Avail-01** | Availability | System | Medium | Sprint 2 | Samarth | Sharana | [#99](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/99) | Not Run |
| **TC-Rel-01** | Backup and restore drill | System | High | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#100](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/100) | Not Run |
| **TC-UX-01** | Responsive UI | System | Medium | Sprint 2 | Raghunandan | Thijil Thejaswi M.N. | [#101](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/101) | Not Run |
| **TC-Compat-01**| Browser compatibility | System | Medium | Sprint 2 | Raghunandan | Samarth S. Maladkar | [#102](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/102) | Not Run |
| **TC-Sec-01** | HTTPS and TLS 1.2+ | System | High | Sprint 2 | Samarth | Sharana | [#103](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/103) | Not Run |
| **TC-Sec-02** | Password storage | Integration | High | Sprint 1 | Samarth | Thijil Thejaswi M.N. | [#104](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/104) | Not Run |
| **TC-Sec-03** | RBAC matrix, deny by default | System | High | Sprint 2 | Samarth | Raghunandan Kotary | [#105](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/105) | Not Run |
| **TC-Sec-04** | Encryption at rest | Integration | High | Sprint 2 | Samarth | Sharana | [#106](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/106) | Not Run |
| **TC-Sec-05** | Account lockout | System | High | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#107](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/107) | Not Run |
| **TC-Sec-06** | Injection and XSS defence | System | High | Sprint 2 | Samarth | Raghunandan Kotary | [#108](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/108) | Not Run |
| **TC-Sec-07** | Secure file upload | System | Medium | Sprint 1 | Samarth | Sharana | [#109](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/109) | Not Run |
| **TC-Sec-08** | Audit entries are append-only and attributed | Integration | Medium | Sprint 2 | Thijil | Samarth S. Maladkar | [#110](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/110) | Not Run |
| **TC-Sec-09** | Insurer isolation | System | High | Sprint 2 | Samarth | Thijil Thejaswi M.N. | [#111](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/111) | Not Run |
| **TC-Sec-10** | Session inactivity timeout | System | High | Sprint 2 | Samarth | Sharana | [#112](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/112) | Not Run |
| **TC-Sec-11** | No sensitive data in logs | Integration | Medium | Sprint 2 | Samarth | Raghunandan Kotary | [#113](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/113) | Not Run |

---

## Traceability and Fixture Audit Notes (Prep-Week Verification)

1. **Test Case Register Verification:** All 52 test cases (`TC-Acc-01` through `TC-Sec-11`) have been independently audited on GitHub and verified to exist as active GitHub Issues ([#57](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/57) and [#63](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/63) through [#113](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/113)).
2. **Fixture Data Alignment:**
   - Fixtures located in `backend/src/test/resources/fixtures/` (`insurers.json`, `users.json`, `plans.json`, `policies.json`, `claims.json`) provide synthetic data conforming to STP Section 6.
   - **Schema Reconciliation Finding:** `policies.json` contains legacy fixture fields (`premium`, `hasPaidClaim`, `type`, `sumInsured`) that differ from the normalized database entity [`Policy.java`](../backend/src/main/java/com/team11/insurance/policies/Policy.java) (which uses `premiumPaid` and derives claims from the claims table). The Person 4 test data loader must map `premium -> premiumPaid` to avoid ingestion failure.
3. **Execution Policy Enforcement:** Every test case retains the `Not Run` status. In compliance with CONTRIBUTING Section 8, no test case is marked as passed until verified test evidence is attached during Sprint 1 / Sprint 2 execution.
