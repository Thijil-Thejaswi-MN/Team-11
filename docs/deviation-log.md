# Sprint 2 Deviation Log

> Prepared by: Thijil Thejaswi M N (QA Lead)  
> Date: 30 Oct 2026 (code freeze)  
> All entries based on verifiable facts (GitHub PRs, commits, `docs/decisions.md`).  
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

### D-03 — Flyway not adopted in Sprint 1

| Field | Detail |
|-------|--------|
| Category | Technical |
| Description | Database schema managed by Hibernate `ddl-auto=update` instead of Flyway migrations as originally intended. |
| Root cause | Sprint 1 time pressure; Flyway setup deprioritised by team agreement. |
| Impact | No versioned migration history; rollbacks require manual DDL. |
| Mitigation | Documented in `docs/decisions.md`; Flyway adoption planned for Sprint 3. |
| Evidence | [`docs/decisions.md`](decisions.md) |
| Status | Open (deferred to Sprint 3) |

---

### D-10 — Five requirements deferred to Sprint 3

| Field | Detail |
|-------|--------|
| Category | Scope |
| Description | INS-F-017 (quote filter), INS-F-021 (duplicate-claim auto-flag full), INS-F-022 (application filter), INS-F-025 (System Admin reports), NF-007 (performance/load test) not delivered in Sprint 2. |
| Root cause | Team capacity, inter-module dependency complexity, and test-environment limitations. |
| Impact | UAT-01 filter step (INS-F-022) blocked; UAT-08 reports step (INS-F-025) blocked; NF-007 not testable at code freeze. |
| Mitigation | UAT rows UAT-01 and UAT-08 marked Blocked for those sub-steps; deferred rows noted in `uat-29-oct.md`. |
| Evidence | [`docs/decisions.md`](decisions.md) |
| Status | Open (deferred to Sprint 3) |

---

### D-11 — Claims ownership split between P1 and P4

