package com.yohan.event_planner.graphql.input;

import java.util.List;

public record AddEventRecapInput(
        String eventId,
        String recapName,
        String notes,
        Boolean isUnconfirmed,
        List<CreateRecapMediaInput> media
) {}
