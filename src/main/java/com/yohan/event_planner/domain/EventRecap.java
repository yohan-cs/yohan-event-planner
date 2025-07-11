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

/**
 * Entity representing a recap or summary of a completed event with optional media attachments.
 * 
 * <p>Event recaps provide a way for users to document and reflect on their events after completion.
 * They support rich content including:</p>
 * <ul>
 *   <li><strong>Textual notes</strong>: Free-form reflection and summary content</li>
 *   <li><strong>Named recaps</strong>: Custom titles for easier organization</li>
 *   <li><strong>Media attachments</strong>: Images, videos, and audio related to the event</li>
 *   <li><strong>Draft workflow</strong>: Unconfirmed recaps for gradual content creation</li>
 * </ul>
 * 
 * <h2>Relationship Structure</h2>
 * <p>Event recaps maintain several important relationships:</p>
 * <ul>
 *   <li><strong>One-to-One with Event</strong>: Each event can have at most one recap</li>
 *   <li><strong>Creator ownership</strong>: Only the event creator can create its recap</li>
 *   <li><strong>Media collection</strong>: Ordered list of attached media items</li>
 * </ul>
 * 
 * <h2>Ownership and Security</h2>
 * <p>Strict ownership rules are enforced:</p>
 * <ul>
 *   <li>Recap creator must be the same as the event creator</li>
 *   <li>This constraint is validated at construction time</li>
 *   <li>Prevents unauthorized recap creation or modification</li>
 * </ul>
 * 
 * <h2>Confirmation Workflow</h2>
 * <p>Like events, recaps support a draft-to-confirmed workflow:</p>
 * <ul>
 *   <li><strong>Unconfirmed</strong>: Draft state for incomplete recaps</li>
 *   <li><strong>Confirmed</strong>: Finalized recaps visible in reports and analytics</li>
 *   <li>Allows gradual content creation and editing before publication</li>
 * </ul>
 * 
 * <h2>Media Management</h2>
 * <p>Media attachments are managed through cascading operations:</p>
 * <ul>
 *   <li>Adding media creates new {@link RecapMedia} entities</li>
 *   <li>Removing the recap automatically deletes all associated media</li>
 *   <li>Media ordering is preserved for consistent presentation</li>
 * </ul>
 * 
 * <h2>Content Flexibility</h2>
 * <p>Recaps support various content patterns:</p>
 * <ul>
 *   <li><strong>Text-only</strong>: Simple notes without media</li>
 *   <li><strong>Media-rich</strong>: Multiple attachments with minimal text</li>
 *   <li><strong>Comprehensive</strong>: Detailed notes with supporting media</li>
 *   <li><strong>Placeholder</strong>: Named recap with minimal content</li>
 * </ul>
 * 
 * @see Event
 * @see RecapMedia
 * @see User
 */
@Entity
@Table(name = "event_recaps")
public class EventRecap {

    /** Unique identifier for this event recap. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * The event being recapped. One-to-one relationship ensures
     * each event can have at most one recap.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    /** 
     * The user who created this recap. Must be the same as the event creator
     * to ensure proper ownership and security.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** 
     * Free-form notes providing detailed recap content.
     * Stored as TEXT for unlimited length content.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** 
     * Optional name/title for the recap to aid in organization.
     * Provides a quick identifier for the recap content.
     */
    @Column(length = ApplicationConstants.MEDIUM_TEXT_MAX_LENGTH)
    private String recapName;

    /** 
     * Whether this recap is in draft (unconfirmed) state.
     * Unconfirmed recaps are not included in analytics and reports.
     */
    @Column(nullable = false)
    private boolean unconfirmed = true;

    /** 
     * Collection of media attachments associated with this recap.
     * Automatically deleted when the recap is removed.
     */
    @OneToMany(mappedBy = "recap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecapMedia> media = new ArrayList<>();

    /**
     * Default constructor for JPA.
     */
    protected EventRecap() {
        // for JPA
    }

    /**
     * Private constructor that enforces ownership constraints.
     * Validates that the recap creator matches the event creator.
     * 
     * @param event the event being recapped
     * @param creator the user creating the recap
     * @param notes optional recap notes
     * @param recapName optional recap name
     * @param unconfirmed whether this is a draft recap
     * @throws IllegalArgumentException if creator doesn't match event creator
     */
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

    /**
     * Creates a confirmed (finalized) recap for the specified event.
     * 
     * @param event the event to recap
     * @param creator the user creating the recap (must match event creator)
     * @param notes optional recap notes
     * @param recapName optional recap name
     * @return a new confirmed EventRecap
     * @throws IllegalArgumentException if creator doesn't match event creator
     */
    public static EventRecap createConfirmedRecap(Event event, User creator, String notes, String recapName) {
        return new EventRecap(event, creator, notes, recapName, false);
    }

    /**
     * Creates an unconfirmed (draft) recap for the specified event.
     * 
     * @param event the event to recap
     * @param creator the user creating the recap (must match event creator)
     * @param notes optional recap notes
     * @param recapName optional recap name
     * @return a new unconfirmed EventRecap
     * @throws IllegalArgumentException if creator doesn't match event creator
     */
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