| Field | Detail |
|-------|--------|
| Category | Technical / Scope |
| Description | Claims Officer desk (INS-F-014) owned by P1 (Thijil) while core backend entities and APIs owned by P4 (Raghunandan); renewal/cancellation/recovery owned by P3 (Samarth). |
| Root cause | Initial SRS ownership mapping did not anticipate cross-feature controller overlap. |
| Impact | PR #157 and PR #167 touched adjacent controller classes and required conflict resolution and isolation fixes. |
| Mitigation | Documented in `docs/decisions.md`; P1-12 conflict resolved in PR #157; PR #167 isolation bug remediated in latest push. |
| Evidence | [`docs/decisions.md`](decisions.md); PR #157, PR #167 |
| Status | Open (PR #167 and PR #157 pending team approval) |

---

### D-PR147 — Admin-override merge (Sprint 1, retrospective review)

| Field | Detail |
|-------|--------|
| Category | Process |
| Description | PR #147 merged via admin override; formal peer review not recorded at merge time. |
| Root cause | Time constraint before Sprint 1 demo. |
| Impact | Change bypassed standard review gate; reviewed retrospectively. |
| Mitigation | Retrospective review conducted; findings documented; process tightened for Sprint 2 with mandatory peer review. |
| Evidence | PR #147 |
| Status | Closed (retrospective review completed) |

---

### D-VID — Sprint 1 demo video recorded after deadline

| Field | Detail |
|-------|--------|
| Category | Timeline |
| Description | Sprint 1 demo video recorded 27 Oct 2026; original deadline was 23 Oct 2026. |
| Root cause | Sprint 1 implementation was completed and merged on 27 Oct 2026; recording before that date was not feasible. |
| Impact | Submission was late by ~4 days. |
| Mitigation | Video committed and PR #165 merged on 27 Oct 2026; Sprint 1 screens showing sample data (live data pending D-10 features) noted in video README. |
| Evidence | PR #165; `docs/demo/README.md` |
| Status | Closed (video submitted) |

---

### D-167-DELAY — Renewal/cancellation/recovery delivered late in Sprint 2

| Field | Detail |
|-------|--------|
| Category | Timeline / Quality |
| Description | PR #167 (INS-F-008/009/010/012 — Samarth) not merged into main at freeze due to initial code-quality blockers (misplaced files, `.jav` test extensions, insurer isolation check). |
| Root cause | Late submission and file placement errors; fixes committed on 30 Oct (commits `0505c78`, `1763092`, `12d199b`, `d954583`). |
| Impact | Renewal and cancellation features not present in main at freeze SHA `00add84`. |
| Mitigation | Samarth pushed remediation commits to branch; PR #167 is passing all 4 CI workflows and awaiting team review approval. |
| Evidence | PR #167; local `review-notes-29-oct.md` |
| Status | Open — awaiting approval & merge |

---

### D-P4-16 — Customer claim filing screen (P4-16) not delivered at freeze

| Field | Detail |
|-------|--------|
| Category | Scope / Deliverable |
| Description | Customer claim filing UI (P4-16) is not delivered to `main` at code freeze. Customer portal routes to `ClaimsPlaceholder.jsx` rather than a live filing form. |
| Root cause | Person 4 deliverable not completed / not merged before freeze deadline. |
| Impact | Customers cannot file claims directly via self-service UI; claims must be seeded or submitted via API. |
| Mitigation | Documented as open bug [#176](https://github.com/Thijil-Thejaswi-MN/Team-11/issues/176); fallback placeholder UI informs user. |
| Evidence | `main` tree check at SHA `00add84`; Issue #176; `ClaimsPlaceholder.jsx` |
| Status | Open (deferred / defect #176) |

---

### D-P4-14 — Database backup and restore procedure (P4-14) not in main at freeze

| Field | Detail |
|-------|--------|
| Category | Scope / Deliverable |
| Description | Database backup script and restore procedure (P4-14 / TC-Rel-01) is not present in `main` at freeze SHA `00add84`. |
| Root cause | PR #185 opened late (30 Oct 14:10 UTC) and has not received peer approval for merge. |
| Impact | Automated backup procedure not part of current freeze baseline. |
| Mitigation | Implementation exists in branch `feat/p4-14-database-backup-recovery` (PR #185); awaiting review and merge. |
| Evidence | `main` tree check at SHA `00add84`; PR #185 |
| Status | Open — pending PR #185 review & merge |

---

### D-CLM-06-UPLOAD — Claim document upload for TC-Clm-06 not available

| Field | Detail |
|-------|--------|
| Category | Technical / Scope |
| Description | Document re-upload endpoint for customers in response to `CLAIM_DOCUMENTS_REQUESTED` is not available for end-to-end TC-Clm-06 flow. |
| Root cause | Customer-facing claim document upload endpoint assigned to Person 4 (under Decision D-11) remains unmapped in controller. |
| Impact | Sub-step of TC-Clm-06 requiring customer document upload cannot be executed via API/UI; marked Blocked for that sub-step. |
| Mitigation | Documented in `docs/test-execution-matrix.md` and `docs/test-evidence/2026-10-29-sharana-automated-results.md`. |
| Evidence | Decision D-11; `ClaimAutoCloseSchedulerIntegrationTest.java` |
| Status | Open |

---

### D-COMMIT-48DC530 — [to confirm] Commit 48dc530 authored by non-member account

| Field | Detail |
|-------|--------|
| Category | Process / Governance |
| Description | Commit `48dc5304f77e71291a56dc1e70a41a510cc76644` on branch `181-p2-16-sharana-test-evidence` (PR #183) was authored by non-member GitHub account `vineethpmural123-rgb` (`vineethpmural123@gmail.com`) instead of Sharana (`sharanabasavagouda113-debug`). |
| Root cause | Local git configuration on the executing machine was set to an external personal account during commit creation. |
| Impact | Violates repository authorship integrity policy; PR #183 blocked from merge until re-authored. |
| Mitigation | Commit must be re-authored with Sharana's credentials (`git commit --amend --author="sharanabasavagouda113-debug <...>"`), force-pushed, and re-reviewed. |
| Evidence | PR #183 commit log (`48dc5304f77e71291a56dc1e70a41a510cc76644`) |
| Status | Open — [to confirm] awaiting re-authoring |

---

## Pending Items (to be filled after UAT)

| ID | Description | Owner |
|----|-------------|-------|
| _(to fill)_ | Any defects found during UAT execution | Thijil (records), team |
| _(to fill)_ | Actual test counts vs. planned | Thijil |

---

*End of deviation log. Do not mark items Closed without verifiable evidence.*
