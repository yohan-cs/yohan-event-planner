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
 * Applies validated partial updates to an existing {@link User} entity based on data
 * from a {@link UserUpdateDTO} patch object.
 *
 * <p>
 * This handler is responsible for in-place updates to fields such as username,
 * email, first name, last name, timezone, and password. Fields are only updated if:
 * <ul>
 *   <li>The field in the DTO is non-null</li>
 *   <li>The new value differs from the current value</li>
 *   <li>All field-specific validations pass</li>
 * </ul>
 * </p>
 *
 * <p>
 * Updates are <strong>atomic</strong>: if any field fails validation
 * (e.g., duplicate username, email, or reused password), <em>no changes</em> are
 * applied to the user entity.
 * </p>
 *
 * <p>
 * This component performs:
 * <ul>
 *   <li>Username and email normalization and uniqueness validation</li>
 *   <li>Password duplication checks and rehashing if needed</li>
 *   <li>Conditional updates of all fields</li>
 * </ul>
 * </p>
 *
 * <p>
 * Authorization and persistence are the responsibility of the service layer.
 * This handler strictly performs validation and in-memory mutation of the domain object.
 * </p>
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
     * Validates and applies non-null, changed fields from the given DTO
     * to the specified {@link User} entity.
     *
     * <p>
     * All validations are performed first. If any validation fails, the method
     * throws an exception and no updates are applied. This ensures atomic patching.
     * </p>
     *
     * <p>
     * Fields updated include:
     * <ul>
     *   <li><b>Username:</b> normalized to lowercase; must be unique if changed</li>
     *   <li><b>Email:</b> normalized to lowercase; must be unique if changed</li>
     *   <li><b>Password:</b> must not match current hashed password; re-encoded if changed</li>
     *   <li><b>First name, last name, timezone:</b> replaced if different</li>
     * </ul>
     * </p>
     *
     * @param existingUser the user entity to apply updates to
     * @param dto the patch data (nullable fields allowed)
     * @return {@code true} if any field was updated; {@code false} if all values were unchanged
     * @throws UsernameException if the new username already exists
     * @throws EmailException if the new email already exists
     * @throws PasswordException if the new password matches the current one
     */


    public boolean applyPatch(User existingUser, UserUpdateDTO dto) {
        boolean updated = false;

        // Normalize inputs
        String normalizedUsername = dto.username() != null ? dto.username().toLowerCase() : null;
        String normalizedEmail = dto.email() != null ? dto.email().toLowerCase() : null;

        // === 1. VALIDATION PHASE ===

        if (normalizedUsername != null && !normalizedUsername.equals(existingUser.getUsername())) {
            if (userBO.existsByUsername(normalizedUsername)) {
                throw new UsernameException(DUPLICATE_USERNAME, dto.username());
            }
        }

        if (normalizedEmail != null && !normalizedEmail.equals(existingUser.getEmail())) {
            if (userBO.existsByEmail(normalizedEmail)) {
                throw new EmailException(DUPLICATE_EMAIL, dto.email());
            }
        }

        if (dto.password() != null) {
            if (passwordBO.isMatch(dto.password(), existingUser.getHashedPassword())) {
                throw new PasswordException(ErrorCode.DUPLICATE_PASSWORD);
            }
        }

        // === 2. UPDATE PHASE ===

        if (normalizedUsername != null && !normalizedUsername.equals(existingUser.getUsername())) {
            existingUser.setUsername(normalizedUsername);
            updated = true;
        }

        if (dto.password() != null) {
            existingUser.setHashedPassword(passwordBO.encryptPassword(dto.password()));
            updated = true;
        }

        if (normalizedEmail != null && !normalizedEmail.equals(existingUser.getEmail())) {
            existingUser.setEmail(normalizedEmail);
            updated = true;
        }

        if (dto.firstName() != null && !dto.firstName().equals(existingUser.getFirstName())) {
            existingUser.setFirstName(dto.firstName());
            updated = true;
        }

        if (dto.lastName() != null && !dto.lastName().equals(existingUser.getLastName())) {
            existingUser.setLastName(dto.lastName());
            updated = true;
        }

        if (dto.timezone() != null && !dto.timezone().equals(existingUser.getTimezone())) {
            existingUser.setTimezone(dto.timezone());
            updated = true;
        }

        return updated;
    }
}
