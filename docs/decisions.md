# Team Decisions Log — Insurance Workflow Automation System

**Team:** 11  
**Updated:** 17 October 2026
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
| D-01 | Repository ownership and protected `main` | Thijil (`Thijil-Thejaswi-MN`) owns the repository and is the merge owner. Every PR requires another member's approval and all required CI checks (`Validate repository structure`, `Frontend CI`, `Backend CI`). Branch protection verified via GitHub API. | Confirmed | Thijil (Person 1) | Confirmed 16 Oct 2026 via live GitHub branch-protection audit |
| D-02 | Frontend build tool | Vite 5.4.8 + React 18 is the standard frontend build tool. Verified via `frontend/package.json` (`build: "vite build"`, `test: "vitest run"`), with 100% passing automated builds in GitHub Actions Frontend CI. | Confirmed | Thijil (Person 1) / Raghunandan (Person 4) | Confirmed 16 Oct 2026 via package.json & CI execution evidence |
| D-03 | Database migration strategy | MySQL 8 with Hibernate `ddl-auto: update` for development and `create-drop` for automated tests during Sprint 1. Planned Sprint 2 work: introduce Flyway and versioned database migrations. Flyway is planned, not already implemented. Update and create-drop are strictly non-production strategies. | Confirmed | Raghunandan (Person 4) & Thijil (Person 1) | Confirmed 17 Oct 2026 by team agreement |
| D-04 | STP test-role baseline and implementation split | Preserve the submitted STP §8 role mapping: Person 1 (QA Lead), Person 2 (Functional Test), Person 3 (NF & Security), Person 4 (Developer/Environment). Verified preserved across all Day 1–4 deliverables. | Confirmed | All four members; Thijil records the outcome | Confirmed 16 Oct 2026 across prep-week deliverables |
| D-05 | Planning-capacity assumption | 5 story points per person-day planning heuristic. Backlog estimation accounting verified across all 102 items (31 positive estimates totalling 118 pts, 2 explicit zeros, 69 unestimated; 118 pts represents total positively estimated project scope, not a Sprint 1 commitment). | Provisional | Thijil (Person 1) facilitates; all four estimate | Review during Sprint 1 execution and Sprint 1 retrospective |
| D-06 | Repository folder layout and existing course documents | `backend/`, `frontend/`, and `docs/` are the intended top-level layout. `backend/` and `frontend/` are on `main` following PRs #49 and #47. Existing Deliverable 1 files remain at the root as original submission artifacts; all new supporting documentation goes under `docs/`. | Confirmed | Thijil (Person 1) | 15 Oct 2026 — maintain for new documents |
| D-07 | Source of truth for backlog and story points | Use GitHub Issues and the existing GitHub Project as the source of truth; the Day 2 plan replaces Jira tracking with GitHub. Do not maintain duplicate points in Jira. | Confirmed | Thijil (Person 1); all owners maintain their own issues | Effective immediately |
| D-08 | `tier-b` label convention | Apply `tier-b` only to medium-priority work explicitly identified as deferrable to Sprint 2. The label does not replace or change the SRS priority. | Confirmed | Thijil (Person 1); each issue owner applies it to their own eligible issues | During backlog review; before Sprint 1 starts |
| D-09 | API-contract sign-off | Contract documents drafted and merged for P1-03 (PR #53), P2-01 (PR #54), and P3-01 (PR #46); P4-03 interface contracts drafted in PR #128. Formal joint sign-off across all four members remains pending until technical contract discrepancies (DTO naming, IFileStore signatures, Clock usage) are resolved. D-09.5 business rule confirmed (claims paid only when SETTLED). | Proposed | Person 1: Thijil; Person 2: Sharana; Person 3: Samarth; Person 4: Raghunandan | Pre-Sprint 1 sign-off meeting |

## Decision details

### D-01 — Repository ownership and pull-request approval

**Status: Confirmed.**

- **Repository owner / merge owner:** Thijil, using the `Thijil-Thejaswi-MN` GitHub account (Person 1 / QA Lead).
- **Workflow:** Changes enter `main` exclusively through pull requests; each PR requires at least one approving review from a team member other than the PR author; all required GitHub Actions checks must pass.
- **Configured `main` Branch Protection (Verified via GitHub API):**
  - `strict: true` (Branch must be strictly up to date with `main` before merging)
  - `required_status_checks`: `Validate repository structure`, `Frontend CI`, `Backend CI`
  - `required_approving_review_count: 1`
  - `require_last_push_approval: true` (New pushes dismiss approvals, requiring re-review)
  - `dismiss_stale_reviews: true`
  - `required_conversation_resolution: true`
  - `enforce_admins: true` (Admin enforcement active; owner cannot bypass rules)
- **Rule for owner-authored PRs:** Another teammate must independently review and approve (e.g., Samarth approved PR #117 and approved PR #115, merged at `9acef3d`); Thijil never self-approves.

### D-02 — Frontend build tool

**Status: Confirmed.**

- **Selected Toolchain:** Vite 5.4.8 with `@vitejs/plugin-react` (4.3.1), React 18.3.1, React Router DOM 6.26.2, and Bootstrap 5.3.3.
- **Verification Evidence:**
  - `frontend/package.json` contains:
    - `"build": "vite build"`
    - `"dev": "vite"`
    - `"test": "vitest run"`
  - Automated build and test pipeline in `.github/workflows/ci.yml` (`Frontend CI`) runs `npm run build` and `npm test` on Node 20, passing consistently on all PRs.
- **Owner:** Confirmed by Person 4 (Raghunandan, frontend setup) and Person 1 (Thijil, QA lead).

### D-03 — Database migration strategy (Hibernate ddl-auto for Sprint 1, Flyway for Sprint 2)

**Status: Confirmed.** (Agreed by team 17 October 2026)

#### Agreed Strategy
1. **Database Platform:** The backend application continues to use MySQL 8 as the primary relational database.
2. **Development Environment (Sprint 1):** Hibernate `ddl-auto: update` is used in `application-dev.yml` to allow rapid feature iteration while entity models are actively being established across modules.
3. **Automated Tests (Sprint 1):** Hibernate `ddl-auto: create-drop` is used in `application-test.yml` against the in-memory H2 test database to guarantee clean test fixture isolation.
4. **Planned Sprint 2 Work:** Introduce Flyway (`org.flywaydb:flyway-core`) and versioned database migrations (`V1__baseline.sql`, etc.) once the core schema stabilizes at the conclusion of Sprint 1. Flyway is planned, not already implemented.
5. **Production Safeguard:** Neither `update` nor `create-drop` will be permitted as the production schema-management strategy. Production deployments will strictly use versioned, audited SQL migrations.

#### Current Repository State
- `backend/pom.xml` does **not** include `org.flywaydb:flyway-core`.
- `backend/src/main/resources/db/migration` directory does **not** exist.
- `application-dev.yml` uses: `spring.jpa.hibernate.ddl-auto: update` (against MySQL 8).
- `application-test.yml` uses: `spring.jpa.hibernate.ddl-auto: create-drop` (against in-memory H2).
- PR #124 description explicitly notes: *"Database migration: D-03 (Flyway) remains undecided by the team. This PR uses Hibernate ddl-auto consistent with existing branches without unilaterally introducing migration scripts."*

#### Comparative Analysis
| Evaluation Criteria | Option A: Immediate Flyway (`org.flywaydb:flyway-core`) | Option B (Selected): Hibernate `ddl-auto` for Sprint 1 + Flyway for Sprint 2 |
| :--- | :--- | :--- |
| **Schema Reproducibility** | High: Every environment migrates through exact, versioned `V1__...sql` scripts. | Phased: Rapid prototyping in Sprint 1; strict migration versioning in Sprint 2. |
| **Sprint 1 Development Friction** | High: Every JPA entity modification requires manual DDL migration scripts; frequent checksum conflicts during parallel branch merges. | Low: Entity annotations (`@Entity`, `@Table`, `@Column`) automatically update schema in dev/test; zero overhead for rapid feature iteration. |
| **Risk of Schema Drift** | Very Low: Migrations fail if checksums do not match. | Mitigated: Entity definitions are version-controlled in Git; schema baseline will freeze before Sprint 2 Flyway introduction. |
| **Impact on Existing Codebase** | Requires baseline DDL for 10+ JPA entities, adjusting H2 test configuration, and adding dependency to `pom.xml`. | Zero code changes or dependency changes required for Sprint 1 kickoff; all automated test suites continue running cleanly. |

### D-04 — STP §8 testing-role baseline and implementation split

**Status: Confirmed.**

The formal STP §8 testing-role baseline is confirmed and actively maintained across all project tasks:

| Person | Member | STP role | Verified Prep-Week Scope |
|---|---|---|---|
| Person 1 | Thijil | QA Lead | RTM, test coordination, defect triage, TSR, and P1-05 / P1-17 implementation; reviewed by Samarth |
| Person 2 | Sharana | Functional Test Engineer | Functional & UI testing, Selenium/Newman frameworks (P2-04), security baseline (P2-05), and auth (P2-05); reviewed by Thijil |
| Person 3 | Samarth | Non-functional and Security Test Engineer | JMeter load/perf skeletons (P3-03), pen-test checklist, and P3-02 data model; reviewed by Thijil |
| Person 4 | Raghunandan | Developer / Environment Owner | CI pipeline (P4-04), frontend setup (P4-01), backend setup (P4-02), and environment requirements |

Every developer writes unit tests for their own implementation, and every PR undergoes mandatory independent review by another team member before merge.

### D-05 — Planning-capacity assumption and Sprint 1 comparison

**Status: Provisional.**

- **Planning Heuristic:** 5 story-point units per person-day (100 nominal points for a 5-day sprint with 4 members).
- **Backlog Estimation Accounting:** On 16 Oct 2026, the team completed the story-point estimation audit across all 102 items in GitHub Project #1:
  - **31 issues with positive estimates**, totalling **118 story points** using Fibonacci sizing (1, 2, 3, 5, 8).
  - **2 documentation issues** (#52 and #121) explicitly assigned numeric `0`.
  - **69 items unestimated** under the team's estimation policy: 16 requirement-tracking issues, 52 individual test cases, and 1 duplicate issue (#58).
  - **Scope Distinction:** 118 points represents the total positively estimated work across the project under the team's current estimation policy, not a commitment to complete 118 points in Sprint 1. Sprint 1 scope is prioritized and sized against the nominal 100-point capacity heuristic.
- **Review Point:** Actual team velocity will be measured at the conclusion of Sprint 1 (23 Oct 2026) to calibrate Sprint 2 capacity.

### D-06 — Repository folder layout and existing course documents

**Status: Confirmed.** Top-level layout remains:
```text
backend/
frontend/
docs/
.github/
```
Deliverable 1 documents remain at the repository root; all new working documentation resides under `docs/`.

### D-07 — Source of truth for backlog and story points

**Status: Confirmed.** GitHub Issues and GitHub Project #1 (`SE Mini Project - Team 11 - Insurance Workflow Automation System`) are the sole sources of truth. Jira tracking is discontinued.

### D-08 — `tier-b` convention

**Status: Confirmed.** The `tier-b` label is strictly reserved for medium-priority Sprint 2 items agreed to be deferrable if capacity requires. SRS priorities remain unaltered.

### D-09 — Contract sign-off topics

**Status: Proposed.** (Pending formal joint team sign-off)

The contract documents define cross-module API payloads, status models, and shared Java interfaces. While initial contract drafts have been reviewed and merged (or submitted as draft PRs), **formal joint team sign-off has not yet been recorded**. Implementation PRs passing CI do not constitute architectural sign-off.

| Contract | Owner | Contract Document & PR | Downstream Implementation | Contract Lifecycle State | Next Action for Sign-off |
|---|---|---|---|---|---|
| **P1-03** — Quotes, underwriting, claim decisions, audit | Thijil (Person 1) | [`api-contract/01-quotes-underwriting-claims-audit.md`](api-contract/01-quotes-underwriting-claims-audit.md)<br>Issue #52, Merged in PR #53 | P1-05 quote/audit stubs (PR #115 merged at `9acef3d`); P1-17 plan rules (PR #117 merged) | Contract Drafted & Merged to `main`; Implementation in progress; **Formal Sign-Off Pending** | Review endpoint signatures and transaction semantics in joint team meeting |
| **P2-01** — Authentication, KYC, CurrentUser, plans | Sharana (Person 2) | [`api-contract/02-auth-kyc-plans.md`](api-contract/02-auth-kyc-plans.md)<br>Merged in PR #54 | P2-02 CurrentUser stub (PR #118 merged); P2-05 backend auth (PR #124 in review) | Contract Drafted & Merged to `main`; Implementation in progress; **Formal Sign-Off Pending** | Confirm KYC payload, role scoping, and auth contract alignment in joint team meeting |
| **P3-01** — Applications, issuance, policies, renewal, cancellation | Samarth (Person 3) | [`contracts/p3-contract.md`](contracts/p3-contract.md)<br>Merged in PR #46 | P3-02 data models for application, policy, cancellation (PR #56 merged) | Contract Drafted & Merged to `main`; Implementation in progress; **Formal Sign-Off Pending** | Confirm policy number format, cancellation refund state transitions in joint team meeting |
| **P4-03** — Payments, notifications, claims, file store, test clock | Raghunandan (Person 4) | Shared Java interfaces in branch `p4-03-interface-contracts`<br>Submitted in PR #128 (Draft) | P4-04 CI pipeline (PR #60 merged) is infrastructure, not contract implementation | Contract Drafted (PR #128 Draft); Not yet merged to `main`; **Formal Sign-Off Pending** | Resolve DTO naming (`reason`), `IFileStore` signatures, and clock usage before merging PR #128 |

#### D-09.5 — Claim and Cancellation-Refund Rule (INS-F-020, SAD §4.1.5, p3-contract §3.5)

**Status: Confirmed Business Decision (Pending Contract Update & Implementation Verification).**

- **Agreed Business Rule:** For policy cancellation-refund eligibility under INS-F-020, a claim counts as paid **only when its status is `SETTLED`**, not merely `APPROVED`. If a claim is in `APPROVED` status but has not completed settlement payout, it does not disqualify the policyholder from cancellation refund calculation.
- **Traceability and Lifecycle Status:**
  - **Business Decision:** Confirmed by team agreement on 17 October 2026.
  - **Contract Update Required:** Raghunandan (Person 4) must update `docs/contracts/p4-contract.md` (specifically `IClaimService.hasPaidClaim` specification lines 285–287 and Decision Table 8 item D-09.5) to change the condition from `APPROVED or SETTLED` to strictly `SETTLED`.
  - **Implementation & QA Verification:** Once the contract and backend services (`ClaimService.java` / `CancellationService.java`) are updated, Person 2 and Person 1 will execute automated test cases verifying that `hasPaidClaim` returns `false` for `APPROVED` claims and `true` only for `SETTLED` claims.
  - *Distinction:* The code does not yet implement this behavior; this entry documents the agreed business requirement.

---

## TEAM-03 Sign-Off Checklist & Pre-Sprint-1 Architecture Agenda

Before beginning Sprint 1 on Monday, 19 October 2026, the team must hold a 15-minute architecture sign-off meeting to address the following outstanding agenda:

- [x] **Item 1: D-03 Database Migration Sign-Off**
  - **Outcome:** Confirmed on 17 Oct 2026. MySQL 8 with Hibernate `ddl-auto: update` (dev) and `create-drop` (test) for Sprint 1; Flyway versioned migrations planned for Sprint 2.
- [x] **Item 2: PR #115 (P1-05) Closure**
  - **Outcome:** Completed. Samarth (Person 3) approved re-review at commit `57e289f` / `9dabe20`; CI checks passed; merged into `main` at `9acef3d`.
- [ ] **Item 3: PR #124 (P2-05) Remediation**
  - **Action:** Sharana (Person 2) addresses the critical Spring constructor injection defect in `FieldEncryptionService` preventing production fail-secure key validation.
- [ ] **Item 4: API Contracts Final Approval (D-09)**
  - **Action:** Joint sign-off across Person 1, Person 2, Person 3, and Person 4 on unified endpoint signatures, D-09.5 `SETTLED` rule, and data fixtures.

---

## Decision history

| Date | Change |
|---|---|
| 15 October 2026 | Standardized statuses to Confirmed / Proposed / Provisional; named owners and due/review points. |
| 17 October 2026 | Updated D-01 (confirmed via live GitHub branch protection audit), D-02 (confirmed via package.json & CI build evidence), D-04 (confirmed via role baseline audit), and D-05 (provisional accounting across 102 items: 31 positive estimates totalling 118 pts, 2 explicit zeros, 69 unestimated). Expanded D-03 with comparative analysis, migration impact assessment, and Sprint 1/2 recommendation (undecided pending formal sign-off). Clarified D-09 contract documents and PR references (P1-03 PR #53, P2-01 PR #54, P3-01 PR #46, P4-03 PR #128) distinguishing drafted contracts from downstream implementations, retaining D-09 as pending formal joint sign-off. Noted PR #115 closure (approved by Samarth, merged into main at `9acef3d`) in D-01, D-09, and agenda checklist. Added TEAM-03 pre-Sprint 1 sign-off checklist and agenda. |
| 17 October 2026 | Confirmed D-03 (database migration strategy: MySQL 8 with Hibernate ddl-auto: update in dev and create-drop in test for Sprint 1; Flyway versioned migrations planned for Sprint 2; update/create-drop strictly non-production). Confirmed D-09.5 business rule (cancellation-refund eligibility: a claim counts as paid only when SETTLED, not merely APPROVED; contract update and implementation verification pending). Retained D-09 as Proposed pending technical contract alignment across PR #128. |
