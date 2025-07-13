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
 * 
 * <h2>Architecture Integration</h2>
 * <p>
 * This filter is registered in {@link SecurityConfig} and executes once per request before
 * the standard Spring Security authentication filters. It operates as part of the Spring
 * Security filter chain and integrates with the overall JWT-based stateless authentication
 * strategy.
 * </p>
 * 
 * <h2>Processing Flow</h2>
 * <ol>
 *   <li>Extract JWT token from Authorization header using {@link JwtUtils}</li>
 *   <li>Validate token and extract user ID</li>
 *   <li>Load user details via {@link UserDetailsServiceImpl}</li>
 *   <li>Create and set {@link UsernamePasswordAuthenticationToken} in security context</li>
 *   <li>Continue filter chain execution</li>
 * </ol>
 * 
 * <h2>Error Handling</h2>
 * <p>
 * All exceptions during JWT processing are caught and logged but do not interrupt
 * the filter chain. This ensures that requests without valid JWT tokens can still
 * reach protected endpoints where Spring Security will handle the authentication
 * failure appropriately through {@link com.yohan.event_planner.security.AuthEntryPointJwt}.
 * </p>
 * 
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Stateless Authentication</strong>: No server-side session state maintained</li>
 *   <li><strong>Token Validation</strong>: Comprehensive JWT signature and expiration validation</li>
 *   <li><strong>Security Context Integration</strong>: Seamless integration with Spring Security</li>
 *   <li><strong>Audit Logging</strong>: Security events logged for monitoring and compliance</li>
 * </ul>
 * 
 * @see SecurityConfig
 * @see JwtUtils
 * @see UserDetailsServiceImpl
 * @see CustomUserDetails
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
     * {@inheritDoc}
     * 
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
                logger.debug("JWT token found in Authorization header");

                Long userId = jwtUtils.getUserIdFromJwtToken(jwt); // this now includes validation
                logger.debug("Extracted user ID from JWT: {}", userId);

                UserDetails userDetails = userDetailsService.loadUserByUserId(userId);
                logger.debug("Loaded UserDetails for ID {}: {}", userId, userDetails.getUsername());

                setAuthentication(userDetails, request);
            }

        } catch (Exception ex) {
            logger.error("JWT authentication failed for URI {}: {}", 
                        request.getRequestURI(), ex.getMessage());
            logger.debug("Authentication failure details - User-Agent: {}, Remote-Addr: {}", 
                        request.getHeader("User-Agent"), request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Sets up Spring Security authentication context with the provided user details.
     * 
     * <p>
     * Creates a {@link UsernamePasswordAuthenticationToken} with the user details,
     * sets authentication details from the request, and stores it in the 
     * {@link SecurityContextHolder} for use by downstream components.
     * </p>
     *
     * @param userDetails the authenticated user details
     * @param request the HTTP request for authentication details
     */
    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.debug("User authenticated and security context set for: {}", userDetails.getUsername());
        logger.info("User {} authenticated successfully from {}", 
                   userDetails.getUsername(), request.getRemoteAddr());
    }
}
