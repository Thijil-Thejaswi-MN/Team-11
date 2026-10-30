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

### D-11 — Claims ownership split between P1 and P3

| Field | Detail |
|-------|--------|
| Category | Technical / Scope |
| Description | Claims Officer desk (INS-F-014) owned by P1 (Thijil) while renewal/cancellation/recovery (P3) owned by P3 (Samarth); boundary caused duplicate `PolicyCancellationController` paths. |
| Root cause | Initial SRS ownership mapping did not anticipate cross-feature controller overlap. |
| Impact | PR #157 and PR #167 touch adjacent controller classes; review identified isolation bug in #167 (pending Samarth's fix). |
| Mitigation | Documented in `docs/decisions.md`; PR #167 blocked from merge until isolation bug fixed. |
| Evidence | [`docs/decisions.md`](decisions.md); PR #157, PR #167 |
| Status | Open (PR #167 pending Samarth's fix) |

---

### D-PR147 — Admin-override merge (Sprint 1, retrospective review)

| Field | Detail |
|-------|--------|
| Category | Process |
| Description | PR #147 merged via admin override; formal peer review not recorded at merge time. |
| Root cause | Time constraint before Sprint 1 demo. |
| Impact | Change bypassed standard review gate; reviewed retrospectively. |
| Mitigation | Retrospective review conducted; findings documented; process tightened for Sprint 2. |
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

### D-167-DELAY — Renewal/cancellation/recovery (PR #167) not merged at code freeze

| Field | Detail |
|-------|--------|
| Category | Timeline / Quality |
| Description | PR #167 (INS-F-008/009/010/012 — Samarth) not merged at code freeze due to three code-quality blockers identified in QA review. |
| Root cause | (1) `PolicyRenewalController.java` and `CancellationQueueView.jsx` placed at repo root instead of `src/…` paths — not compiled or wired. (2) `aPolicyIssuanceRecoveryScheduler.jav` and `aPolicyLifecycleExtensionTest.jav` use `.jav` extension — Maven Surefire never executed them. (3) `PolicyCancellationController.java` compares `cancelReq.getInsurerId()` vs `currentUser.getUserId()` instead of `currentUser.getInsurerId()` — breaks multi-tenant isolation. |
| Impact | Customer renewal, cancellation, recovery flows not in main at code freeze; UAT-03, UAT-04 partially blocked. |
| Mitigation | QA review findings communicated to Samarth (PR #167 review notes, local file). Merge blocked until fixes pushed and CI re-verified. |
| Evidence | PR #167 (OPEN); local `review-notes-29-oct.md` (not committed) |
| Status | Open — awaiting Samarth's fix |

---

## Pending Items (to be filled after UAT)

| ID | Description | Owner |
|----|-------------|-------|
| _(to fill)_ | Any defects found during UAT execution | Thijil (records), team |
| _(to fill)_ | Actual test counts vs. planned | Thijil |

---

*End of deviation log. Do not mark items Closed without verifiable evidence.*
