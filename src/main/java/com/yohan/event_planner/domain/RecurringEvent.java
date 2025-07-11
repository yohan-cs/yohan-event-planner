package com.yohan.event_planner.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a recurring event pattern that can generate multiple individual events.
 * 
 * <p>Recurring events define a template for creating multiple events based on recurrence rules
 * (daily, weekly, monthly, etc.) with support for complex patterns including:</p>
 * 
 * <ul>
 *   <li><strong>Time-based recurrence</strong>: Daily, weekly, monthly, yearly patterns</li>
 *   <li><strong>Flexible scheduling</strong>: Start/end dates for the recurrence pattern</li>
 *   <li><strong>Skip days</strong>: Specific dates to exclude from the pattern</li>
 *   <li><strong>Label categorization</strong>: Optional label for event organization</li>
 *   <li><strong>Draft support</strong>: Unconfirmed drafts for partial input</li>
 * </ul>
 * 
 * <h2>Time Handling</h2>
 * <p>Unlike individual {@link Event} objects which store timezone-aware times in UTC,
 * recurring events use {@link LocalTime} and {@link LocalDate} to represent:</p>
 * <ul>
 *   <li><strong>Time of day</strong>: When during the day the event occurs (startTime/endTime)</li>
 *   <li><strong>Date range</strong>: The period during which the recurrence is active</li>
 *   <li><strong>Timezone neutrality</strong>: Generated events inherit timezone from creation context</li>
 * </ul>
 * 
 * <h2>Recurrence Rules</h2>
 * <p>Recurrence patterns are defined by embedded {@link RecurrenceRuleVO} which supports:</p>
 * <ul>
 *   <li>Standard frequencies (DAILY, WEEKLY, MONTHLY, YEARLY)</li>
 *   <li>Interval specifications (every N days/weeks/months)</li>
 *   <li>Complex patterns using RRule syntax</li>
 *   <li>Infinite recurrence (no end date)</li>
 * </ul>
 * 
 * <h2>Skip Days Management</h2>
 * <p>Individual dates can be excluded from the recurrence pattern:</p>
 * <ul>
 *   <li>Skip days are stored as a {@link Set} of {@link LocalDate}</li>
 *   <li>Generated events automatically exclude skip days</li>
 *   <li>Skip days can be added/removed dynamically</li>
 * </ul>
 * 
 * <h2>Lifecycle States</h2>
 * <p>Recurring events support the same confirmation workflow as individual events:</p>
 * <ul>
 *   <li><strong>Unconfirmed</strong>: Draft state for partial input (unconfirmed = true)</li>
 *   <li><strong>Confirmed</strong>: Active recurrence pattern (unconfirmed = false)</li>
 * </ul>
 * 
 * @see Event
 * @see RecurrenceRuleVO
 * @see Label
 */
@Entity
public class RecurringEvent {

    /** Unique identifier for this recurring event. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    /** The user who created this recurring event pattern. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** Optional name for the recurring event. May be null for draft events. */
    @Column
    private String name;

    /** 
     * Time of day when the event starts (timezone-neutral).
     * Generated events will use this time in their local timezone.
     */
    @Column
    private LocalTime startTime;

    /** 
     * Time of day when the event ends (timezone-neutral).
     * May be null for events without a defined end time.
     */
    @Column
    private LocalTime endTime;

    /** 
     * First date on which this recurrence pattern becomes active.
     * Used as the base date for calculating subsequent occurrences.
     */
    @Column
    @Temporal(TemporalType.DATE)
    private LocalDate startDate;

    /** 
     * Last date on which this recurrence pattern is active.
     * May be null for infinite recurrence patterns.
     */
    @Column
    @Temporal(TemporalType.DATE)
    private LocalDate endDate;

    /** Optional description providing additional context for the recurring event. */
    @Column
    private String description;

    /** Optional label for categorizing and organizing related recurring events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id")
    private Label label;

    /** 
     * Embedded recurrence rule defining the pattern frequency and schedule.
     * Contains the core logic for determining when events should occur.
     */
    @Embedded
    private RecurrenceRuleVO recurrenceRule;

    /** 
     * Set of specific dates to exclude from the recurrence pattern.
     * Events will not be generated for dates in this collection.
     */
    @ElementCollection
    @CollectionTable(name = "recurring_event_skip_days", joinColumns = @JoinColumn(name = "recurring_event_id"))
    @Column(name = "skip_day")
    private Set<LocalDate> skipDays = new HashSet<>();

