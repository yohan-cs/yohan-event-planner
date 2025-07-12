package com.yohan.event_planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

/**
 * Entity representing a user-defined label for categorizing and organizing events.
 * 
 * <p>Labels serve as the fundamental categorization mechanism in the event planning system,
 * enabling users to:</p>
 * <ul>
 *   <li><strong>Organize events</strong>: Group related events under meaningful categories</li>
 *   <li><strong>Track time</strong>: Generate time statistics and analytics by category</li>
 *   <li><strong>Filter and search</strong>: Find events based on categorization</li>
 *   <li><strong>Create badges</strong>: Combine multiple labels into higher-level groupings</li>
 * </ul>
 * 
 * <h2>Uniqueness Constraints</h2>
 * <p>Labels enforce uniqueness per user through a composite unique constraint on
 * {@code (creator_id, name)}. This ensures that:</p>
 * <ul>
 *   <li>Each user can have only one label with a given name</li>
 *   <li>Different users can have labels with the same name</li>
 *   <li>Label names are case-sensitive within a user's scope</li>
 * </ul>
 * 
 * <h2>Relationship Mapping</h2>
 * <p>Labels participate in several important relationships:</p>
 * <ul>
 *   <li><strong>Events</strong>: Many events can be associated with one label</li>
 *   <li><strong>Recurring Events</strong>: Recurring patterns can be categorized with labels</li>
 *   <li><strong>Badges</strong>: Multiple labels can be grouped under badges for analytics</li>
 *   <li><strong>Time Buckets</strong>: Labels generate time-based statistics through bucket aggregation</li>
 * </ul>
 * 
 * <h2>Lifecycle Management</h2>
 * <p>Labels support:</p>
 * <ul>
 *   <li><strong>Creation</strong>: Simple name-based creation by authenticated users</li>
 *   <li><strong>Updating</strong>: Name modifications (subject to uniqueness constraints)</li>
 *   <li><strong>Deletion</strong>: Removal with cascading cleanup of associations</li>
 * </ul>
 * 
 * @see Event
 * @see RecurringEvent  
 * @see Badge
 * @see User
 */
@Entity
@Table(
        name = "labels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "name"})
)
public class Label {

    /** Unique identifier for this label. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * The display name for this label, unique per user.
     * Used for categorization and must be non-null.
     */
    @Column(nullable = false)
    private String name;

    /** 
     * The user who created and owns this label.
     * Labels are scoped to individual users for privacy and organization.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    /**
     * Default constructor for JPA.
     */
    public Label() {}

    /**
     * Creates a new label with the specified name and creator.
     * 
     * @param name the display name for the label (must be unique per user)
     * @param creator the user who will own this label
     */
    public Label(String name, User creator) {
        this.name = name;
        this.creator = creator;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getCreator() {
        return creator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    /**
     * Equality is based on database identity (id), not name or creator.
     * This ensures that labels with the same name from different users are distinct,
     * and prevents issues with transient vs persistent entities.
     * 
     * @param o the object to compare with
     * @return true if the objects represent the same label entity
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        Label that = (Label) o;
        return id != null && id.equals(that.id);
    }

    /**
     * Hash code based on the database ID for consistency with equals().
     * 
     * @return hash code for this label
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
