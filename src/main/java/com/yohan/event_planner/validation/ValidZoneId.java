package com.yohan.event_planner.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation to verify that a field or parameter
 * represents a valid {@link java.time.ZoneId}.
 * <p>
 * Can be applied to fields or method parameters to ensure
 * the annotated value corresponds to a recognized time zone identifier.
 * </p>
 * <p>
 * Validation logic is implemented by {@link ZoneIdValidator}.
 * </p>
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ZoneIdValidator.class)
public @interface ValidZoneId {

    /**
     * Default error message returned when the annotated value
     * is not a valid time zone ID.
     *
     * @return the error message string
     */
    String message() default "Invalid ZoneId";

    /**
     * Allows grouping of constraints to selectively apply validations.
     *
     * @return an array of validation group classes
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients to associate metadata with a constraint.
     *
     * @return an array of payload classes
     */
    Class<? extends Payload>[] payload() default {};
}
