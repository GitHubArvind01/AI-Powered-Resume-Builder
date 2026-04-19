package com.resumeai.resume_service.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting security information from HTTP requests
 * Provides helper methods to extract JWT tokens and user information
 */
@Component
public class SecurityContextUtils {

    /**
     * Extract JWT token from Authorization header in the current request
     *
     * @return JWT token string without "Bearer " prefix
     * @throws IllegalArgumentException if Authorization header is missing or malformed
     */
    public String extractTokenFromRequest() {
        HttpServletRequest request = getHttpServletRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            throw new IllegalArgumentException("Authorization header is missing");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header format. Expected 'Bearer <token>'");
        }

        return authHeader.substring(7);
    }

    /**
     * Get the current HTTP servlet request
     *
     * @return HttpServletRequest from request context
     * @throws IllegalStateException if not in a servlet request context
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException("Request context not available");
        }

        return attributes.getRequest();
    }
}

