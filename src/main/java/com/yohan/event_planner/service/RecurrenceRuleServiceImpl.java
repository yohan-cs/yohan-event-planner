package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.RecurrenceFrequency;
import com.yohan.event_planner.exception.InvalidRecurrenceRuleException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_RECURRENCE_INTERVAL;
import static com.yohan.event_planner.exception.ErrorCode.INVALID_RECURRENCE_RULE;
import static com.yohan.event_planner.exception.ErrorCode.MONTHLY_INVALID_ORDINAL;
import static com.yohan.event_planner.exception.ErrorCode.MONTHLY_MISSING_ORDINAL_OR_DAY;
import static com.yohan.event_planner.exception.ErrorCode.UNSUPPORTED_RECURRENCE_COMBINATION;
import static com.yohan.event_planner.exception.ErrorCode.WEEKLY_INVALID_DAY;
import static com.yohan.event_planner.exception.ErrorCode.WEEKLY_MISSING_DAYS;

@Service
public class RecurrenceRuleServiceImpl implements RecurrenceRuleService {

    @Override
    public ParsedRecurrenceInput parseFromString(String rule) {
        String[] parts = rule.split(":", -1);

        if (parts.length < 2) {
            throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
        }

        RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(parts[0].toUpperCase());
        Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
        Integer ordinal = null;

        switch (frequency) {
            case DAILY -> Collections.addAll(daysOfWeek, DayOfWeek.values());

            case WEEKLY -> {
                String[] days = parts[1].split(",");
                if (days.length == 0) {
                    throw new InvalidRecurrenceRuleException(WEEKLY_MISSING_DAYS);
                }
                for (String day : days) {
                    try {
                        daysOfWeek.add(DayOfWeek.valueOf(day.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidRecurrenceRuleException(WEEKLY_INVALID_DAY);
                    }
                }
            }

            case MONTHLY -> {
                if (parts.length < 3) {
                    throw new InvalidRecurrenceRuleException(MONTHLY_MISSING_ORDINAL_OR_DAY);
                }
                try {
                    ordinal = Integer.parseInt(parts[1]);
                    if (ordinal < 1 || ordinal > 4) {
                        throw new InvalidRecurrenceRuleException(MONTHLY_INVALID_ORDINAL);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidRecurrenceRuleException(MONTHLY_INVALID_ORDINAL);
                }
                String[] days = parts[2].split(",");
                if (days.length == 0) throw new InvalidRecurrenceRuleException(MONTHLY_MISSING_ORDINAL_OR_DAY);
                for (String day : days) {
                    try {
                        daysOfWeek.add(DayOfWeek.valueOf(day.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new InvalidRecurrenceRuleException(WEEKLY_INVALID_DAY);
                    }
                }
            }

            default -> throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
        }

        return new ParsedRecurrenceInput(frequency, daysOfWeek, ordinal);
    }

    @Override
    public List<LocalDate> expandRecurrence(
            ParsedRecurrenceInput parsed,
            LocalDate startInclusive,
            LocalDate endInclusive,
            Set<LocalDate> skipDays
    ) {
        if (parsed == null || parsed.frequency() == null) return List.of();

        RecurrenceFrequency frequency = parsed.frequency();
        Set<DayOfWeek> daysOfWeek = parsed.daysOfWeek();
        Integer ordinal = parsed.ordinal();

        List<LocalDate> occurrences = new ArrayList<>();
        LocalDate cursor = startInclusive;

        while (!cursor.isAfter(endInclusive)) {
            if (skipDays.contains(cursor)) {
                cursor = cursor.plusDays(1);
                continue;
            }

            switch (frequency) {
                case DAILY -> occurrences.add(cursor);
                case WEEKLY -> {
                    if (daysOfWeek.contains(cursor.getDayOfWeek())) {
                        occurrences.add(cursor);
                    }
                }
                case MONTHLY -> {
                    if (!daysOfWeek.contains(cursor.getDayOfWeek())) break;

                    LocalDate firstOfMonth = cursor.withDayOfMonth(1);
                    int count = 0;
                    for (int d = 1; d <= cursor.getDayOfMonth(); d++) {
                        if (firstOfMonth.plusDays(d - 1).getDayOfWeek() == cursor.getDayOfWeek()) {
                            count++;
                        }
                    }
                    if (Objects.equals(count, ordinal)) {
                        occurrences.add(cursor);
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        return occurrences;
    }

    public String buildSummary(ParsedRecurrenceInput parsedRecurrence, LocalDate startDate, LocalDate endDate) {
        RecurrenceFrequency frequency = parsedRecurrence.frequency();
        Set<DayOfWeek> daysOfWeek = parsedRecurrence.daysOfWeek();
        Integer ordinal = parsedRecurrence.ordinal();

        List<DayOfWeek> sortedDays = new ArrayList<>(daysOfWeek);
        sortedDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        String untilPart = (endDate != null)
                ? "until " + formatDate(endDate)
                : "forever";

        return switch (frequency) {
            case DAILY -> "Every day from " + formatDate(startDate) + " " + untilPart;
            case WEEKLY -> "Every " + formatDayList(sortedDays) + " from " + formatDate(startDate) + " " + untilPart;
            case MONTHLY -> "Every " + ordinalToString(ordinal) + " " + formatDayList(sortedDays)
                    + " of the month from " + formatDate(startDate) + " " + untilPart;
            default -> throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
        };
    }

    @Override
    public boolean occursOn(ParsedRecurrenceInput parsed, LocalDate date) {
        if (parsed == null || parsed.frequency() == null) return false;

        RecurrenceFrequency frequency = parsed.frequency();
        Set<DayOfWeek> daysOfWeek = parsed.daysOfWeek();
        Integer ordinal = parsed.ordinal();

        switch (frequency) {
            case DAILY:
                return true;

            case WEEKLY:
                return daysOfWeek.contains(date.getDayOfWeek());

            case MONTHLY:
                if (!daysOfWeek.contains(date.getDayOfWeek())) return false;

                LocalDate firstOfMonth = date.withDayOfMonth(1);
                int count = 0;
                for (int d = 1; d <= date.getDayOfMonth(); d++) {
                    if (firstOfMonth.plusDays(d - 1).getDayOfWeek() == date.getDayOfWeek()) {
                        count++;
                    }
                }
                return Objects.equals(count, ordinal);

            default:
                return false;
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private String formatDayList(List<DayOfWeek> days) {
        return days.stream()
                .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .collect(Collectors.joining(" and "));
    }

    private String ordinalToString(Integer ordinal) {
        return switch (ordinal) {
            case 1 -> "first";
            case 2 -> "second";
            case 3 -> "third";
            case 4 -> "fourth";
            default -> "unknown";
        };
    }
}