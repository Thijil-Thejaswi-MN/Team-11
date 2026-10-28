# Test Automation Framework — Insurance Workflow Automation System

**Work Item:** P2-04 (Day 4 Deliverable)  
**Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Tools:** Postman / Newman (API Functional Tests), Python / Selenium / Pytest (Frontend UI Smoke Tests)

---

## 1. Directory Structure

```text
tests/
├── postman/
│   ├── insurance-workflow-api.postman_collection.json      # Modular collection across 3 modules
│   └── insurance-workflow-environment.postman_environment.json # Non-secret local dev & test environment
├── selenium/
│   ├── conftest.py             # Headless browser fixture (Chrome/Edge), zero arbitrary sleeps
│   ├── test_home_smoke.py      # Home page & navigation smoke test with explicit waits
│   └── requirements.txt        # Python test dependencies (selenium, pytest, webdriver-manager)
└── package.json                # NPM helper scripts for clean checkout runs
```

---

## 2. API Tests (Newman)

### Quick Run via NPM
From repository root or `tests/`:
```bash
cd tests
npm install
npm run test:api
```

### Running Newman directly
```bash
npx newman run tests/postman/insurance-workflow-api.postman_collection.json \
  -e tests/postman/insurance-workflow-environment.postman_environment.json \
  --folder "01 - Authentication, KYC and Plans (Contract 02)"
```

See the full [Newman API Test Guide](../docs/guides/newman-api-test-guide.md) for details on adding endpoints and contract dependencies.

---

## 3. Frontend Smoke Tests (Selenium)

### Prerequisites
- Python 3.10+
- Google Chrome, Microsoft Edge, or Mozilla Firefox installed
- `pip install -r tests/selenium/requirements.txt`

### Quick Run
1. Start the React frontend dev server:
   ```bash
   cd frontend
   npm run dev
   ```
   (Starts application on `http://localhost:5173`)

2. In another terminal, run pytest:
   ```bash
   pytest tests/selenium -v
   ```

### Cross-Browser Testing (INS-NF-006 / TC-Compat-01)
Run the smoke suite against different browsers using the `--browser` flag:
- **Google Chrome (Default):**
  ```bash
  pytest tests/selenium -v --browser chrome
  ```
- **Microsoft Edge:**
  ```bash
  pytest tests/selenium -v --browser edge
  ```
- **Mozilla Firefox:**
  ```bash
  pytest tests/selenium -v --browser firefox
  ```

### Headed Mode (Display Browser UI)
```bash
pytest tests/selenium -v --headed
```

See the full [Selenium Smoke Test Guide](../docs/guides/selenium-smoke-test-guide.md).
