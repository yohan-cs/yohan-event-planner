package com.yohan.event_planner.domain;

import com.yohan.event_planner.service.ParsedRecurrenceInput;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

import java.util.Objects;

/**
 * Value object representing a recurrence rule for recurring events.
 * 
 * <p>This embeddable entity encapsulates the recurrence pattern definition with both
 * a persistent summary representation and a transient parsed representation for
 * efficient processing:</p>
 * 
 * <ul>
 *   <li><strong>Summary</strong>: String-based rule stored in the database</li>
 *   <li><strong>Parsed</strong>: Runtime object for recurrence calculations</li>
 * </ul>
 * 
 * <h2>Recurrence Rule Format</h2>
 * <p>The summary field supports multiple recurrence rule formats:</p>
 * <ul>
 *   <li><strong>Simple frequencies</strong>: "DAILY", "WEEKLY", "MONTHLY", "YEARLY"</li>
 *   <li><strong>Interval-based</strong>: "DAILY,2" (every 2 days), "WEEKLY,3" (every 3 weeks)</li>
 *   <li><strong>RRule syntax</strong>: Standard RRULE format for complex patterns</li>
 *   <li><strong>Draft placeholder</strong>: "UNSPECIFIED" for unconfirmed recurring events</li>
 * </ul>
 * 
 * <h2>Persistence Strategy</h2>
 * <p>As an {@code @Embeddable} component, this value object:</p>
 * <ul>
 *   <li>Stores only the {@code summary} field in the database</li>
 *   <li>Reconstructs the {@code parsed} representation on demand</li>
 *   <li>Maintains data consistency through immutable design</li>
 * </ul>
 * 
 * <h2>Processing Workflow</h2>
 * <p>The typical usage pattern involves:</p>
 * <ol>
 *   <li>Creating rules with both summary and parsed representations</li>
 *   <li>Persisting only the summary to the database</li>
 *   <li>Re-parsing the summary when loaded from database</li>
 *   <li>Using the parsed representation for date calculations</li>
 * </ol>
 * 
 * <h2>Equality and Hashing</h2>
 * <p>Value object equality is based solely on the persistent {@code summary} field,
 * ensuring consistent behavior across persistence boundaries and avoiding issues
 * with transient parsed representations.</p>
 * 
 * @see RecurringEvent
 * @see ParsedRecurrenceInput
 */
@Embeddable
public class RecurrenceRuleVO {

    /** 
     * String representation of the recurrence rule, persisted to the database.
     * Contains the rule definition in a format that can be parsed into a 
     * {@link ParsedRecurrenceInput} object.
     */
    private String summary;

    /** 
     * Transient parsed representation of the recurrence rule.
     * Not persisted to the database - reconstructed from the summary when needed.
     * Used for efficient date calculations and recurrence processing.
     */
    @Transient
    private ParsedRecurrenceInput parsed;

    /**
     * Default constructor for JPA.
     */
    protected RecurrenceRuleVO() {
        // JPA
    }

    /**
     * Creates a new recurrence rule with both summary and parsed representations.
     * 
     * @param summary the string representation of the recurrence rule
     * @param parsed the parsed representation for date calculations
     */
    public RecurrenceRuleVO(String summary, ParsedRecurrenceInput parsed) {
        this.summary = summary;
        this.parsed = parsed;
    }

    public String getSummary() {
        return summary;
    }

    public ParsedRecurrenceInput getParsed() {
        return parsed;
    }

    /**
     * Value object equality based on the persistent summary field only.
     * This ensures consistent equality semantics across persistence boundaries,
     * as the transient parsed field may not be available in all contexts.
     * 
     * @param o the object to compare with
     * @return true if the summary fields are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurrenceRuleVO that)) return false;
        return Objects.equals(summary, that.summary);
    }

    /**
     * Hash code based on the persistent summary field for consistency with equals().
     * 
     * @return hash code for this recurrence rule
     */
    @Override
    public int hashCode() {
        return Objects.hash(summary);
    }
}