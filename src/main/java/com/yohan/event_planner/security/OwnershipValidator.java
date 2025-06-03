package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.UserOwnershipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;

/**
 * Validates ownership and access permissions for user- and event-related operations.
 *
 * <p>
 * Ensures that only the authenticated user can access or modify their own {@link User}
 * or {@link Event} resources. Throws ownership-related exceptions when violations occur.
 * </p>
 *
 * <p>
 * This class is a pure helper and expects the caller to provide the authenticated user context.
 * </p>
 *
 * @see UserOwnershipException
 * @see EventOwnershipException
 */
@Component
public class OwnershipValidator {

    private static final Logger logger = LoggerFactory.getLogger(OwnershipValidator.class);

    /**
     * Ensures that the authenticated user is the creator of the specified {@link Event}.
     *
     * <p>
     * If the event does not belong to the current user, this method throws an
     * {@link EventOwnershipException}. This check is intended to prevent unauthorized
     * access to another user's events.
     * </p>
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param event the event whose ownership is being validated
     * @throws EventOwnershipException if the current user is not the event's creator
     */
    public void validateEventOwnership(Long currentUserId, Event event) {
        if (!currentUserId.equals(event.getCreator().getId())) {
            logger.warn("User {} is not authorized to access event {}", currentUserId, event.getId());
            throw new EventOwnershipException(UNAUTHORIZED_EVENT_ACCESS, event.getId());
        }
    }

    /**
     * Ensures that the current user is attempting to access or modify their own {@link User} resource.
     *
     * <p>
     * If the provided {@code userId} does not match the authenticated user's ID,
     * this method throws a {@link UserOwnershipException}.
     * </p>
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param userId the user ID to validate against
     * @throws UserOwnershipException if the current user is not the same as the target user
     */
    public void validateUserOwnership(Long currentUserId, Long userId) {
        if (!currentUserId.equals(userId)) {
            logger.warn("User {} is not authorized to access user {}", currentUserId, userId);
            throw new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, userId);
        }
    }
}
