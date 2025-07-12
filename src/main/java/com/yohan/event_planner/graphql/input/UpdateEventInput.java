package com.yohan.event_planner.graphql.input;

/**
 * Input for updating event properties with selective field updates.
 * 
 * Uses {@link UpdateFieldInput} wrapper to distinguish between omitted fields
 * and explicitly provided values (including null). This enables partial updates
 * where only specified fields are modified.
 * 
 * @param name Event name (optional update)
 * @param startTime Event start time in user's timezone (optional update)
 * @param endTime Event end time in user's timezone (optional update)
 * @param description Event description (optional update)
 * @param labelId Associated label ID (optional update)
 * @param isCompleted Whether event is completed (optional update)
 */
public record UpdateEventInput(
        UpdateFieldInput<String> name,
        UpdateFieldInput<String> startTime,
        UpdateFieldInput<String> endTime,
        UpdateFieldInput<String> description,
        UpdateFieldInput<String> labelId,
        UpdateFieldInput<Boolean> isCompleted
) {}
