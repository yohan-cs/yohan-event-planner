package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.util.List;

public record DayViewDTO(
        LocalDate date,
        List<EventResponseDTO> events
) {}