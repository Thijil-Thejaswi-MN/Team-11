# Sprint 2 Deviation Log

> Prepared by: Thijil Thejaswi M N (QA Lead)  
> Date: 2 Nov 2026  
> All entries based on verifiable facts (GitHub PRs, commits, and codebase inspection).  
> No invented dates, meeting references, or outcomes.

---

## How to Read This Log

| Column | Meaning |
|--------|---------|
| ID | Sequential deviation identifier (D-xx) |
| Category | Type: Process / Scope / Technical / Timeline |
| Description | What deviated from the original plan |
| Root cause | Why it happened |
| Impact | Effect on quality / schedule |
| Mitigation | Action taken or planned |
| Evidence | Verifiable reference (PR, commit, doc) |
| Status | Open / Closed / Deferred |

---

## Deviations

### D-03 — Flyway not used (Hibernate ddl-auto manages schema)

| Field | Detail |
|-------|--------|
| Category | Technical |
| Description | Database schema is managed dynamically by Hibernate `ddl-auto=update` instead of Flyway versioned migrations. |
| Root cause | Sprint 1 delivery time constraints; Flyway migration setup deprioritised by team agreement. |
| Impact | Lack of versioned migration DDL scripts; rollbacks require manual schema adjustments. |
| Mitigation | Documented in `docs/decisions.md`; Flyway adoption planned for Sprint 3. |
| Evidence | [`docs/decisions.md`](decisions.md); `application-dev.yml` |
| Status | Open (deferred to Sprint 3) |

---

### D-10 — Five requirements deferred to Sprint 3 (QA Lead decision)

| Field | Detail |
|-------|--------|
| Category | Scope / Process |
| Description | INS-F-017 Renewal reminders 30/15/1 days before expiry (P4-13), INS-F-021 Role-specific dashboards (P2-15), INS-F-022 Search and filter plans (P2-13), INS-F-025 System Admin reports (P3-11), and INS-NF-007 Audit records retained and retrievable for 5 years (P1-13) were deferred to Sprint 3. This decision was made unilaterally by the QA Lead without a team meeting. |
| Root cause | Schedule pressure, team capacity limitations, and test environment dependencies. |
| Impact | UAT-01 filter step (INS-F-022) blocked; UAT-08 reports step (INS-F-025) blocked; NF-007 not executed at freeze. |
| Mitigation | Scope formalised in `docs/decisions.md` (Decision D-10); requirements marked as Deferred (D) in RTM and execution matrix. |
| Evidence | [`docs/decisions.md`](decisions.md); `docs/requirements-traceability-matrix.md` |
| Status | Open (deferred to Sprint 3) |

---

### D-11 — Claims ownership split between P1 and P4

| Field | Detail |
|-------|--------|
| Category | Technical / Architecture |
| Description | Claims module split across roles: Person 4 (Raghunandan) owns core backend entities, service APIs, and filing; Person 1 (Thijil) owns Claims Officer appraisal desk (P1-12), daily claim auto-close scheduler (P1-19), and STP independent test verification. |
| Root cause | Original SRS ownership mapping lacked separation between customer claims submission and internal officer appraisal workflows. |
| Impact | Required coordination on controller mappings and interface contracts; PR #157 merged into `main` with conflict resolution. |
| Mitigation | Architecture documented in `docs/decisions.md` (Decision D-11); PR #157 merged into `main` (commit `693a229`). |
| Evidence | [`docs/decisions.md`](decisions.md); PR #157 |
| Status | Closed (merged into `main`) |

---

### D-PR147 — Admin-override merge of PR #147 (retrospective review)

| Field | Detail |
|-------|--------|
| Category | Process |
| Description | PR #147 was merged into `main` on 23 Oct 2026 using an admin override without peer approval recorded at merge time. |
| Root cause | Deadline pressure immediately prior to the Sprint 1 demo milestone. |
| Impact | Bypassed standard peer code review gate at the time of merging. |
| Mitigation | Reviewed retrospectively by Sharana on 27 Oct 2026; findings recorded in PR #166; review gates enforced for Sprint 2. |
| Evidence | PR #147; PR #166 (`docs/pr-155-157-158-reviews`) |
| Status | Closed (retrospective review completed) |

---

### D-VID-S1 — Sprint 1 demo video recorded after deadline with sample data

| Field | Detail |
|-------|--------|
| Category | Timeline / Quality |
| Description | The Sprint 1 demo video was recorded on 27 Oct 2026 (4 days after the 23 Oct deadline) and demonstrated screens populated with sample data. |
| Root cause | Implementation PRs were finalised on 27 Oct 2026, preventing earlier recording; live data integrations were pending Sprint 2. |
| Impact | Late deliverable submission for Sprint 1 demo milestone. |
| Mitigation | Video and documentation committed and merged via PR #165 on 27 Oct 2026; limitations disclosed in `docs/demo/README.md`. |
| Evidence | PR #165; `docs/demo/README.md` |
| Status | Closed (submitted) |

