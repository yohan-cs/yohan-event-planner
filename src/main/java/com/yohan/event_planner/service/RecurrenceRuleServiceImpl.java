package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.RecurrenceFrequency;
import com.yohan.event_planner.exception.InvalidRecurrenceRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.yohan.event_planner.exception.ErrorCode.MONTHLY_INVALID_ORDINAL;
import static com.yohan.event_planner.exception.ErrorCode.MONTHLY_MISSING_ORDINAL_OR_DAY;
import static com.yohan.event_planner.exception.ErrorCode.UNSUPPORTED_RECURRENCE_COMBINATION;
import static com.yohan.event_planner.exception.ErrorCode.WEEKLY_INVALID_DAY;
import static com.yohan.event_planner.exception.ErrorCode.WEEKLY_MISSING_DAYS;

/**
 * Implementation of {@link RecurrenceRuleService} providing comprehensive recurrence rule processing.
 * 
 * <p><strong>Architectural Role:</strong> This service operates in the Service layer as a pure
 * business logic component, providing recurrence pattern parsing, date expansion, and formatting
 * capabilities without any persistence or external service dependencies.</p>
 * 
 * <p><strong>Implementation Approach:</strong></p>
 * <ul>
 *   <li><strong>Parsing Strategy:</strong> Uses a colon-delimited format ("FREQUENCY:PARAMS") for rule representation</li>
 *   <li><strong>Date Expansion:</strong> Employs day-by-day iteration with pattern matching for accuracy</li>
 *   <li><strong>Skip Day Handling:</strong> Applies exclusions after initial pattern calculation</li>
 *   <li><strong>Error Handling:</strong> Throws domain-specific exceptions with detailed error codes</li>
 * </ul>
 * 
 * <p><strong>Supported Formats:</strong></p>
 * <ul>
 *   <li>{@code DAILY:} - Every day occurrence</li>
 *   <li>{@code WEEKLY:MON,WED,FRI} - Specific weekdays</li>
 *   <li>{@code MONTHLY:2:TUE,THU} - Nth occurrence of weekdays in month</li>
 * </ul>
 * 
 * <p><strong>Performance Characteristics:</strong></p>
 * <ul>
 *   <li>Optimized for typical date ranges (days to months)</li>
 *   <li>Memory-efficient iteration without pre-allocation of large arrays</li>
 *   <li>Stateless design enabling concurrent usage</li>
 * </ul>
 * 
 * @see RecurrenceRuleService
 * @see ParsedRecurrenceInput
 * @see RecurrenceFrequency
 */
@Service
public class RecurrenceRuleServiceImpl implements RecurrenceRuleService {

