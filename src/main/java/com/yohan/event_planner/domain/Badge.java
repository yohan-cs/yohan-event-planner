package com.yohan.event_planner.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a badge - a multi-label collection for organizing and tracking time statistics.
 * 
 * <p>Badges serve as containers for grouping multiple {@link Label}s together to enable:
 * <ul>
 *   <li><strong>Multi-label time tracking</strong>: Aggregate time statistics across related labels</li>
 *   <li><strong>Organized categorization</strong>: Group related labels under a single badge</li>
 *   <li><strong>Custom ordering</strong>: Define the display order of labels within the badge</li>
 *   <li><strong>Analytics grouping</strong>: Generate combined reports for badge-associated labels</li>
 * </ul>
 * 
 * <h2>Label Management</h2>
 * <p>Badges maintain labels in two complementary collections:</p>
 * <ul>
 *   <li><strong>labelIds</strong>: A {@link Set} for fast membership checking and uniqueness</li>
 *   <li><strong>labelOrder</strong>: A {@link List} defining the display order of labels</li>
 * </ul>
 * 
 * <p>This dual-collection approach ensures both performance and ordering consistency.
 * When labels are added or removed, both collections are updated atomically.</p>
 * 
 * <h2>Sorting and Organization</h2>
 * <p>Badges support hierarchical organization through:</p>
 * <ul>
 *   <li><strong>Badge-level sorting</strong>: {@code sortOrder} determines badge display order</li>
 *   <li><strong>Label-level ordering</strong>: {@code labelOrder} controls label sequence within badges</li>
 * </ul>
 * 
 * <h2>Time Statistics Integration</h2>
 * <p>Badges enable sophisticated time analytics by:</p>
 * <ul>
 *   <li>Aggregating time spent across all associated labels</li>
 *   <li>Providing comparative statistics between label groups</li>
 *   <li>Supporting custom time bucket analysis</li>
 * </ul>
 * 
 * @see Label
 * @see User
 */
@Entity
public class Badge {

    /** Unique identifier for this badge. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name for the badge, used in UI and analytics. */
    @Column(nullable = false)
    private String name;

    /** The user who owns this badge and its associated labels. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    /** 
     * Set of label IDs associated with this badge for fast membership checking.
     * Maintained in sync with {@code labelOrder} to ensure consistency.
     */
    @ElementCollection
    @CollectionTable(name = "badge_label_ids", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "label_id")
    private Set<Long> labelIds = new HashSet<>();

    /** 
     * Ordered list of label IDs defining the display sequence within this badge.
     * The order is preserved using {@code @OrderColumn} for consistent display.
     */
    @ElementCollection
    @OrderColumn(name = "label_order_index")
    @CollectionTable(name = "badge_label_order", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "label_id")
    private List<Long> labelOrder = new ArrayList<>();

    /** 
     * Sort order for this badge relative to other badges owned by the same user.
     * Lower values appear first in sorted lists.
     */
    @Column(nullable = false)
    private int sortOrder;

    /**
     * Default constructor for JPA.
     */
    public Badge() {
    }

    /**
     * Creates a new badge with the specified properties.
     * 
     * @param name the display name for the badge
     * @param user the user who will own this badge
     * @param sortOrder the sort order relative to other badges
     */
    public Badge(String name, User user, int sortOrder) {
        this.name = name;
        this.user = user;
        this.sortOrder = sortOrder;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }

    public Set<Long> getLabelIds() {
        return labelIds;
    }

    public List<Long> getLabelOrder() {
        return labelOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setLabelOrder(List<Long> labelOrder) {
        this.labelOrder = labelOrder;
    }

    /**
     * Adds multiple label IDs to this badge, maintaining both set membership and ordering.
     * Only adds labels that are not already present in the badge.
     * 
     * @param labelIdsToAdd the set of label IDs to add to this badge
     */
    public void addLabelIds(Set<Long> labelIdsToAdd) {
        for (Long id : labelIdsToAdd) {
            if (this.labelIds.add(id)) {
                this.labelOrder.add(id);
            }
        }
    }

    /**
     * Removes multiple label IDs from this badge, updating both collections atomically.
     * Only removes labels that are currently present in the badge.
     * 
     * @param labelIdsToRemove the set of label IDs to remove from this badge
     */
    public void removeLabelIds(Set<Long> labelIdsToRemove) {
        for (Long id : labelIdsToRemove) {
            if (this.labelIds.remove(id)) {
                this.labelOrder.remove(id);
            }
        }
    }
}