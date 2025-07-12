package com.yohan.event_planner.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Custom validator for timezone ID validation using Jakarta Bean Validation.
 * 
 * <p>
 * This validator ensures that string values represent valid IANA timezone identifiers
 * as recognized by {@link java.time.ZoneId}. It is designed to work with the
 * {@link ValidZoneId} annotation on DTO fields requiring timezone validation.
 * </p>
 *
 * <h2>Architecture Context</h2>
 * <p>
 * This validator operates at the validation layer and is used primarily by:
 * <ul>
 *   <li>{@link com.yohan.event_planner.dto.UserCreateDTO} - User registration</li>
 *   <li>{@link com.yohan.event_planner.dto.UserUpdateDTO} - Profile updates</li>
 * </ul>
 * </p>
 *
 * <h2>Validation Strategy</h2>
 * <p>
 * Follows the "fail-fast" principle where null/blank values are considered valid,
 * delegating null checks to dedicated annotations like {@code @NotBlank}.
 * </p>
 *
 * <h2>Validation Logic</h2>
 * <ul>
 *   <li>Returns {@code true} if the input is {@code null} or blank,
 *       deferring null/blank checks to annotations like {@link jakarta.validation.constraints.NotBlank}</li>
 *   <li>Attempts to parse the input string using {@link ZoneId#of(String)};
 *       returns {@code true} if parsing succeeds</li>
 *   <li>Returns {@code false} if parsing fails, indicating an invalid time zone ID</li>
 * </ul>
 *
 * <p>
 * Intended for use on {@code String} fields or parameters representing IANA time zone IDs
 * such as "America/New_York", "Europe/London", or "UTC".
 * </p>
 *
 * @see ValidZoneId
 * @see java.time.ZoneId
 * @author Event Planner Development Team
 * @since 2.0.0
 */
public class ZoneIdValidator implements ConstraintValidator<ValidZoneId, String> {

    private static final Logger logger = LoggerFactory.getLogger(ZoneIdValidator.class);

    /**
     * Initializes the validator instance.
     * 
     * <p>
     * No configuration is required for timezone validation as it relies entirely
     * on the standard {@link ZoneId#of(String)} method for validation logic.
     * </p>
     *
     * @param constraintAnnotation the {@link ValidZoneId} annotation instance 
     *                            containing validation configuration
     */
    @Override
    public void initialize(ValidZoneId constraintAnnotation) {
        // No initialization needed
    }

    /**
     * Validates that the given string is a valid time zone ID.
     *
     * @param zoneIdStr the time zone ID string to validate; may be {@code null} or blank
     * @param context   the context in which the constraint is evaluated (unused)
     * @return {@code true} if the input is {@code null}, blank, or a valid time zone ID; {@code false} otherwise
     */
    @Override
    public boolean isValid(String zoneIdStr, ConstraintValidatorContext context) {
        if (zoneIdStr == null || zoneIdStr.isBlank()) {
            logger.debug("Timezone validation passed - null/blank value accepted");
            return true;
        }

        try {
            ZoneId.of(zoneIdStr);
            logger.debug("Timezone validation successful for zone: {}", zoneIdStr);
            return true;
        } catch (DateTimeException e) {
            logger.debug("Timezone validation failed for zone '{}': {}", zoneIdStr, e.getMessage());
            return false;
        }
    }
}
