package com.yohan.event_planner.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

/**
 * Validator implementation for the {@link ValidZoneId} annotation.
 * <p>
 * Checks whether a given {@link String} represents a valid time zone ID
 * as defined by {@link java.time.ZoneId}.
 * </p>
 *
 * <p>
 * Validation logic:
 * <ul>
 *   <li>Returns {@code true} if the input is {@code null} or blank,
 *       deferring null/blank checks to annotations like {@link jakarta.validation.constraints.NotBlank}.</li>
 *   <li>Attempts to parse the input string using {@link ZoneId#of(String)};
 *       returns {@code true} if parsing succeeds.</li>
 *   <li>Returns {@code false} if parsing fails, indicating an invalid time zone ID.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Intended for use on {@code String} fields or parameters representing time zone IDs.
 * </p>
 */
public class ZoneIdValidator implements ConstraintValidator<ValidZoneId, String> {

    /**
     * No initialization required for this validator.
     *
     * @param constraintAnnotation the annotation instance for a given constraint declaration
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
            return true;
        }

        try {
            ZoneId.of(zoneIdStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
