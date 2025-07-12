package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing authenticated user settings and account operations.
 *
 * <p>This controller provides endpoints for user profile management including:</p>
 * <ul>
 *   <li>Retrieving current user settings</li>
 *   <li>Performing partial updates to user profiles</li>
 *   <li>Soft deletion of user accounts</li>
 * </ul>
 *
 * <h3>Architecture Context</h3>
 * <p><strong>Layer Responsibilities:</strong></p>
 * <ul>
 *   <li><strong>This Controller</strong>: HTTP request/response handling, input validation</li>
 *   <li><strong>UserService</strong>: Business orchestration, authorization, transaction management</li>
 *   <li><strong>UserPatchHandler</strong>: Atomic patch operations and field validation (via service)</li>
 *   <li><strong>UserBO</strong>: Persistence operations and domain queries (via service)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>All endpoints require JWT authentication via the {@code Authorization} header.
 * User identification is handled automatically through {@link UserService} integration
 * with authentication context.</p>
 *
 * <h3>Error Handling</h3>
 * <p>Business errors are handled by the global exception handler and mapped to appropriate
 * HTTP status codes. Validation errors return 400, authentication errors return 401,
 * and conflict errors (duplicate username/email) return 409.</p>
 *
 * @see UserService
 * @see UserUpdateDTO
 * @see UserResponseDTO
 * @since 1.0
 */
@Tag(name = "User Management", description = "User profile and account management")
@RestController
@RequestMapping("/settings")
@SecurityRequirement(name = "Bearer Authentication")
public class UserSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);

    private final UserService userService;

    /**
     * Constructs a new UserSettingsController with required dependencies.
     *
     * @param userService service layer for user business operations and authentication context
     */
    public UserSettingsController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the current authenticated user's profile settings.
     *
     * <p>Returns comprehensive user information including username, email, personal details,
     * and timezone preferences. This endpoint serves the user's own profile data.</p>
     *
     * @return {@link ResponseEntity} containing the user's profile as {@link UserResponseDTO}
     * @throws UserNotFoundException if the authenticated user no longer exists
     * @see UserService#getUserSettings()
     */
    @Operation(
            summary = "Get profile of the authenticated user",
            description = "Retrieve the current user's profile information including username, email, and account settings"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "User profile returned successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping
    public ResponseEntity<UserResponseDTO> getSettings() {
        logger.debug("Retrieving settings for authenticated user");
        UserResponseDTO result = userService.getUserSettings();
        logger.info("Successfully retrieved settings for user: {}", result.username());
        return ResponseEntity.ok(result);
    }

    /**
     * Performs partial updates to the authenticated user's profile.
     *
     * <p>Supports atomic updates of user fields using patch semantics. Only non-null
     * fields in the request are updated. Validates uniqueness for username and email
     * changes before applying any modifications.</p>
     *
     * @param updateDTO the partial update data with fields to modify
     * @return {@link ResponseEntity} containing the updated user profile
     * @throws UsernameException if the new username is already taken
     * @throws EmailException if the new email is already taken
     * @throws UserNotFoundException if the authenticated user no longer exists
     * @see UserService#updateUserSettings(UserUpdateDTO)
     */
    @Operation(
            summary = "Update authenticated user's profile",
            description = "Perform partial updates to the user's profile including username, email, and display preferences"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    @PatchMapping
    public ResponseEntity<UserResponseDTO> updateSettings(
            @Parameter(description = "User profile update data", required = true)
            @RequestBody @Valid UserUpdateDTO updateDTO) {
        logger.debug("Updating settings for authenticated user");
        UserResponseDTO result = userService.updateUserSettings(updateDTO);
        logger.info("Successfully updated settings for user: {}", result.username());
        return ResponseEntity.ok(result);
    }

    /**
     * Soft deletes the authenticated user's account.
     *
     * <p>Marks the user account for deletion with a grace period. The account becomes
     * inactive immediately but data is retained for potential recovery. After the grace
     * period, the account becomes eligible for permanent deletion.</p>
     *
     * @return {@link ResponseEntity} with no content (204) on successful deletion
     * @throws UserNotFoundException if the authenticated user no longer exists
     * @see UserService#markUserForDeletion()
     */
    @Operation(
            summary = "Delete authenticated user's account",
            description = "Permanently mark the user account for deletion. This will soft-delete the user and all associated data."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account successfully marked for deletion"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount() {
        logger.info("Processing account deletion request for authenticated user");
        userService.markUserForDeletion();
        logger.warn("User account has been marked for deletion");
        return ResponseEntity.noContent().build();
    }
}
