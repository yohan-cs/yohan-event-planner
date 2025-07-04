package com.yohan.event_planner.domain;

import jakarta.persistence.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Entity representing a scheduled event created by a user.
 *
 * <p>This entity supports core event functionality, including event creation,
 * start and end times, timezone information, and optional descriptions. It also
 * tracks the user who created the event.</p>
 *
 * <h2>Timezone Handling</h2>
 * <ul>
 *     <li>Start and end times are stored internally in UTC.</li>
 *     <li>Original time zone IDs are stored separately to support display in the user's local time zone.</li>
 * </ul>
 *
 * <h2>Duration</h2>
 * <ul>
 *     <li>{@code durationMinutes} is automatically calculated when both start and end times are provided.</li>
 *     <li>If {@code endTime} is removed or not provided, {@code durationMinutes} is set to {@code null}.</li>
 *     <li>Duration is stored in whole minutes and must be managed in the service layer.</li>
 * </ul>
 *
 * <p>All input validation and business logic should be handled at the service or DTO layer.</p>
 */
@Entity
@Table(name = "events")
@Access(AccessType.FIELD)
public class Event {

    /** Unique identifier for the event. Auto-generated by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Event name or title. */
    @Column(nullable = false, length = 50)
    private String name;

    /** The user who created the event. */
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private final User creator;

    /** The start time of the event, stored in UTC. */
    @Column(name = "starttime", nullable = false)
    private ZonedDateTime startTime;

    /**
     * The end time of the event, stored in UTC.
     * May be {@code null} for open-ended events.
     */
    @Column(name = "endtime")
    private ZonedDateTime endTime;

    /** Time zone ID used for the original start time. */
    @Column(name = "starttimezone", nullable = false, length = 50)
    private String startTimezone;

    /**
     * Time zone ID used for the original end time.
     * May be {@code null} if {@code endTime} is not provided.
     */
    @Column(name = "endtimezone", length = 50)
    private String endTimezone;

    /**
     * Duration of the event in whole minutes, calculated from start to end time.
     *
     * <p>This field is set only when {@code endTime} is present. It is cleared if
     * {@code endTime} is removed. Calculation is handled externally (e.g., in the service layer).</p>
     */
    @Column(name = "durationminutes")
    private Integer durationMinutes;

    /** Optional description of the event. */
    @Column(length = 255)
    private String description;

    /**
     * Protected no-args constructor required by JPA.
     * Not intended for direct use.
     */
    protected Event() {
        this.creator = null;
    }

    /**
     * Constructs a new event with the required core fields.
     * <p>
     * The given start and end times are expected to include time zone information.
     * Internally, they are converted to UTC and their original time zone IDs are preserved.
     * This conversion is performed via setter methods to ensure consistency.
     *
     * @param name      the name of the event
     * @param startTime the local start time including zone (converted and stored in UTC)
     * @param creator   the user creating the event
     */
    public Event(String name, ZonedDateTime startTime, User creator) {
        this.name = name;
        this.creator = creator;
        setStartTime(startTime);
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getCreator() {
        return creator;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public String getStartTimezone() {
        return startTimezone;
    }

    public String getEndTimezone() {
        return endTimezone;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    // --- Setters ---

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTimezone = startTime.getZone().getId();
        this.startTime = startTime.withZoneSameInstant(ZoneOffset.UTC);
    }

    public void setEndTime(ZonedDateTime endTime) {
        if (endTime != null) {
            this.endTimezone = endTime.getZone().getId();
            this.endTime = endTime.withZoneSameInstant(ZoneOffset.UTC);
        } else {
            this.endTimezone = null;
            this.endTime = null;
            this.durationMinutes = null;
        }
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // --- Equality & Hashing ---

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
