package com.yohan.event_planner.domain;

import com.yohan.event_planner.constants.ApplicationConstants;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Entity representing a user-created event, either scheduled (fully defined),
 * impromptu (minimal and immediate), or unconfirmed (a saved partial draft).
 *
 * <p>This entity supports flexible creation workflows:
 * <ul>
 *     <li><b>Scheduled events</b>: fully defined with name, start, and end times; immediately confirmed.</li>
 *     <li><b>Impromptu events</b>: minimal events that begin immediately with a start time only; confirmed later.</li>
 *     <li><b>Unconfirmed events</b>: saved drafts with partial or incomplete input, such as just a name or time.</li>
 * </ul>
 * Required fields are enforced at the controller or service layer, depending on the entry point.
 * </p>
 *
 * <h2>Timezone Handling</h2>
 * <ul>
 *     <li>Start and end times are stored internally in UTC.</li>
 *     <li>The original time zone ID is stored separately for accurate UI display.</li>
 *     <li>Time zones are extracted from {@link ZonedDateTime} inputs when setting time fields.</li>
 * </ul>
 *
 * <h2>Duration Handling</h2>
 * <ul>
 *     <li>{@code durationMinutes} is derived from start and end times in whole minutes.</li>
 *     <li>If either time is missing, duration is {@code null}.</li>
 *     <li>Duration is automatically recalculated when start or end is changed.</li>
 * </ul>
 *
 * <h2>Confirmation & Completion Lifecycle</h2>
 * <ul>
 *     <li>Scheduled events are created with {@code unconfirmed = false}.</li>
 *     <li>Impromptu events and saved partial drafts are marked {@code unconfirmed = true}.</li>
 *     <li>{@code completed = true} is only valid if {@code endTime} is set.</li>
 * </ul>
 *
 * <p>
 * Validation rules are applied selectively:
 * <ul>
 *     <li>DTO-based scheduled event creation uses field-level annotations.</li>
 *     <li>Programmatic creation (e.g. impromptu or unconfirmed drafts) bypasses validation and relies on custom logic.</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "events")
@Access(AccessType.FIELD)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
    private String name;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private final User creator;

    @Column(name = "starttime", nullable = true)
    private ZonedDateTime startTime;

    @Column(name = "endtime", nullable = true)
    private ZonedDateTime endTime;

    @Column(name = "starttimezone", length = ApplicationConstants.SHORT_NAME_MAX_LENGTH, nullable = true)
    private String startTimezone;

    @Column(name = "endtimezone", length = ApplicationConstants.SHORT_NAME_MAX_LENGTH, nullable = true)
    private String endTimezone;

    @Column(name = "durationminutes", nullable = true)
    private Integer durationMinutes;

    @Column(length = ApplicationConstants.STANDARD_TEXT_MAX_LENGTH, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = true)
    private Label label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_event_id", nullable = true)
    private RecurringEvent recurringEvent;

    @Column(nullable = false)
    private boolean isCompleted = false;

    @Column(nullable = false)
    private boolean unconfirmed = true;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EventRecap recap;

    protected Event() {
        this.creator = null;
    }

    // === Static Factory Methods ===

    public static Event createEvent(String name, ZonedDateTime startTime, ZonedDateTime endTime, User creator) {
        Event event = new Event(name, startTime, creator, false);
        event.setEndTime(endTime);
        return event;
    }

    public static Event createImpromptuEvent(ZonedDateTime startTime, User creator) {
        return new Event(null, startTime, creator, true);
    }

    /**
     * Creates an unconfirmed event draft with partial input (e.g. missing name or time).
     * Fields may be {@code null}. Used to save user drafts.
     *
     * @param name optional name
     * @param startTime optional start time
     * @param endTime optional end time
     * @param creator the user creating the draft (required)
     * @return an unconfirmed {@link Event} draft
     */
    public static Event createUnconfirmedDraft(String name, ZonedDateTime startTime, ZonedDateTime endTime, User creator) {
        Event event = new Event(name, startTime, creator, true);
        event.setEndTime(endTime);
        return event;
    }

    private Event(String name, ZonedDateTime startTime, User creator, boolean unconfirmed) {
        this.name = name;
        this.creator = creator;
        this.unconfirmed = unconfirmed;
        if (startTime != null) setStartTime(startTime);
    }

    // === Getters ===

    public Long getId() { return id; }

    public String getName() { return name; }

    public User getCreator() { return creator; }

    public ZonedDateTime getStartTime() { return startTime; }

    public ZonedDateTime getEndTime() { return endTime; }

    public String getStartTimezone() { return startTimezone; }

    public String getEndTimezone() { return endTimezone; }

    public Integer getDurationMinutes() { return durationMinutes; }

    public String getDescription() { return description; }

    public Label getLabel() { return label; }

    public RecurringEvent getRecurringEvent() { return recurringEvent; }

    public boolean isCompleted() { return isCompleted; }

    public boolean isUnconfirmed() { return unconfirmed; }

    public EventRecap getRecap() {
        return recap;
    }

    // === Setters ===

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(ZonedDateTime startTime) {
        if (startTime != null) {
            this.startTimezone = startTime.getZone().getId();
            this.startTime = startTime.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.startTimezone = null;
            this.startTime = null;
        }
        recalculateDuration();
    }

    public void setEndTime(ZonedDateTime endTime) {
        if (endTime != null) {
            this.endTimezone = endTime.getZone().getId();
            this.endTime = endTime.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.endTimezone = null;
            this.endTime = null;
        }
        recalculateDuration();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public void setRecurringEvent(RecurringEvent recurringEvent) {
        this.recurringEvent = recurringEvent;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public void setUnconfirmed(boolean unconfirmed) {
        this.unconfirmed = unconfirmed;
    }

    public void setRecap(EventRecap recap) {
        this.recap = recap;
    }

    // === Internal Helpers ===

    private void recalculateDuration() {
        if (this.startTime != null && this.endTime != null) {
            long minutes = Duration.between(
                    this.startTime.withZoneSameInstant(ZoneOffset.UTC),
                    this.endTime.withZoneSameInstant(ZoneOffset.UTC)
            ).toMinutes();
            this.durationMinutes = (int) minutes;
        } else {
            this.durationMinutes = null;
        }
    }

    // === Equality & Hashing ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        if (id != null && event.id != null) {
            return id.equals(event.id);
        }
        return Objects.equals(name, event.name)
                && Objects.equals(startTime, event.startTime)
                && Objects.equals(endTime, event.endTime)
                && Objects.equals(creator, event.creator)
                && Objects.equals(startTimezone, event.startTimezone)
                && Objects.equals(endTimezone, event.endTimezone);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode()
                : Objects.hash(name, startTime, endTime, creator, startTimezone, endTimezone);
    }
}
