package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.service.BadgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user badges in the event planning system.
 * 
 * <p>This controller provides endpoints for complete badge lifecycle management including
 * creation, retrieval, updates, and deletion. Badges serve as multi-label collections
 * that enable users to group related labels for enhanced time tracking and analytics.</p>
 * 
 * <h2>Security</h2>
 * <p>All endpoints require JWT authentication except where explicitly noted.
 * Badge operations are restricted to the badge owner through service-layer validation.</p>
 * 
 * <h2>API Features</h2>
 * <ul>
 *   <li>Full CRUD operations for badge management</li>
 *   <li>Integrated time statistics aggregation</li>
 *   <li>Label association and ordering management</li>
 *   <li>Comprehensive OpenAPI documentation</li>
 * </ul>
 * 
 * @see BadgeService
 * @see com.yohan.event_planner.domain.Badge
 * @see BadgeResponseDTO
 */
@Tag(name = "Badges", description = "Multi-label collections and time analytics")
@RestController
@RequestMapping("/badges")
@SecurityRequirement(name = "Bearer Authentication")
public class BadgeController {

    private static final Logger logger = LoggerFactory.getLogger(BadgeController.class);
    private final BadgeService badgeService;

    /**
     * Constructs a new BadgeController with the required service dependency.
     * 
     * <p>This constructor is used by Spring's dependency injection to provide
     * the BadgeService implementation for all badge operations. The controller
     * delegates all business logic to the service layer while handling HTTP
     * request/response concerns.</p>
     * 
     * @param badgeService the service for badge operations, must not be null
     */
    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /**
     * Retrieves a specific badge by its unique identifier.
     * 
     * <p>Returns detailed badge information including associated labels and aggregated time statistics.
     * The badge must be owned by the authenticated user to be accessible.</p>
     * 
     * <p><strong>Architecture Note:</strong> Currently, this endpoint does not enforce ownership
     * validation at the service layer, allowing read access to badges across users. This design
     * decision differs from write operations (create/update/delete) which strictly enforce
     * ownership. This may be intentional for scenarios like badge sharing or public viewing,
     * but should be reviewed for consistency with the application's security model.</p>
     * 
     * @param id the unique identifier of the badge to retrieve
     * @return ResponseEntity containing the badge data with HTTP 200 status
     * @throws com.yohan.event_planner.exception.BadgeNotFoundException if badge doesn't exist
     * @throws com.yohan.event_planner.exception.BadgeOwnershipException if user doesn't own the badge
     */
    @Operation(
            summary = "Get badge by ID",
            description = "Retrieve detailed information about a specific badge including associated labels and time statistics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Badge retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BadgeResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the badge owner"),
            @ApiResponse(responseCode = "404", description = "Badge not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BadgeResponseDTO> getBadgeById(
            @Parameter(description = "Badge ID", required = true)
            @PathVariable Long id) {
        logger.debug("Received request to get badge with ID: {}", id);
        try {
            BadgeResponseDTO badgeResponseDTO = badgeService.getBadgeById(id);
            logger.info("Successfully retrieved badge {} for user", badgeResponseDTO.id());
            return ResponseEntity.ok(badgeResponseDTO);
        } catch (Exception e) {
            logger.warn("Failed to retrieve badge with ID: {} - {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a new badge for the authenticated user.
     * 
     * <p>Creates a badge with the specified name and optional label associations.
     * The badge will be assigned the next available sort order for the user.</p>
     * 
     * @param badgeCreateDTO the badge creation data containing name and optional label IDs
     * @return ResponseEntity containing the created badge data with HTTP 201 status
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     */
    @Operation(
            summary = "Create a new badge",
            description = "Create a new badge for organizing multiple labels with time tracking capabilities"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Badge created successfully",
                    content = @Content(schema = @Schema(implementation = BadgeResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping
    public ResponseEntity<BadgeResponseDTO> createBadge(
            @Parameter(description = "Badge creation data", required = true)
            @Valid @RequestBody BadgeCreateDTO badgeCreateDTO) {
        logger.debug("Received request to create badge with name: {}", badgeCreateDTO.name());
        try {
            BadgeResponseDTO createdBadge = badgeService.createBadge(badgeCreateDTO);
            logger.info("Badge created successfully with ID: {}", createdBadge.id());
            return new ResponseEntity<>(createdBadge, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.warn("Failed to create badge with name: {} - {}", badgeCreateDTO.name(), e.getMessage());
            throw e;
        }
    }

    /**
     * Updates an existing badge with partial data.
     * 
     * <p>Performs partial updates to a badge including name modifications.
     * Only provided fields in the DTO will be updated, others remain unchanged.
     * The badge must be owned by the authenticated user.</p>
     * 
     * @param id the unique identifier of the badge to update
     * @param badgeUpdateDTO the badge update data containing fields to modify
     * @return ResponseEntity containing the updated badge data with HTTP 200 status
     * @throws com.yohan.event_planner.exception.BadgeNotFoundException if badge doesn't exist
     * @throws com.yohan.event_planner.exception.BadgeOwnershipException if user doesn't own the badge
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     */
    @Operation(
            summary = "Update an existing badge",
            description = "Perform partial updates to a badge including name, description, and label associations"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Badge updated successfully",
                    content = @Content(schema = @Schema(implementation = BadgeResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the badge owner"),
            @ApiResponse(responseCode = "404", description = "Badge not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<BadgeResponseDTO> updateBadge(
            @Parameter(description = "Badge ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Badge update data", required = true)
            @Valid @RequestBody BadgeUpdateDTO badgeUpdateDTO) {
        logger.debug("Received request to update badge with ID: {}", id);
        try {
            BadgeResponseDTO updatedBadge = badgeService.updateBadge(id, badgeUpdateDTO);
            logger.info("Badge {} updated successfully", id);
            return ResponseEntity.ok(updatedBadge);
        } catch (Exception e) {
            logger.warn("Failed to update badge with ID: {} - {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Permanently deletes a badge and all its associations.
     * 
     * <p>Removes the badge from the system including all label associations.
     * This operation cannot be undone. The badge must be owned by the authenticated user.</p>
     * 
     * @param id the unique identifier of the badge to delete
     * @return ResponseEntity with HTTP 204 No Content status on successful deletion
     * @throws com.yohan.event_planner.exception.BadgeNotFoundException if badge doesn't exist
     * @throws com.yohan.event_planner.exception.BadgeOwnershipException if user doesn't own the badge
     */
    @Operation(
            summary = "Delete a badge",
            description = "Permanently delete a badge and remove all label associations"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Badge deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the badge owner"),
            @ApiResponse(responseCode = "404", description = "Badge not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadge(
            @Parameter(description = "Badge ID", required = true)
            @PathVariable Long id) {
        logger.debug("Received request to delete badge with ID: {}", id);
        try {
            badgeService.deleteBadge(id);
            logger.info("Badge {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.warn("Failed to delete badge with ID: {} - {}", id, e.getMessage());
            throw e;
        }
    }
}
