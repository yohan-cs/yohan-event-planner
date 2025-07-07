package com.yohan.event_planner.graphql.input;

import java.util.List;

public record UpdateEventRecapInput(
        String notes,
        List<CreateRecapMediaInput> media
) {}
