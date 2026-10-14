package com.team11.insurance.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for distributed tracing and request correlation (SAD Section 4.4 & 4.5).
 * - Reads incoming 'X-Correlation-Id' or generates a fresh UUID.
 * - Injects into SLF4J MDC context under key 'correlationId'.
 * - Attaches 'X-Correlation-Id' to the outgoing HTTP response.
 * - Safely cleans up MDC after request completion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            // Sanitize to prevent log injection
            correlationId = correlationId.trim().replaceAll("[\r\n]", "");
        }

        try {
            MDC.put(MDC_KEY, correlationId);
            response.setHeader(HEADER_NAME, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
