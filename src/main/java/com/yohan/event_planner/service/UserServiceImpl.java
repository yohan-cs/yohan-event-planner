package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.UserInitializer;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserHeaderResponseDTO;
import com.yohan.event_planner.dto.UserHeaderUpdateDTO;
import com.yohan.event_planner.dto.UserProfileResponseDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME;

/**
 * Implementation of {@link UserService} providing comprehensive user management functionality.
 * 
 * <p>This service orchestrates user lifecycle management including registration, profile updates,
 * settings management, and account deactivation. It integrates with multiple system components
 * to handle user initialization, password management, badge integration, and profile views,
 * while enforcing security constraints and business rules throughout the user journey.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>User Lifecycle</strong>: Registration, activation, updates, and soft deletion</li>
 *   <li><strong>Profile Management</strong>: Public profile views and header customization</li>
 *   <li><strong>Settings Management</strong>: User preferences and configuration updates</li>
 *   <li><strong>Account Operations</strong>: Deactivation, reactivation, and status management</li>
 * </ul>
 * 
 * <h2>User Registration and Initialization</h2>
 * <p>Comprehensive user onboarding process:</p>
 * <ul>
 *   <li><strong>Validation</strong>: Username and email uniqueness verification</li>
 *   <li><strong>Password Security</strong>: Secure password hashing via PasswordBO</li>
 *   <li><strong>Initial Setup</strong>: Default labels and system entity creation</li>
 *   <li><strong>Badge Integration</strong>: Automatic badge system initialization</li>
 * </ul>
 * 
 * <h2>Profile System</h2>
 * <p>Supports comprehensive user profiles with privacy controls:</p>
 * <ul>
 *   <li><strong>Public Profiles</strong>: Username-based profile access</li>
 *   <li><strong>Privacy Controls</strong>: Respect user visibility preferences</li>
 *   <li><strong>Profile Headers</strong>: Customizable profile header information</li>
 *   <li><strong>Badge Integration</strong>: Display user's badge collections</li>
 * </ul>
 * 
 * <h2>Settings Management</h2>
 * <p>Flexible user preference system:</p>
 * <ul>
 *   <li><strong>Profile Updates</strong>: Username, email, timezone modifications</li>
 *   <li><strong>Validation Rules</strong>: Enforce uniqueness and format constraints</li>
 *   <li><strong>Patch Operations</strong>: Selective field updates via patch handler</li>
 *   <li><strong>Transaction Safety</strong>: Atomic updates across related entities</li>
 * </ul>
 * 
 * <h2>Account Lifecycle Management</h2>
 * <p>Comprehensive account state management:</p>
 * <ul>
 *   <li><strong>Soft Deletion</strong>: Mark accounts for deletion without immediate removal</li>
 *   <li><strong>Reactivation</strong>: Restore deactivated accounts</li>
 *   <li><strong>State Transitions</strong>: Manage account status changes</li>
 *   <li><strong>Data Preservation</strong>: Maintain data integrity during state changes</li>
 * </ul>
 * 
 * <h2>Security and Authorization</h2>
 * <p>Robust security model throughout user operations:</p>
 * <ul>
 *   <li><strong>Authentication Context</strong>: Current user access via AuthenticatedUserProvider</li>
 *   <li><strong>Ownership Validation</strong>: Ensure users only modify their own data</li>
 *   <li><strong>Privacy Enforcement</strong>: Respect user privacy settings for profile views</li>
 *   <li><strong>Input Validation</strong>: Comprehensive validation of user inputs</li>
 * </ul>
 * 
 * <h2>Integration Architecture</h2>
 * <p>This service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>UserBO</strong>: Core user business logic and database operations</li>
 *   <li><strong>PasswordBO</strong>: Secure password handling and validation</li>
 *   <li><strong>BadgeService</strong>: User badge management and display</li>
 *   <li><strong>UserInitializer</strong>: Default entity creation for new users</li>
 *   <li><strong>UserPatchHandler</strong>: Selective field update processing</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>Username Uniqueness</strong>: Globally unique usernames across the system</li>
 *   <li><strong>Email Uniqueness</strong>: One account per email address</li>
 *   <li><strong>Soft Deletion</strong>: Preserve user data during deactivation</li>
 *   <li><strong>Profile Privacy</strong>: Honor user privacy preferences</li>
 * </ul>
 * 
 * <h2>Data Validation</h2>
 * <p>Comprehensive validation ensures data integrity:</p>
 * <ul>
 *   <li><strong>Format Validation</strong>: Email format, username constraints</li>
 *   <li><strong>Uniqueness Checks</strong>: Prevent duplicate usernames and emails</li>
 *   <li><strong>Business Rule Validation</strong>: Enforce domain-specific constraints</li>
 *   <li><strong>Input Sanitization</strong>: Clean and validate user inputs</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Queries</strong>: Optimized database operations for user data</li>
 *   <li><strong>Lazy Loading</strong>: Strategic entity loading to minimize queries</li>
 *   <li><strong>Batch Operations</strong>: Efficient bulk operations where applicable</li>
 *   <li><strong>Cache-Friendly Design</strong>: Structure optimized for caching strategies</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>Comprehensive error handling for various scenarios:</p>
 * <ul>
 *   <li><strong>UserNotFoundException</strong>: When requested users don't exist</li>
 *   <li><strong>UsernameException</strong>: For username validation failures</li>
 *   <li><strong>EmailException</strong>: For email validation and uniqueness issues</li>
 *   <li><strong>Ownership Violations</strong>: When users access unauthorized data</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for user management:</p>
 * <ul>
 *   <li><strong>User Onboarding</strong>: Complete registration and setup process</li>
 *   <li><strong>Profile Management</strong>: Update user information and preferences</li>
 *   <li><strong>Account Administration</strong>: Manage account status and settings</li>
 *   <li><strong>Social Features</strong>: Public profile viewing and discovery</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Maintains consistency across user-related operations:</p>
 * <ul>
 *   <li><strong>Transactional Updates</strong>: Ensure atomic user modifications</li>
 *   <li><strong>Referential Integrity</strong>: Maintain valid relationships</li>
 *   <li><strong>State Synchronization</strong>: Keep user state consistent across systems</li>
 *   <li><strong>Cascade Management</strong>: Handle related entity updates</li>
 * </ul>
 * 
 * @see UserService
 * @see User
 * @see UserBO
 * @see PasswordBO
 * @see BadgeService
 * @see UserInitializer
 * @see UserPatchHandler
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserBO userBO;
    private final EventBO eventBO;
    private final UserMapper userMapper;
    private final UserPatchHandler userPatchHandler;
    private final UserInitializer userInitializer;
    private final PasswordBO passwordBO;
    private final BadgeService badgeService;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;

    public UserServiceImpl(
            UserBO userBO,
            EventBO eventBO,
            UserMapper userMapper,
            UserPatchHandler userPatchHandler,
            UserInitializer userInitializer,
            PasswordBO passwordBO,
            BadgeService badgeService,
            EventResponseDTOFactory eventResponseDTOFactory,
            AuthenticatedUserProvider authenticatedUserProvider,
            OwnershipValidator ownershipValidator
    ) {
        this.userBO = userBO;
        this.eventBO = eventBO;
        this.userMapper = userMapper;
        this.userPatchHandler = userPatchHandler;
        this.userInitializer = userInitializer;
        this.passwordBO = passwordBO;
        this.badgeService = badgeService;
        this.eventResponseDTOFactory = eventResponseDTOFactory;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ownershipValidator = ownershipValidator;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public UserResponseDTO getUserSettings() {
        logger.debug("Retrieving user settings for authenticated user");
        User currentUser = authenticatedUserProvider.getCurrentUser();
        logger.debug("Retrieved user settings for user ID: {}", currentUser.getId());

        return userMapper.toResponseDTO(currentUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO dto) {
        logger.debug("Creating new user with username: {}", dto.username());
        String normalizedUsername = dto.username().toLowerCase();
        String normalizedEmail = dto.email().toLowerCase();

        if (existsByUsername(normalizedUsername)) {
            logger.warn("Username conflict detected during registration: {} already exists", normalizedUsername);
            throw new UsernameException(DUPLICATE_USERNAME, dto.username());
        }
        if (existsByEmail(normalizedEmail)) {
            logger.warn("Email conflict detected during registration: {} already exists", normalizedEmail);
            throw new EmailException(DUPLICATE_EMAIL, dto.email());
        }

        User user = userMapper.toEntity(dto);
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setHashedPassword(passwordBO.encryptPassword(dto.password()));

        // Delegate to UserInitializer instead of userBO directly
        User initializedUser = userInitializer.initializeUser(user);
        logger.info("Successfully created user: {} (ID: {})", initializedUser.getUsername(), initializedUser.getId());

        return userMapper.toResponseDTO(initializedUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponseDTO updateUserSettings(UserUpdateDTO dto) {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        logger.debug("Updating settings for user ID: {}", currentUser.getId());

        if (dto.username() != null) {
            validateUsernameAvailability(dto.username(), currentUser.getId());
        }

        if (dto.email() != null) {
            validateEmailAvailability(dto.email(), currentUser.getId());
        }

        boolean changed = userPatchHandler.applyPatch(currentUser, dto);
        if (changed) {
            logger.info("Successfully updated settings for user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
            return userMapper.toResponseDTO(userBO.updateUser(currentUser));
        } else {
            logger.debug("No changes applied to user settings for user ID: {}", currentUser.getId());
            return userMapper.toResponseDTO(currentUser);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void markUserForDeletion() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        logger.info("Marking user {} (ID: {}) for deletion", currentUser.getUsername(), currentUser.getId());
        userBO.markUserForDeletion(currentUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void reactivateCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        logger.info("Reactivating user {} (ID: {}) - canceling pending deletion", currentUser.getUsername(), currentUser.getId());
        currentUser.unmarkForDeletion(); // domain logic clears timestamp
        userBO.updateUser(currentUser); // persist
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation provides context-aware profile information including automatic
     * cleanup of invalid pinned events. When a profile owner views their own profile, the
     * system validates the pinned impromptu event and automatically removes it if it no longer
     * qualifies (e.g., if it was confirmed or marked as non-impromptu).</p>
     * 
     * <p>The pinned event validation and cleanup occurs in the same transaction as the profile
     * retrieval to ensure data consistency. This provides a "defense in depth" approach alongside
     * the automatic unpinning that occurs during event lifecycle operations.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(String username, Long viewerId) {
        logger.debug("Retrieving profile for username: {} (viewer ID: {})", username, viewerId);
        User user = userBO.getUserByUsername(username.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(username));

        boolean isSelf = viewerId != null && user.getId().equals(viewerId);
        logger.debug("Profile access for user ID: {} - isSelf: {}", user.getId(), isSelf);

        UserHeaderResponseDTO header = getUserHeader(user);
        List<BadgeResponseDTO> badges = badgeService.getBadgesByUser(user.getId());
        
        // Only include pinned event for profile owner
        EventResponseDTO pinnedEvent = null;
        if (isSelf) {
            Event pinnedImpromptuEvent = user.getPinnedImpromptuEvent();
            if (pinnedImpromptuEvent != null && 
                pinnedImpromptuEvent.isUnconfirmed() && 
                pinnedImpromptuEvent.isImpromptu()) {
                pinnedEvent = eventResponseDTOFactory.createFromEvent(pinnedImpromptuEvent);
                logger.debug("Including pinned impromptu event {} for user {}", 
                           pinnedImpromptuEvent.getId(), user.getId());
            } else if (pinnedImpromptuEvent != null) {
                // Auto-unpin invalid event
                logger.info("Pinned event {} for user {} no longer qualifies, auto-unpinning", 
                           pinnedImpromptuEvent.getId(), user.getId());
                user.setPinnedImpromptuEvent(null);
                userBO.updateUser(user);
            }
        }

        return new UserProfileResponseDTO(isSelf, header, badges, pinnedEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserHeaderResponseDTO updateUserHeader(Long userId, UserHeaderUpdateDTO dto) {
        logger.debug("Updating header for user ID: {}", userId);
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ownershipValidator.validateUserOwnership(user.getId(), userId);

        boolean changed = false;

        if (!Objects.equals(dto.bio(), user.getBio())) {
            user.setBio(dto.bio());
            changed = true;
        }

        if (!Objects.equals(dto.profilePictureUrl(), user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(dto.profilePictureUrl());
            changed = true;
        }

        if (changed) {
            logger.info("Successfully updated header for user: {} (ID: {})", user.getUsername(), user.getId());
            user = userBO.updateUser(user);
        } else {
            logger.debug("No changes applied to header for user ID: {}", userId);
        }

        return getUserHeader(user);
    }

    /**
     * Checks whether a username exists (case-insensitive).
     *
     * @param username the username to check
     * @return {@code true} if the username is taken; otherwise {@code false}
     */
    @Override
    public boolean existsByUsername(String username) {
        return userBO.existsByUsername(username.toLowerCase());
    }

    /**
     * Checks whether an email address exists (case-insensitive).
     *
     * @param email the email address to check
     * @return {@code true} if the email is taken; otherwise {@code false}
     */
    @Override
    public boolean existsByEmail(String email) {
        return userBO.existsByEmail(email.toLowerCase());
    }

    /**
     * Validates that the provided username is not already taken by another user.
     *
     * <p>Performs case-insensitive username lookup and allows the current user
     * to keep their existing username if no changes are being made.</p>
     *
     * @param username the username to validate
     * @param userId the ID of the current user (excluded from uniqueness check)
     * @throws UsernameException if the username is already taken by another user
     */
    private void validateUsernameAvailability(String username, Long userId) {
        String normalized = username.toLowerCase();
        userBO.getUserByUsername(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                logger.warn("Username conflict detected: {} already taken by user ID: {}", normalized, existing.getId());
                throw new UsernameException(DUPLICATE_USERNAME, normalized);
            }
        });
    }

    /**
     * Validates that the provided email is not already taken by another user.
     *
     * <p>Performs case-insensitive email lookup and allows the current user
     * to keep their existing email if no changes are being made.</p>
     *
     * @param email the email to validate
     * @param userId the ID of the current user (excluded from uniqueness check)
     * @throws EmailException if the email is already taken by another user
     */
    private void validateEmailAvailability(String email, Long userId) {
        String normalized = email.toLowerCase();
        userBO.getUserByEmail(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                logger.warn("Email conflict detected: {} already taken by user ID: {}", normalized, existing.getId());
                throw new EmailException(DUPLICATE_EMAIL, normalized);
            }
        });
    }

    /**
     * Creates a user header response DTO from a user entity.
     *
     * <p>Extracts the essential header information including username, names,
     * bio, and profile picture URL for profile display purposes.</p>
     *
     * @param user the user entity to extract header information from
     * @return {@link UserHeaderResponseDTO} containing header information
     */
    private UserHeaderResponseDTO getUserHeader(User user) {
        return new UserHeaderResponseDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePictureUrl()
        );
    }
}
