# Team Decisions Log — Insurance Workflow Automation System

**Team:** 11  
**Updated:** 15 October 2026  
**Repository:** `Thijil-Thejaswi-MN/Team-11`  
**Purpose:** Record confirmed working practices and distinguish them from proposals that still need team sign-off.

## Status rules

Use only these three status values:

- **Confirmed** — the team has explicitly agreed, or the configuration/evidence has been verified and is available to reviewers.
- **Proposed** — a decision or implementation choice is awaiting discussion or approval.
- **Provisional** — a planning baseline or working assumption is in use but still requires confirmation or measurement.

If a decision changes the approved SRS, SAD, or STP, record the change and rationale in the project deviation log. Do not claim an endpoint, build, or test is complete unless it has actually been verified.

## Summary register

| ID | Decision area | Current position | Status | Owner | Due / review point |
|---|---|---|---|---|---|
| D-01 | Repository ownership and protected `main` | Thijil (`Thijil-Thejaswi-MN`) owns the repository and is the merge owner. Every PR requires another member's approval and the required CI check. | Provisional until the settings evidence is attached to this PR | Thijil (Person 1) | 15 Oct 2026 — attach settings evidence before re-review |
| D-02 | Frontend build tool | Vite is proposed. Verify `frontend/package.json` and the actual build command before sign-off. | Proposed | Thijil (Person 1), with Raghunandan (Person 4) verifying the frontend | 15 Oct 2026 team sign-off |
| D-03 | Database migration tool | Flyway is proposed. Verify `org.flywaydb:flyway-core` in `backend/pom.xml` and whether migration scripts exist before sign-off. | Proposed | Raghunandan (Person 4), with Thijil (Person 1) recording the decision | 15 Oct 2026 team sign-off |
| D-04 | STP test-role baseline and implementation split | Preserve the submitted STP §8 role mapping unless the team explicitly agrees and records an exception. | Provisional | All four members; Thijil records the outcome | 15 Oct 2026 team sign-off |
| D-05 | Planning-capacity assumption | Use 5 story-point-equivalent units per person-day only as a provisional planning heuristic; it is not observed velocity. | Provisional | Thijil (Person 1) facilitates; all four estimate | Review at Sprint 1 planning and after Sprint 1 completes |
| D-06 | Repository folder layout and existing course documents | `backend/`, `frontend/`, and `docs/` are the intended top-level layout. `backend/` and `frontend/` are on `main` following PRs #49 and #47. Existing Deliverable 1 files remain at the root as original submission artifacts; all new supporting documentation goes under `docs/`. | Confirmed | Thijil (Person 1) | 15 Oct 2026 — maintain for new documents |
| D-07 | Source of truth for backlog and story points | Use GitHub Issues and the existing GitHub Project as the source of truth; the Day 2 plan replaces Jira tracking with GitHub. Do not maintain duplicate points in Jira. | Confirmed | Thijil (Person 1); all owners maintain their own issues | Effective immediately |
| D-08 | `tier-b` label convention | Apply `tier-b` only to medium-priority work explicitly identified as deferrable to Sprint 2. The label does not replace or change the SRS priority. | Confirmed | Thijil (Person 1); each issue owner applies it to their own eligible issues | During backlog review; before Sprint 1 starts |
| D-09 | API-contract sign-off | Outcomes from contracts P1-03, P2-01, P3-01, and P4-03 must be recorded after the team sign-off; listed open points remain proposals until agreed. | Proposed | Person 1: Thijil; Person 2: Sharana; Person 3: Samarth; Person 4: Raghunandan | 15 Oct 2026 contract sign-off meeting |

## Decision details

### D-01 — Repository ownership and pull-request approval

**Status: Provisional until settings evidence is attached.**

- **Repository owner / merge owner:** Thijil, using the `Thijil-Thejaswi-MN` GitHub account (Person 1 / QA Lead).
- **Workflow:** changes enter `main` through pull requests; each PR needs at least one approval from a team member other than the PR author; the required GitHub Actions check must pass.
- **Configured `main` rules shown in Settings:** require a pull request before merging; require one approval; require approval from someone other than the most recent pusher; require status check `Validate repository structure`; require the branch to be up to date before merging; require conversation resolution.
- **Evidence needed for the review:** attach a screenshot from **Settings → Branches** (or **Settings → Rules**) to PR #50 showing the `main` rule and its required approval/status-check settings. Also note in the PR that the required `Repository CI / Validate repository structure` check passed. A passing repository-structure check does not mean application tests have run.
- **Rule for owner-authored PRs:** another teammate must review and approve; Thijil does not approve their own PR.

### D-02 — Frontend build tool

**Status: Proposed.** Vite was proposed, but this log does not treat the choice as final until the team confirms it.

- **Files to inspect:** `frontend/package.json` (dependencies and scripts) and the frontend lockfile.
- **Verification:** confirm the actual build command in `scripts` and run that command from `frontend/` when the environment is available. Record the real outcome; do not infer an application build passed from the repository-structure CI check.
- **Owner:** Thijil records the team decision; Raghunandan verifies the current frontend setup.

### D-03 — Database migration tool

**Status: Proposed.** Flyway was proposed for schema migrations; confirm it against the backend project and with the team.

- **Files to inspect:** `backend/pom.xml` for `org.flywaydb:flyway-core`, plus the backend migration directory (for example, `backend/src/main/resources/db/migration/`) if present.
- **Verification:** note whether the dependency and actual versioned migration scripts are present. A dependency alone does not prove migrations have been created or run.
- **Owner:** Raghunandan verifies the backend state; Thijil records the team outcome.

### D-04 — STP §8 testing-role baseline and implementation split

