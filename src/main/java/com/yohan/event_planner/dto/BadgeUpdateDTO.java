package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Data Transfer Object for updating existing badges using PATCH semantics.
 *
 * <p>This DTO supports partial updates to badge entities, allowing modification of
 * the badge name while preserving other properties. Label associations are managed
 * separately through dedicated label management endpoints.</p>
 *
 * <h2>PATCH Operation Semantics</h2>
 * <p>The update operation follows HTTP PATCH semantics:</p>
 * <ul>
 *   <li><strong>Partial Updates</strong>: Only provided fields are updated</li>
 *   <li><strong>Name Updates</strong>: Badge name can be modified while preserving label associations</li>
 *   <li><strong>Label Management</strong>: Label associations are managed through separate operations</li>
 *   <li><strong>Sort Order</strong>: Preserved during name updates</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Update badge name only
 * BadgeUpdateDTO nameUpdate = new BadgeUpdateDTO("Updated Project Name");
 *
 * // Badge labels and sort order remain unchanged
 * PATCH /badges/123
 * {
 *   "name": "Updated Project Name"
 * }
 * }</pre>
 *
 * <h2>Label Association Management</h2>
 * <p>Label associations are managed through separate operations:</p>
 * <ul>
 *   <li><strong>Adding Labels</strong>: Use dedicated label association endpoints</li>
 *   <li><strong>Removing Labels</strong>: Use dedicated label removal endpoints</li>
 *   <li><strong>Reordering Labels</strong>: Use label reordering endpoints</li>
 *   <li><strong>Bulk Operations</strong>: Replace entire label set through specialized endpoints</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Name</strong>: Required, non-blank, maximum 255 characters</li>
 *   <li><strong>Uniqueness</strong>: Badge name must be unique per user</li>
 *   <li><strong>Ownership</strong>: Only badge owners can perform updates</li>
 *   <li><strong>Existing Badge</strong>: Badge must exist and be accessible</li>
 * </ul>
 *
 * <h2>Side Effects</h2>
 * <p>Badge updates trigger the following:</p>
 * <ul>
 *   <li><strong>Time Statistics</strong>: Preserved and remain accurate</li>
 *   <li><strong>Label Associations</strong>: Maintained unchanged</li>
 *   <li><strong>Sort Order</strong>: Preserved within user's badge list</li>
 *   <li><strong>Analytics History</strong>: Continues uninterrupted</li>
 * </ul>
 *
 * @param name the new display name for the badge (required, max 255 characters)
 */
public record BadgeUpdateDTO(

        @NotBlank(message = "Badge name must not be blank")
        @Size(max = 255, message = "Badge name must not exceed 255 characters")
        String name
) {}
