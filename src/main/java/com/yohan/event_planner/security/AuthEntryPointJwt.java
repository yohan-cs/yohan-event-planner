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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles unauthorized access attempts by customizing the response returned to the client.
 * Implements the {@link AuthenticationEntryPoint} interface to provide a custom error message and HTTP status
 * when authentication is required but not provided.
 *
 * <p>
 * When an unauthenticated user attempts to access a protected resource, this component returns a 401 Unauthorized
 * status along with a JSON body describing the error, including the error message, the HTTP status, and the
 * requested path.
 * </p>
 *
 * @see AuthenticationEntryPoint
 * @see AuthenticationException
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    /**
     * Handles an unauthorized access attempt by sending a custom error response.
     *
     * @param request the {@link HttpServletRequest} object containing the request information
     * @param response the {@link HttpServletResponse} object to send the error response
     * @param authException the {@link AuthenticationException} that triggered the error
     * @throws IOException if an I/O error occurs during response writing
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        logger.error("Unauthorized error: {}", authException.getMessage());
        System.out.println(authException);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                authException.getMessage(),
                ErrorCode.UNAUTHORIZED_ACCESS.name(),
                Instant.now().toEpochMilli()
        );

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
