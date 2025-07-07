package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.dto.MonthlyCalendarResponseDTO;
import com.yohan.event_planner.service.MonthlyCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final MonthlyCalendarService monthlyCalendarService;

    public CalendarController(MonthlyCalendarService monthlyCalendarService) {
        this.monthlyCalendarService = monthlyCalendarService;
    }

    @GetMapping
    public ResponseEntity<MonthlyCalendarResponseDTO> getMonthlyCalendarView(
            @RequestParam(value = "labelId", required = false) Long labelId,
            @RequestParam(value = "year", required = false) Integer year,
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
