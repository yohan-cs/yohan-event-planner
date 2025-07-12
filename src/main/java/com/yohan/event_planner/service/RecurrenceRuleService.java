package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RecurringEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Service interface for parsing, processing, and expanding recurrence rules for recurring events.
 * 
 * <p>This service provides the core recurrence logic for the event planning system, handling:</p>
 * <ul>
 *   <li><strong>Rule parsing</strong>: Converting string representations to structured recurrence data</li>
 *   <li><strong>Date expansion</strong>: Generating specific occurrence dates from recurrence patterns</li>
 *   <li><strong>Skip day filtering</strong>: Excluding specific dates from recurrence patterns</li>
 *   <li><strong>Summary generation</strong>: Creating human-readable descriptions of recurrence rules</li>
 *   <li><strong>Occurrence testing</strong>: Determining if a pattern occurs on a specific date</li>
 * </ul>
 * 
 * <h2>Supported Recurrence Formats</h2>
 * <p>The service supports multiple recurrence rule formats:</p>
 * <ul>
 *   <li><strong>Simple frequencies</strong>: "DAILY", "WEEKLY", "MONTHLY", "YEARLY"</li>
 *   <li><strong>Interval-based</strong>: "DAILY,2" (every 2 days), "WEEKLY,3" (every 3 weeks)</li>
 *   <li><strong>Complex patterns</strong>: RFC 5545 RRULE syntax for advanced scenarios</li>
 *   <li><strong>Draft placeholders</strong>: "UNSPECIFIED" for incomplete recurring events</li>
 * </ul>
 * 
 * <h2>Date Expansion Process</h2>
 * <p>Date expansion follows a consistent process:</p>
 * <ol>
 *   <li>Parse the recurrence rule into structured data</li>
 *   <li>Generate all potential occurrence dates within the range</li>
 *   <li>Filter out dates that fall on skip days</li>
 *   <li>Return the final list of occurrence dates</li>
 * </ol>
 * 
 * <h2>Skip Day Integration</h2>
 * <p>Skip days provide flexible exclusion of specific dates:</p>
 * <ul>
 *   <li>Applied after initial recurrence calculation</li>
 *   <li>Allows permanent or temporary exclusions</li>
 *   <li>Supports holiday exclusions, vacation periods, etc.</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <p>The service is designed for efficient recurrence processing:</p>
 * <ul>
 *   <li>Optimized algorithms for common recurrence patterns</li>
 *   <li>Date range limiting to prevent infinite expansion</li>
 *   <li>Caching strategies for parsed recurrence rules</li>
 * </ul>
 * 
 * @see RecurringEvent
 * @see ParsedRecurrenceInput
 * @see com.yohan.event_planner.domain.RecurrenceRuleVO
 * @see com.yohan.event_planner.domain.enums.RecurrenceFrequency
 * @since 1.0
 * @author Event Planner Team
 */
public interface RecurrenceRuleService {

    /**
     * Parses a string-based recurrence rule into a structured representation.
     * 
     * <p>Supports multiple input formats including simple frequencies, interval-based rules,
     * and complex RRULE syntax. Returns a parsed representation that can be used for
     * date expansion and occurrence testing.</p>
     * 
     * @param rule the string representation of the recurrence rule (must not be null)
     * @return a {@link ParsedRecurrenceInput} containing the structured recurrence data
     * @throws InvalidRecurrenceRuleException if the rule format is invalid or unsupported
     * @throws NullPointerException if rule is null
     */
    ParsedRecurrenceInput parseFromString(String rule);

    /**
     * Expands a recurrence pattern into a list of specific occurrence dates within a date range.
     * 
     * <p>Generates all dates where the recurrence pattern would create an event occurrence,
     * then filters out any dates that appear in the skip days set. The result is a complete
     * list of actual event dates for the specified time period.</p>
     * 
     * @param input the parsed recurrence pattern to expand (must not be null)
     * @param startInclusive the earliest date to include in the expansion (inclusive, must not be null)
     * @param endInclusive the latest date to include in the expansion (inclusive, must not be null)
     * @param skipDays set of dates to exclude from the expansion (must not be null, can be empty)
     * @return ordered list of dates where events should occur (never null, can be empty)
     * @throws NullPointerException if any parameter is null
     */
    List<LocalDate> expandRecurrence(ParsedRecurrenceInput input, LocalDate startInclusive, LocalDate endInclusive, Set<LocalDate> skipDays);

    /**
     * Builds a human-readable summary description of a recurrence pattern.
     * 
     * <p>Creates a textual description that summarizes the recurrence rule and date range
     * in language that users can easily understand. Useful for UI display and user
     * confirmation of recurrence settings.</p>
     * 
     * @param parsedRecurrence the parsed recurrence pattern to describe (must not be null)
     * @param startDate the start date of the recurrence period (must not be null)
     * @param endDate the end date of the recurrence period (may be null for infinite recurrence)
     * @return a human-readable description of the recurrence pattern (never null)
     * @throws NullPointerException if parsedRecurrence or startDate is null
     */
    String buildSummary(ParsedRecurrenceInput parsedRecurrence, LocalDate startDate, LocalDate endDate);

    /**
     * Tests whether a recurrence pattern would generate an occurrence on a specific date.
     * 
     * <p>Performs a point-in-time check to determine if the recurrence rule would create
     * an event on the specified date, ignoring skip days. This is useful for validation
     * and conflict detection without expanding entire date ranges.</p>
     * 
     * @param parsed the parsed recurrence pattern to test (must not be null)
     * @param date the specific date to test for occurrence (must not be null)
     * @return true if the pattern would create an occurrence on the specified date
     * @throws NullPointerException if any parameter is null
     */
    boolean occursOn(ParsedRecurrenceInput parsed, LocalDate date);
}
