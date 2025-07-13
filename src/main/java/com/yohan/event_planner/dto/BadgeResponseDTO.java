package com.yohan.event_planner.dto;

import java.util.Set;

/**
 * Data Transfer Object for badge responses containing complete badge information with analytics.
 *
 * <p>This response DTO provides comprehensive badge information including associated labels,
 * time tracking statistics, and organizational data. It serves as the primary data structure
 * for badge-related API responses and supports rich client-side functionality.</p>
 *
 * <h2>Response Structure</h2>
 * <p>The response includes:</p>
 * <ul>
 *   <li><strong>Identity</strong>: Unique badge identifier and display name</li>
 *   <li><strong>Organization</strong>: Sort order within user's badge collection</li>
 *   <li><strong>Analytics</strong>: Comprehensive time tracking statistics</li>
 *   <li><strong>Labels</strong>: Associated labels with color information for visual display</li>
 * </ul>
 *
 * <h2>Time Statistics Integration</h2>
 * <p>The {@code timeStats} field provides real-time analytics aggregated across all labels:</p>
 * <ul>
 *   <li><strong>Today</strong>: Minutes spent on badge activities today</li>
 *   <li><strong>This Week</strong>: Current week total with week-over-week comparison</li>
 *   <li><strong>This Month</strong>: Current month total with month-over-month comparison</li>
 *   <li><strong>Historical Data</strong>: Last week, last month, and all-time statistics</li>
 * </ul>
 *
 * <h2>Label Information</h2>
 * <p>Each label in the {@code labels} set provides:</p>
 * <ul>
 *   <li><strong>Identity</strong>: Label ID and name for reference</li>
 *   <li><strong>Visual Design</strong>: Color information from predefined palette</li>
 *   <li><strong>User Interface</strong>: Data needed for color-coded badge displays</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // API Response for a fitness badge
 * {
 *   "id": 123,
 *   "name": "Fitness & Health",
 *   "sortOrder": 1,
 *   "timeStats": {
 *     "minutesToday": 45,
 *     "minutesThisWeek": 180,
 *     "minutesThisMonth": 720
 *   },
 *   "labels": [
 *     {"id": 101, "name": "Gym", "color": "RED"},
 *     {"id": 102, "name": "Running", "color": "ORANGE"},
 *     {"id": 103, "name": "Yoga", "color": "GREEN"}
 *   ]
 * }
 * }</pre>
 *
 * <h2>Client-Side Integration</h2>
 * <p>This DTO supports rich client functionality:</p>
 * <ul>
 *   <li><strong>Dashboard Widgets</strong>: Time statistics for progress tracking</li>
 *   <li><strong>Badge Organization</strong>: Sort order for custom arrangements</li>
 *   <li><strong>Visual Design</strong>: Color-coded labels for quick identification</li>
 *   <li><strong>Analytics Views</strong>: Historical trends and comparisons</li>
 * </ul>
 *
 * @param id unique identifier for the badge
 * @param name display name of the badge
 * @param sortOrder position within the user's badge collection (0-based)
 * @param timeStats comprehensive time tracking statistics across all badge labels
 * @param labels set of associated labels with visual color information
 */
public record BadgeResponseDTO(
        Long id,
        String name,
        int sortOrder,
        TimeStatsDTO timeStats,
        Set<BadgeLabelDTO> labels
) {}