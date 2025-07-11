package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.UserToolsResponseDTO;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.service.BadgeService;
import com.yohan.event_planner.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User Tools", description = "User utilities and aggregated data")
@RestController
@RequestMapping("/usertools")
@SecurityRequirement(name = "Bearer Authentication")
public class UserToolsController {

    private final BadgeService badgeService;
    private final LabelService labelService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserToolsController(BadgeService badgeService,
                               LabelService labelService,
                               AuthenticatedUserProvider authenticatedUserProvider) {
        this.badgeService = badgeService;
        this.labelService = labelService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Operation(
            summary = "Get user tools",
            description = "Retrieve all badges and labels for the authenticated user in a single aggregated response for quick access to categorization tools"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "User tools retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserToolsResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping
    public ResponseEntity<UserToolsResponseDTO> getUserTools() {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();

        List<BadgeResponseDTO> badges = badgeService.getBadgesByUser(userId);
        List<LabelResponseDTO> labels = labelService.getLabelsByUser(userId);

        return ResponseEntity.ok(new UserToolsResponseDTO(badges, labels));
    }
}