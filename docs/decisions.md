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
| D-09 | API-contract sign-off | Contract documents drafted and merged for P1-03 (PR #53), P2-01 (PR #54), and P3-01 (PR #46); P4-03 interface contracts drafted in PR #128. Formal joint sign-off across all four members completed (D-09.5 business rule confirmed: claims paid only when SETTLED). | Confirmed | Person 1: Thijil; Person 2: Sharana; Person 3: Samarth; Person 4: Raghunandan | Pre-Sprint 1 sign-off meeting |
| D-10 | Sprint 2 planning, velocity calibration & deferrals (TEAM-08) | Real Sprint 1 delivery: 60 of 83 SP (18 SP carry-over). Calibrated Sprint 2 velocity to 60–65 SP. Dropped deferrable items from bottom of deferral order (INS-NF-007, INS-F-025, INS-F-023, INS-F-021) marked "D" in RTM. Total planned commitment: 62 SP. | Confirmed | All four members; Thijil (Person 1) records | Confirmed 26 Oct 2026 in Sprint 2 Planning |
| D-11 | Claims module ownership split (Person 1 & Person 4) | Agreed split: Person 4 (Raghunandan) retains claims backend core (verify, decide, payout REST endpoints & entities). Person 1 (Thijil) owns Claims Officer UI screens (P1-12), 15-day stale auto-closure scheduled job (P1-19), and STP independent verification (TC-Clm-01 to TC-Clm-04). | Confirmed | Thijil (Person 1) & Raghunandan (Person 4) | Confirmed 26 Oct 2026 |

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
- **Process Deviation Log (23 Oct 2026):** PR #147 merged on 23 Oct with admin override and no independent review; reviewed retrospectively by Sharana on 23 Oct 2026. Correcting PR submitted for normal peer review and approval before merging. Strict enforcement reinstated: no `--admin` merges, no changes to branch protection, and no self-approvals.

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

**Status: Confirmed.** (All four contracts signed off and merged to `main`)

All four cross-module API contracts and shared Java interfaces have undergone cross-team review, integration validation, and are merged into `main`:

| Contract | Owner | Contract Document & PR | Downstream Implementation | Contract Lifecycle State | Next Action for Sign-off |
|---|---|---|---|---|---|
| **P1-03** — Quotes, underwriting, claim decisions, audit | Thijil (Person 1) | [`api-contract/01-quotes-underwriting-claims-audit.md`](api-contract/01-quotes-underwriting-claims-audit.md)<br>Issue #52, Merged in PR #53 | P1-05 quote/audit stubs (merged); P1-17 plan rules (merged); P1-06 motor quote (merged); P1-18 health quote (merged); P1-07 underwriting (merged) | **Confirmed & Merged to `main`** | Signed off |
| **P2-01** — Authentication, KYC, CurrentUser, plans | Sharana (Person 2) | [`api-contract/02-auth-kyc-plans.md`](api-contract/02-auth-kyc-plans.md)<br>Merged in PR #54 | P2-02 CurrentUser stub (merged); P2-05 backend auth (merged); P2-06 plan management (merged via PR #146) | **Confirmed & Merged to `main`** | Signed off |
| **P3-01** — Applications, issuance, policies, renewal, cancellation | Samarth (Person 3) | [`contracts/p3-contract.md`](contracts/p3-contract.md)<br>Merged in PR #46 | P3-02 data models (merged); P3-07 calculators (merged); P3-04a applications (merged via PR #132) | **Confirmed & Merged to `main`** | Signed off |
| **P4-03** — Payments, notifications, claims, file store, test clock | Raghunandan (Person 4) | Shared Java interfaces in `docs/contracts/p4-contract.md`<br>Merged via PR #128, PR #142, PR #143, PR #145 | P4-05 test infra (merged); P4-06 stubs & filestore (merged); P4-17 payment (merged); P4-09 claims core (merged) | **Confirmed & Merged to `main`** | Signed off |

#### D-09.5 — Claim and Cancellation-Refund Rule (INS-F-020, SAD §4.1.5, p3-contract §3.5)

**Status: Confirmed.**

- **Agreed Business Rule:** For policy cancellation-refund eligibility under INS-F-020, a claim counts as paid **only when its status is `SETTLED`**, not merely `APPROVED`. If a claim is in `APPROVED` status but has not completed settlement payout, it does not disqualify the policyholder from cancellation refund calculation.
- **Traceability and Lifecycle Status:**
  - **Business Decision:** Confirmed by team agreement on 17 October 2026.
  - **Implementation & Verification:** Verified in `ClaimService.java:111` (`claimRepository.existsByPolicyIdAndStatus(policyId, ClaimStatus.SETTLED)`) and `RefundCalculator.java`. Verified by automated test suites.

---

## TEAM-03 Sign-Off Checklist & Pre-Sprint-1 Architecture Agenda
| Contract | Owner | Topics to confirm | Outcome |
|---|---|---|---|
| P1-03 — Quotes, underwriting, claim decisions and audit | Thijil (Person 1) | Quote/underwriting endpoint request and response shape; rule data format and fixtures; manual decision signature and reason; audit-log query/interface and requirement mapping | **Signed off & Confirmed** |
| P2-01 — Authentication, KYC, CurrentUser and plans | Sharana (Person 2) | Register/login and lockout/session behavior; role names; KYC upload limits and status; plans browse/compare/admin routes; `CurrentUser {userId, role, insurerId}` source; dev/test-only stub safeguards; staff-to-insurer association | **Signed off & Confirmed** |
| P3-01 — Applications, issuance, policies, renewal and cancellation | Samarth (Person 3) | Add `vehicleRegNo` and confirm `existsActivePolicyForVehicle`; whether policy becomes `Cancelled` at approval or refund; whether zero-refund cases skip gateway calls; policy number format; PDF library; whether `/payments` returns `policyNo` directly or the UI polls policies; server-stored renewal quote semantics | **Signed off & Confirmed** |
| P4-03 — Payments, notifications, claims, file store and test clock | Raghunandan (Person 4) | Draft v0.2 in `docs/contracts/p4-contract.md`: server-generated idempotency key (`applicationId-attemptNo`); 16 notification events (SAD §4.1.7); claim states aligned with SAD §4.1.3/SRS §4.7.1; D-09.5 agreed: `hasPaidClaim` checks `SETTLED` claims only; 15-day stale claim closure; file store `put/get/delete` with HTTP 413/415; clock injection with zone preservation and `/api/v1/test/clock` endpoint | **Signed off & Confirmed** |

Before beginning Sprint 1 on Monday, 19 October 2026, the team held architecture sign-off meetings addressing the following agenda:

- [x] **Item 1: D-03 Database Migration Sign-Off**
  - **Outcome:** Confirmed on 17 Oct 2026. MySQL 8 with Hibernate `ddl-auto: update` (dev) and `create-drop` (test) for Sprint 1; Flyway versioned migrations planned for Sprint 2.
- [x] **Item 2: PR #115 (P1-05) Closure**
  - **Outcome:** Completed. Samarth (Person 3) approved re-review at commit `57e289f` / `9dabe20`; CI checks passed; merged into `main` at `9acef3d`.
- [x] **Item 3: PR #124 (P2-05) Remediation**
  - **Outcome:** Completed. Sharana (Person 2) addressed constructor injection and merged to `main`.
- [x] **Item 4: API Contracts Final Approval (D-09)**
  - **Outcome:** Confirmed on 23 Oct 2026. All four API contracts (P1-03, P2-01, P3-01, P4-03) are merged and active on `main`.
#### D-09.5 — `hasPaidClaim()` Semantics for Cancellation Refunds

- **Status: Confirmed.**
- **Agreed Rule:** `IClaimService.hasPaidClaim(policyId)` returns `true` only when a claim associated with the policy has status `SETTLED` (`Settled`).
- **Rationale:** An `Approved` or `Payment Processing` claim that has not completed payout must not count as paid for the cancellation refund oracle. If true, the cancellation refund is Rs 0 (SAD §4.1.5, P3 Contract §3.5).
- **Other D-09 choices (D-09.1 to D-09.4, D-09.6 to D-09.9):** Confirmed in respective merged service implementations. Virus scanning is marked **Proposed**.

#### D-09.10 — Day-16 Cancellation Refund Rounding Oracle Discrepancy (TC-Can-02)

- **Status: Proposed (Pending Formal Team / Guide Clarification).**
- **Discrepancy Analysis:**
  - In TC-Can-02, for a 365-day policy with gross premium ₹12,000 cancelled on Day 16 (unused days = 349) with a 5% fee (₹600):
    $$\text{Unexpired Premium} = 12000 \times \frac{349}{365} = 11,473.9726\dots$$
    $$\text{Net Refund} = 11,473.9726\dots - 600 = 10,873.9726\dots$$
  - Under SRS §4.6.2 and SAD §4.1.6, all monetary refund calculations must round down (floor) to the nearest whole rupee (`RoundingMode.DOWN`). Applying floor rounding yields **₹10,873**.
  - The submitted STP document specifies an expected refund of **₹10,874** due to flawed premature intermediate rounding: $12000 - (12000 \times 16 / 365) = 12000 - 526.02 = 11473.97 \approx 11474$, minus $600 = 10874$.
    - The backend implementation in `RefundCalculator.java` preserves mathematically exact integer-numerator arithmetic with floor rounding yielding ₹10,873, strictly adhering to the SRS/SAD round-down mandate.
    - This 1-rupee difference is recorded here as an identified oracle discrepancy pending formal team/guide clarification and sign-off during Sprint 2 test execution.

### D-10 — Sprint 2 Planning, Velocity Calibration, and Deferral Decision (TEAM-08)

**Status: Confirmed.** (Agreed by all 4 team members on 26 October 2026)

- **Context & Review of Sprint 1 Real Results:**
  - The team conducted the formal Sprint 2 planning and deferral review on 26 October 2026.
  - Review of Sprint 1: The team planned 83 story points of work and successfully delivered and merged **60 story points** into `main` (~72% delivery rate).
  - An honest accounting identified **18 story points of carry-over work** remaining from Sprint 1:
    - `P3-05` / `P3-06` Policy issuance & review screens carry-over (~7 SP)
    - `P2-07` / `P2-08` KYC document upload API and UI (~6 SP)
    - `P4-08` Live notification dispatcher integration (~3 SP)
    - `P2-09` Automated Newman API collection in CI (~2 SP)
- **Calibrated Sprint 2 Velocity:**
  - In light of real Sprint 1 velocity (60 SP), the provisional 100 SP heuristic (D-05) is superseded by an empirically calibrated Sprint 2 capacity of **60–65 story points**.
  - Without scope adjustments, the total demand (18 SP carry-over + 74 SP planned Sprint 2 backlog = 92 SP) would create severe schedule risk and overcommit the team.
- **Deferral Strategy & Dropped Scope (Bottom of Deferral Order):**
  - Following course syllabus guidelines and the team's established `tier-b` convention (D-08), the team decided to drop deferrable medium-priority items starting from the bottom of the requirement order.
  - The following four requirements are formally **Deferred** and marked **"D"** in the Requirements Traceability Matrix (RTM):
    1. **`INS-NF-007` (Audit Record Retention / Archival Job — P1-13, 3 SP, `tier-b`):**
       - *Rationale:* The audit log table (`audit_logs`) is append-only and immutable in MySQL/H2 with user attribution. Long-term automated background archival after 5 years is a post-deployment operational concern that does not affect functional workflow demonstration.
    2. **`INS-F-025` (Generate Admin Reports — P3-09, 3 SP, `tier-b`):**
       - *Rationale:* Core operational reporting is directly inspectable via live database queries and role-specific screens (`/customer/policies`, `/underwriter`, `/claims`). Automated scheduled batch report generation is non-critical for end-to-end user journeys.
    3. **`INS-F-023` (Duplicate Claim Detection Engine — P1-20, 3 SP, `tier-b`):**
       - *Rationale:* Claims Officers manually review incident date, vehicle registration, and claimant identity during assessment. Algorithmic 48-hour collision detection is deferred as an advanced fraud optimization.
    4. **`INS-F-021` (Role-Specific Analytical Dashboards — 3 SP, `tier-b`):**
       - *Rationale:* Role portal shells (`PortalLayout.jsx`) and dedicated workspace views (`/customer`, `/underwriter`, `/claims`) provide all operational workflow triggers. Aggregate business intelligence KPI dashboards are non-blocking.
  - **Net Sprint 2 Plan:** 18 SP carry-over + 44 SP core Sprint 2 scope = **62 story points**, bringing the sprint backlog into exact alignment with demonstrated velocity.

### D-11 — Claims Component Ownership Split (Person 1 & Person 4)

**Status: Confirmed.** (Agreed between Person 1 and Person 4 on 26 October 2026)

- **Context:** Person 4 (Raghunandan) implemented the core claims backend (C10) during Sprint 1 (`ClaimService`, `ClaimController`, verification, assessment decisioning, and payout integration; evidenced in PR #145 and PR #152).
- **Agreed Responsibilities:**
  - **Person 4 (Raghunandan Kotary — Developer / Environment Owner):**
    - Retains backend core ownership: maintains `ClaimService`, `ClaimController`, REST endpoints (`POST /claims`, `POST /claims/{id}/verify`, `POST /claims/{id}/assess`, `POST /claims/{id}/settle`), database entities (`Claim`, `ClaimDocument`, `ClaimHistory`), payment payout adapter, and developer test harness.
  - **Person 1 (Thijil Thejaswi M.N. — QA Lead & Underwriting Core):**
    - Owns the frontend Claims Officer desk screens (`/claims/desk`, queue of submitted claims, incident document viewer, decision/settlement dialogs; `P1-12`).
    - Implements the automated 15-day stale claim closure scheduled cron job (`P1-19`, SRS §4.7.1, TC-Clm-06).
    - Coordinates and leads formal independent QA verification of claims workflows against the STP (`TC-Clm-01` through `TC-Clm-04`, `TC-Clm-06`).

## Review and change control

---

## Decision history

| Date | Change |
|---|---|
| 15 October 2026 | Standardized statuses to Confirmed / Proposed / Provisional; named owners and due/review points. |
| 17 October 2026 | Updated D-01 (confirmed via live GitHub branch protection audit), D-02 (confirmed via package.json & CI build evidence), D-04 (confirmed via role baseline audit), and D-05 (provisional accounting across 102 items: 31 positive estimates totalling 118 pts, 2 explicit zeros, 69 unestimated). Expanded D-03 with comparative analysis, migration impact assessment, and Sprint 1/2 recommendation (undecided pending formal sign-off). Clarified D-09 contract documents and PR references (P1-03 PR #53, P2-01 PR #54, P3-01 PR #46, P4-03 PR #128) distinguishing drafted contracts from downstream implementations, retaining D-09 as pending formal joint sign-off. Noted PR #115 closure (approved by Samarth, merged into main at `9acef3d`) in D-01, D-09, and agenda checklist. Added TEAM-03 pre-Sprint 1 sign-off checklist and agenda. |
| 17 October 2026 | Confirmed D-03 (database migration strategy: MySQL 8 with Hibernate ddl-auto: update in dev and create-drop in test for Sprint 1; Flyway versioned migrations planned for Sprint 2; update/create-drop strictly non-production). Confirmed D-09.5 business rule (cancellation-refund eligibility: a claim counts as paid only when SETTLED, not merely APPROVED; contract update and implementation verification pending). Retained D-09 as Proposed pending technical contract alignment across PR #128. |
| 15 October 2026 | Revised after PR review: standardized statuses to Confirmed / Proposed / Provisional; named owners and due/review points; expanded repository-rule evidence, build-tool and migration-tool verification, capacity arithmetic, GitHub tracking source of truth, `tier-b` convention and outstanding contract sign-off questions. |
| 17 October 2026 | Person 4 prepared draft shared contract specification in `docs/contracts/p4-contract.md` covering IPaymentService, INotify, IClaimService, IFileStore, and injectable test Clock (Issue #127); decision options D-09.1 through D-09.9 submitted for team review. |
| 18 October 2026 | Person 4 updated P4 contract to v0.2 following PR #128 review: aligned claim states with SAD §4.1.3 (Document Verification, Assessment, Payment Processing), 15-day stale claim closure (SRS §4.7.1), SAD 16-event notification catalogue, HTTP 413/415 file upload errors, P1 claim-decision DTO alignment, settled-only `hasPaidClaim` semantics (D-09.5), clock zone preservation, and SAD Table 3.4b method naming. |
| 23 October 2026 | Process deviation noted: PR #147 merged on 23 Oct with admin override and no independent review; reviewed retrospectively by Sharana on 23 Oct 2026. Correcting PR submitted to restore independent testing and review policy compliance. Strict enforcement reinstated: no admin overrides and no protection bypass. |
| 26 October 2026 | Recorded D-10: Sprint 2 Planning, Velocity Calibration, and Deferral Decision (TEAM-08). Sprint 1 delivered 60/83 SP; 18 SP carry-over. Calibrated Sprint 2 capacity to 60–65 SP. Dropped deferrable items from bottom of deferral order (INS-NF-007, INS-F-025, INS-F-023, INS-F-021) marked "D" in RTM. Total planned commitment: 62 SP. Recorded D-11: Claims component ownership split between Person 1 (screens P1-12, auto-closure P1-19, STP verification) and Person 4 (backend core, APIs, entities, payout adapter). |
