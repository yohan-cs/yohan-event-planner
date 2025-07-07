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

@Entity
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ElementCollection
    @CollectionTable(name = "badge_label_ids", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "label_id")
    private Set<Long> labelIds = new HashSet<>();

    @ElementCollection
    @OrderColumn(name = "label_order_index")
    @CollectionTable(name = "badge_label_order", joinColumns = @JoinColumn(name = "badge_id"))
    @Column(name = "label_id")
    private List<Long> labelOrder = new ArrayList<>();

    @Column(nullable = false)
    private int sortOrder;

    // Constructors
    public Badge() {
    }

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

    // Label management
    public void addLabelIds(Set<Long> labelIdsToAdd) {
        for (Long id : labelIdsToAdd) {
            if (this.labelIds.add(id)) {
                this.labelOrder.add(id);
            }
        }
    }

    public void removeLabelIds(Set<Long> labelIdsToRemove) {
        for (Long id : labelIdsToRemove) {
            if (this.labelIds.remove(id)) {
                this.labelOrder.remove(id);
            }
        }
    }
}