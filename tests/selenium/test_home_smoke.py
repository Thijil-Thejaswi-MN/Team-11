"""
Frontend Smoke Test - Home Page & Application Shell
Work Item: P2-04
Owner: Person 2 (Sharana - Functional Test Engineer)
Reference: SAD §4.5, SRS INS-NF-005, INS-NF-006

Verifies that the React application shell boots successfully, the home page renders,
and core navigation and portal links are interactive.
Strictly relies on Selenium explicit waits (WebDriverWait) - zero arbitrary fixed sleeps.
"""

import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

@pytest.mark.smoke
def test_home_page_smoke(driver, frontend_url):
    """
    Smoke test: Opens the frontend application home page.
    Waits explicitly for the main heading and hero action buttons to become visible.
    Asserts application branding, page title, and key action elements.
    """
    driver.get(frontend_url)

    # Explicit wait (up to 15 seconds) for primary application heading
    wait = WebDriverWait(driver, 15)
    main_heading = wait.until(
        EC.visibility_of_element_located((By.XPATH, "//h1[contains(text(), 'Insurance Workflow Automation System')]")),
        message=f"Timed out waiting for main heading on {frontend_url}. Ensure frontend dev server is running."
    )
    assert main_heading is not None, "Main heading element was not found."
    assert "Insurance Workflow Automation System" in main_heading.text

    # Explicit wait for core action buttons: Browse Plans and Compare Plans
    browse_btn = wait.until(
        EC.element_to_be_clickable((By.ID, "home-browse-btn")),
        message="Browse Plans button with id 'home-browse-btn' was not clickable."
    )
    assert browse_btn.is_displayed(), "Browse Plans button is not visible."

    compare_btn = wait.until(
        EC.element_to_be_clickable((By.ID, "home-compare-btn")),
        message="Compare Plans button with id 'home-compare-btn' was not clickable."
    )
    assert compare_btn.is_displayed(), "Compare Plans button is not visible."

    # Verify brand logo and navigation bar
    nav_bar = wait.until(
        EC.visibility_of_element_located((By.ID, "app-main-navbar")),
        message="Main navigation bar element with id 'app-main-navbar' was not found."
    )
    assert nav_bar.is_displayed(), "Navigation bar is not visible."

    brand_logo = wait.until(
        EC.visibility_of_element_located((By.ID, "navbar-brand-link")),
        message="Navbar brand link with id 'navbar-brand-link' was not found."
    )
    assert brand_logo.is_displayed(), "Brand logo is not visible."
    assert "InsureFlow" in brand_logo.text

    # Verify role portals section rendered
    role_section = wait.until(
        EC.visibility_of_element_located((By.XPATH, "//h2[contains(text(), 'Role-Based Portals')]")),
        message="Role-based portals section heading was not found."
    )
    assert role_section.is_displayed(), "Role portals section is not visible."


@pytest.mark.smoke
def test_navigation_to_browse_plans(driver, frontend_url):
    """
    Smoke test: Navigates from Home to Browse Plans via explicit button click.
    Verifies URL transition and page element display using explicit waits.
    """
    driver.get(frontend_url)
    wait = WebDriverWait(driver, 15)

    browse_btn = wait.until(
        EC.element_to_be_clickable((By.ID, "home-browse-btn")),
        message="Browse Plans button not found on home page."
    )
    browse_btn.click()

    # Explicit wait for URL to transition to /plans
    wait.until(
        EC.url_contains("/plans"),
        message="URL did not transition to '/plans' after clicking Browse Plans."
    )
    assert "/plans" in driver.current_url
