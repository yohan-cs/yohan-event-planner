package com.yohan.event_planner.domain;

import com.yohan.event_planner.constants.ApplicationConstants;

import com.yohan.event_planner.domain.enums.RecapMediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "recap_media")
public class RecapMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recap_id", nullable = false)
    private EventRecap recap;

    @Column(length = ApplicationConstants.URL_MAX_LENGTH, nullable = false)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = ApplicationConstants.MEDIA_TYPE_MAX_LENGTH, nullable = false)
    private RecapMediaType mediaType; // IMAGE, VIDEO, AUDIO

    @Column
    private Integer durationSeconds; // nullable, only populated for videos

    @Column(nullable = false)
    private int mediaOrder; // ordering of media in recap

    protected RecapMedia() {
        // JPA
    }

    public RecapMedia(EventRecap recap, String mediaUrl, RecapMediaType mediaType, Integer durationSeconds, int mediaOrder) {
        this.recap = recap;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.durationSeconds = durationSeconds;
        this.mediaOrder = mediaOrder;
    }

    // === Getters and setters ===

    public Long getId() {
        return id;
    }

    public EventRecap getRecap() {
        return recap;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public RecapMediaType getMediaType() {
        return mediaType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public int getMediaOrder() {
        return mediaOrder;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setMediaType(RecapMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setMediaOrder(int mediaOrder) {
        this.mediaOrder = mediaOrder;
    }

    // === Equality ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecapMedia that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
