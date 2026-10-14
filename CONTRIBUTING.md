# Contributing Guidelines

## 1. Project Overview

This repository contains the Software Engineering mini-project **Insurance Workflow Automation System**, developed by Team 11.

The system covers insurance workflows for two-wheelers, four-wheelers, and personal health insurance.

## 2. Team Members and Responsibilities

| Person | Team Member | Primary Responsibility |
|---|---|---|
| Person 1 | Thijil Thejaswi M.N. | QA Lead, requirements traceability, test coordination, defect triage, and assigned implementation tasks |
| Person 2 | Sharana | Functional Test Engineer, functional testing, UI testing, acceptance testing, and assigned implementation tasks |
| Person 3 | Samarth | Non-functional and Security Test Engineer, performance testing, security testing, and assigned implementation tasks |
| Person 4 | Raghunandan | Developer and Environment Owner, build setup, test environment, CI configuration, and assigned implementation tasks |

Testing responsibilities must remain consistent with the submitted Software Test Plan (STP). Team members should independently test their own features through peer review or testing by another member.

## 3. Branching Strategy

- Keep the `main` branch stable.
- Create a separate branch for each feature, fix, or documentation change.
- Use descriptive branch names.

Examples:

- `feature/INS-F-005-motor-quote`
- `feature/INS-F-001-login`
- `test/functional-tests`
- `docs/contributing-guidelines`
- `fix/input-validation`

Do not commit incomplete or untested work directly to `main`.

## 4. Commit Message Convention

Write clear commit messages that describe the change.

Examples:

- `feat: implement motor insurance quote`
- `feat: add user registration and login`
- `test: add functional tests for policy issuance`
- `fix: validate insurance application inputs`
- `docs: update project setup instructions`
- `ci: configure automated build checks`

Keep each commit focused on a logical change.

## 5. Pull Request Workflow

Every assigned team member must raise a pull request for their development work.

1. Update the local branch with the latest changes from `main`.
2. Create a feature branch.
3. Implement the assigned requirement.
4. Run the relevant tests and verify the build.
5. Commit and push the changes to GitHub.
6. Open a pull request targeting `main`.
7. Include the relevant requirement ID in the pull request title or description.
8. Explain the implementation, testing performed, and any known limitations.
9. Request review from another team member.
10. Address review comments and merge only after approval.

The repository owner is responsible for reviewing and merging pull requests unless the team agrees on a documented alternative.

## 6. Pull Request Template

Each pull request should include:

- **Requirement ID(s):**
- **Summary of changes:**
- **Implementation details:**
- **Tests performed:**
- **Build status:**
- **Screenshots or video evidence, if applicable:**
- **Known issues or limitations:**
- **Reviewer:**

## 7. Definition of Done

A task is considered complete when:

- The implementation satisfies its requirement and acceptance criteria.
- Relevant unit and integration tests have been run.
- The application builds successfully.
- Changes have been reviewed by another team member.
- The pull request has been approved and merged.
- Relevant documentation and test evidence have been updated.
- No known critical defect remains unresolved for the task.

A task must not be marked complete merely because its code has been written.

## 8. Testing Guidelines

- Developers must write unit tests for their own implementation.
- Functional testing must cover valid inputs, invalid inputs, and relevant boundary cases.
- Security testing must cover applicable authentication, authorization, input validation, and file-upload controls.
- Non-functional requirements must be assessed using the submitted STP.
- Record actual test results and defects; do not report unexecuted tests as passed.
- Link test cases to the corresponding functional or non-functional requirement IDs.

## 9. Technology Stack

The submitted Software Architecture Document (SAD) specifies the following stack:

- **Frontend:** React 18 and Bootstrap
- **Backend:** Java 17 and Spring Boot 3
- **Backend components:** Spring MVC, Spring Security, Spring Data JPA, and scheduled jobs
- **Database:** MySQL 8
- **Document generation:** OpenPDF
- **Authentication:** JWT and server-side session

Implementation and dependency changes should be discussed with the team and reflected in the relevant documentation.

## 10. Sprint and Delivery Rules

- Follow the sprint dates and deliverables defined in the project brief.
- Keep issue assignments and task status updated in GitHub Projects.
- Link pull requests to their relevant issues and requirements wherever possible.
- Sprint 1 must include the required working-product demonstration video.
- Sprint 2 must include the required demonstration video and completed documentation.
- Record incomplete functionality and deviations honestly.
- Respect the development freeze and submission deadlines.

## 11. Security and Repository Hygiene

- Never commit passwords, API keys, access tokens, private keys, or real sensitive customer information.
- Use synthetic or test data for development and demonstrations.
- Keep configuration secrets outside source control.
- Review dependencies and input handling before merging.
- Do not commit generated files, temporary files, or local environment files unless required by the project.

## 12. Communication

- Inform the team about blockers as early as possible.
- Discuss changes that affect shared interfaces, database schemas, dependencies, or another member's work before implementation.
- Resolve merge conflicts carefully and test the resulting code.
- Ask for clarification rather than silently changing a requirement.

---

**Team 11 — Insurance Workflow Automation System**

All contributors are expected to follow these guidelines to maintain code quality, traceability, collaboration, and reliable project delivery.
