package com.yohan.event_planner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentication filter that intercepts incoming HTTP requests and processes JWT authentication.
 * <p>
 * This filter is registered automatically by Spring Security and is executed once per request.
 * It extracts the JWT token from the request header, validates it, retrieves user details,
 * and sets the authentication in the Spring Security context.
 * </p>
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    /**
     * Constructs an {@code AuthTokenFilter} with dependencies on JWT utilities
     * and user details service.
     *
     * @param jwtUtils            utility class for handling JWT operations
     * @param userDetailsService  service to load user details by user ID
     */
    public AuthTokenFilter(JwtUtils jwtUtils,
                           UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Performs JWT authentication before the request reaches the controller.
     * <p>
     * If a valid JWT is found in the Authorization header, the user is authenticated
     * and their details are stored in the {@link SecurityContextHolder}.
     * </p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());

        try {
            String jwt = jwtUtils.getJwtFromHeader(request);

            if (jwt != null) {
                logger.debug("Authorization header: {}", request.getHeader("Authorization"));
                logger.debug("Extracted JWT: {}", jwt);

                Long userId = jwtUtils.getUserIdFromJwtToken(jwt); // this now includes validation
                logger.debug("Extracted user ID from JWT: {}", userId);

                UserDetails userDetails = userDetailsService.loadUserByUserId(userId);
                logger.debug("Loaded UserDetails for ID {}: {}", userId, userDetails.getUsername());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("User authenticated and security context set for: {}", userDetails.getUsername());
            }

        } catch (Exception ex) {
            logger.error("Error during JWT authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
