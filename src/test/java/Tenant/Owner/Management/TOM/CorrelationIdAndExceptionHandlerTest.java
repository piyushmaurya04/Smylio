package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import Tenant.Owner.Management.TOM.Exceptions.GlobalExceptionHandler;
import Tenant.Owner.Management.TOM.Exceptions.RateLimitExceededException;
import Tenant.Owner.Management.TOM.Security.CorrelationIdFilter;

class CorrelationIdAndExceptionHandlerTest {

    @Test
    void correlationIdFilterPreservesValidRequestIdAndCleansMdc() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("req-abc-123", response.getHeader("X-Request-Id"));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void correlationIdFilterGeneratesNewIdWhenHeaderInvalid() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "bad\nheader\rvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String generated = response.getHeader("X-Request-Id");
        assertNotNull(generated);
        assertNotEquals("bad\nheader\rvalue", generated);
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void globalExceptionHandlerMapsRateLimitAndConflictWithClockTimestamp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-03-15T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
        GlobalExceptionHandler handler = new GlobalExceptionHandler(fixed);

        var rateLimited = handler.handleRateLimit(new RateLimitExceededException("Slow down"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rateLimited.getStatusCode());
        assertEquals("Slow down", rateLimited.getBody().getMessage());

        var conflict = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("fk"));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }
}

