# Selenium Frontend Smoke Test Guide

**Work Item:** P2-04 (Day 4 Deliverable)  
**Author / Owner:** Person 2 — Sharana (Functional Test Engineer)  
**Target Audience:** Person 1, Person 3, Person 4  
**Project Path:** `tests/selenium/`  
**Test File:** `tests/selenium/test_home_smoke.py`

---

## 1. Overview & Framework Design

The Selenium smoke-test suite automates end-to-end verification of the React frontend application shell.

### Engineering Best Practices:
1. **Explicit Waits Only:** Zero reliance on `time.sleep()`. All element lookups and interactions use Selenium's `WebDriverWait` combined with `expected_conditions` (e.g. `visibility_of_element_located`, `element_to_be_clickable`, `url_contains`).
2. **Headless by Default:** Runs headless in automated test pipelines and CI runners to prevent GUI requirements and window focus issues.
3. **Multi-Browser Support:** Seamlessly supports both Google Chrome and Microsoft Edge with automatic fallback if one browser is not present.
4. **Machine Independent:** Zero hardcoded local machine paths or secrets.

---

## 2. Prerequisites & Environment Setup

- **Python 3.10+** (Python 3.13 installed in this environment).
- **Google Chrome** or **Microsoft Edge** browser.
- **Node.js 18+** for running the React Vite development server.

### Install Python Dependencies:
```bash
pip install -r tests/selenium/requirements.txt
```

---

## 3. Running the Smoke Test

### Step 1: Start the Frontend Application
In a dedicated terminal:
```bash
cd frontend
npm run dev
```
By default, the Vite dev server starts at `http://localhost:5173`.

### Step 2: Execute Pytest Smoke Tests
In another terminal:
```bash
# Run headless (default)
pytest tests/selenium -v

# Run with visible browser window (headed mode for debugging)
pytest tests/selenium -v --headed

# Run against a specific browser
pytest tests/selenium -v --browser edge
pytest tests/selenium -v --browser chrome

# Run against a custom URL (e.g. preview build or staging)
pytest tests/selenium -v --frontend-url http://localhost:4173
```

---

## 4. Test Coverage in `test_home_smoke.py`

1. **`test_home_page_smoke`:**
   - Navigates to `http://localhost:5173`.
   - Waits explicitly for the main heading (`h1` containing `"Insurance Workflow Automation System"`).
   - Asserts visibility and clickability of the hero buttons: Browse Plans (`#home-browse-btn`) and Compare Plans (`#home-compare-btn`).
   - Asserts brand logo in the header navbar (`InsuranceFlow`).
   - Asserts rendering of role portal cards (Customer, Insurer Admin, Underwriter, Claims Officer, System Admin).

2. **`test_navigation_to_browse_plans`:**
   - Clicks `#home-browse-btn`.
   - Waits explicitly for the URL transition to `/plans`.
   - Asserts correct view transition.

---

## 5. Adding New Frontend E2E / Functional Tests

To add test cases for your portal or screen (e.g. Customer Quote Form, KYC Upload, or Claims Dashboard):
1. Create a new test file under `tests/selenium/`, for example `test_browse_plans.py`.
2. Use the shared `driver` and `frontend_url` fixtures:
   ```python
   from selenium.webdriver.common.by import By
   from selenium.webdriver.support.ui import WebDriverWait
   from selenium.webdriver.support import expected_conditions as EC

   def test_plan_filter_interaction(driver, frontend_url):
       driver.get(f"{frontend_url}/plans")
       wait = WebDriverWait(driver, 10)
       
       # Explicit wait for plan cards
       plan_cards = wait.until(
           EC.presence_of_all_elements_located((By.CLASS_NAME, "plan-card"))
       )
       assert len(plan_cards) > 0
   ```
3. Commit test scripts alongside your frontend view changes.
