package com.yohan.event_planner.graphql.input;

import java.util.List;

/**
 * Input for updating existing event recap properties.
 * 
 * Allows modification of recap notes and replacement of the entire media list.
 * All fields are optional - only provided fields will be updated.
 * 
 * @param notes Updated notes or description for the recap (optional)
 * @param media Updated list of media items (optional, replaces existing media)
 */
public record UpdateEventRecapInput(
        String notes,
        List<CreateRecapMediaInput> media
) {}
