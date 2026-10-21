# Team 11 — Requirements Traceability Matrix (RTM v0)

**Project:** Insurance Workflow Automation System  
**Deliverable:** P1-04 Traceability Deliverable  
**QA Lead:** Person 1 (Thijil)  
**Status:** Baseline established for Sprint 1 / Sprint 2 execution.  
**Approved Workbook Source:** `Team_11_Insurance_Workflow_RTM.xlsx`

---

## 1. Functional Requirements Traceability (INS-F-001 to INS-F-025)

| Req ID | Title | Type | Priority | Module | STP §12 Test Cases | Req Issue | Status |
| :--- | :--- | :---: | :---: | :--- | :--- | :---: | :---: |
| **INS-F-001** | Register / login with roles | Functional | High | C1 Account & Auth | [TC-Acc-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/57), [TC-Acc-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/63) | [#15](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/15) | Not Run |
| **INS-F-002** | KYC upload and verification | Functional | High | C2 KYC Service | [TC-KYC-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/64) | [#16](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/16) | Not Run |
| **INS-F-003** | Manage plans & premium rules | Functional | High | C4 Plan Management | [TC-Plan-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/65) | [#17](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/17) | Not Run |
| **INS-F-004** | Browse and compare plans | Functional | High | C4 Plan Management | [TC-Plan-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/66) | [#18](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/18) | Not Run |
| **INS-F-005** | Motor quote | Functional | High | C5 Quote Engine | [TC-Quote-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/68) | [#1](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/1) | Not Run |
| **INS-F-006** | Health quote | Functional | High | C5 Quote Engine | [TC-Quote-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/69) | [#5](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/5) | Not Run |
| **INS-F-007** | Submit insurance application | Functional | High | C7 Application | [TC-App-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/70) | [#2](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/2) | Not Run |
| **INS-F-008** | Payment processing & retry | Functional | High | C8 Payment Adapter | [TC-Pay-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/75), [TC-Pay-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/76), [TC-Pay-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/77) | [#42](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/42) | Not Run |
| **INS-F-009** | Generate policy & document | Functional | High | C7 Policy Issuance | [TC-Pol-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/79), [TC-Pay-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/78) | [#3](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/3) | Not Run |
| **INS-F-010** | Rule-based auto-underwriting | Functional | High | C6 Underwriting | [TC-UW-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/71), [TC-UW-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/72), [TC-UW-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/73) | [#6](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/6) | Not Run |
| **INS-F-011** | Manual review of referred cases | Functional | High | C6 Underwriting | [TC-UW-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/74) | [#10](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/10) | Not Run |
| **INS-F-012** | File a claim | Functional | High | C10 Claims | [TC-Clm-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/86) | [#32](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/32) | Not Run |
| **INS-F-013** | Claim state machine | Functional | High | C10 Claims | [TC-Clm-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/87) | [#43](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/43) | Not Run |
| **INS-F-014** | Verify, assess & decide claim | Functional | High | C10 Claims | [TC-Clm-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/88), [TC-Clm-06](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/91) | [#11](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/11) | Not Run |
| **INS-F-015** | Track claim status | Functional | Medium | C10 Claims | [TC-Clm-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/89) | [#33](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/33) | Not Run |
| **INS-F-016** | Notification on status changes | Functional | High | C9 Notifications | [TC-Ntf-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/92) | [#34](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/34) | Not Run |
| **INS-F-017** | Renewal reminders | Functional | Medium | C9 Notifications | [TC-Ren-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/81) | [#35](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/35) | Not Run |
| **INS-F-018** | Audit log | Functional | Medium | C11 Admin & Report | [TC-Aud-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/93), [TC-Sec-08](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/110) | [#19](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/19) | Not Run |
| **INS-F-019** | Renew an existing policy | Functional | High | C7 Policy | [TC-Ren-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/82), [TC-Ren-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/83) | [#4](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/4) | Not Run |
| **INS-F-020** | Cancel policy & refund | Functional | High | C7 Policy | [TC-Can-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/84), [TC-Can-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/85) | [#7](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/7) | Not Run |
| **INS-F-021** | Role-specific dashboards | Functional | Medium | C11 Admin & Report | [TC-Dash-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/95) | [#20](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/20) | Not Run |
| **INS-F-022** | Search and filter plans | Functional | Medium | C4 Plan Management | [TC-Plan-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/67) | [#21](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/21) | Not Run |
| **INS-F-023** | Duplicate claim detection | Functional | Medium | C10 Claims | [TC-Clm-05](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/90) | [#12](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/12) | Not Run |
| **INS-F-024** | View & download policy documents | Functional | Medium | C7 Policy | [TC-Pol-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/80) | [#8](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/8) | Not Run |
| **INS-F-025** | Generate admin reports | Functional | Medium | C11 Admin & Report | [TC-Rpt-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/96) | [#9](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/9) | Not Run |

---

## 2. Non-Functional Requirements Traceability (INS-NF-001 to INS-NF-007)

| Req ID | Title | Priority | Metric / Target | Test Cases | Req Issue | Status |
| :--- | :--- | :---: | :--- | :--- | :---: | :---: |
| **INS-NF-001** | Quote response time | High | < 2.0 seconds (p95) | [TC-Perf-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/97), [TC-Quote-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/68), [TC-Quote-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/69) | [#13](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/13) | Not Run |
| **INS-NF-002** | Concurrent user capacity | High | 500 concurrent sessions | [TC-Load-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/98) | [#38](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/38) | Not Run |
| **INS-NF-003** | Service availability | Medium | 99.5% uptime | [TC-Avail-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/99) | [#39](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/39) | Not Run |
| **INS-NF-004** | Backup and recovery | High | Daily backup, RPO < 24h, RTO < 2h | [TC-Rel-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/100) | [#40](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/40) | Not Run |
| **INS-NF-005** | Responsive UI | Medium | 360px to 1920px viewports | [TC-UX-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/101) | [#29](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/29) | Not Run |
| **INS-NF-006** | Browser compatibility | Medium | Chrome, Firefox, Safari, Edge | [TC-Compat-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/102) | [#30](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/30) | Not Run |
| **INS-NF-007** | Audit retention | Medium | 5-year retention | [TC-Aud-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/94) | [#14](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/14) | Not Run |

---

## 3. Security Requirements Traceability (INS-SR-001 to INS-SR-008)

| Req ID | Title | Priority | Security Control | Test Cases | Req Issue | Status |
| :--- | :--- | :---: | :--- | :--- | :---: | :---: |
| **INS-SR-001** | Transport security | High | TLS 1.2+ HTTPS only | [TC-Sec-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/103), [TC-Pay-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/77) | [#36](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/36) | Not Run |
| **INS-SR-002** | Password storage | High | bcrypt with work factor >= 12 | [TC-Sec-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/104), [TC-Sec-11](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/113) | [#22](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/22) | Not Run |
| **INS-SR-003** | RBAC & data isolation | High | Strict RBAC, deny-by-default, insurer scoping | [TC-Sec-03](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/105), [TC-Sec-09](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/111), [TC-Acc-02](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/63) | [#23](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/23) | Not Run |
| **INS-SR-004** | Encryption at rest | High | AES-256 for documents/PII | [TC-Sec-04](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/106), [TC-Sec-11](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/113) | [#37](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/37) | Not Run |
| **INS-SR-005** | Account lockout & timeouts | High | Lockout after 5 fails (15 min); 15m session timeout | [TC-Sec-05](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/107), [TC-Sec-10](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/112) | [#24](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/24) | Not Run |
| **INS-SR-006** | Injection & XSS defence | High | Param queries, input validation, CSP | [TC-Sec-06](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/108) | [#25](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/25) | Not Run |
| **INS-SR-007** | Secure file upload | Medium | Allow-list PDF/JPG/PNG, <= 5 MB, virus scan | [TC-Sec-07](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/109), [TC-KYC-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/64) | [#27](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/27) | Not Run |
| **INS-SR-008** | Append-only audit log | Medium | Non-updatable audit log, actor attribution | [TC-Sec-08](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/110), [TC-Aud-01](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/93) | [#28](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/28) | Not Run |
