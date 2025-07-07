package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.BadgeOwnershipException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.RecurringEventOwnershipException;
import com.yohan.event_planner.exception.UserOwnershipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_BADGE_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_LABEL_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_RECURRING_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;

/**
 * Validates ownership and access permissions for user-, event-, and label-related operations.
 *
 * <p>
 * Ensures that only the authenticated user can access or modify their own {@link User},
 * {@link Event}, or {@link Label} resources. Throws ownership-related exceptions when violations occur.
 * </p>
 *
 * <p>
 * This class is a pure helper and expects the caller to provide the authenticated user context.
 * </p>
 *
 * @see UserOwnershipException
 * @see EventOwnershipException
 * @see LabelOwnershipException
 */
@Component
public class OwnershipValidator {

    private static final Logger logger = LoggerFactory.getLogger(OwnershipValidator.class);

    public void validateEventOwnership(Long currentUserId, Event event) {
        if (!currentUserId.equals(event.getCreator().getId())) {
            logger.warn("User {} is not authorized to access event {}", currentUserId, event.getId());
            throw new EventOwnershipException(UNAUTHORIZED_EVENT_ACCESS, event.getId());
        }
    }

    /**
     * Ensures that the current user is the creator of the specified {@link RecurringEvent}.
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param recurringEvent the recurring event whose ownership is being validated
     * @throws RecurringEventOwnershipException if the current user is not the recurring event's creator
     */
    public void validateRecurringEventOwnership(Long currentUserId, RecurringEvent recurringEvent) {
        if (!currentUserId.equals(recurringEvent.getCreator().getId())) {
            logger.warn("User {} is not authorized to access recurring event {}", currentUserId, recurringEvent.getId());
            throw new RecurringEventOwnershipException(UNAUTHORIZED_RECURRING_EVENT_ACCESS, recurringEvent.getId());
        }
    }

    public void validateUserOwnership(Long currentUserId, Long userId) {
        if (!currentUserId.equals(userId)) {
            logger.warn("User {} is not authorized to access user {}", currentUserId, userId);
            throw new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, userId);
        }
    }

    /**
     * Ensures that the current user is the creator of the specified {@link Label}.
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param label the label whose ownership is being validated
     * @throws LabelOwnershipException if the current user is not the label's creator
     */
    public void validateLabelOwnership(Long currentUserId, Label label) {
        if (!currentUserId.equals(label.getCreator().getId())) {
            logger.warn("User {} is not authorized to access label {}", currentUserId, label.getId());
            throw new LabelOwnershipException(UNAUTHORIZED_LABEL_ACCESS, label.getId());
        }
    }

    /**
     * Ensures that the current user is the creator of the specified {@link Badge}.
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param badge the badge whose ownership is being validated
     * @throws BadgeOwnershipException if the current user is not the badge's creator
     */
    public void validateBadgeOwnership(Long currentUserId, Badge badge) {
        if (!currentUserId.equals(badge.getUser().getId())) {
            logger.warn("User {} is not authorized to access badge {}", currentUserId, badge.getId());
            throw new BadgeOwnershipException(UNAUTHORIZED_BADGE_ACCESS, badge.getId());
        }
    }
}
