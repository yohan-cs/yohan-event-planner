package com.yohan.event_planner.domain;

import com.yohan.event_planner.service.ParsedRecurrenceInput;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

import java.util.Objects;

@Embeddable
public class RecurrenceRuleVO {

    private String summary;

    @Transient
    private ParsedRecurrenceInput parsed;

    protected RecurrenceRuleVO() {
        // JPA
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurrenceRuleVO that)) return false;
        return Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary);
    }
}