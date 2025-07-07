package com.yohan.event_planner.domain;

import com.yohan.event_planner.constants.ApplicationConstants;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "event_recaps")
public class EventRecap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = ApplicationConstants.MEDIUM_TEXT_MAX_LENGTH)
    private String recapName;

    @Column(nullable = false)
    private boolean unconfirmed = true;

    @OneToMany(mappedBy = "recap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecapMedia> media = new ArrayList<>();

    protected EventRecap() {
        // for JPA
    }

    private EventRecap(Event event, User creator, String notes, String recapName, boolean unconfirmed) {
        if (!event.getCreator().equals(creator)) {
            throw new IllegalArgumentException("Recap creator must match event creator.");
        }
        this.event = event;
        this.creator = creator;
        this.notes = notes;
        this.recapName = recapName;
        this.unconfirmed = unconfirmed;
    }

    // === Static Factory Methods ===

    public static EventRecap createConfirmedRecap(Event event, User creator, String notes, String recapName) {
        return new EventRecap(event, creator, notes, recapName, false);
    }

    public static EventRecap createUnconfirmedRecap(Event event, User creator, String notes, String recapName) {
        return new EventRecap(event, creator, notes, recapName, true);
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public User getCreator() {
        return creator;
    }

    public String getNotes() {
        return notes;
    }

    public String getRecapName() {
        return recapName;
    }

    public boolean isUnconfirmed() {
        return unconfirmed;
    }

    public List<RecapMedia> getMedia() {
        return media;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setRecapName(String recapName) {
        this.recapName = recapName;
    }

    public void setUnconfirmed(boolean unconfirmed) {
        this.unconfirmed = unconfirmed;
    }

    public void setMedia(List<RecapMedia> media) {
        this.media = media;
    }

    // === Equality ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventRecap that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
