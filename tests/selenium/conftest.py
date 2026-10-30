import os
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions

def pytest_addoption(parser):
    parser.addoption(
        "--browser",
        action="store",
        default=os.getenv("SELENIUM_BROWSER", "chrome"),
        help="Browser to run tests with: 'chrome', 'firefox', or 'edge'"
    )
    parser.addoption(
        "--headed",
        action="store_true",
        default=os.getenv("HEADED", "false").lower() in ("true", "1", "yes"),
        help="Run browser in headed mode (display UI). Default is headless."
    )
    parser.addoption(
        "--frontend-url",
        action="store",
        default=os.getenv("FRONTEND_URL", "http://localhost:5173"),
        help="Base URL for the frontend application"
    )

@pytest.fixture(scope="session")
def frontend_url(request):
    """Provides the target URL of the frontend application under test."""
    return request.config.getoption("--frontend-url").rstrip("/")

@pytest.fixture(scope="function")
def driver(request):
    """
    Initializes and yields a clean WebDriver instance per test.
    Supports Chrome, Firefox, and Edge with headless mode by default.
    Ensures safe teardown via driver.quit().
    """
    browser_name = request.config.getoption("--browser").lower()
    is_headed = request.config.getoption("--headed")

    driver_instance = None

    if browser_name == "edge":
        options = EdgeOptions()
        if not is_headed:
            options.add_argument("--headless=new")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1920,1080")
        try:
            driver_instance = webdriver.Edge(options=options)
        except Exception as e:
            pytest.skip(f"Microsoft Edge driver initialization failed: {e}")
    elif browser_name == "firefox":
        options = FirefoxOptions()
        if not is_headed:
            options.add_argument("-headless")
        options.add_argument("--window-size=1920,1080")
        try:
            driver_instance = webdriver.Firefox(options=options)
        except Exception as e:
            pytest.skip(f"Mozilla Firefox driver initialization failed: {e}")
    else:
        # Default to Google Chrome
        options = ChromeOptions()
        if not is_headed:
            options.add_argument("--headless=new")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        options.add_argument("--window-size=1920,1080")
        try:
            driver_instance = webdriver.Chrome(options=options)
        except Exception as chrome_err:
            # Fallback to Edge if Chrome is not available on system
            try:
                edge_options = EdgeOptions()
                if not is_headed:
                    edge_options.add_argument("--headless=new")
                edge_options.add_argument("--window-size=1920,1080")
                driver_instance = webdriver.Edge(options=edge_options)
            except Exception as edge_err:
                pytest.skip(f"Neither Chrome nor Edge WebDriver could be initialized: Chrome({chrome_err}), Edge({edge_err})")

    driver_instance.implicitly_wait(0)  # Rely on explicit waits, never fixed sleeps or implicit wait traps
    yield driver_instance
    try:
        driver_instance.quit()
    except Exception:
        pass
