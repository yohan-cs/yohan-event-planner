package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for retrieving information about the currently authenticated user.
 * <p>
 * Provides both the full {@link User} entity and the user’s ID. If authentication fails
 * or the principal is of an unexpected type, a runtime exception is thrown.
 * </p>
 */
@Service
public class SecurityService {

    /**
     * Retrieves the currently authenticated {@link User} from the security context.
     *
     * @return the authenticated {@link User}
     * @throws UnauthorizedException if the user is not authenticated or the principal is invalid
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        return ((CustomUserDetails) authentication.getPrincipal()).getUser();
    }

    /**
     * Returns the ID of the currently authenticated user.
     * <p>
     * This method throws an {@link UnauthorizedException} if the user is not authenticated.
     * </p>
     *
     * @return the authenticated user's ID
     */
    public Long requireCurrentUserId() {
        return getAuthenticatedUser().getId();
    }

    /**
     * Returns the ID of the currently authenticated user, or {@code null} if unauthenticated.
     * <p>
     * Use this method if authentication is optional in the given context.
     * </p>
     *
     * @return the user ID, or {@code null} if not authenticated
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }

        return ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
    }
}