**Status: Provisional until the team confirms it.** The names and roles below preserve the submitted STP §8 baseline:

| Person | Member | STP role | Important responsibility |
|---|---|---|---|
| Person 1 | Thijil | QA Lead | RTM, test coordination, defect triage and Test Summary Report; independently tested by another member on his own implementation |
| Person 2 | Sharana | Functional Test Engineer | Functional, UI and acceptance testing; Selenium/Newman where appropriate |
| Person 3 | Samarth | Non-functional and Security Test Engineer | Performance, availability, backup, compatibility and security testing |
| Person 4 | Raghunandan | Developer / Environment Owner | Build and test environment, fixtures/mocks/test clock, unit-test infrastructure and fixes |

All developers write unit tests for their own code, but their own work must also receive independent testing/review. If the team agrees to change a formal STP role, record the specific exception and rationale rather than silently rewriting the baseline.

### D-05 — Planning-capacity assumption and Sprint 1 comparison

**Status: Provisional.** The working assumption is 5 story-point-equivalent units per person-day. This is a planning heuristic, not a statement of measured velocity.

- **Nominal capacity arithmetic:** 4 people × 5 working days × 5 points per person-day = **100 points** for the Sprint 1 week (19–23 October 2026).
- **Current visible Sprint 1 estimates:** the tracker screenshot reviewed so far shows 8 points estimated on Person 3's Sprint 1 work (`INS-F-007` = 3 and `INS-F-009` = 5). Several other Sprint 1 issues still have blank estimates, so **8 is only the currently known subtotal, not the Sprint 1 total**.
- **Comparison:** a reliable planned-total-versus-100 comparison cannot be made until every Sprint 1 issue has an agreed story-point estimate. The team should complete planning poker, sum the actual Sprint 1 issue estimates from the GitHub Project, and record `planned Sprint 1 total = ___ points` here. Do not use 100 as a promise or force the team to fill capacity artificially.
- **Owner:** Thijil facilitates; all four team members estimate together. Compare the assumption with actual throughput after Sprint 1.

### D-06 — Repository folder layout and existing course documents

**Status: Confirmed.** As of 15 October 2026, `frontend/` is present on `main` after PR #47 and `backend/` is present on `main` after PR #49. The intended top-level layout is:

```text
backend/
frontend/
docs/
.github/
```

The existing `SRS.docx`, `SAD.docx`, and `STP.docx` at the repository root are the previously submitted Deliverable 1 artifacts and will remain there for continuity and easy course-submission access unless the instructor asks for them to be moved. **All new working documentation**—API contracts, decisions, test plans, and supporting notes—must be placed under `docs/`.

### D-07 — Source of truth for backlog and story points

**Status: Confirmed from the Day 2 GitHub-only work plan.** GitHub Issues plus the existing `SE Mini Project - Team 11 - Insurance Workflow Automation System` GitHub Project are the source of truth for backlog status, assignments, priority, sprint and story points. Jira was superseded by the GitHub-only workflow; avoid keeping parallel or conflicting story-point values in Jira.

### D-08 — `tier-b` convention

**Status: Confirmed from the Day 2 work plan.** Use the `tier-b` label only on medium-priority Sprint 2 items explicitly agreed to be deferrable. It is a deferral/capacity label, not an alternative priority. Keep the issue's SRS priority unchanged and make deferral decisions based on agreed scope, dependencies and observed Sprint 1 progress.

### D-09 — Contract sign-off topics still to record

**Status: Proposed until the meeting records the outcomes.** Do not invent or pre-fill decisions. Update this section after review of each contract:

| Contract | Owner | Topics to confirm | Outcome |
|---|---|---|---|
| P1-03 — Quotes, underwriting, claim decisions and audit | Thijil (Person 1) | Quote/underwriting endpoint request and response shape; rule data format and fixtures; manual decision signature and reason; audit-log query/interface and requirement mapping | Pending team sign-off |
| P2-01 — Authentication, KYC, CurrentUser and plans | Sharana (Person 2) | Register/login and lockout/session behavior; role names; KYC upload limits and status; plans browse/compare/admin routes; `CurrentUser {userId, role, insurerId}` source; dev/test-only stub safeguards; staff-to-insurer association | Pending team sign-off |
| P3-01 — Applications, issuance, policies, renewal and cancellation | Samarth (Person 3) | Add `vehicleRegNo` and confirm `existsActivePolicyForVehicle`; whether policy becomes `Cancelled` at approval or refund; whether zero-refund cases skip gateway calls; policy number format; PDF library; whether `/payments` returns `policyNo` directly or the UI polls policies; server-stored renewal quote semantics | Pending team sign-off |
| P4-03 — Payments, notifications, claims and file store | Raghunandan (Person 4) | Idempotency/retry behavior; payment/notification interfaces and required events; claim states and transition ownership; allowed files and simulated scanning; `Clock` injection for time-dependent logic | Pending team sign-off |

Record the agreed answer, who agreed, date, and rationale. If an answer changes the SRS/SAD/STP, add a deviation entry.

## Review and change control

- Revisit Proposed and Provisional items at the stated meeting or review point; change a status to Confirmed only after explicit team agreement or sufficient evidence is available.
- Record decision date, participants/forum, rationale and any deviations when an item is confirmed or changed.
- Keep implementation/test claims truthful. A configured workflow or passing repository-structure check is not evidence that the frontend, backend, or application-level tests pass.

## Decision history

| Date | Change |
|---|---|
| 15 October 2026 | Revised after PR review: standardized statuses to Confirmed / Proposed / Provisional; named owners and due/review points; expanded repository-rule evidence, build-tool and migration-tool verification, capacity arithmetic, GitHub tracking source of truth, `tier-b` convention and outstanding contract sign-off questions. |
