package com.jifen.auth;

import com.jifen.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String[] PUBLIC_PATHS = {
        "/auth/login", "/auth/register",
        "/auth/admin/login",
        "/auth/wx-login",
        "/products", "/products/"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Strip context-path
        String ctxPath = request.getContextPath();
        if (ctxPath != null && !ctxPath.isEmpty()) {
            path = path.substring(ctxPath.length());
        }

        // Static uploads - skip filter
        if (path.startsWith("/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Skip filter for public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或token已过期\"}");
            return;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                throw new RuntimeException("token invalid");
            }
            UserContextUtil.setUserId(jwtUtil.getUserId(token));
            UserContextUtil.setUsername(jwtUtil.getUsername(token));
            UserContextUtil.setIsAdmin(jwtUtil.isAdmin(token));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期\"}");
        } finally {
            UserContextUtil.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // Also support token in query parameter (for download links)
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p) || path.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
