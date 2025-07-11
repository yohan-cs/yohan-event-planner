package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * Business operations interface for managing {@link User} entities.
 *
 * <p>
 * Defines core use cases related to user creation, updates, scheduled deletion,
 * and lookup operations. This layer encapsulates persistence coordination
 * and domain enforcement such as deletion scheduling and entity initialization.
 * </p>
 *
 * <h3>Design Notes:</h3>
 * <ul>
 *   <li>Assumes that the service layer has already validated all business rules,
 *       such as username/email availability, user ownership, and patch logic.</li>
 *   <li>This layer is not defensive and will not repeat validation performed upstream.</li>
 *   <li>Soft deletion is implemented using a grace period. Users are marked as
 *       pending deletion and scheduled for hard deletion by a background job.</li>
 *   <li>Intended for use by the service layer only; not exposed to controllers or external code.</li>
 * </ul>
 */
public interface UserBO {

    /**
     * Retrieves a user by their ID.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Basic user retrieval for authentication or profile operations
     * Optional<User> user = userBO.getUserById(123L);
     * if (user.isPresent()) {
     *     String username = user.get().getUsername();
     *     // Process user data...
     * }
     * 
     * // Common pattern for user validation in services
     * User user = userBO.getUserById(userId)
     *     .orElseThrow(() -> new UserNotFoundException(userId));
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method does NOT filter by pending deletion status,
     * meaning it can return users who are marked for deletion. Use this method when you need
     * to access users regardless of their deletion status (e.g., for cleanup operations).</p>
     *
     * @param userId the user ID
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserById(Long userId);

    /**
     * Retrieves a user by their unique username.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Authentication flow - check if user exists and is active
     * Optional<User> user = userBO.getUserByUsername("john_doe");
     * if (user.isPresent()) {
     *     // User exists and is not pending deletion
     *     authenticateUser(user.get(), providedPassword);
     * } else {
     *     throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ACCESS);
     * }
     * 
     * // Profile lookup by username for social features
     * String targetUsername = "alice_smith"; 
     * User targetUser = userBO.getUserByUsername(targetUsername)
     *     .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUsername));
     * return createUserProfileDTO(targetUser);
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method automatically excludes users who are 
     * marked for deletion ({@code isPendingDeletion = true}). Only active users are returned.</p>
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Retrieves a user by their unique email address.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Password reset flow - verify email exists for active user
     * Optional<User> user = userBO.getUserByEmail("user@example.com");
     * if (user.isPresent()) {
     *     emailService.sendPasswordResetEmail(user.get().getEmail(), resetToken);
     * } else {
     *     // Don't reveal whether email exists for security
     *     logger.info("Password reset attempted for unknown email");
     * }
     * 
     * // Email verification during registration
     * String newUserEmail = userCreateRequest.email();
     * if (userBO.getUserByEmail(newUserEmail).isPresent()) {
     *     throw new EmailException("Email already registered");
     * }
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method automatically excludes users who are 
     * marked for deletion ({@code isPendingDeletion = true}). Only active users are returned.</p>
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Retrieves all users with the given role.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Administrative operations - get all admin users
     * List<User> adminUsers = userBO.getUsersByRole(Role.ADMIN);
     * for (User admin : adminUsers) {
     *     emailService.sendMaintenanceNotification(admin.getEmail());
     * }
     * 
     * // Analytics and reporting
     * List<User> regularUsers = userBO.getUsersByRole(Role.USER);
     * int activeUserCount = regularUsers.size();
     * generateUserActivityReport(regularUsers);
     * 
     * // Role-based feature access
     * if (userBO.getUsersByRole(Role.MODERATOR).contains(currentUser)) {
     *     enableModerationFeatures();
     * }
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method automatically excludes users who are 
     * marked for deletion ({@code isPendingDeletion = true}). Only active users are returned.</p>
     *
     * @param role the role to filter by
     * @return a list of users matching the given role
     */
    List<User> getUsersByRole(Role role);

    /**
     * Retrieves all user records in the system.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // System administration - user migration or backup operations
     * List<User> allUsers = userBO.getAllUsers();
     * for (User user : allUsers) {
     *     migrateUserDataToNewFormat(user);
     * }
     * 
     * // Analytics and reporting - full user statistics
     * List<User> users = userBO.getAllUsers();
     * Map<String, Long> usersByTimezone = users.stream()
     *     .collect(Collectors.groupingBy(User::getTimezone, Collectors.counting()));
     * 
     * // Batch operations (with pagination for large datasets)
     * List<User> users = userBO.getAllUsers();
     * if (users.size() > 1000) {
     *     logger.warn("Large user dataset ({} users) - consider pagination", users.size());
     * }
     * sendWeeklyNewsletterToUsers(users);
     * }</pre>
     * 
     * <p><strong>Performance Warning:</strong> This method may return a large dataset.
     * Use with caution in performance-sensitive operations and consider implementing
     * pagination at the service layer for large user bases.</p>
     * 
     * <p><strong>Important Note:</strong> Excludes users marked for deletion 
     * ({@code isPendingDeletion = true}). Intended primarily for administrative 
     * or diagnostic workflows.</p>
     *
     * @return a list of all active {@link User} entities
     */
    List<User> getAllUsers();

