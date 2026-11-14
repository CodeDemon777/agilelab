package com.agilelab.chatapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production-ready in-memory rate limiter filter for auth endpoints
 * to protect against brute-force password cracking and registration spam.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_MS = 60_000L;

    private static class RequestCount {
        long windowStart;
        AtomicInteger count;

        RequestCount(long start) {
            this.windowStart = start;
            this.count = new AtomicInteger(1);
        }
    }

    private final ConcurrentHashMap<String, RequestCount> ipRequests = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST operations on auth endpoints
        boolean isAuthPost = "POST".equalsIgnoreCase(method) &&
                (path.startsWith("/api/auth/") || "/login".equals(path) || "/signup".equals(path));

        if (!isAuthPost) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestCount current = ipRequests.compute(clientIp, (ip, rc) -> {
            if (rc == null || now - rc.windowStart > WINDOW_MS) {
                return new RequestCount(now);
            }
            rc.count.incrementAndGet();
            return rc;
        });

        // Periodic maintenance cleanup if map grows
        if (ipRequests.size() > 5000) {
            ipRequests.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS * 2);
        }

        if (current.count.get() > MAX_REQUESTS_PER_MINUTE) {
            logger.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, path);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again after 60 seconds.\"}");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}

