package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.service.LabelService;
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

@Tag(name = "Labels", description = "Event categorization and organization")
@RestController
@RequestMapping("/labels")
@SecurityRequirement(name = "Bearer Authentication")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

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
            @Parameter(description = "Label ID", required = true)
            @PathVariable Long id) {
        LabelResponseDTO labelResponseDTO = labelService.getLabelById(id);
        return labelResponseDTO != null ? ResponseEntity.ok(labelResponseDTO) : ResponseEntity.notFound().build();
    }

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
        LabelResponseDTO createdLabel = labelService.createLabel(labelCreateDTO);
        return new ResponseEntity<>(createdLabel, HttpStatus.CREATED);
    }

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
            @Parameter(description = "Label ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Label update data", required = true)
            @Valid @RequestBody LabelUpdateDTO labelUpdateDTO) {
        LabelResponseDTO updatedLabel = labelService.updateLabel(id, labelUpdateDTO);
        return ResponseEntity.ok(updatedLabel);
    }


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
            @Parameter(description = "Label ID", required = true)
            @PathVariable Long id) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }
}
