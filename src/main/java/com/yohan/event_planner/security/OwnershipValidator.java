package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.UserOwnershipException;
import org.springframework.stereotype.Component;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;

/**
 * Utility component for enforcing ownership rules on secured resources.
 *
 * <p>
 * Ensures that only the authenticated user can access or modify their own {@link User}
 * or {@link Event}. Throws appropriate exceptions when access is denied.
 * </p>
 *
 * <p>
 * Relies on {@link SecurityService} for retrieving the current authenticated user.
 * </p>
 *
 * @see SecurityService
 * @see UserOwnershipException
 * @see EventOwnershipException
 */
@Component
public class OwnershipValidator {

    private final SecurityService securityService;

    /**
     * Constructs a new {@code OwnershipValidator}.
     *
     * @param securityService the service responsible for resolving the authenticated user
     */
    public OwnershipValidator(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Validates that the current user owns the given {@link Event}.
     *
     * @param event the event to validate
     * @throws EventOwnershipException if the user is not the event's creator
     */
    public void validateEventOwnership(Event event) {
        Long currentUserId = securityService.requireCurrentUserId();
        if (!currentUserId.equals(event.getCreator().getId())) {
            throw new EventOwnershipException(UNAUTHORIZED_EVENT_ACCESS, event.getId());
        }
    }

    /**
     * Validates that the current user owns the specified user account.
     *
     * @param userId the ID to validate against the current user
     * @throws UserOwnershipException if the authenticated user does not match the target ID
     */
    public void validateUserOwnership(Long userId) {
        Long currentUserId = securityService.requireCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, userId);
        }
    }

    /**
     * Returns the currently authenticated {@link User}.
     *
     * @return the user associated with the current security context
     */
    public User getCurrentUser() {
        return securityService.getAuthenticatedUser();
    }
}
