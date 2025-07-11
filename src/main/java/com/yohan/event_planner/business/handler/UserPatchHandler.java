package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.PasswordException;
import com.yohan.event_planner.exception.UsernameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME;


/**
 * Handles partial updates to {@link User} entities using atomic patch semantics.
 * 
 * <p>This handler implements field patching patterns consistent with other domain handlers
 * ({@link EventPatchHandler}, {@link RecurringEventPatchHandler}) but uses standard
 * null-check semantics rather than Optional-based skip/clear semantics.</p>
 * 
 * <h3>Architecture Context</h3>
 * <p><strong>Layer Responsibilities:</strong>
 * <ul>
 *   <li><strong>UserService</strong>: Authorization, business validation, transaction management</li>
 *   <li><strong>This Handler</strong>: Field-level validation, atomic patching, normalization</li>
 *   <li><strong>UserBO</strong>: Uniqueness checks, persistence operations</li>
 *   <li><strong>PasswordBO</strong>: Password encryption and validation</li>
 * </ul>
 * 
 * <h3>Patch Semantics</h3>
 * <p>Unlike event handlers that use Optional-based skip/clear semantics, user patching
 * uses simpler null-based conditional updates:</p>
 * <ul>
 *   <li><strong>Skip</strong>: DTO field is null → no change to user field</li>
 *   <li><strong>Update</strong>: DTO field is non-null and different → field updated</li>
 * </ul>
 * 
 * <h3>Atomic Operation Guarantee</h3>
 * <p>This handler implements strict atomic semantics using a two-phase approach:</p>
 * <ol>
 *   <li><strong>Validation Phase</strong>: All field validations performed upfront</li>
 *   <li><strong>Update Phase</strong>: Changes applied only if all validations pass</li>
 * </ol>
 * 
 * <p>If any validation fails, no changes are applied to the user entity, ensuring
 * data consistency and preventing partial updates.</p>
 * 
 * <h3>Validation Strategy</h3>
 * <p>The handler performs field-specific validations while delegating to specialized components:</p>
 * <ul>
 *   <li><strong>Username uniqueness</strong>: Via {@link UserBO#existsByUsername(String)}</li>
 *   <li><strong>Email uniqueness</strong>: Via {@link UserBO#existsByEmail(String)}</li>
 *   <li><strong>Password duplication prevention</strong>: Via {@link PasswordBO#isMatch(String, String)}</li>
 *   <li><strong>Input normalization</strong>: Username and email converted to lowercase</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> Bean validation (size limits, format validation) is handled
 * at the controller/service layer. This handler focuses on business-specific validations
 * that require domain knowledge or external dependencies.</p>
 * 
 * @since 1.0
 * @see UserService
 * @see UserUpdateDTO
 * @see User
 */


