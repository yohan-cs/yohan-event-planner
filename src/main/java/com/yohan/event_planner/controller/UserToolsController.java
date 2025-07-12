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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller providing aggregated access to user management tools.
 * 
 * <p>This controller serves as a unified endpoint for user settings and management
 * interfaces, providing consolidated access to badges and labels. It reduces client-side
 * complexity by aggregating related user tools into a single API response.</p>
 * 
 * <h2>Purpose and Scope</h2>
 * <ul>
 *   <li><strong>Tool Aggregation</strong>: Combines badges and labels in single response</li>
 *   <li><strong>Settings Support</strong>: Primary data source for user settings interfaces</li>
 *   <li><strong>Management Interface</strong>: Enables user organization and configuration tools</li>
 * </ul>
 * 
 * <h2>Security</h2>
 * <p>All endpoints require JWT authentication. User tools are filtered to show only
 * items owned by the authenticated user.</p>
 * 
 * <h2>Architecture Integration</h2>
 * <p>This controller provides read-only aggregation and does not duplicate business logic
 * from BadgeController and LabelController. For CRUD operations, clients should use
 * the respective specialized controllers.</p>
 * 
 * @see BadgeService
 * @see LabelService
 * @see UserToolsResponseDTO
 * @see com.yohan.event_planner.controller.BadgeController
 * @see com.yohan.event_planner.controller.LabelController
 */
@Tag(name = "User Tools", description = "User utilities and aggregated data")
@RestController
@RequestMapping("/usertools")
@SecurityRequirement(name = "Bearer Authentication")
public class UserToolsController {

    private static final Logger logger = LoggerFactory.getLogger(UserToolsController.class);
    private final BadgeService badgeService;
    private final LabelService labelService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Constructs a UserToolsController with required service dependencies.
     * 
     * @param badgeService service for badge operations, must not be null
     * @param labelService service for label operations, must not be null
     * @param authenticatedUserProvider provider for authenticated user context, must not be null
     */
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
    /**
     * Retrieves all user tools for the authenticated user in a single aggregated response.
     * 
     * <p>This method provides a consolidated view of user management tools, combining
     * badges and labels owned by the authenticated user. The response is optimized
     * for user settings and management interfaces that need access to both tool types.</p>
     * 
     * <p>The operation includes:</p>
     * <ul>
     *   <li>All badges owned by the user with computed statistics</li>
     *   <li>All labels owned by the user (excluding system-managed labels)</li>
     *   <li>Proper ordering based on user preferences</li>
     * </ul>
     * 
     * @return ResponseEntity containing aggregated user tools with HTTP 200 status
     * @throws com.yohan.event_planner.exception.UnauthorizedException if JWT token is invalid
     * @throws com.yohan.event_planner.exception.UserNotFoundException if authenticated user doesn't exist
     */
    @GetMapping
    public ResponseEntity<UserToolsResponseDTO> getUserTools() {
        logger.debug("Received request to get user tools");
        try {
            Long userId = authenticatedUserProvider.getCurrentUser().getId();
            logger.debug("Retrieving tools for user ID: {}", userId);

            List<BadgeResponseDTO> badges = badgeService.getBadgesByUser(userId);
            List<LabelResponseDTO> labels = labelService.getLabelsByUser(userId);

            logger.info("Successfully retrieved {} badges and {} labels for user {}", 
                       badges.size(), labels.size(), userId);
            return ResponseEntity.ok(new UserToolsResponseDTO(badges, labels));
        } catch (Exception e) {
            logger.warn("Failed to retrieve user tools - {}", e.getMessage());
            throw e;
        }
    }
}