---

### D-P3-LATE — Renewal, cancellation, and recovery job delivered late in Sprint 2

| Field | Detail |
|-------|--------|
| Category | Timeline / Quality |
| Description | Policy renewal (P3-09), cancellation & refund (P3-10), and policy issuance recovery scheduler (P3-12) were delivered late in Sprint 2 via PR #167. Initial submission had misplaced files, `.jav` test extensions, and a Spring bean conflict (`NoUniqueBeanDefinitionException`). |
| Root cause | Implementation complexity across multiple lifecycle workflows and dependency injection issues in controllers. |
| Impact | Features were not merged into `main` before the 30 Oct code freeze deadline; PR was remediated on 1 Nov. |
| Mitigation | Remediated in commits `0505c78`, `85e28e4`, `2c40785`, `a472dc4`, and `bc691ce`. PR #167 approved and merged into `main` on 1 Nov. |
| Evidence | PR #167 (merged commit `eb6dd25`); local `review-notes-29-oct.md` |
| Status | Closed (merged into `main` on 1 Nov 2026) |

---

### D-POST-FREEZE-MERGES — Merges completed after 30 Oct code freeze

| Field | Detail |
|-------|--------|
| Category | Timeline / Process |
| Description | Twelve pull requests were reviewed, approved, and merged into `main` after the 30 Oct code freeze: |
| Details | • **PR #184** (fix TLS environment scripts): merged `2026-11-01T03:21:33Z` by `Thijil-Thejaswi-MN`<br>• **PR #185** (P4-14 database backup and restore procedure): merged `2026-11-01T03:23:35Z` by `Thijil-Thejaswi-MN`<br>• **PR #186** (Customer-role test evidence): merged `2026-11-01T03:25:35Z` by `Thijil-Thejaswi-MN`<br>• **PR #157** (P1-12 Claims Officer desk): merged `2026-11-01T09:40:09Z` by `Samarth-S-Maladkar`<br>• **PR #167** (P3 policies lifecycle & recovery): merged `2026-11-01T17:08:09Z` by `Thijil-Thejaswi-MN`<br>• **PR #183** (P2-16 automated test evidence & UAT checklist): merged `2026-11-02T11:48:28Z` by `Thijil-Thejaswi-MN`<br>• **PR #188** (P4-16 customer claim filing screen & timeline): merged `2026-11-02T11:51:02Z` by `Thijil-Thejaswi-MN`<br>• **PR #182** (P1-14 UAT sheet, QA status & deviation log): merged `2026-11-03T03:03:37Z` by `Thijil-Thejaswi-MN`<br>• **PR #189** (align D-10 descriptions with SRS, Newman/Selenium counts): merged `2026-11-15T04:42:06Z` by `Thijil-Thejaswi-MN`<br>• **PR #191** (remove fabricated and hard-coded UI data): merged `2026-11-15T04:44:07Z` by `Thijil-Thejaswi-MN`<br>• **PR #192** (remove developer/spec text from UI screens): merged `2026-11-15T04:59:18Z` by `Thijil-Thejaswi-MN`<br>• **PR #193** (UAT walkthrough results 8 Nov): merged `2026-11-15T05:06:26Z` by `Thijil-Thejaswi-MN` |
| Root cause | Final peer reviews, bug remediation, documentation alignment, UI cleanup, and UAT artifact delivery completed across 1–15 Nov 2026. |
| Impact | Main branch baseline was updated after the formal freeze date; all critical post-freeze fixes passed CI. |
| Mitigation | All PRs passed required CI checks and received approvals prior to merge. |
| Evidence | GitHub PRs #184, #185, #186, #157, #167, #183, #188, #182, #189, #191, #192, #193 merge timestamps (`gh pr view <n> --json mergedAt`) |
| Status | Closed |

---

### D-P4-16 — Customer claim filing screen (P4-16) delivered post-freeze

| Field | Detail |
|-------|--------|
| Category | Scope / Deliverable |
| Description | Delivered after the 30 Oct freeze: PR #188, merged `2026-11-02T11:51:02Z` by `Thijil-Thejaswi-MN`. Replaced `ClaimsPlaceholder.jsx` with full `CustomerClaimsView.jsx` integrated with backend `GET /claims` and `POST /claims`. |
| Root cause | Person 4 deliverable completed after code freeze milestone. |
| Impact | Customer self-service claim filing screen and multi-status timeline available in `main`. |
| Mitigation | Verified by PR #188 component tests and CI workflows. |
| Evidence | PR #188 merged at `a9d78bc` |
| Status | Closed (delivered) |

---

### D-CLM-06-UPLOAD — No claim document-upload endpoint (TC-Clm-06 sub-steps Blocked)

