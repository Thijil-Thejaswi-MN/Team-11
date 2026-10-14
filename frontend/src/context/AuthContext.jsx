import React, { createContext, useContext, useState, useEffect } from 'react';

/**
 * Role Definitions matching SAD Section 4.3 and 4.5:
 * C = Customer
 * IA = Insurer Admin
 * UW = Underwriter
 * CO = Claims Officer
 * SA = System Admin
 */
export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  INSURER_ADMIN: 'INSURER_ADMIN',
  UNDERWRITER: 'UNDERWRITER',
  CLAIMS_OFFICER: 'CLAIMS_OFFICER',
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  GUEST: 'GUEST',
};

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Shell state stub (designed for Person 2 to replace with Spring Security JWT integration)
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('demo_role') || ROLES.CUSTOMER;
    return {
      userId: 101,
      name: 'Asha Verma',
      email: 'asha@example.com',
      role: saved,
      insurerId: saved === ROLES.CUSTOMER ? null : 1, // 'SafeShield General'
    };
  });

  const isAuthenticated = currentUser.role !== ROLES.GUEST;

  /**
   * Role Switcher helper for frontend development and portal testing.
   * Enables team members to preview all 5 portals without waiting for live auth endpoints.
   */
  const switchRole = (newRole) => {
    localStorage.setItem('demo_role', newRole);
    let name = 'Asha Verma';
    let email = 'asha@example.com';
    let insurerId = null;

    if (newRole === ROLES.INSURER_ADMIN) {
      name = 'Admin Rajesh (SafeShield)';
      email = 'admin@safeshield.com';
      insurerId = 1;
    } else if (newRole === ROLES.UNDERWRITER) {
      name = 'Underwriter Priya';
      email = 'priya.uw@safeshield.com';
      insurerId = 1;
    } else if (newRole === ROLES.CLAIMS_OFFICER) {
      name = 'Officer Manoj';
      email = 'manoj.co@safeshield.com';
      insurerId = 1;
    } else if (newRole === ROLES.SYSTEM_ADMIN) {
      name = 'SysAdmin Vikram';
      email = 'sysadmin@portal.gov';
      insurerId = null;
    } else if (newRole === ROLES.GUEST) {
      name = 'Guest Visitor';
      email = '';
      insurerId = null;
    }

    setCurrentUser({
      userId: newRole === ROLES.GUEST ? null : 101,
      name,
      email,
      role: newRole,
      insurerId,
    });
  };

  /**
   * Stub login method (Person 2 will implement real /api/v1/auth/login)
   */
  const login = async (email, password) => {
    // Person 2 integration hook
    console.log('[Person 2 Hook] Login requested for:', email);
    switchRole(ROLES.CUSTOMER);
    return true;
  };

  /**
   * Stub logout method
   */
  const logout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('demo_role');
    switchRole(ROLES.GUEST);
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        isAuthenticated,
        switchRole,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