    /** 
     * Whether this recurring event is in draft/unconfirmed state.
     * Unconfirmed events are not used for event generation.
     */
    @Column(nullable = false)
    private boolean unconfirmed = true;

    protected RecurringEvent() {
        // For JPA
    }

    /**
     * Creates a fully-specified recurring event with all scheduling details.
     * 
     * @param name the name of the recurring event
     * @param startTime time of day when events start
     * @param endTime time of day when events end (may be null)
     * @param startDate first date for the recurrence pattern
     * @param endDate last date for the recurrence pattern (may be null for infinite)
     * @param description optional description
     * @param recurrenceRule the recurrence pattern definition
     * @param creator the user creating the recurring event
     * @param unconfirmed whether this should be created as a draft
     * @return a new RecurringEvent instance
     */
    public static RecurringEvent createRecurringEvent(
            String name,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            RecurrenceRuleVO recurrenceRule,
            User creator,
            boolean unconfirmed
    ) {
        RecurringEvent recurringEvent = new RecurringEvent(creator, unconfirmed);
        recurringEvent.setName(name);
        recurringEvent.setStartTime(startTime);
        recurringEvent.setEndTime(endTime);
        recurringEvent.setStartDate(startDate);
        recurringEvent.setEndDate(endDate);
        recurringEvent.setDescription(description);
        recurringEvent.setRecurrenceRule(recurrenceRule);
        return recurringEvent;
    }

    /**
     * Creates an unconfirmed draft recurring event with minimal information.
     * Used for saving partial user input that can be completed later.
     * 
     * @param name the name of the recurring event (may be null)
     * @param startDate the start date for the pattern (may be null)
     * @param endDate the end date for the pattern (may be null)
     * @param creator the user creating the draft
     * @return an unconfirmed RecurringEvent draft
     */
    public static RecurringEvent createUnconfirmedDraftRecurringEvent(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            User creator
    ) {
        RecurrenceRuleVO draftRule = new RecurrenceRuleVO("UNSPECIFIED", null);
        RecurringEvent draft = new RecurringEvent(creator, true);
        draft.setName(name);
        draft.setStartDate(startDate);
        draft.setEndDate(endDate);
        draft.setRecurrenceRule(draftRule);
        return draft;
    }

    private RecurringEvent(User creator, boolean unconfirmed) {
        this.creator = creator;
        this.unconfirmed = unconfirmed;
    }

    public Long getId() {
        return id;
    }

    public User getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public Label getLabel() {
        return label;
    }

    public RecurrenceRuleVO getRecurrenceRule() {
        return recurrenceRule;
    }

    public Set<LocalDate> getSkipDays() {
        return skipDays;
    }

    public boolean isUnconfirmed() {
        return unconfirmed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime != null ? startTime.truncatedTo(ChronoUnit.MINUTES) : null;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime != null ? endTime.truncatedTo(ChronoUnit.MINUTES) : null;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public void setRecurrenceRule(RecurrenceRuleVO recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public void setSkipDays(Set<LocalDate> skipDays) {
        this.skipDays = skipDays;
    }

    public void setUnconfirmed(boolean unconfirmed) {
        this.unconfirmed = unconfirmed;
    }

    /**
     * Adds a date to the skip days collection, preventing event generation on that date.
     * 
     * @param skipDay the date to exclude from the recurrence pattern
     */
    public void addSkipDay(LocalDate skipDay) {
        if (skipDay != null) {
            this.skipDays.add(skipDay);
        }
    }

    /**
     * Removes a date from the skip days collection, allowing event generation on that date.
     * 
     * @param skipDay the date to include in the recurrence pattern
     */
    public void removeSkipDay(LocalDate skipDay) {
        if (skipDay != null) {
            this.skipDays.remove(skipDay);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurringEvent that = (RecurringEvent) o;
        return unconfirmed == that.unconfirmed &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(description, that.description) &&
                Objects.equals(recurrenceRule, that.recurrenceRule) &&
                Objects.equals(label, that.label) &&
                Objects.equals(creator, that.creator) &&
                Objects.equals(skipDays, that.skipDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startTime, endTime, startDate, endDate, description, recurrenceRule, label, creator, unconfirmed, skipDays);
    }
}