| Field | Detail |
|-------|--------|
| Category | Technical / Scope |
| Description | Backend `ClaimController.java` lacks a document re-upload endpoint (e.g. `POST /api/v1/claims/{id}/documents`). As a result, the sub-steps of `TC-Clm-06` requiring customer document upload (Day-14 resume from `DOCUMENT_VERIFICATION` to `ASSESSMENT`, and HTTP 409 rejection on a closed claim) cannot be executed. |
| Root cause | Customer-facing claim document re-upload API was not implemented in backend controllers. |
| Impact | `TC-Clm-06` sub-steps for document submission and closed-claim re-upload are Blocked. |
| Mitigation | Documented in `docs/test-execution-matrix.md` and `docs/p4-customer-role-test-execution-report.md`. Sub-steps recorded as Blocked. |
| Evidence | `ClaimController.java` on `main` (only `/verify` and `/decision` endpoints exist); `test-execution-matrix.md` |
| Status | Open |

---

### D-COMMIT-48DC530 — Commit originally authored by non-member account re-authored before merge

| Field | Detail |
|-------|--------|
| Category | Process / Governance |
| Description | Commit originally authored by a non-member account; re-authored to Sharana before merge (PR #183, merged `2026-11-02T11:48:28Z` by `Thijil-Thejaswi-MN`). |
| Root cause | Local git environment misconfiguration on the test execution workstation. |
| Impact | Resolved prior to merge; all commits on `main` carry verified team member authorship (`Sharanabasava Gouda`). |
| Mitigation | Commits re-authored and force-pushed by Sharana prior to peer review approval and merge. |
| Evidence | PR #183 commit history and git log on `main` |
| Status | Closed (resolved) |

---

### D-UAT-PROFILE — UAT executed on H2 test profile instead of MySQL

| Field | Detail |
|-------|--------|
| Category | Technical / Environment |
| Description | UAT walkthrough was executed using Spring Boot `test` profile with in-memory H2 database seeded with canonical fixtures, rather than MySQL 8 production profile. |
| Root cause | MySQL local root access was denied on the host, and Docker container engine was unavailable to host a containerized database. |
| Impact | System acceptance verification was performed against H2 in-memory database rather than production target database. |
| Mitigation | H2 MySQL compatibility mode and canonical test fixtures used; all repository operations validated by JUnit integration test suite. |
| Evidence | `docs/test-evidence/uat-2026-11-08/uat-results.md`; `docs/uat-29-oct.md` |
| Status | Closed |

---

### D-UAT-OPERATOR — UAT executed as single-operator agent-run walkthrough

| Field | Detail |
|-------|--------|
| Category | Process |
| Description | UAT was executed as an agent-run browser walkthrough on Sharana's machine (`sharanabasavagouda113-debug`) with a single operator playing all application roles, rather than a multi-person role-play session. |
| Root cause | Workstation accessibility and team coordination during walkthrough execution. |
| Impact | Role isolation was tested sequentially by one operator rather than concurrent independent team members. |
| Mitigation | Role-switching, permissions, and audit logs were verified sequentially across all 8 scenarios. |
| Evidence | `docs/test-evidence/uat-2026-11-08/uat-results.md`; `docs/uat-29-oct.md` |
| Status | Closed |

---

### D-UAT-PARTIAL — UAT-03, UAT-05, and UAT-06 not fully demonstrable

| Field | Detail |
|-------|--------|
| Category | Scope / Quality |
| Description | Three UAT scenarios could not be fully demonstrated to their STP expected conclusion: UAT-03 (renewal certificate download and linked renewed policy generation not demonstrable on screen; portfolio view shown), UAT-05 (claim stopped at `PAYMENT_PROCESSING`; automated settlement payout trigger `POST /claims/{id}/payout` blocked on backend per Decision D-11), and UAT-06 (customer claim document re-upload blocked due to missing backend API `POST /claims/{id}/documents`). |
| Root cause | Backend payout trigger unmapped per Decision D-11; missing claim document upload endpoint; renewal certificate generation not wired in UI. |
| Impact | UAT-03 and UAT-06 recorded as Partial; UAT-05 recorded as Blocked in official UAT execution sheet. |
| Mitigation | Documented in `docs/uat-29-oct.md`, `docs/test-summary-report.md`, and `docs/decisions.md`. Full end-to-end automation planned for Sprint 3. |
| Evidence | `docs/test-evidence/uat-2026-11-08/uat-results.md`; `docs/uat-29-oct.md`; `docs/decisions.md` (Decision D-11) |
| Status | Open (deferred to Sprint 3) |

### D-VID-S2 — Sprint 2 demo video

| Field | Detail |
|-------|--------|
| Category | Timeline |
| Description | Sprint 2 demo video recording and submission. |
| Root cause | [to fill after recording] |
| Impact | [to fill after recording] |
| Mitigation | [to fill after recording] |
| Evidence | [to fill after recording] |
| Status | Open — [to fill after recording] |

---

*End of deviation log. Do not mark items Closed without verifiable evidence.*