    /**
     * Creates a new user.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // User registration flow
     * User newUser = new User("john_doe", hashedPassword, "john@example.com",
     *                        "John", "Doe", "America/New_York");
     * // Role.USER is added by default, but can add additional roles:
     * // newUser.addRole(Role.MODERATOR);
     * 
     * User savedUser = userBO.createUser(newUser);
     * logger.info("Created user with ID: {}", savedUser.getId());
     * 
     * // Administrative user creation
     * User adminUser = new User("admin_user", hashedPassword, "admin@company.com",
     *                          "Admin", "User", "UTC");
     * adminUser.addRole(Role.ADMIN);
     * adminUser.addRole(Role.MODERATOR);
     * User savedAdmin = userBO.createUser(adminUser);
     * }</pre>
     * 
     * <p><strong>Validation Note:</strong> This method assumes the entity is fully 
     * prepared and validated by the service layer. It performs no validation on
     * required fields, uniqueness constraints, or business rules.</p>
     *
     * @param user the prepared user entity to persist
     * @return the saved {@link User} with an assigned ID
     */
    User createUser(User user);

    /**
     * Updates an existing user.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Profile update after service layer validation
     * User existingUser = userBO.getUserById(userId).orElseThrow();
     * existingUser.setEmail("newemail@example.com");
     * existingUser.setTimezone("Europe/London");
     * User updatedUser = userBO.updateUser(existingUser);
     * 
     * // Role management
     * User user = userBO.getUserById(userId).orElseThrow();
     * user.addRole(Role.MODERATOR);
     * userBO.updateUser(user);
     * 
     * // Profile settings update
     * User user = userBO.getUserByUsername(username).orElseThrow();
     * user.setBio(newBio);
     * user.setProfilePictureUrl(newProfilePictureUrl);
     * User saved = userBO.updateUser(user);
     * }</pre>
     * 
     * <p><strong>Validation Note:</strong> This method assumes all patching and 
     * validation have been performed upstream. It performs no business rule 
     * validation or conflict detection.</p>
     *
     * @param user the updated user entity with a valid ID
     * @return the updated and saved {@link User}
     */
    User updateUser(User user);

    /**
     * Marks a user for deletion with a grace period.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Account deletion request from user
     * User user = userBO.getUserById(userId).orElseThrow();
     * userBO.markUserForDeletion(user);
     * emailService.sendDeletionNotification(user.getEmail(), user.getScheduledDeletionDate());
     * 
     * // Administrative account deletion upon user request
     * User targetUser = userBO.getUserByUsername("user123").orElseThrow();
     * userBO.markUserForDeletion(targetUser);
     * logger.info("User {} marked for deletion per administrative request", 
     *              targetUser.getUsername());
     * 
     * // Bulk cleanup of inactive accounts
     * List<User> inactiveUsers = findUsersInactiveSince(sixMonthsAgo);
     * for (User user : inactiveUsers) {
     *     userBO.markUserForDeletion(user);
     * }
     * }</pre>
     * 
     * <p><strong>Grace Period Behavior:</strong>
     * <ul>
     *   <li>Sets the user's pending deletion flag ({@code isPendingDeletion = true})</li>
     *   <li>Schedules deletion date 30 days from current time in user's timezone</li>
     *   <li>User remains accessible via {@link #getUserById(Long)} during grace period</li>
     *   <li>User is excluded from {@link #getUserByUsername(String)}, {@link #getUserByEmail(String)}, etc.</li>
     *   <li>A background job performs final deletion after the grace period</li>
     * </ul></p>
     * 
     * <p><strong>Timezone Handling:</strong> The deletion date is calculated using the 
     * user's timezone to ensure consistent behavior across different time zones.</p>
     *
     * @param user the user to mark for deletion
     */
    void markUserForDeletion(User user);

    /**
     * Checks if a user exists with the given username.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Registration validation - check username availability
     * String desiredUsername = userCreateRequest.username();
     * if (userBO.existsByUsername(desiredUsername)) {
     *     throw new UsernameException("Username already taken");
     * }
     * 
     * // Security audit - check for suspicious usernames
     * List<String> suspiciousPatterns = Arrays.asList("admin", "root", "test");
     * for (String pattern : suspiciousPatterns) {
     *     if (userBO.existsByUsername(pattern)) {
     *         securityLogger.warn("Suspicious username found: {}", pattern);
     *     }
     * }
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method checks ALL users in the system,
     * including those marked for deletion. This ensures username uniqueness is 
     * maintained even during the grace period.</p>
     *
     * @param username the username to check
     * @return {@code true} if a user with the username exists, otherwise {@code false}
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Registration validation - prevent duplicate emails
     * String emailAddress = userCreateRequest.email();
     * if (userBO.existsByEmail(emailAddress)) {
     *     throw new EmailException("Email already registered");
     * }
     * 
     * // Email change validation in profile updates
     * String newEmail = userUpdateRequest.email();
     * User currentUser = getCurrentUser();
     * if (!currentUser.getEmail().equals(newEmail) && userBO.existsByEmail(newEmail)) {
     *     throw new EmailException("Email already in use by another account");
     * }
     * 
     * // Account recovery flow
     * String recoveryEmail = forgotPasswordRequest.email();
     * if (!userBO.existsByEmail(recoveryEmail)) {
     *     // Don't reveal if email exists for security reasons
     *     logger.info("Password reset requested for non-existent email: {}", recoveryEmail);
     *     return new ForgotPasswordResponseDTO("If email exists, reset link sent");
     * }
     * }</pre>
     * 
     * <p><strong>Important Note:</strong> This method checks ALL users in the system,
     * including those marked for deletion. This ensures email uniqueness is 
     * maintained even during the grace period.</p>
     *
     * @param email the email address to check
     * @return {@code true} if a user with the email exists, otherwise {@code false}
     */
    boolean existsByEmail(String email);
}
