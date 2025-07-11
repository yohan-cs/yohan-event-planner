package com.yohan.event_planner.domain;

import com.yohan.event_planner.domain.enums.TimeBucketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

/**
 * Entity representing aggregated time statistics for a label within a specific time bucket.
 * 
 * <p>This entity serves as a denormalized reporting table that efficiently tracks time spent
 * on labeled events across different time periods. It enables fast analytics queries without
 * the need to aggregate individual event durations at query time.</p>
 * 
 * <h2>Time Bucketing Strategy</h2>
 * <p>Time is aggregated using two distinct bucketing strategies:</p>
 * <ul>
 *   <li><strong>WEEK buckets</strong>: Use ISO week numbering (ISO 8601)
 *     <ul>
 *       <li>bucketYear = ISO week-based year (may differ from calendar year)</li>
 *       <li>bucketValue = ISO week number (1-53)</li>
 *       <li>Example: Week 1 of 2024 starts on Monday, January 1, 2024</li>
 *     </ul>
 *   </li>
 *   <li><strong>MONTH buckets</strong>: Use calendar months
 *     <ul>
 *       <li>bucketYear = calendar year</li>
 *       <li>bucketValue = month number (1-12)</li>
 *       <li>Example: March 2024 = bucketYear=2024, bucketValue=3</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h2>Uniqueness and Constraints</h2>
 * <p>The unique constraint ensures that there is exactly one time bucket record for each
 * combination of (user, label, bucket type, year, value). This prevents duplicate aggregations
 * and ensures data consistency.</p>
 * 
 * <h2>Label Name Snapshotting</h2>
 * <p>The {@code labelName} field captures the label's name at the time of bucket creation.
 * This provides several benefits:</p>
 * <ul>
 *   <li>Historical accuracy when label names change</li>
 *   <li>Faster queries without joins to the labels table</li>
 *   <li>Continued analytics even if labels are deleted</li>
 * </ul>
 * 
 * <h2>Time Accumulation</h2>
 * <p>Duration is accumulated incrementally as events are processed:</p>
 * <ul>
 *   <li>New buckets start with 0 minutes</li>
 *   <li>Event durations are added via {@link #incrementMinutes(int)}</li>
 *   <li>Total time represents all labeled activity in the time period</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <p>Common usage scenarios include:</p>
 * <ul>
 *   <li><strong>Time tracking reports</strong>: Weekly/monthly summaries by label</li>
 *   <li><strong>Productivity analytics</strong>: Trending analysis over time periods</li>
 *   <li><strong>Badge calculations</strong>: Aggregating time across multiple labels</li>
 *   <li><strong>Dashboard widgets</strong>: Real-time time allocation visualization</li>
 * </ul>
 * 
 * @see Label
 * @see TimeBucketType
 * @see Event
 */
@Entity
@Table(name = "label_time_bucket",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "user_id", "label_id", "bucket_type", "bucket_year", "bucket_value"
        }))
public class LabelTimeBucket {

    /** Unique identifier for this time bucket record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who owns the labeled events being aggregated. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** ID of the label being tracked in this time bucket. */
    @Column(name = "label_id", nullable = false)
    private Long labelId;

    /** 
     * Snapshot of the label name at the time this bucket was created.
     * Provides historical accuracy and query performance benefits.
     */
    @Column(name = "label_name", nullable = false)
    private String labelName;

    /** 
     * Type of time bucket - either WEEK (ISO week) or MONTH (calendar month).
     * Determines how bucketYear and bucketValue should be interpreted.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "bucket_type", nullable = false)
    private TimeBucketType bucketType;

    /** 
     * Year component of the time bucket.
     * For WEEK: ISO week-based year (may differ from calendar year).
     * For MONTH: calendar year.
     */
    @Column(name = "bucket_year", nullable = false)
    private int bucketYear;

    /** 
     * Value component of the time bucket.
     * For WEEK: ISO week number (1-53).
     * For MONTH: calendar month (1-12).
     */
    @Column(name = "bucket_value", nullable = false)
    private int bucketValue;

    /** 
     * Total accumulated time in minutes for this label within this time bucket.
     * Incremented as events are processed and aggregated.
     */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 0;

    /**
     * Default constructor for JPA.
     */
    public LabelTimeBucket() {
    }

    /**
     * Creates a new time bucket with the specified parameters.
     * Duration starts at 0 and should be incremented as events are processed.
     * 
     * @param userId the ID of the user who owns the events
     * @param labelId the ID of the label being tracked
     * @param labelName snapshot of the label name for historical accuracy
     * @param bucketType the type of time bucket (WEEK or MONTH)
     * @param bucketYear the year component (ISO week-year or calendar year)
     * @param bucketValue the value component (ISO week or calendar month)
     */
    public LabelTimeBucket(Long userId, Long labelId, String labelName,
                           TimeBucketType bucketType, int bucketYear, int bucketValue) {
        this.userId = userId;
        this.labelId = labelId;
        this.labelName = labelName;
        this.bucketType = bucketType;
        this.bucketYear = bucketYear;
        this.bucketValue = bucketValue;
        this.durationMinutes = 0;
    }

    /**
     * Incrementally adds time to this bucket's total duration.
     * Used when processing events to aggregate their durations.
     * 
     * @param minutes the number of minutes to add to the total
     */
    public void incrementMinutes(int minutes) {
        this.durationMinutes += minutes;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getLabelId() {
        return labelId;
    }

    public String getLabelName() {
        return labelName;
    }

    public TimeBucketType getBucketType() {
        return bucketType;
    }

    public int getBucketYear() {
        return bucketYear;
    }

    public int getBucketValue() {
        return bucketValue;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    // Setters (only for fields that might change)

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    public void setBucketType(TimeBucketType bucketType) {
        this.bucketType = bucketType;
    }

    public void setBucketYear(int bucketYear) {
        this.bucketYear = bucketYear;
    }

    public void setBucketValue(int bucketValue) {
        this.bucketValue = bucketValue;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Equality based on the natural key (user, label, bucket type, year, value).
     * This matches the unique constraint and ensures proper entity semantics.
     * Duration is intentionally excluded as it can change over time.
     * 
     * @param o the object to compare with
     * @return true if the objects represent the same time bucket
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabelTimeBucket that)) return false;
        return bucketYear == that.bucketYear &&
                bucketValue == that.bucketValue &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(labelId, that.labelId) &&
                bucketType == that.bucketType;
    }

    /**
     * Hash code based on the natural key for consistency with equals().
     * 
     * @return hash code for this time bucket
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId, labelId, bucketType, bucketYear, bucketValue);
    }
}
