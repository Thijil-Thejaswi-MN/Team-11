package com.team11.insurance.auth;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Production implementation of {@link ICurrentUserProvider}.
 * <p>
 * Strictly active under the 'prod' profile.
 * Resolves the CurrentUser identity from server-side authenticated session or JWT filter context.
 * Never allows arbitrary client-selected identities or unauthenticated requests.
 */
@Component
@Profile("prod")
public class ProductionCurrentUserProvider implements ICurrentUserProvider {

    public static final String CURRENT_USER_ATTRIBUTE = "COM_TEAM11_CURRENT_USER";

    @Override
    public CurrentUser getCurrentUser() {
        RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
        if (attribs instanceof ServletRequestAttributes servletAttribs) {
            HttpServletRequest request = servletAttribs.getRequest();
            Object userObj = request.getAttribute(CURRENT_USER_ATTRIBUTE);
            if (userObj instanceof CurrentUser currentUser) {
                return currentUser;
            }
        }
        throw new AuthenticationException("Authentication required: no active server-side session or valid token found.");
    }

    @Override
    public boolean isAuthenticated() {
        try {
            return getCurrentUser() != null;
        } catch (AuthenticationException ex) {
            return false;
        }
    }
}
