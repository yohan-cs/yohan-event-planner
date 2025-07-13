package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.LabelColor;

/**
 * Data Transfer Object for label information within badge responses.
 *
 * <p>This lightweight DTO provides essential label information for badge contexts,
 * including visual color data needed for user interface rendering. It represents
 * a simplified view of labels specifically designed for badge composition and display.</p>
 *
 * <h2>Purpose and Context</h2>
 * <p>BadgeLabelDTO serves as the label representation within badge responses:</p>
 * <ul>
 *   <li><strong>Badge Composition</strong>: Shows which labels are associated with a badge</li>
 *   <li><strong>Visual Design</strong>: Provides color information for UI rendering</li>
 *   <li><strong>Lightweight Transfer</strong>: Minimal data needed for badge displays</li>
 *   <li><strong>Reference Information</strong>: ID and name for client-side operations</li>
 * </ul>
 *
 * <h2>Color Integration</h2>
 * <p>The color field provides access to the full LabelColor palette:</p>
 * <ul>
 *   <li><strong>Base Colors</strong>: Primary color for normal badge display</li>
 *   <li><strong>Pastel Variants</strong>: Softer colors for incomplete activities</li>
 *   <li><strong>Metallic Variants</strong>: Rich colors for completed activities</li>
 *   <li><strong>Consistency</strong>: Same color scheme across all label contexts</li>
 * </ul>
 *
 * <h2>Usage in Badge Responses</h2>
 * <pre>{@code
 * // Badge response with colored labels
 * {
 *   "id": 123,
 *   "name": "Fitness",
 *   "labels": [
 *     {"id": 101, "name": "Gym", "color": "RED"},
 *     {"id": 102, "name": "Running", "color": "ORANGE"},
 *     {"id": 103, "name": "Yoga", "color": "GREEN"}
 *   ]
 * }
 * }</pre>
 *
 * <h2>Client-Side Benefits</h2>
 * <p>This DTO enables rich client functionality:</p>
 * <ul>
 *   <li><strong>Color-Coded Displays</strong>: Visual distinction between different activity types</li>
 *   <li><strong>Badge Visualization</strong>: Colorful badge cards with label indicators</li>
 *   <li><strong>Quick Identification</strong>: Users can quickly identify activity categories</li>
 *   <li><strong>Consistent Theming</strong>: Unified color scheme across the application</li>
 * </ul>
 *
 * <h2>Difference from LabelResponseDTO</h2>
 * <p>Unlike the full LabelResponseDTO, this simplified version:</p>
 * <ul>
 *   <li><strong>Excludes</strong>: Creator information and metadata</li>
 *   <li><strong>Focuses on</strong>: Essential display information only</li>
 *   <li><strong>Optimizes for</strong>: Badge composition and visual rendering</li>
 *   <li><strong>Reduces</strong>: Response payload size for badge endpoints</li>
 * </ul>
 *
 * @param id unique identifier for the label
 * @param name display name of the label
 * @param color visual color from the predefined palette for UI rendering
 */
public record BadgeLabelDTO(
        Long id,
        String name,
        LabelColor color
) {}