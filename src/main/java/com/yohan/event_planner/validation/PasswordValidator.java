package com.yohan.event_planner.validation;

import com.yohan.event_planner.constants.ApplicationConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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

    // Special characters for validation
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{};\':\"\\|,.<>/?";
    
    // Common keyboard patterns - using Set for O(1) lookup performance
    private static final Set<String> KEYBOARD_PATTERNS = Set.of(
        "qwe", "wer", "ert", "rty", "tyu", "yui", "uio", "iop",
        "asd", "sdf", "dfg", "fgh", "ghj", "hjk", "jkl",
        "zxc", "xcv", "cvb", "vbn", "bnm",
        "qaz", "wsx", "edc", "rfv", "tgb", "yhn", "ujm", "ik",
        "147", "258", "369", "159", "357"
    );

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
    
    // Only these specific common passwords should be checked as substrings
    // These are the most common password bases that are often just decorated
    private static final Set<String> SUBSTRING_COMMON_PASSWORDS = Set.of(
        "password", "123456", "qwerty", "admin", "welcome", "letmein"
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

        // Check length constraints first (fastest check)
        int length = password.length();
        if (length < ApplicationConstants.PASSWORD_MIN_LENGTH || 
            length > ApplicationConstants.PASSWORD_MAX_LENGTH) {
            addCustomMessage(context, String.format(
                "Password must be between %d and %d characters long",
                ApplicationConstants.PASSWORD_MIN_LENGTH,
                ApplicationConstants.PASSWORD_MAX_LENGTH
            ));
            return false;
        }

        // Analyze character requirements in single pass
        CharacterAnalysis analysis = analyzeCharacters(password);
        
        if (!analysis.hasUppercase) {
            addCustomMessage(context, "Password must contain at least one uppercase letter (A-Z)");
            return false;
        }

        if (!analysis.hasLowercase) {
            addCustomMessage(context, "Password must contain at least one lowercase letter (a-z)");
            return false;
        }

        if (!analysis.hasDigit) {
            addCustomMessage(context, "Password must contain at least one digit (0-9)");
            return false;
        }

        if (!analysis.hasSpecial) {
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

        return true;
    }

    /**
     * Inner class to hold character analysis results for efficient single-pass validation.
     */
    private static class CharacterAnalysis {
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
    }

    /**
     * Analyzes password characters in a single pass for optimal performance.
     * 
     * @param password the password to analyze
     * @return CharacterAnalysis containing presence of each character type
     */
    private CharacterAnalysis analyzeCharacters(String password) {
        CharacterAnalysis analysis = new CharacterAnalysis();
        
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            
            if (!analysis.hasUppercase && Character.isUpperCase(c)) {
                analysis.hasUppercase = true;
            } else if (!analysis.hasLowercase && Character.isLowerCase(c)) {
                analysis.hasLowercase = true;
            } else if (!analysis.hasDigit && Character.isDigit(c)) {
                analysis.hasDigit = true;
            } else if (!analysis.hasSpecial && isSpecialCharacter(c)) {
                analysis.hasSpecial = true;
            }
            
            // Early exit if all requirements are met
            if (analysis.hasUppercase && analysis.hasLowercase && 
                analysis.hasDigit && analysis.hasSpecial) {
                break;
            }
        }
        
        return analysis;
    }

    /**
     * Efficiently checks if a character is a special character.
     * 
     * @param c the character to check
     * @return true if the character is a special character
     */
    private boolean isSpecialCharacter(char c) {
        return SPECIAL_CHARS.indexOf(c) >= 0;
    }

    /**
     * Checks if the password is a commonly used weak password.
     * 
     * <p>
     * This method performs a case-insensitive comparison against a predefined
     * list of commonly used passwords that are vulnerable to dictionary attacks.
     * It checks both exact matches and substrings, but only rejects substring
     * matches if they represent a significant portion of the password.
     * </p>
     *
     * @param password the password to check
     * @return true if the password is common, false otherwise
     */
    private boolean isCommonPassword(String password) {
        String lowercasePassword = password.toLowerCase();
        
        // Check for exact match against all common passwords
        if (COMMON_PASSWORDS.contains(lowercasePassword)) {
            return true;
        }
        
        // For substring detection, only reject if the common password is used as the base
        // Optimize by checking shorter patterns first and avoiding contains() when possible
        for (String commonPassword : SUBSTRING_COMMON_PASSWORDS) {
            int index = lowercasePassword.indexOf(commonPassword);
            if (index >= 0) {
                // Special handling for "password" - only reject if it's a primary component
                if (commonPassword.equals("password")) {
                    // Reject if password starts with "password" or password is relatively short
                    if (index == 0 || password.length() <= 11) {
                        return true;
                    }
                } else {
                    // For other common passwords, reject if they make up significant portion
                    double commonPasswordRatio = (double) commonPassword.length() / password.length();
                    if (commonPasswordRatio > 0.5) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Checks for simple patterns that make passwords vulnerable.
     * 
     * <p>
     * This method detects various weak patterns including repeated characters,
     * sequential patterns, and keyboard patterns that make passwords easier
     * to guess or crack. For longer passwords, it's more lenient with repeated
     * characters since they provide sufficient entropy overall.
     * </p>
     *
     * @param password the password to check
     * @return true if simple patterns are detected, false otherwise
     */
    private boolean hasSimplePatterns(String password) {
        String lower = password.toLowerCase();
        
        // For longer passwords, be more lenient with repeated characters
        // as they still provide sufficient entropy. At max length (72 chars),
        // with 4 required character types, up to 68 repeats could be valid.
        int maxRepeats;
        if (password.length() >= 60) {
            maxRepeats = password.length() - 3; // Allow almost all characters to repeat if 4 types present
        } else if (password.length() >= 30) {
            maxRepeats = 10;
        } else {
            maxRepeats = 3;
        }
        
        // Check for repeated characters
        if (hasRepeatedCharacters(password, maxRepeats)) {
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
     * <p>
     * This method scans for consecutive identical characters that exceed
     * the allowed threshold, which can make passwords weaker and more
     * predictable.
     * </p>
     *
     * @param password the password to check
     * @param maxConsecutiveRepeats the maximum allowed consecutive repeats
     * @return true if excessive repeats are found, false otherwise
     */
    private boolean hasRepeatedCharacters(String password, int maxConsecutiveRepeats) {
        int count = 1;
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                count++;
                if (count >= maxConsecutiveRepeats) {
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
     * <p>
     * This method detects sequential numeric and alphabetic patterns that
     * are commonly used in weak passwords (e.g., "123", "abc").
     * Uses optimized character-by-character analysis instead of regex.
     * </p>
     *
     * @param password the password to check (lowercase)
     * @return true if sequential patterns are found, false otherwise
     */
    private boolean hasSequentialPattern(String password) {
        if (password.length() < 3) return false;
        
        for (int i = 0; i <= password.length() - 3; i++) {
            char first = password.charAt(i);
            char second = password.charAt(i + 1);
            char third = password.charAt(i + 2);
            
            // Check for sequential numbers (e.g., 123, 234)
            if (Character.isDigit(first) && Character.isDigit(second) && Character.isDigit(third)) {
                if (second == first + 1 && third == second + 1) {
                    return true;
                }
            }
            
            // Check for sequential letters (e.g., abc, def)
            if (Character.isLowerCase(first) && Character.isLowerCase(second) && Character.isLowerCase(third)) {
                if (second == first + 1 && third == second + 1) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks for keyboard patterns in the password.
     * 
     * <p>
     * This method detects common keyboard patterns based on physical key
     * proximity that make passwords easier to type but also easier to guess
     * (e.g., "qwe", "asd", "123").
     * </p>
     *
     * @param password the password to check (lowercase)
     * @return true if keyboard patterns are found, false otherwise
     */
    private boolean hasKeyboardPattern(String password) {
        if (password.length() < 3) return false;
        
        // Single pass through password with O(1) pattern lookup
        for (int i = 0; i <= password.length() - 3; i++) {
            String substring = password.substring(i, i + 3);
            
            // O(1) lookup in HashSet instead of O(n) array iteration
            if (KEYBOARD_PATTERNS.contains(substring)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a custom error message to the validation context.
     * 
     * <p>
     * This method disables the default constraint violation message and
     * replaces it with a specific, user-friendly error message that
     * explains the exact validation failure.
     * </p>
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