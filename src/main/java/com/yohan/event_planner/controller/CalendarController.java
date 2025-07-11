package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.dto.MonthlyCalendarResponseDTO;
import com.yohan.event_planner.service.MonthlyCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Calendar", description = "Calendar views and analytics")
@RestController
@RequestMapping("/calendar")
@SecurityRequirement(name = "Bearer Authentication")
public class CalendarController {

    private final MonthlyCalendarService monthlyCalendarService;

    public CalendarController(MonthlyCalendarService monthlyCalendarService) {
        this.monthlyCalendarService = monthlyCalendarService;
    }

    @Operation(
            summary = "Get monthly calendar view",
            description = "Retrieve calendar view for a specific month with optional label filtering and time statistics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Monthly calendar view retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MonthlyCalendarResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "404", description = "Label not found (if labelId provided)")
    })
    @GetMapping
    public ResponseEntity<MonthlyCalendarResponseDTO> getMonthlyCalendarView(
            @Parameter(description = "Filter by specific label ID (optional)")
            @RequestParam(value = "labelId", required = false) Long labelId,
            @Parameter(description = "Year to view (defaults to current year if not specified)")
            @RequestParam(value = "year", required = false) Integer year,
            @Parameter(description = "Month to view (1-12, defaults to current month if not specified)")
            @RequestParam(value = "month", required = false) Integer month
    ) {
        List<String> eventDates;
        LabelMonthStatsDTO stats = null;

        if (labelId == null) {
            List<LocalDate> datesWithEvents = monthlyCalendarService.getDatesWithEventsByMonth(year, month);
            eventDates = datesWithEvents.stream()
                    .map(LocalDate::toString)
                    .collect(Collectors.toList());
        } else {
            List<LocalDate> datesWithLabel = monthlyCalendarService.getDatesByLabel(labelId, year, month);
            eventDates = datesWithLabel.stream()
                    .map(LocalDate::toString)
                    .collect(Collectors.toList());

            stats = monthlyCalendarService.getMonthlyBucketStats(labelId, year, month);
        }

        return ResponseEntity.ok(new MonthlyCalendarResponseDTO(eventDates, stats));
    }

}
