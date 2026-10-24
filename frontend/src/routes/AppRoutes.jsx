import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { ROUTES } from './routePaths';

// Public Views
import HomeView from '../views/public/HomeView';
import BrowsePlansView from '../views/public/BrowsePlansView';
import ComparePlansView from '../views/public/ComparePlansView';
import LoginView from '../views/public/LoginView';
import RegisterView from '../views/public/RegisterView';

// Customer Portal Views
import CustomerDashboardView from '../views/customer/CustomerDashboardView';
import MotorQuoteView from '../views/customer/MotorQuoteView';
import HealthQuoteView from '../views/customer/HealthQuoteView';
import QuotingPlaceholder from '../views/customer/QuotingPlaceholder';
import ApplicationsPlaceholder from '../views/customer/ApplicationsPlaceholder';
import MyPoliciesView from '../views/customer/MyPoliciesView';
import PaymentView from '../views/customer/PaymentView';
import ClaimsPlaceholder from '../views/customer/ClaimsPlaceholder';

// Insurer Admin Views
import InsurerAdminDashboardView from '../views/insurerAdmin/InsurerAdminDashboardView';

// Underwriter Views
import UnderwriterDashboardView from '../views/underwriter/UnderwriterDashboardView';

// Claims Officer Views
import ClaimsOfficerDashboardView from '../views/claimsOfficer/ClaimsOfficerDashboardView';

// System Admin Views
import SystemAdminDashboardView from '../views/systemAdmin/SystemAdminDashboardView';

// Fallback View
import NotFoundView from '../views/NotFoundView';

/**
 * Centralized Route Switcher
 * Structured into modular blocks so Person 1, Person 2, and Person 3
 * can add or replace screens without modifying unrelated routes.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* ============================================================ */}
      {/* 1. PUBLIC ROUTES (No Login Required)                        */}
      {/* ============================================================ */}
      <Route path={ROUTES.HOME} element={<HomeView />} />
      <Route path={ROUTES.PLANS} element={<BrowsePlansView />} />
      <Route path={ROUTES.PLANS_COMPARE} element={<ComparePlansView />} />

      {/* ============================================================ */}
      {/* 2. PERSON 2 MODULE: AUTH & KYC HOOKS                         */}
      {/* ============================================================ */}
      <Route path={ROUTES.LOGIN} element={<LoginView />} />
      <Route path={ROUTES.REGISTER} element={<RegisterView />} />

      {/* ============================================================ */}
      {/* 3. CUSTOMER PORTAL ROUTES                                    */}
      {/* ============================================================ */}
      <Route path={ROUTES.CUSTOMER_DASHBOARD} element={<CustomerDashboardView />} />

      {/* Person 1 Customer Screens: Motor & Health Quoting (P1-08) */}
      <Route
        path={ROUTES.CUSTOMER_QUOTE_MOTOR}
        element={<MotorQuoteView />}
      />
      <Route
        path={ROUTES.CUSTOMER_QUOTE_HEALTH}
        element={<HealthQuoteView />}
      />

      {/* Person 3 Customer Hooks: Applications & Policies */}
      <Route
        path={ROUTES.CUSTOMER_APPLICATIONS}
        element={<ApplicationsPlaceholder />}
      />
      {/* Person 4 Payment Flow Hook (P4-17) */}
      <Route
        path={ROUTES.CUSTOMER_PAYMENT}
        element={<PaymentView />}
      />
      <Route
        path="/customer/payment"
        element={<PaymentView />}
      />
      <Route
        path={ROUTES.CUSTOMER_POLICIES}
        element={<MyPoliciesView />}
      />

      {/* Person 4 Customer Hook: Claims Tracker & Filing */}
      <Route
        path={ROUTES.CUSTOMER_CLAIMS}
        element={<ClaimsPlaceholder />}
      />

      {/* ============================================================ */}
      {/* 4. PERSON 1 & PERSON 3 MODULE: INSURER ADMIN                 */}
      {/* ============================================================ */}
      <Route
        path={ROUTES.INSURER_ADMIN_DASHBOARD}
        element={<InsurerAdminDashboardView />}
      />
      <Route
        path={ROUTES.INSURER_ADMIN_PLANS}
        element={<InsurerAdminDashboardView />}
      />
      <Route
        path={ROUTES.INSURER_ADMIN_CANCELLATIONS}
        element={<InsurerAdminDashboardView />}
      />

      {/* ============================================================ */}
      {/* 5. PERSON 1 MODULE: UNDERWRITER DESK                         */}
      {/* ============================================================ */}
      <Route
        path={ROUTES.UNDERWRITER_DASHBOARD}
        element={<UnderwriterDashboardView />}
      />

      {/* ============================================================ */}
      {/* 6. PERSON 4 MODULE: CLAIMS OFFICER DESK                      */}
      {/* ============================================================ */}
      <Route
        path={ROUTES.CLAIMS_OFFICER_DASHBOARD}
        element={<ClaimsOfficerDashboardView />}
      />

      {/* ============================================================ */}
      {/* 7. SYSTEM ADMINISTRATION & AUDIT (SHARED C11)                */}
      {/* ============================================================ */}
      <Route
        path={ROUTES.SYSTEM_ADMIN_DASHBOARD}
        element={<SystemAdminDashboardView />}
      />
      <Route
        path={ROUTES.SYSTEM_ADMIN_REPORTS}
        element={<SystemAdminDashboardView />}
      />
      <Route
        path={ROUTES.SYSTEM_ADMIN_AUDIT}
        element={<SystemAdminDashboardView />}
      />

      {/* ============================================================ */}
      {/* 8. 404 CATCH-ALL ROUTE                                       */}
      {/* ============================================================ */}
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  );
}