@Component
public class UserPatchHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserPatchHandler.class);
    private final PasswordBO passwordBO;
    private final UserBO userBO;

    public UserPatchHandler(PasswordBO passwordBO, UserBO userBO) {
        this.passwordBO = passwordBO;
        this.userBO = userBO;
    }

    /**
     * Validates and applies partial updates from a {@link UserUpdateDTO} to an existing user.
     * 
     * <p>This method implements atomic patch semantics with two distinct phases:</p>
     * 
     * <h3>Phase 1: Validation</h3>
     * <p>All field validations are performed before any updates:</p>
     * <ul>
     *   <li><strong>Username uniqueness</strong>: Via {@link UserBO#existsByUsername(String)}</li>
     *   <li><strong>Email uniqueness</strong>: Via {@link UserBO#existsByEmail(String)}</li>
     *   <li><strong>Password duplication prevention</strong>: Via {@link PasswordBO#isMatch(String, String)}</li>
     * </ul>
     * 
     * <h3>Phase 2: Conditional Updates</h3>
     * <p>Fields are updated only if they are non-null and represent actual changes:</p>
     * <ul>
     *   <li><strong>Username, Email</strong>: Normalized to lowercase before comparison and update</li>
     *   <li><strong>Password</strong>: Re-encrypted via {@link PasswordBO#encryptPassword(String)}</li>
     *   <li><strong>Personal fields</strong>: Direct replacement if different (firstName, lastName, timezone)</li>
     * </ul>
     * 
     * <h3>Atomic Guarantee</h3>
     * <p>If any validation fails, the method throws an exception and <strong>no changes</strong>
     * are applied to the user entity. This prevents partial updates and maintains data consistency.</p>
     * 
     * @param existingUser the user entity to patch (must not be null)
     * @param dto the patch data with optional fields (must not be null)
     * @return true if any field was modified, false if no changes were made
     * @throws UsernameException if username is taken by another user
     * @throws EmailException if email is taken by another user  
     * @throws PasswordException if new password matches current password
     */


    public boolean applyPatch(User existingUser, UserUpdateDTO dto) {
        logger.debug("Applying patch to user ID {}", existingUser.getId());
        
        // Normalize case-sensitive fields for consistent comparison and storage
        String normalizedUsername = dto.username() != null ? dto.username().toLowerCase() : null;
        String normalizedEmail = dto.email() != null ? dto.email().toLowerCase() : null;

        // Phase 1: Validate all changes before applying any (atomic semantics)
        logger.debug("Starting validation phase for user ID {}", existingUser.getId());
        validatePatchFields(existingUser, dto, normalizedUsername, normalizedEmail);
        logger.debug("Validation phase completed for user ID {}", existingUser.getId());

        // Phase 2: Apply all validated changes
        logger.debug("Starting update phase for user ID {}", existingUser.getId());
        boolean updated = applyValidatedUpdates(existingUser, dto, normalizedUsername, normalizedEmail);
        
        if (updated) {
            logger.debug("User ID {} was modified during patch operation", existingUser.getId());
        } else {
            logger.debug("No changes made to user ID {} during patch operation", existingUser.getId());
        }
        
        return updated;
    }

    /**
     * Validates all fields that will be updated, ensuring atomic patch semantics.
     * 
     * <p>This method performs all validations upfront before any entity modifications.
     * If any validation fails, an exception is thrown and no changes are applied
     * to the user entity.</p>
     * 
     * @param existingUser the current user state
     * @param dto the patch data
     * @param normalizedUsername normalized username (null if not changing)
     * @param normalizedEmail normalized email (null if not changing)
     * @throws UsernameException if username validation fails
     * @throws EmailException if email validation fails
     * @throws PasswordException if password validation fails
     */
    private void validatePatchFields(User existingUser, UserUpdateDTO dto, 
                                   String normalizedUsername, String normalizedEmail) {
        // Username uniqueness validation
        if (normalizedUsername != null && !normalizedUsername.equals(existingUser.getUsername())) {
            logger.debug("Validating username uniqueness for user ID {}: {}", existingUser.getId(), normalizedUsername);
            if (userBO.existsByUsername(normalizedUsername)) {
                logger.warn("Username already exists for user ID {}: {}", existingUser.getId(), normalizedUsername);
                throw new UsernameException(DUPLICATE_USERNAME, dto.username());
            }
        }

        // Email uniqueness validation
        if (normalizedEmail != null && !normalizedEmail.equals(existingUser.getEmail())) {
            logger.debug("Validating email uniqueness for user ID {}: {}", existingUser.getId(), normalizedEmail);
            if (userBO.existsByEmail(normalizedEmail)) {
                logger.warn("Email already exists for user ID {}: {}", existingUser.getId(), normalizedEmail);
                throw new EmailException(DUPLICATE_EMAIL, dto.email());
            }
        }

        // Password duplication prevention
        if (dto.password() != null) {
            logger.debug("Validating password is different for user ID {}", existingUser.getId());
            if (passwordBO.isMatch(dto.password(), existingUser.getHashedPassword())) {
                logger.warn("New password matches current password for user ID {}", existingUser.getId());
                throw new PasswordException(ErrorCode.DUPLICATE_PASSWORD);
            }
        }
    }

    /**
     * Applies validated field updates to the user entity.
     * 
     * <p>This method is called only after all validations have passed, ensuring
     * that any changes made here will not be rolled back due to validation failures.</p>
     * 
     * @param existingUser the user to update
     * @param dto the patch data
     * @param normalizedUsername normalized username (null if not changing)
     * @param normalizedEmail normalized email (null if not changing)
     * @return true if any field was updated
     */
    private boolean applyValidatedUpdates(User existingUser, UserUpdateDTO dto,
                                        String normalizedUsername, String normalizedEmail) {
        boolean updated = false;

        // Apply credential updates (username, email, password)
        updated = applyCredentialUpdates(existingUser, dto, normalizedUsername, normalizedEmail) || updated;
        
        // Apply personal information updates (firstName, lastName, timezone)
        updated = applyPersonalUpdates(existingUser, dto) || updated;

        return updated;
    }

    /**
     * Updates username, email, and password fields.
     * 
     * @param existingUser the user to update
     * @param dto the patch data
     * @param normalizedUsername normalized username (null if not changing)
     * @param normalizedEmail normalized email (null if not changing)
     * @return true if any credential field was updated
     */
    private boolean applyCredentialUpdates(User existingUser, UserUpdateDTO dto,
                                         String normalizedUsername, String normalizedEmail) {
        boolean updated = false;

        if (normalizedUsername != null && !normalizedUsername.equals(existingUser.getUsername())) {
            logger.info("Updating username for user {}: [{}] -> [{}]", 
                    existingUser.getId(), existingUser.getUsername(), normalizedUsername);
            existingUser.setUsername(normalizedUsername);
            updated = true;
        }

        if (dto.password() != null) {
            logger.info("Updating password for user ID {}", existingUser.getId());
            existingUser.setHashedPassword(passwordBO.encryptPassword(dto.password()));
            updated = true;
        }

        if (normalizedEmail != null && !normalizedEmail.equals(existingUser.getEmail())) {
            logger.info("Updating email for user {}: [{}] -> [{}]", 
                    existingUser.getId(), existingUser.getEmail(), normalizedEmail);
            existingUser.setEmail(normalizedEmail);
            updated = true;
        }

        return updated;
    }

    /**
     * Updates personal information fields (firstName, lastName, timezone).
     * 
     * @param existingUser the user to update
     * @param dto the patch data
     * @return true if any personal field was updated
     */
    private boolean applyPersonalUpdates(User existingUser, UserUpdateDTO dto) {
        boolean updated = false;

        if (dto.firstName() != null && !dto.firstName().equals(existingUser.getFirstName())) {
            logger.debug("Updating first name for user {}: [{}] -> [{}]", 
                    existingUser.getId(), existingUser.getFirstName(), dto.firstName());
            existingUser.setFirstName(dto.firstName());
            updated = true;
        }

        if (dto.lastName() != null && !dto.lastName().equals(existingUser.getLastName())) {
            logger.debug("Updating last name for user {}: [{}] -> [{}]", 
                    existingUser.getId(), existingUser.getLastName(), dto.lastName());
            existingUser.setLastName(dto.lastName());
            updated = true;
        }

        if (dto.timezone() != null && !dto.timezone().equals(existingUser.getTimezone())) {
            logger.info("Updating timezone for user {}: [{}] -> [{}]", 
                    existingUser.getId(), existingUser.getTimezone(), dto.timezone());
            existingUser.setTimezone(dto.timezone());
            updated = true;
        }

        return updated;
    }
}
