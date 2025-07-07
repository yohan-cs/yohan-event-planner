package com.yohan.event_planner.graphql.input;

/**
 * GraphQL input type for updating an event.
 * Uses {@link UpdateFieldInput} to distinguish omitted vs provided fields.
 */
public record UpdateEventInput(
        UpdateFieldInput<String> name,
        UpdateFieldInput<String> startTime,
        UpdateFieldInput<String> endTime,
        UpdateFieldInput<String> description,
        UpdateFieldInput<String> labelId,
        UpdateFieldInput<Boolean> isCompleted
) {}
