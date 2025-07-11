package com.yohan.event_planner.validation;

import com.yohan.event_planner.constants.ApplicationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for the {@link ValidPassword} annotation.
 *
 * <p>
 * This validator enforces comprehensive password security requirements including
 * character diversity, length constraints, and common password detection. It helps
 * prevent weak passwords that are vulnerable to brute-force and dictionary attacks.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Length</strong>: Must be between 8-72 characters</li>
 *   <li><strong>Uppercase</strong>: At least 1 uppercase letter (A-Z)</li>
 *   <li><strong>Lowercase</strong>: At least 1 lowercase letter (a-z)</li>
 *   <li><strong>Digits</strong>: At least 1 numeric digit (0-9)</li>
 *   <li><strong>Special Characters</strong>: At least 1 special character</li>
 *   <li><strong>No Common Passwords</strong>: Prevents use of well-known weak passwords</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Multi-layer Validation</strong>: Combines multiple security checks</li>
 *   <li><strong>Pattern Recognition</strong>: Detects and prevents common weak patterns</li>
 *   <li><strong>Comprehensive Coverage</strong>: Validates against known attack vectors</li>
 *   <li><strong>Performance Optimized</strong>: Efficient validation with early termination</li>
 * </ul>
 *
 * @see ValidPassword
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidator.class);

    // Pre-compiled regex patterns for performance
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    // Common passwords to reject (case-insensitive)
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "password123", "123456", "123456789", "qwerty", "qwertyui",
        "abc123", "password1", "admin", "root", "user", "guest", "welcome",
        "login", "passw0rd", "letmein", "monkey", "dragon", "sunshine",
        "master", "shadow", "football", "baseball", "superman", "batman",
        "trustno1", "hello", "welcome123", "admin123", "test", "testing",
        "changeme", "default", "secret", "temp", "temporary", "password!",
        "Password123", "Password123!", "p@ssw0rd", "P@ssw0rd", "P@ssword123"
    );

    /**
     * Initializes the validator with the annotation configuration.
     *
     * @param constraintAnnotation the password validation annotation
     */
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    /**
     * Validates a password against comprehensive security requirements.
     *
     * <p>
     * This method performs multiple validation checks to ensure the password
     * meets security standards. It validates length, character diversity,
     * and checks against common weak passwords.
     * </p>
     *
     * @param password the password to validate
     * @param context the validation context
     * @return true if the password is valid, false otherwise
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Allow null values (will be caught by @NotBlank)
        if (password == null) {
            return true;
        }

        // Check length constraints
        if (password.length() < ApplicationConstants.PASSWORD_MIN_LENGTH || 
            password.length() > ApplicationConstants.PASSWORD_MAX_LENGTH) {
            addCustomMessage(context, String.format(
                "Password must be between %d and %d characters long",
                ApplicationConstants.PASSWORD_MIN_LENGTH,
                ApplicationConstants.PASSWORD_MAX_LENGTH
            ));
            return false;
        }

        // Check for required character types
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            addCustomMessage(context, "Password must contain at least one uppercase letter (A-Z)");
            return false;
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            addCustomMessage(context, "Password must contain at least one lowercase letter (a-z)");
            return false;
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            addCustomMessage(context, "Password must contain at least one digit (0-9)");
            return false;
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            addCustomMessage(context, "Password must contain at least one special character (!@#$%^&* etc.)");
            return false;
        }

        // Check against common passwords
        if (isCommonPassword(password)) {
            addCustomMessage(context, "Password is too common and easily guessable. Please choose a more unique password");
            return false;
        }

        // Check for simple patterns
        if (hasSimplePatterns(password)) {
            addCustomMessage(context, "Password contains simple patterns that are easy to guess. Please use a more complex password");
            return false;
        }

        logger.debug("Password validation passed for user input");
        return true;
    }

    /**
     * Checks if the password is a commonly used weak password.
     *
     * @param password the password to check
     * @return true if the password is common, false otherwise
     */
    private boolean isCommonPassword(String password) {
        return COMMON_PASSWORDS.contains(password.toLowerCase());
    }

    /**
     * Checks for simple patterns that make passwords vulnerable.
     *
     * @param password the password to check
     * @return true if simple patterns are detected, false otherwise
     */
    private boolean hasSimplePatterns(String password) {
        String lower = password.toLowerCase();
        
        // Check for repeated characters (more than 2 consecutive)
        if (hasRepeatedCharacters(password, 3)) {
            return true;
        }

        // Check for sequential patterns
        if (hasSequentialPattern(lower)) {
            return true;
        }

        // Check for keyboard patterns
        if (hasKeyboardPattern(lower)) {
            return true;
        }

        return false;
    }

    /**
     * Checks for repeated characters in the password.
     *
     * @param password the password to check
     * @param maxRepeats the maximum allowed consecutive repeats
     * @return true if excessive repeats are found, false otherwise
     */
    private boolean hasRepeatedCharacters(String password, int maxRepeats) {
        int count = 1;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                count++;
                if (count >= maxRepeats) {
                    return true;
                }
            } else {
                count = 1;
            }
        }
        return false;
    }

    /**
     * Checks for sequential patterns in the password.
     *
     * @param password the password to check (lowercase)
     * @return true if sequential patterns are found, false otherwise
     */
    private boolean hasSequentialPattern(String password) {
        // Check for sequential numbers
        if (password.contains("123") || password.contains("234") || password.contains("345") ||
            password.contains("456") || password.contains("567") || password.contains("678") ||
            password.contains("789") || password.contains("890") || password.contains("012")) {
            return true;
        }

        // Check for sequential letters
        if (password.contains("abc") || password.contains("bcd") || password.contains("cde") ||
            password.contains("def") || password.contains("efg") || password.contains("fgh") ||
            password.contains("ghi") || password.contains("hij") || password.contains("ijk") ||
            password.contains("jkl") || password.contains("klm") || password.contains("lmn") ||
            password.contains("mno") || password.contains("nop") || password.contains("opq") ||
            password.contains("pqr") || password.contains("qrs") || password.contains("rst") ||
            password.contains("stu") || password.contains("tuv") || password.contains("uvw") ||
            password.contains("vwx") || password.contains("wxy") || password.contains("xyz")) {
            return true;
        }

        return false;
    }

    /**
     * Checks for keyboard patterns in the password.
     *
     * @param password the password to check (lowercase)
     * @return true if keyboard patterns are found, false otherwise
     */
    private boolean hasKeyboardPattern(String password) {
        // Check for common keyboard patterns
        String[] keyboardPatterns = {
            "qwe", "wer", "ert", "rty", "tyu", "yui", "uio", "iop",
            "asd", "sdf", "dfg", "fgh", "ghj", "hjk", "jkl",
            "zxc", "xcv", "cvb", "vbn", "bnm",
            "qaz", "wsx", "edc", "rfv", "tgb", "yhn", "ujm", "ik",
            "147", "258", "369", "159", "357"
        };

        for (String pattern : keyboardPatterns) {
            if (password.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a custom error message to the validation context.
     *
     * @param context the validation context
     * @param message the custom error message
     */
    private void addCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}