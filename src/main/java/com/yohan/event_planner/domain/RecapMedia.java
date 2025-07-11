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

/**
 * Entity representing a media attachment (image, video, or audio) associated with an event recap.
 * 
 * <p>RecapMedia entities provide rich multimedia content for event documentation, supporting:</p>
 * <ul>
 *   <li><strong>Multiple media types</strong>: Images, videos, and audio files</li>
 *   <li><strong>External hosting</strong>: URL-based references to media stored externally</li>
 *   <li><strong>Ordered presentation</strong>: Explicit ordering for consistent display</li>
 *   <li><strong>Duration tracking</strong>: Time-based metadata for video and audio content</li>
 * </ul>
 * 
 * <h2>Media Type Support</h2>
 * <p>The system supports three primary media categories:</p>
 * <ul>
 *   <li><strong>IMAGE</strong>: Static images (photos, screenshots, graphics)</li>
 *   <li><strong>VIDEO</strong>: Video content with optional duration tracking</li>
 *   <li><strong>AUDIO</strong>: Audio recordings with optional duration tracking</li>
 * </ul>
 * 
 * <h2>Storage Strategy</h2>
 * <p>Media files are stored externally with URL references:</p>
 * <ul>
 *   <li>Media URLs point to external storage (CDN, cloud storage, etc.)</li>
 *   <li>No binary data is stored in the database</li>
 *   <li>URLs must be accessible and properly formatted</li>
 *   <li>External storage manages actual file lifecycle</li>
 * </ul>
 * 
 * <h2>Ordering and Presentation</h2>
 * <p>Media items maintain explicit ordering within recaps:</p>
 * <ul>
 *   <li>mediaOrder field determines display sequence</li>
 *   <li>Lower values appear first in ordered lists</li>
 *   <li>Supports reordering without database ID changes</li>
 *   <li>Enables consistent narrative flow in recaps</li>
 * </ul>
 * 
 * <h2>Duration Metadata</h2>
 * <p>Time-based media can include duration information:</p>
 * <ul>
 *   <li>Duration stored in seconds for precise tracking</li>
 *   <li>Optional field - not required for all media types</li>
 *   <li>Particularly useful for video and audio content</li>
 *   <li>Enables time-based analytics and reporting</li>
 * </ul>
 * 
 * <h2>Lifecycle Management</h2>
 * <p>Media lifecycle is tightly coupled to recaps:</p>
 * <ul>
 *   <li>Media is automatically deleted when parent recap is removed</li>
 *   <li>Orphaned media records are prevented through cascading</li>
 *   <li>Media cannot exist without an associated recap</li>
 * </ul>
 * 
 * @see EventRecap
 * @see RecapMediaType
 */
@Entity
@Table(name = "recap_media")
public class RecapMedia {

    /** Unique identifier for this media attachment. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * The event recap that owns this media attachment.
     * Relationship is mandatory - media cannot exist without a recap.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recap_id", nullable = false)
    private EventRecap recap;

    /** 
     * URL pointing to the externally hosted media file.
     * Must be a valid, accessible URL to the media content.
     */
    @Column(length = ApplicationConstants.URL_MAX_LENGTH, nullable = false)
    private String mediaUrl;

    /** 
     * Type of media content (IMAGE, VIDEO, or AUDIO).
     * Determines how the media should be processed and displayed.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = ApplicationConstants.MEDIA_TYPE_MAX_LENGTH, nullable = false)
    private RecapMediaType mediaType;

    /** 
     * Duration of the media content in seconds.
     * Optional field, typically used for video and audio content.
     * Null for images or when duration is unknown.
     */
    @Column
    private Integer durationSeconds;

    /** 
     * Display order of this media within the recap.
     * Lower values appear first in ordered presentations.
     */
    @Column(nullable = false)
    private int mediaOrder;

    /**
     * Default constructor for JPA.
     */
    protected RecapMedia() {
        // JPA
    }

    /**
     * Creates a new media attachment for an event recap.
     * 
     * @param recap the event recap that will own this media
     * @param mediaUrl URL pointing to the hosted media file
     * @param mediaType type of media content (IMAGE, VIDEO, AUDIO)
     * @param durationSeconds optional duration in seconds (for video/audio)
     * @param mediaOrder display order within the recap
     */
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

    /**
     * Equality based on database identity for consistent entity semantics.
     * 
     * @param o the object to compare with
     * @return true if the objects represent the same media entity
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecapMedia that)) return false;
        return id != null && id.equals(that.id);
    }

    /**
     * Hash code based on the database ID for consistency with equals().
     * 
     * @return hash code for this media attachment
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
