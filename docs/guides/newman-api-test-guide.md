# Newman API Test Automation Guide

**Work Item:** P2-04 (Day 4 Deliverable)  
**Author / Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Target Audience:** Person 1 (Quotes/Underwriting), Person 3 (Claims/Audit), Person 4 (Backend Skeleton/CI)  
**Collection Path:** `tests/postman/insurance-workflow-api.postman_collection.json`  
**Environment Path:** `tests/postman/insurance-workflow-environment.postman_environment.json`

---

## 1. Overview & Architectural Principles

The API test framework provides an automated, runnable test collection executed using [Newman](https://github.com/postmanlabs/newman), Postman's headless CLI runner.

### Key Rules (P2-04 Acceptance Criteria):
1. **Zero Committed Secrets:** The Postman environment uses placeholders (`<customer-bearer-token-placeholder>`). Real credentials, passwords, personal Aadhaar/PAN numbers, and private tokens MUST NEVER be committed to Git.
2. **Modular Folder Organization:**
   - `01 - Authentication, KYC and Plans (Contract 02)` — Owned by Person 2
   - `02 - Quoting and Underwriting (Contract 01)` — Owned by Person 1
   - `03 - Claims and Audit (Contract 01 / SAD)` — Owned by Person 3 & shared modules
3. **Honest Endpoint Dependency Tracking:**
   - **Real Endpoint:** `GET /actuator/health` is fully implemented in the backend skeleton and passes in live runs.
   - **Contract Specification Requests:** All other requests (`/auth/register`, `/auth/login`, `/kyc`, `/quotes/*`, etc.) are structured according to the agreed API contracts (01 and 02). They serve as executable specifications ready to validate each endpoint as soon as the respective backend feature is merged.
   - Do NOT falsely mark endpoints as implemented before their backend code is merged.

---

## 2. Quick Execution from Clean Checkout

### Prerequisites
- Node.js 18+ and npm installed.
- (Optional for live runs) Backend Spring Boot server running on `http://localhost:8080`.

### Option A: Via NPM Scripts in `tests/`
```bash
# 1. Install newman locally in tests directory
cd tests
npm install

# 2. Run the authentication and health check folder
npm run test:api

# 3. Run all folders in the collection
npm run test:api:all
```

### Option B: Running Newman via NPX directly
```bash
npx newman run tests/postman/insurance-workflow-api.postman_collection.json \
  -e tests/postman/insurance-workflow-environment.postman_environment.json \
  --folder "01 - Authentication, KYC and Plans (Contract 02)"
```

### Option C: Validating the Live Backend Health Check
When the Spring Boot backend is running locally (`cd backend && ./mvnw spring-boot:run`):
```bash
npx newman run tests/postman/insurance-workflow-api.postman_collection.json \
  -e tests/postman/insurance-workflow-environment.postman_environment.json \
  --folder "01 - Authentication, KYC and Plans (Contract 02)" \
  --bail
```

---

## 3. How Teammates Add Requests for Their Endpoints

Each feature owner (Person 1, Person 3, Person 4) can easily expand the collection:

### Step 1: Open or Import in Postman
- Open the Postman desktop application.
- Click **Import** and select:
  1. `tests/postman/insurance-workflow-api.postman_collection.json`
  2. `tests/postman/insurance-workflow-environment.postman_environment.json`

### Step 2: Navigate to Your Module Folder
- **Person 1:** Add or update requests under `02 - Quoting and Underwriting (Contract 01)`.
- **Person 2:** Maintain requests under `01 - Authentication, KYC and Plans (Contract 02)`.
- **Person 3:** Add requests under `03 - Claims and Audit (Contract 01 / SAD)`.

### Step 3: Configure URL, Headers, and Correlation ID
Always use environment variables:
- URL: `{{baseUrl}}{{apiPrefix}}/your-endpoint-path`
- Header `X-Correlation-Id`: `{{correlationId}}`
- Header `Authorization`: `Bearer {{customerToken}}` (or `{{underwriterToken}}`, `{{insurerAdminToken}}`)
- Header `Content-Type`: `application/json`

### Step 4: Write Postman Test Assertions
In the **Tests** tab, add assertions conforming to SAD §4.4:
```javascript
pm.test("Status code is 200 OK", function () {
    pm.response.to.have.status(200);
});

pm.test("Response body matches schema", function () {
    var data = pm.response.json();
    pm.expect(data).to.have.property("quoteId");
    pm.expect(data.premium).to.be.above(0);
});

pm.test("Correlation ID is echoed in response headers", function () {
    pm.response.to.have.header("X-Correlation-Id");
});
```

### Step 5: Export and Commit
1. Export the updated collection (choose Collection v2.1 format) over `tests/postman/insurance-workflow-api.postman_collection.json`.
2. Ensure no personal credentials or live tokens are saved in the environment file.
3. Commit with a message referencing your requirement ID (e.g. `test: add Newman tests for motor quotes (INS-F-005)`).