    private static final Logger logger = LoggerFactory.getLogger(RecurrenceRuleServiceImpl.class);

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong> Parses colon-delimited rule strings with
     * format-specific validation and error handling. Uses enum parsing for type safety
     * and provides detailed error codes for different validation failures.</p>
     * 
     * <p><strong>Parsing Logic:</strong></p>
     * <ul>
     *   <li>Splits rule on colon delimiter</li>
     *   <li>Validates frequency against {@link RecurrenceFrequency} enum</li>
     *   <li>Applies frequency-specific parameter parsing and validation</li>
     *   <li>Constructs immutable {@link ParsedRecurrenceInput} result</li>
     * </ul>
     */
    @Override
    public ParsedRecurrenceInput parseFromString(String rule) {
        logger.debug("Parsing recurrence rule: {}", rule);
        String[] parts = rule.trim().split(":", -1);

        if (parts.length < 2) {
            throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
        }

        RecurrenceFrequency frequency;
        try {
            frequency = RecurrenceFrequency.valueOf(parts[0].trim().toUpperCase());
            logger.debug("Parsed frequency: {}", frequency);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid frequency in rule: {}", parts[0]);
            throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
        }
        
        Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
        Integer ordinal = null;

        switch (frequency) {
            case DAILY -> {
                Collections.addAll(daysOfWeek, DayOfWeek.values());
                logger.debug("Configured DAILY recurrence for all days");
            }

            case WEEKLY -> {
                daysOfWeek = parseDaysOfWeek(parts[1].trim(), WEEKLY_MISSING_DAYS, WEEKLY_INVALID_DAY);
                logger.debug("Configured WEEKLY recurrence for days: {}", daysOfWeek);
            }

            case MONTHLY -> {
                if (parts.length < 3) {
                    logger.warn("Monthly rule missing ordinal or days: {}", rule);
                    throw new InvalidRecurrenceRuleException(MONTHLY_MISSING_ORDINAL_OR_DAY);
                }
                ordinal = parseOrdinal(parts[1].trim());
                daysOfWeek = parseDaysOfWeek(parts[2].trim(), MONTHLY_MISSING_ORDINAL_OR_DAY, WEEKLY_INVALID_DAY);
                logger.debug("Configured MONTHLY recurrence: ordinal={}, days={}", ordinal, daysOfWeek);
            }

            default -> {
                logger.error("Unsupported frequency: {}", frequency);
                throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
            }
        }

        ParsedRecurrenceInput result = new ParsedRecurrenceInput(frequency, daysOfWeek, ordinal);
        logger.debug("Successfully parsed recurrence rule: {}", result);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Strategy:</strong> Uses day-by-day iteration through the date range,
     * applying frequency-specific pattern matching and skip day filtering. This approach ensures
     * accuracy for complex patterns while maintaining reasonable performance for typical ranges.</p>
     * 
     * <p><strong>Expansion Process:</strong></p>
     * <ol>
     *   <li>Validate input parameters and handle edge cases</li>
     *   <li>Iterate through each day in the specified range</li>
     *   <li>Apply skip day filtering before pattern matching</li>
     *   <li>Test each date against the frequency-specific occurrence rules</li>
     *   <li>Collect matching dates in chronological order</li>
     * </ol>
     */
    @Override
    public List<LocalDate> expandRecurrence(
            ParsedRecurrenceInput parsed,
            LocalDate startInclusive,
            LocalDate endInclusive,
            Set<LocalDate> skipDays
    ) {
        logger.debug("Expanding recurrence from {} to {} with {} skip days", 
                     startInclusive, endInclusive, skipDays.size());
        if (parsed == null || parsed.frequency() == null) {
            logger.warn("Cannot expand null or invalid recurrence input");
            return List.of();
        }

        RecurrenceFrequency frequency = parsed.frequency();
        Set<DayOfWeek> daysOfWeek = parsed.daysOfWeek();
        Integer ordinal = parsed.ordinal();

        List<LocalDate> occurrences = new ArrayList<>();
        LocalDate cursor = startInclusive;
        int totalDays = 0;

        while (!cursor.isAfter(endInclusive)) {
            totalDays++;
            if (skipDays.contains(cursor)) {
                logger.trace("Skipping date: {}", cursor);
                cursor = cursor.plusDays(1);
                continue;
            }

            switch (frequency) {
                case DAILY -> {
                    occurrences.add(cursor);
                    logger.trace("Added daily occurrence: {}", cursor);
                }
                case WEEKLY -> {
                    if (daysOfWeek.contains(cursor.getDayOfWeek())) {
                        occurrences.add(cursor);
                        logger.trace("Added weekly occurrence: {} ({})", cursor, cursor.getDayOfWeek());
                    }
                }
                case MONTHLY -> {
                    if (isNthOccurrenceOfDayInMonth(cursor, ordinal, daysOfWeek)) {
                        occurrences.add(cursor);
                        logger.trace("Added monthly occurrence: {} ({}th {})", cursor, ordinal, cursor.getDayOfWeek());
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        logger.debug("Expansion complete: {} occurrences found from {} total days", 
                     occurrences.size(), totalDays);
        return occurrences;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong> Generates human-readable descriptions using
     * English ordinals, full day names, and formatted dates. Handles infinite recurrence
     * with "forever" terminology and provides consistent formatting across all frequency types.</p>
     */
    @Override
    public String buildSummary(ParsedRecurrenceInput parsedRecurrence, LocalDate startDate, LocalDate endDate) {
        logger.debug("Building summary for recurrence from {} to {}", startDate, endDate);
        RecurrenceFrequency frequency = parsedRecurrence.frequency();
        Set<DayOfWeek> daysOfWeek = parsedRecurrence.daysOfWeek();
        Integer ordinal = parsedRecurrence.ordinal();

        List<DayOfWeek> sortedDays = new ArrayList<>(daysOfWeek);
        sortedDays.sort(Comparator.comparingInt(DayOfWeek::getValue));

        String untilPart = (endDate != null)
                ? "until " + formatDate(endDate)
                : "forever";

        String summary = switch (frequency) {
            case DAILY -> "Every day from " + formatDate(startDate) + " " + untilPart;
            case WEEKLY -> "Every " + formatDayList(sortedDays) + " from " + formatDate(startDate) + " " + untilPart;
            case MONTHLY -> "Every " + ordinalToString(ordinal) + " " + formatDayList(sortedDays)
                    + " of the month from " + formatDate(startDate) + " " + untilPart;
            default -> {
                logger.error("Cannot build summary for unsupported frequency: {}", frequency);
                throw new InvalidRecurrenceRuleException(UNSUPPORTED_RECURRENCE_COMBINATION);
            }
        };
        
        logger.debug("Generated summary: {}", summary);
        return summary;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong> Performs point-in-time occurrence testing
     * without date range iteration. Uses the same logic as {@link #expandRecurrence} but
     * optimized for single-date evaluation.</p>
     */
    @Override
    public boolean occursOn(ParsedRecurrenceInput parsed, LocalDate date) {
        logger.debug("Testing occurrence on {} for frequency {}", date, 
                     parsed != null ? parsed.frequency() : null);
        if (parsed == null || parsed.frequency() == null) {
            logger.debug("No occurrence due to null or invalid input");
            return false;
        }

        RecurrenceFrequency frequency = parsed.frequency();
        Set<DayOfWeek> daysOfWeek = parsed.daysOfWeek();
        Integer ordinal = parsed.ordinal();

        boolean occurs = switch (frequency) {
            case DAILY -> true;
            case WEEKLY -> daysOfWeek.contains(date.getDayOfWeek());
            case MONTHLY -> isNthOccurrenceOfDayInMonth(date, ordinal, daysOfWeek);
            default -> {
                logger.warn("Unknown frequency for occurrence test: {}", frequency);
                yield false;
            }
        };
        
        logger.debug("Occurrence result for {}: {}", date, occurs);
        return occurs;
    }

    /**
     * Parses a comma-separated list of day names into a set of {@link DayOfWeek} enums.
     * 
     * @param daysString comma-separated day names (case-insensitive)
     * @param missingDaysError error code to throw if no days are provided
     * @param invalidDayError error code to throw if an invalid day name is found
     * @return set of parsed days of the week
     * @throws InvalidRecurrenceRuleException if parsing fails
     */
    private Set<DayOfWeek> parseDaysOfWeek(String daysString, 
                                           com.yohan.event_planner.exception.ErrorCode missingDaysError,
                                           com.yohan.event_planner.exception.ErrorCode invalidDayError) {
        String[] days = daysString.split(",");
        if (days.length == 0 || (days.length == 1 && days[0].trim().isEmpty())) {
            logger.warn("No days provided in days string: {}", daysString);
            throw new InvalidRecurrenceRuleException(missingDaysError);
        }
        
        Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
        for (String day : days) {
            try {
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.trim().toUpperCase());
                daysOfWeek.add(dayOfWeek);
                logger.trace("Parsed day: {}", dayOfWeek);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid day name: {}", day);
                throw new InvalidRecurrenceRuleException(invalidDayError);
            }
        }
        return daysOfWeek;
    }

    /**
     * Parses and validates an ordinal value for monthly recurrence rules.
     * 
     * @param ordinalString string representation of the ordinal (1-4)
     * @return validated ordinal value
     * @throws InvalidRecurrenceRuleException if the ordinal is invalid
     */
    private Integer parseOrdinal(String ordinalString) {
        try {
            Integer ordinal = Integer.parseInt(ordinalString.trim());
            if (ordinal < 1 || ordinal > 4) {
                logger.warn("Ordinal out of range (1-4): {}", ordinal);
                throw new InvalidRecurrenceRuleException(MONTHLY_INVALID_ORDINAL);
            }
            logger.trace("Parsed ordinal: {}", ordinal);
            return ordinal;
        } catch (NumberFormatException e) {
            logger.warn("Invalid ordinal format: {}", ordinalString);
            throw new InvalidRecurrenceRuleException(MONTHLY_INVALID_ORDINAL);
        }
    }

    /**
     * Determines if a given date represents the Nth occurrence of its day of the week within its month.
     * 
     * <p>For example, if {@code ordinal} is 2 and {@code daysOfWeek} contains TUESDAY,
     * this method returns true if {@code date} is the second Tuesday of its month.</p>
     * 
     * @param date the date to test
     * @param ordinal the occurrence number (1-4)
     * @param daysOfWeek the set of days of the week to match against
     * @return true if the date is the Nth occurrence of one of the specified days
     */
    private boolean isNthOccurrenceOfDayInMonth(LocalDate date, Integer ordinal, Set<DayOfWeek> daysOfWeek) {
        if (!daysOfWeek.contains(date.getDayOfWeek())) {
            return false;
        }
        
        LocalDate firstOfMonth = date.withDayOfMonth(1);
        int count = 0;
        for (int d = 1; d <= date.getDayOfMonth(); d++) {
            if (firstOfMonth.plusDays(d - 1).getDayOfWeek() == date.getDayOfWeek()) {
                count++;
            }
        }
        
        boolean isNthOccurrence = Objects.equals(count, ordinal);
        logger.trace("Date {} is {}th occurrence of {}: {}", date, count, date.getDayOfWeek(), isNthOccurrence);
        return isNthOccurrence;
    }

    /**
     * Formats a date into a human-readable string using the pattern "MMMM d, yyyy".
     * 
     * @param date the date to format
     * @return formatted date string (e.g., "June 15, 2025")
     */
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    /**
     * Formats a list of days of the week into a human-readable string.
     * 
     * <p>Joins multiple days with " and " (e.g., "Monday and Friday").
     * Single days are returned as-is (e.g., "Wednesday").</p>
     * 
     * @param days the list of days to format (should be sorted for consistent output)
     * @return formatted string of day names
     */
    private String formatDayList(List<DayOfWeek> days) {
        return days.stream()
                .map(day -> day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .collect(Collectors.joining(" and "));
    }

    /**
     * Converts a numeric ordinal (1-4) into its English word equivalent.
     * 
     * @param ordinal the numeric ordinal value
     * @return English ordinal word ("first", "second", "third", "fourth", or "unknown")
     */
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