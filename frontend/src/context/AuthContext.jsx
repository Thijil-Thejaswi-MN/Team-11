import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient, { setAuthToken, onUnauthorized } from '../api/apiClient';

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

const GUEST_USER = {
  userId: null,
  name: 'Guest Visitor',
  email: '',
  role: ROLES.GUEST,
  insurerId: null,
};

const AuthContext = createContext(null);

export function AuthProvider({ children, initialUser = null }) {
  const [currentUser, setCurrentUser] = useState(() => {
    if (initialUser) return initialUser;
    // Default initial user for shell preview / testing
    return {
      userId: 101,
      name: 'Asha Verma',
      email: 'asha@example.com',
      role: ROLES.CUSTOMER,
      insurerId: null,
    };
  });

  const isAuthenticated = currentUser.role !== ROLES.GUEST;

  // Listen for 401 Unauthorized from apiClient (session expired or bad token)
  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      setAuthToken(null);
      setCurrentUser(GUEST_USER);
    });
    return unsubscribe;
  }, []);

  /**
   * Role Switcher helper for frontend development and portal testing.
   * In-memory only (does NOT persist demo_role to localStorage).
   */
  const switchRole = (newRole) => {
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
   * Real login implementation (POST /api/v1/auth/login per INS-F-001)
   * Tokens are held strictly in memory.
   */
  const login = async (email, password) => {
    const response = await apiClient.post('/auth/login', { email, password });
    const { accessToken, role } = response.data;
    setAuthToken(accessToken);

    let user = {
      userId: null,
      name: email.split('@')[0],
      email,
      role: role || ROLES.CUSTOMER,
      insurerId: null,
    };

    try {
      const profileRes = await apiClient.get('/auth/me');
      if (profileRes.data) {
        user = {
          userId: profileRes.data.userId,
          name: profileRes.data.name || user.name,
          email: profileRes.data.email || email,
          role: profileRes.data.role || user.role,
          insurerId: profileRes.data.insurerId ?? null,
        };
      }
    } catch (err) {
      // Profile fetch fallback keeps login response data
    }

    setCurrentUser(user);
    return user;
  };

  /**
   * Real register implementation (POST /api/v1/auth/register per INS-F-001)
   */
  const register = async (userData) => {
    const response = await apiClient.post('/auth/register', userData);
    return response.data;
  };

  /**
   * Real logout implementation (POST /api/v1/auth/logout)
   * Clears in-memory token and resets state.
   */
  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch (e) {
      // Ignore network/logout failure
    } finally {
      setAuthToken(null);
      setCurrentUser(GUEST_USER);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        isAuthenticated,
        switchRole,
        login,
        register,
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
