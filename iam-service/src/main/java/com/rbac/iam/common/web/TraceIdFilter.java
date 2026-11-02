package com.rbac.iam.common.web;

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
 * Gives every request a traceId, used in logs, in error responses and in the X-Trace-Id header.
 * Runs before Spring Security so that 401/403 responses have a traceId too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "traceId";
    private static final String HEADER = "X-Trace-Id";

    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId != null ? traceId : "UNKNOWN";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Threads are reused, so a leftover value would leak into the next request
            MDC.remove(MDC_KEY);
        }
    }
}
