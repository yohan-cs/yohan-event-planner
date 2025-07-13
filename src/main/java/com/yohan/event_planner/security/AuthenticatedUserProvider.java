package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides access to the currently authenticated {@link User} based on the JWT token
 * included in the incoming {@link HttpServletRequest}.
 *
 * <p>
 * This component extracts the token from the {@code Authorization} header,
 * validates it using {@link JwtUtils}, and retrieves the corresponding {@link User}
 * from the business layer.
 * </p>
 *
 * <h2>Architecture Integration</h2>
 * <p>
 * Designed to be injected into service-layer classes to avoid repetitive
 * token-parsing and user-fetching logic. This component acts as a security boundary
 * abstraction that bridges HTTP authentication concerns with domain user entities.
 * Used extensively across service implementations (10+ classes) for consistent
 * authenticated user access patterns.
 * </p>
 *
 * <h2>Threading and Performance</h2>
 * <p>
 * <strong>Thread Safety:</strong> This component is thread-safe due to request-scoped
 * {@link HttpServletRequest} injection. Each HTTP request receives its own instance
 * with proper request context isolation.
 * </p>
 * <p>
 * <strong>Performance:</strong> No caching is currently implemented - each call to
 * {@link #getCurrentUser()} performs fresh token validation and database lookup.
 * Future enhancements may include request-scoped caching of user context
 * (e.g., time zone, roles) to improve performance for multiple calls within
 * the same request.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Throws {@link UnauthorizedException} if the token is missing or invalid,
 * and {@link UserNotFoundException} if the referenced user no longer exists.
 * These exceptions are designed to be handled by the global exception handler
 * for consistent API error responses.
 * </p>
 *
 * @see JwtUtils
 * @see UserBO
 * @see com.yohan.event_planner.service service layer implementations
 */
@Component
public class AuthenticatedUserProvider {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUserProvider.class);

    private final JwtUtils jwtUtils;
    private final UserBO userBO;
    private final HttpServletRequest request;

    /**
     * Constructs a new AuthenticatedUserProvider with required dependencies.
     *
     * <p>
     * The HttpServletRequest is injected as a request-scoped bean, ensuring thread safety
     * and proper isolation between concurrent requests. Each instance of this component
     * operates on a specific HTTP request context.
     * </p>
     *
     * @param jwtUtils utility for JWT token extraction and validation from HTTP headers
     * @param userBO business object for user retrieval operations and domain logic
     * @param request HTTP servlet request containing authentication headers and request context
     */
    @Autowired
    public AuthenticatedUserProvider(JwtUtils jwtUtils,
                                     UserBO userBO,
                                     HttpServletRequest request) {
        this.jwtUtils = jwtUtils;
        this.userBO = userBO;
        this.request = request;
    }

    /**
     * Retrieves the authenticated {@link User} associated with the current request's token.
     *
     * <p>This method relies on {@link JwtUtils#getUserIdFromJwtToken(String)} to validate the
     * token and extract the user ID in one step. It throws {@link UnauthorizedException}
     * if the token is missing or invalid.</p>
     *
     * @return the authenticated user
     * @throws UnauthorizedException if the token is missing or invalid
     * @throws UserNotFoundException if the user ID from the token does not exist in the system
     */
    public User getCurrentUser() {
        logger.debug("Retrieving current user from JWT token");
        String token = jwtUtils.getJwtFromHeader(request);
        Long userId = jwtUtils.getUserIdFromJwtToken(token);
        logger.debug("Extracted user ID {} from JWT token", userId);
        
        return userBO.getUserById(userId)
                .orElseThrow(() -> {
                    logger.warn("Authenticated user ID {} not found in database", userId);
                    return new UserNotFoundException(userId);
                });
    }
}
