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
 * Centralized security component that validates ownership and access permissions for all
 * user-scoped domain entities in the event planner application.
 *
 * <h2>Architecture Integration</h2>
 * <p>
 * This validator operates as a pure security utility component that enforces domain-specific
 * authorization rules. It integrates with the overall security architecture by providing
 * ownership validation after JWT authentication has been completed by {@link AuthTokenFilter}
 * and user context has been established by {@link AuthenticatedUserProvider}.
 * </p>
 *
 * <h2>Supported Domain Entities</h2>
 * <ul>
 *   <li><strong>{@link Event}</strong> - Validates against the event creator</li>
 *   <li><strong>{@link RecurringEvent}</strong> - Validates against the recurring event creator</li>
 *   <li><strong>{@link User}</strong> - Validates user ID matching for self-access</li>
 *   <li><strong>{@link Label}</strong> - Validates against the label creator</li>
 *   <li><strong>{@link Badge}</strong> - Validates against the badge owner</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>
 * This validator is typically used in service layer methods after entity retrieval
 * but before business logic execution:
 * </p>
 * <pre>{@code
 * @Service
 * public class EventServiceImpl {
 *     public void updateEvent(Long eventId, EventUpdateDTO dto) {
 *         User currentUser = authenticatedUserProvider.getCurrentUser();
 *         Event event = eventRepository.findById(eventId)...
 *         ownershipValidator.validateEventOwnership(currentUser.getId(), event);
 *         // Proceed with business logic...
 *     }
 * }
 * }</pre>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Ownership Enforcement</strong>: Ensures users can only access their own resources</li>
 *   <li><strong>Audit Logging</strong>: Logs unauthorized access attempts for security monitoring</li>
 *   <li><strong>Specific Exceptions</strong>: Throws domain-specific exceptions with error codes</li>
 *   <li><strong>Consistent API</strong>: Uniform validation methods across all domain entities</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This component is stateless and thread-safe. It can be safely used in concurrent
 * environments without additional synchronization.
 * </p>
 *
 * @see AuthenticatedUserProvider
 * @see AuthTokenFilter
 * @see SecurityConfig
 * @see UserOwnershipException
 * @see EventOwnershipException
 * @see LabelOwnershipException
 * @see RecurringEventOwnershipException
 * @see BadgeOwnershipException
 */
@Component
public class OwnershipValidator {

    private static final Logger logger = LoggerFactory.getLogger(OwnershipValidator.class);

    /**
     * Constructs an {@code OwnershipValidator} instance.
     *
     * <p>
     * This component is designed to be managed by the Spring container as a singleton
     * bean. It maintains no state and can be safely shared across all application
     * components that require ownership validation functionality.
     * </p>
     *
     * <p>
     * <strong>Integration:</strong> Typically injected into service layer components
     * via constructor injection alongside {@link AuthenticatedUserProvider} for
     * complete user context and authorization validation.
     * </p>
     */
    public OwnershipValidator() {
        // Default constructor for Spring component instantiation
    }

    /**
     * Validates that the current user is authorized to access or modify the specified {@link Event}.
     *
     * <p>
     * Ensures that only the event creator can perform operations on the event. This validation
     * is typically called by service layer methods before executing business logic that
     * modifies event data or accesses event-specific information.
     * </p>
     *
     * <p>
     * <strong>Usage Example:</strong>
     * </p>
     * <pre>{@code
     * Event event = eventRepository.findById(eventId).orElseThrow(...);
     * ownershipValidator.validateEventOwnership(currentUser.getId(), event);
     * // Safe to proceed with event operations
     * }</pre>
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param event the event whose ownership is being validated
     * @throws EventOwnershipException if the current user is not the event's creator
     * @throws NullPointerException if currentUserId is null or event is null
     */
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

    /**
     * Validates that the current user is authorized to access or modify the specified user account.
     *
     * <p>
     * Enforces self-access restrictions by ensuring that users can only access their own
     * user profiles and account information. This validation is fundamental to maintaining
     * user data isolation in the multi-tenant-style architecture.
     * </p>
     *
     * <p>
     * <strong>Usage Example:</strong>
     * </p>
     * <pre>{@code
     * public UserResponseDTO getUserProfile(Long userId) {
     *     User currentUser = authenticatedUserProvider.getCurrentUser();
     *     ownershipValidator.validateUserOwnership(currentUser.getId(), userId);
     *     // Safe to proceed with user profile operations
     * }
     * }</pre>
     *
     * @param currentUserId the ID of the currently authenticated user
     * @param userId the ID of the user account being accessed
     * @throws UserOwnershipException if the current user ID does not match the target user ID
     * @throws NullPointerException if currentUserId is null or userId is null
     */
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
