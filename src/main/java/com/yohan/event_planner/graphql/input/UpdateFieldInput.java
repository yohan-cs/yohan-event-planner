package com.yohan.event_planner.graphql.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper type to distinguish:
 * - Omitted fields (present = false)
 * - Explicitly provided values, including null (present = true, value = null or non-null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateFieldInput<T> {

    private final boolean present;
    private final T value;

    @JsonCreator
    public UpdateFieldInput(@JsonProperty("value") T value) {
        this.present = true;
        this.value = value;
    }

    private UpdateFieldInput() {
        this.present = false;
        this.value = null;
    }

    public static <T> UpdateFieldInput<T> absent() {
        return new UpdateFieldInput<>();
    }

    public boolean isPresent() {
        return present;
    }

    public T getValue() {
        return value;
    }
}
