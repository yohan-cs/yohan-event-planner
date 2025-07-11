package com.yohan.event_planner.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength requirements.
 *
 * <p>
 * This annotation validates that a password meets comprehensive security requirements
 * including length, character diversity, and common password detection. It ensures
 * that passwords provide adequate protection against brute-force and dictionary attacks.
 * </p>
 *
 * <h2>Password Requirements</h2>
 * <ul>
 *   <li><strong>Length</strong>: 8-72 characters (accommodates bcrypt limits)</li>
 *   <li><strong>Uppercase Letters</strong>: At least 1 uppercase letter (A-Z)</li>
 *   <li><strong>Lowercase Letters</strong>: At least 1 lowercase letter (a-z)</li>
 *   <li><strong>Numbers</strong>: At least 1 digit (0-9)</li>
 *   <li><strong>Special Characters</strong>: At least 1 special character (!@#$%^&*)</li>
 *   <li><strong>No Common Patterns</strong>: Prevents obviously weak passwords</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Character Diversity</strong>: Requires multiple character types</li>
 *   <li><strong>Common Password Detection</strong>: Blocks well-known weak passwords</li>
 *   <li><strong>Pattern Prevention</strong>: Prevents sequential and repeated patterns</li>
 *   <li><strong>Unicode Support</strong>: Handles international characters properly</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * public class UserCreateDTO {
 *     @ValidPassword
 *     private String password;
 * }
 * }</pre>
 *
 * <h2>Valid Password Examples</h2>
 * <ul>
 *   <li>{@code MySecureP@ssw0rd}</li>
 *   <li>{@code C0mpl3x!Password}</li>
 *   <li>{@code Str0ng&S@fe123}</li>
 * </ul>
 *
 * <h2>Invalid Password Examples</h2>
 * <ul>
 *   <li>{@code password123} - No uppercase or special characters</li>
 *   <li>{@code PASSWORD123} - No lowercase letters</li>
 *   <li>{@code MyPassword} - No numbers or special characters</li>
 *   <li>{@code 12345678} - No letters</li>
 *   <li>{@code password} - Too common</li>
 * </ul>
 *
 * @see PasswordValidator
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    /**
     * The error message to display when validation fails.
     *
     * @return the validation error message
     */
    String message() default "Password must be 8-72 characters long and contain at least one uppercase letter, " +
                             "one lowercase letter, one number, and one special character (!@#$%^&*)";

    /**
     * Validation groups for conditional validation.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload for validation metadata.
     *
     * @return the validation payload
     */
    Class<? extends Payload>[] payload() default {};
}