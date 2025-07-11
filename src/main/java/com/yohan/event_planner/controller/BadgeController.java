package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.service.BadgeService;
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

@Tag(name = "Badges", description = "Multi-label collections and time analytics")
@RestController
@RequestMapping("/badges")
@SecurityRequirement(name = "Bearer Authentication")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

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
        BadgeResponseDTO badgeResponseDTO = badgeService.getBadgeById(id);
        return badgeResponseDTO != null ? ResponseEntity.ok(badgeResponseDTO) : ResponseEntity.notFound().build();
    }

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
        BadgeResponseDTO createdBadge = badgeService.createBadge(badgeCreateDTO);
        return new ResponseEntity<>(createdBadge, HttpStatus.CREATED);
    }

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
        BadgeResponseDTO updatedBadge = badgeService.updateBadge(id, badgeUpdateDTO);
        return ResponseEntity.ok(updatedBadge);
    }

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
        badgeService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }
}
