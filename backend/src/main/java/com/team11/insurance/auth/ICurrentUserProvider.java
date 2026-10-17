package com.team11.insurance.auth;

/**
 * Contract for accessing the current user's security identity across modules.
 * <p>
 * Consumed by Person 1 (Quotes & Underwriting), Person 3 (Applications & Policies),
 * and Person 4 (Backend Controllers & Services).
 */
public interface ICurrentUserProvider {

    /**
     * Resolves the current authenticated user context.
     *
     * @return CurrentUser instance
     * @throws com.team11.insurance.common.exception.AuthenticationException if no authenticated user is present
     */
    CurrentUser getCurrentUser();

    /**
     * Checks if there is an active authenticated user in the current execution context.
     *
     * @return true if authenticated, false otherwise
     */
    boolean isAuthenticated();
}
