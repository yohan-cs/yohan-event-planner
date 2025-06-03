package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
 * <p>
 * Designed to be injected into service-layer classes to avoid repetitive
 * token-parsing and user-fetching logic. Future enhancements may include caching
 * user context (e.g., time zone, roles) to improve performance.
 * </p>
 *
 * <p>
 * Throws {@link UnauthorizedException} if the token is missing or invalid,
 * and {@link UserNotFoundException} if the referenced user no longer exists.
 * </p>
 */
@Component
public class AuthenticatedUserProvider {

    private final JwtUtils jwtUtils;
    private final UserBO userBO;
    private final HttpServletRequest request;

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
        String token = jwtUtils.getJwtFromHeader(request);
        Long userId = jwtUtils.getUserIdFromJwtToken(token);
        return userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
