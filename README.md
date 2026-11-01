# Insurance Workflow Automation System

**Software Engineering Mini Project — Team 11**

## 1. Project Overview

The Insurance Workflow Automation System is designed to streamline insurance-related workflows for two-wheelers, four-wheelers, and personal health insurance.

The system aims to organize insurance operations through a centralized application supporting insurance plans, premium quotations, applications, policy management, claims, and administrative functions.

This repository contains the project documentation and will contain the implementation, tests, and supporting artifacts as development progresses.

## 2. Objectives

- Support insurance workflows for two-wheelers, four-wheelers, and personal health insurance.
- Provide role-based access to system functionality.
- Support insurance plan management and premium calculation.
- Facilitate insurance application processing and policy document generation.
- Support policy renewal, cancellation, and claims processing.
- Maintain appropriate security controls, audit records, and test evidence.
- Verify functional and non-functional requirements through planned testing.

## 3. Team Members

| Person | Team Member | Responsibility |
|---|---|---|
| Person 1 | Thijil Thejaswi M.N. | QA Lead, requirements traceability, test coordination, defect triage, and assigned development tasks |
| Person 2 | Sharana | Functional Test Engineer, functional and acceptance testing, and assigned development tasks |
| Person 3 | Samarth | Non-functional and Security Test Engineer, performance and security testing, and assigned development tasks |
| Person 4 | Raghunandan | Developer and Environment Owner, environment setup, build configuration, CI, and assigned development tasks |

Testing responsibilities follow the submitted Software Test Plan (STP).

## 4. Technology Stack

The submitted Software Architecture Document specifies the following technologies:

| Component | Technology |
|---|---|
| Frontend | React 18 |
| UI Framework | Bootstrap |
| Backend | Java 17, Spring Boot 3 |
| Backend Framework Components | Spring MVC, Spring Security, Spring Data JPA |
| Database | MySQL 8 |
| Document Generation | OpenPDF |
| Authentication | JWT and server-side session |

## 5. Main Functional Areas

The planned system includes the following functional areas:

- **User Registration and Authentication:** User registration, login, KYC, and role-based access.
- **Insurance Plan Management:** Plan configuration, premium rules, coverage, and optional add-ons.
- **Premium Quotation:** Motor and health insurance quotations.
- **Application Processing:** Insurance application submission and rule-based underwriting.
- **Policy Management:** Policy issuance, policy documents, renewal, and cancellation.
- **Claims Management:** Claim submission, review, assessment, and decision-making.
- **Administration:** Role-specific dashboards, search, filtering, and administrative reports.
- **Security and Auditing:** Input validation, access controls, secure file uploads, and audit logging.

These are planned functional areas; individual features will be marked complete only after implementation and verification.

## 6. Project Documentation

The repository includes the following submitted documents:

| Document | Purpose |
|---|---|
| [Software Requirements Specification (SRS)](SRS.docx) | Functional, non-functional, and security requirements |
| [Software Architecture Document (SAD)](SAD.docx) | System architecture and technology choices |
| [Software Test Plan (STP)](STP.docx) | Test cases, test responsibilities, and acceptance testing |

See [CONTRIBUTING.md](CONTRIBUTING.md) for development and collaboration guidelines.

## 7. Development Workflow

The team follows a branch-based GitHub workflow.

1. Select an assigned issue or requirement.
2. Create a descriptive feature or fix branch.
3. Implement the change and write relevant tests.
4. Commit changes using clear commit messages.
5. Push the branch and create a pull request targeting `main`.
6. Request review from another team member.
7. Address review comments and merge after approval.

The `main` branch should remain stable. Avoid committing incomplete development work directly to it.

## 8. Testing Strategy

Testing is guided by the submitted STP and includes:

- Functional testing
- UI and acceptance testing
- Unit and integration testing
- Security testing
- Performance and availability testing
- Compatibility and backup-related verification

Test results must reflect tests actually executed. Unavailable environments and unverified requirements must be reported transparently.

## 9. Getting Started

### Prerequisites

The planned implementation uses:

- Java Development Kit (JDK) 17
- Node.js and npm
- MySQL 8
- Git

### Quick Start & Verified Commands

#### Backend (Spring Boot 3)
```bash
cd backend
./mvnw test            # Run 610 automated tests (610 passed)
./mvnw spring-boot:run # Start on http://localhost:8080/api/v1/ (in-memory H2 MySQL profile)
```

#### Frontend (React 18 + Vite)
```bash
cd frontend
npm install
npm test -- --run      # Run 79 component tests across 16 files via Vitest (79 passed)
npm run dev            # Start development server on http://localhost:5173/
```

#### Containerized Test Environment (Docker Compose + TLS 1.2+ Nginx)
See `docs/test-environment-guide.md` for detailed topology and TLS setup:
```powershell
.\scripts\generate-test-certs.ps1   # Generate dev TLS certs
docker compose up -d --build         # Launch MySQL, dual backends, frontend, Nginx
.\scripts\verify-tls-environment.ps1 -TargetHost 'localhost' # Run TLS verification
docker compose down                  # Graceful teardown
```

## 10. Project Management

The team uses GitHub Issues and GitHub Projects to track requirements, assignments, priorities, sprint allocation, and progress.

Requirements should be traceable to their corresponding issues, implementation changes, and test evidence wherever applicable.

## 11. Contribution Guidelines

All contributors should follow [CONTRIBUTING.md](CONTRIBUTING.md), including the branching strategy, pull request process, code review expectations, testing requirements, and Definition of Done.

## 12. Project Status

**Current phase:** Project documentation and development preparation.

Implementation and testing status will be updated as work is completed and verified.

---

**Team 11 — Insurance Workflow Automation System**
