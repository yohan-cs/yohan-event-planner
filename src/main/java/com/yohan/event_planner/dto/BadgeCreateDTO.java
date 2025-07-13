package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Data Transfer Object for creating new badges.
 *
 * <p>Badges are collections of related labels that provide comprehensive time tracking
 * and analytics across multiple activity categories. They enable users to group related
 * activities and monitor aggregate time spent across different types of work or personal tasks.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a fitness badge with multiple exercise labels
 * BadgeCreateDTO fitnessBadge = new BadgeCreateDTO(
 *     "Fitness & Health",
 *     Set.of(101L, 102L, 103L)  // gym, running, yoga label IDs
 * );
 *
 * // Create a work badge with project-related labels
 * BadgeCreateDTO workBadge = new BadgeCreateDTO(
 *     "Project Alpha",
 *     Set.of(201L, 202L)  // development, meetings label IDs
 * );
 * }</pre>
 *
 * <h2>Badge Analytics</h2>
 * <p>Once created, badges automatically provide time statistics including:</p>
 * <ul>
 *   <li><strong>Today's Time</strong>: Minutes spent on badge activities today</li>
 *   <li><strong>Weekly Tracking</strong>: Current week and last week comparisons</li>
 *   <li><strong>Monthly Analytics</strong>: Current month and last month totals</li>
 *   <li><strong>All-Time Statistics</strong>: Complete historical tracking</li>
 * </ul>
 *
 * <h2>Label Management</h2>
 * <p>Labels can be:</p>
 * <ul>
 *   <li>Associated during badge creation via {@code labelIds}</li>
 *   <li>Added or removed later through badge updates</li>
 *   <li>Reordered within the badge for custom organization</li>
 *   <li>Shared across multiple badges for flexible categorization</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Name</strong>: Required, non-blank, maximum 255 characters</li>
 *   <li><strong>Label IDs</strong>: Optional set of existing label identifiers</li>
 *   <li><strong>Ownership</strong>: Label IDs must belong to the authenticated user</li>
 *   <li><strong>Uniqueness</strong>: Badge names must be unique per user</li>
 * </ul>
 *
 * @param name the display name for the badge (required, max 255 characters)
 * @param labelIds optional set of label IDs to associate with this badge
 */
public record BadgeCreateDTO(

        @NotBlank(message = "Badge name must not be blank")
        @Size(max = 255, message = "Badge name must not exceed 255 characters")
        String name,
        Set<Long> labelIds
) {}
