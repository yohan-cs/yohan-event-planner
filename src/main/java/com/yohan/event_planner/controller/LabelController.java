package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.service.LabelService;
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
 * REST controller for managing user-defined event labels.
 * 
 * <p>Provides CRUD operations for labels that enable users to categorize and organize
 * their events. All endpoints require JWT authentication and enforce ownership validation
 * to ensure users can only access their own labels.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Label Management</strong>: Complete lifecycle operations (create, read, update, delete)</li>
 *   <li><strong>Ownership Security</strong>: Automatic validation that users can only access their own labels</li>
 *   <li><strong>Validation</strong>: Request validation through Bean Validation annotations</li>
 *   <li><strong>OpenAPI Documentation</strong>: Comprehensive API documentation with examples</li>
 * </ul>
 * 
 * <h2>Security Model</h2>
 * <p>All endpoints require JWT authentication via the Authorization header. The service layer
 * automatically validates label ownership, ensuring users cannot access or modify labels
 * owned by other users.</p>
 * 
 * <h2>Error Handling</h2>
 * <p>Standard HTTP status codes are used:</p>
 * <ul>
 *   <li><strong>200 OK</strong>: Successful retrieval or update</li>
 *   <li><strong>201 Created</strong>: Successful label creation</li>
 *   <li><strong>204 No Content</strong>: Successful deletion</li>
 *   <li><strong>400 Bad Request</strong>: Validation failures or duplicate names</li>
 *   <li><strong>401 Unauthorized</strong>: Missing or invalid JWT token</li>
 *   <li><strong>403 Forbidden</strong>: Insufficient permissions or ownership violations</li>
 *   <li><strong>404 Not Found</strong>: Label does not exist</li>
 * </ul>
 * 
 * @see LabelService
 * @see LabelCreateDTO
 * @see LabelUpdateDTO
 * @see LabelResponseDTO
 * @since 1.0.0
 */
@Tag(name = "Labels", description = "Event categorization and organization")
@RestController
@RequestMapping("/labels")
@SecurityRequirement(name = "Bearer Authentication")
public class LabelController {

    private static final Logger logger = LoggerFactory.getLogger(LabelController.class);
    private final LabelService labelService;

    /**
     * Constructs a new LabelController with the required service dependency.
     * 
     * @param labelService the service for managing label operations and business logic
     */
    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    /**
     * Retrieves a label by its unique identifier.
     * 
     * <p>This endpoint validates that the current authenticated user owns the requested label
     * before returning the label details. Ownership validation is performed automatically
     * by the service layer to ensure security isolation between users.</p>
     * 
     * @param id the unique identifier of the label to retrieve
     * @return ResponseEntity containing the label details if found and owned by current user
     * @throws LabelNotFoundException if no label exists with the given ID (returns 404)
     * @throws LabelOwnershipException if the label is not owned by the current user (returns 403)
     */
    @Operation(
            summary = "Get label by ID",
            description = "Retrieve detailed information about a specific label including color and categorization settings"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Label retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LabelResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the label owner"),
            @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LabelResponseDTO> getLabelById(
            @Parameter(description = "Label ID", required = true, example = "123")
            @PathVariable Long id) {
        logger.debug("Retrieving label with ID: {}", id);
        LabelResponseDTO labelResponseDTO = labelService.getLabelById(id);
        logger.info("Successfully retrieved label ID: {} for user: {}", labelResponseDTO.id(), labelResponseDTO.creatorUsername());
        return ResponseEntity.ok(labelResponseDTO);
    }

    /**
     * Creates a new label for the authenticated user.
     * 
     * <p>This endpoint creates a new label with the provided name and assigns it to the
     * current authenticated user. The label name must be unique within the user's scope.
     * Bean validation ensures the label name meets the required constraints.</p>
     * 
     * @param labelCreateDTO the label creation data containing the name and other properties
     * @return ResponseEntity with HTTP 201 status and the created label details
     * @throws LabelException if a label with the same name already exists for the user (returns 400)
     */
    @Operation(
            summary = "Create a new label",
            description = "Create a new label for categorizing and organizing events with custom colors and names"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Label created successfully",
                    content = @Content(schema = @Schema(implementation = LabelResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping
    public ResponseEntity<LabelResponseDTO> createLabel(
            @Parameter(description = "Label creation data", required = true)
            @Valid @RequestBody LabelCreateDTO labelCreateDTO) {
        logger.debug("Creating new label with name: '{}'", labelCreateDTO.name());
        LabelResponseDTO createdLabel = labelService.createLabel(labelCreateDTO);
        logger.info("Successfully created label '{}' with ID: {}", createdLabel.name(), createdLabel.id());
        return new ResponseEntity<>(createdLabel, HttpStatus.CREATED);
    }

    /**
     * Updates an existing label owned by the authenticated user.
     * 
     * <p>This endpoint supports partial updates using PATCH semantics. Only non-null fields
     * in the update DTO are applied to the label. The service validates ownership and ensures
     * the label is not a system-managed label (e.g., "Unlabeled") before applying changes.
     * Name uniqueness is enforced within the user's scope.</p>
     * 
     * @param id the unique identifier of the label to update
     * @param labelUpdateDTO the update data containing fields to modify
     * @return ResponseEntity containing the updated label details
     * @throws LabelNotFoundException if no label exists with the given ID (returns 404)
     * @throws LabelOwnershipException if the label is not owned by the current user (returns 403)
     * @throws SystemManagedEntityException if attempting to modify a system-managed label (returns 403)
     * @throws LabelException if the new name conflicts with an existing label name for the user (returns 400)
     */
    @Operation(
            summary = "Update an existing label",
            description = "Perform partial updates to a label including name, color, and display settings"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Label updated successfully",
                    content = @Content(schema = @Schema(implementation = LabelResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the label owner"),
            @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<LabelResponseDTO> updateLabel(
            @Parameter(description = "Label ID", required = true, example = "123")
            @PathVariable Long id, 
            @Parameter(description = "Label update data", required = true)
            @Valid @RequestBody LabelUpdateDTO labelUpdateDTO) {
        logger.debug("Updating label ID: {}", id);
        LabelResponseDTO updatedLabel = labelService.updateLabel(id, labelUpdateDTO);
        logger.info("Successfully updated label ID: {}", id);
        return ResponseEntity.ok(updatedLabel);
    }


    /**
     * Deletes a label owned by the authenticated user.
     * 
     * <p>This endpoint permanently removes a label from the system. The service validates
     * ownership and ensures the label is not a system-managed label (e.g., "Unlabeled")
     * before deletion. Deletion has cascading effects on associated entities such as
     * events, badges, and time buckets, which are handled by database constraints.</p>
     * 
     * @param id the unique identifier of the label to delete
     * @return ResponseEntity with HTTP 204 status indicating successful deletion
     * @throws LabelNotFoundException if no label exists with the given ID (returns 404)
     * @throws LabelOwnershipException if the label is not owned by the current user (returns 403)
     * @throws SystemManagedEntityException if attempting to delete a system-managed label (returns 403)
     */
    @Operation(
            summary = "Delete a label",
            description = "Permanently delete a label and remove it from all associated events and badges"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Label deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the label owner"),
            @ApiResponse(responseCode = "404", description = "Label not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(
            @Parameter(description = "Label ID", required = true, example = "123")
            @PathVariable Long id) {
        logger.debug("Deleting label ID: {}", id);
        labelService.deleteLabel(id);
        logger.info("Successfully deleted label ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
