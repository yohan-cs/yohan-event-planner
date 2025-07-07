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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class RecurringEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column
    private String name;

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Column
    @Temporal(TemporalType.DATE)
    private LocalDate startDate;

    @Column
    @Temporal(TemporalType.DATE)
    private LocalDate endDate;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id")
    private Label label;

    @Embedded
    private RecurrenceRuleVO recurrenceRule;

    @ElementCollection
    @CollectionTable(name = "recurring_event_skip_days", joinColumns = @JoinColumn(name = "recurring_event_id"))
    @Column(name = "skip_day")
    private Set<LocalDate> skipDays = new HashSet<>();

    @Column(nullable = false)
    private boolean unconfirmed = true;

    protected RecurringEvent() {
        // For JPA
    }

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
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
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

    public void addSkipDay(LocalDate skipDay) {
        if (skipDay != null) {
            this.skipDays.add(skipDay);
        }
    }

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
