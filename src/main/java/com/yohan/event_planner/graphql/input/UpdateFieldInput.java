package com.yohan.event_planner.graphql.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic wrapper for optional field updates in GraphQL mutations.
 * 
 * This class enables partial updates by distinguishing between:
 * - Omitted fields (present = false) - field should not be updated
 * - Explicitly provided values (present = true) - field should be updated, even if null
 * 
 * This pattern allows clients to specify only the fields they want to update,
 * while still supporting setting fields to null when explicitly requested.
 * 
 * @param <T> the type of the wrapped value
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateFieldInput<T> {

    private final boolean present;
    private final T value;

    /**
     * Creates an UpdateFieldInput with a provided value.
     * This indicates the field should be updated to the given value.
     * 
     * @param value the value to update to (can be null)
     */
    @JsonCreator
    public UpdateFieldInput(@JsonProperty("value") T value) {
        this.present = true;
        this.value = value;
    }

    /**
     * Private constructor for absent fields.
     */
    private UpdateFieldInput() {
        this.present = false;
        this.value = null;
    }

    /**
     * Creates an UpdateFieldInput representing an omitted field.
     * This indicates the field should not be updated.
     * 
     * @param <T> the type of the field value
     * @return an absent UpdateFieldInput
     */
    public static <T> UpdateFieldInput<T> absent() {
        return new UpdateFieldInput<>();
    }

    /**
     * Checks if this field was explicitly provided in the input.
     * 
     * @return true if the field was provided (even if null), false if omitted
     */
    public boolean isPresent() {
        return present;
    }

    /**
     * Gets the provided value for this field.
     * Only meaningful if isPresent() returns true.
     * 
     * @return the field value (can be null)
     */
    public T getValue() {
        return value;
    }
}
