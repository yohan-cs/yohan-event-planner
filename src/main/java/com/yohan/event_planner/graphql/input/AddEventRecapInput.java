package com.yohan.event_planner.graphql.input;

import java.util.List;

/**
 * Input for creating a new event recap.
 * 
 * Event recaps allow users to document completed events with notes and media attachments.
 * They can be created in an unconfirmed state for events that haven't been officially completed.
 * 
 * @param eventId The ID of the event to create a recap for (required)
 * @param recapName Optional name/title for the recap
 * @param notes Optional notes or description for the recap
 * @param isUnconfirmed Whether this recap should be marked as unconfirmed
 * @param media Optional list of media items to attach to the recap
 */
public record AddEventRecapInput(
        String eventId,
        String recapName,
        String notes,
        Boolean isUnconfirmed,
        List<CreateRecapMediaInput> media
) {}
