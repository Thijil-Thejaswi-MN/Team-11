package com.team11.insurance.common;

import com.team11.insurance.common.logging.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Filter preserves supplied X-Correlation-Id header in response and MDC")
    void shouldPreserveSuppliedCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "supplied-corr-id-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcCapture = new String[1];
        FilterChain filterChain = (req, res) -> {
            mdcCapture[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
        };

        filter.doFilter(request, response, filterChain);

        // Verify captured in MDC during request execution
        assertThat(mdcCapture[0]).isEqualTo("supplied-corr-id-1234");

        // Verify set in HTTP response header
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("supplied-corr-id-1234");

        // Verify cleaned up from MDC after filter chain finishes
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Filter generates fresh UUID when X-Correlation-Id is not supplied")
    void shouldGenerateUUIDWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcCapture = new String[1];
        FilterChain filterChain = (req, res) -> {
            mdcCapture[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
        };

        filter.doFilter(request, response, filterChain);

        // Verify generated UUID in MDC
        assertThat(mdcCapture[0]).isNotNull().isNotBlank();
        assertThat(mdcCapture[0]).matches("^[0-9a-fA-F-]{36}$");

        // Verify generated UUID in response header
        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(responseHeader).isEqualTo(mdcCapture[0]);

        // Verify cleaned up from MDC
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
