package com.yohan.event_planner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Custom access denied handler that is triggered when an authenticated user attempts
 * to access a resource they are not authorized to access (e.g., insufficient roles).
 *
 * <p>This handler is called when Spring Security determines that an authenticated
 * user lacks the required permissions for a specific resource or operation. This is
 * distinct from authentication failures (401) handled by {@link AuthEntryPointJwt}.</p>
 *
 * <p>Returns a structured JSON response with a 403 Forbidden status, including a custom
 * {@link ErrorResponse} and error code.</p>
 *
 * <p>Example JSON response:</p>
 * <pre>
 * {
 *   "status": 403,
 *   "message": "Access denied: insufficient permissions",
 *   "errorCode": "ACCESS_DENIED",
 *   "timestamp": 1642781234567,
 *   "path": "/api/admin/users"
 * }
 * </pre>
 *
 * <p><strong>Security Note:</strong> This handler provides generic error messages
 * to prevent information disclosure about system structure or permissions.</p>
 *
 * <p><strong>Architecture:</strong> This component works alongside:</p>
 * <ul>
 *   <li>{@link AuthEntryPointJwt} - handles 401 authentication failures</li>
 *   <li>{@link SecurityConfig} - configures this handler for 403 scenarios</li>
 *   <li>{@link ErrorResponse} - provides consistent error response format</li>
 * </ul>
 *
 * @see ErrorResponse
 * @see ErrorCode#ACCESS_DENIED
 * @see org.springframework.security.web.access.AccessDeniedHandler
 * @see AuthEntryPointJwt
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles access denied scenarios by sending a standardized JSON error response.
     *
     * <p>This method is invoked by Spring Security when an authenticated user
     * attempts to access a resource for which they lack sufficient permissions.</p>
     *
     * @param request the HTTP request that resulted in access denial
     * @param response the HTTP response to write the error to
     * @param accessDeniedException the exception containing details about the access denial
     * @throws IOException if an error occurs writing to the response output stream
     * @throws ServletException if a servlet-specific error occurs
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        logger.warn("Access denied: {}", accessDeniedException.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied: insufficient permissions",
                ErrorCode.ACCESS_DENIED.name(),
                Instant.now().toEpochMilli(),
                request.getRequestURI()
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
