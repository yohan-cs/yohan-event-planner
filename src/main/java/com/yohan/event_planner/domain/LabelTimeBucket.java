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

@Entity
@Table(name = "label_time_bucket",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "user_id", "label_id", "bucket_type", "bucket_year", "bucket_value"
        }))
public class LabelTimeBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @Column(name = "label_name", nullable = false)
    private String labelName; // snapshot of label name at creation

    @Enumerated(EnumType.STRING)
    @Column(name = "bucket_type", nullable = false)
    private TimeBucketType bucketType; // WEEK or MONTH

    @Column(name = "bucket_year", nullable = false)
    private int bucketYear; // ISO week-year or calendar year

    @Column(name = "bucket_value", nullable = false)
    private int bucketValue; // ISO week number or calendar month (1–53 or 1–12)

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 0;

    public LabelTimeBucket() {
    }

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

    // equals and hashCode based on natural key

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

    @Override
    public int hashCode() {
        return Objects.hash(userId, labelId, bucketType, bucketYear, bucketValue);
    }
}
