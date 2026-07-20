package com.tooltrack.tooltrackbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = request.getRemoteAddr() + ':' + request.getMethod() + ':' + request.getRequestURI();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(Instant.now(), 0));
        boolean allowed;
        synchronized (window) {
            Instant now = Instant.now();
            if (window.started.plus(limit.duration()).isBefore(now)) {
                window.started = now;
                window.count = 0;
            }
            allowed = ++window.count <= limit.requests();
        }
        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"detail\":\"Please wait before trying again\",\"status\":429}");
            return;
        }
        if (windows.size() > 10_000) {
            Instant cutoff = Instant.now().minus(Duration.ofHours(1));
            windows.entrySet().removeIf(entry -> entry.getValue().started.isBefore(cutoff));
        }
        chain.doFilter(request, response);
    }

    private Limit limitFor(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        String path = request.getRequestURI();
        if (path.equals("/api/auth/password/forgot") || path.equals("/api/auth/password/reset")) {
            return new Limit(5, Duration.ofMinutes(15));
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/google")) {
            return new Limit(20, Duration.ofMinutes(5));
        }
        if (path.equals("/api/uploads/tool-photo")) {
            return new Limit(30, Duration.ofMinutes(10));
        }
        return null;
    }

    private record Limit(int requests, Duration duration) {
    }

    private static final class Window {
        private Instant started;
        private int count;

        private Window(Instant started, int count) {
            this.started = started;
            this.count = count;
        }
    }
}
