/**
 * Application Route Paths
 * Corresponds to UX Design specifications in SAD Section 4.5
 */
export const ROUTES = {
  // Public Routes (UC-04: No login needed to browse and compare plans)
  HOME: '/',
  PLANS: '/plans',
  PLANS_COMPARE: '/plans/compare',
  LOGIN: '/login',
  REGISTER: '/register',

  // Customer Portal
  CUSTOMER_DASHBOARD: '/customer',
  CUSTOMER_KYC: '/customer/kyc',
  CUSTOMER_QUOTE_MOTOR: '/customer/quote/motor',
  CUSTOMER_QUOTE_HEALTH: '/customer/quote/health',
  CUSTOMER_APPLICATIONS: '/customer/applications',
  CUSTOMER_PAYMENT: '/customer/payment/:applicationId',
  CUSTOMER_POLICIES: '/customer/policies',
  CUSTOMER_CLAIMS: '/customer/claims',

  // Insurer Admin Portal
  INSURER_ADMIN_DASHBOARD: '/insurer-admin',
  INSURER_ADMIN_PLANS: '/insurer-admin/plans',
  INSURER_ADMIN_CANCELLATIONS: '/insurer-admin/cancellations',

  // Underwriter Portal
  UNDERWRITER_DASHBOARD: '/underwriter',

  // Claims Officer Portal
  CLAIMS_OFFICER_DASHBOARD: '/claims-officer',

  // System Admin Portal
  SYSTEM_ADMIN_DASHBOARD: '/system-admin',
  SYSTEM_ADMIN_REPORTS: '/system-admin/reports',
  SYSTEM_ADMIN_AUDIT: '/system-admin/audit',
};
