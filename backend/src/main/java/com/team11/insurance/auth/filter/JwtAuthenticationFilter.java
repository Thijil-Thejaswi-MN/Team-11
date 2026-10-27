package com.team11.insurance.auth.filter;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.ProductionCurrentUserProvider;
import com.team11.insurance.auth.service.AuthenticationService;
import com.team11.insurance.common.exception.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Filter that intercepts incoming HTTP requests, validates Bearer JWT tokens and server-side sessions,
 * and sets the authenticated {@link CurrentUser} on the request context (INS-SR-003, INS-SR-005).
 */
@Component
@Order(10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationService authenticationService;
    private final java.time.Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtAuthenticationFilter(AuthenticationService authenticationService,
                                   org.springframework.beans.factory.ObjectProvider<java.time.Clock> clockProvider) {
        this.authenticationService = authenticationService;
        this.clock = clockProvider != null ? clockProvider.getIfAvailable(java.time.Clock::systemDefaultZone) : java.time.Clock.systemDefaultZone();
    }

    public JwtAuthenticationFilter(AuthenticationService authenticationService, java.time.Clock clock) {
        this.authenticationService = authenticationService;
        this.clock = clock != null ? clock : java.time.Clock.systemDefaultZone();
    }

    public JwtAuthenticationFilter(AuthenticationService authenticationService) {
        this(authenticationService, (java.time.Clock) null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            try {
                CurrentUser currentUser = authenticationService.validateTokenAndSession(token, clock.instant());
                request.setAttribute(ProductionCurrentUserProvider.CURRENT_USER_ATTRIBUTE, currentUser);
            } catch (AuthenticationException ex) {
                // Invalid or expired token: do not set CurrentUser attribute.
                // Request proceeds; any endpoint requiring authentication will be denied with AUTH-401.
                log.debug("Token validation failed for request {}: {}", request.getRequestURI(), ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
