package com.yohan.event_planner.controller;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the authenticated user's account.
 *
 * <p>
 * Provides endpoints for retrieving, updating, and deleting the currently authenticated user.
 * </p>
 */
@RestController
@RequestMapping("/settings")
@SecurityRequirement(name = "Bearer Authentication")
public class UserSettingsController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserPatchHandler userPatchHandler;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserSettingsController(
            UserService userService,
            UserMapper userMapper,
            UserPatchHandler userPatchHandler,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userPatchHandler = userPatchHandler;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return the user's profile as a {@link UserResponseDTO}
     */
    @Operation(summary = "Get profile of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<UserResponseDTO> getSettings() {
        return ResponseEntity.ok(userService.getUserSettings());
    }

    /**
     * Applies a partial update to the authenticated user's profile.
     *
     * @param updateDTO the fields to update
     * @return the updated user profile
     */
    @Operation(summary = "Update authenticated user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping
    public ResponseEntity<UserResponseDTO> updateSettings(@RequestBody @Valid UserUpdateDTO updateDTO) {
        return ResponseEntity.ok(userService.updateUserSettings(updateDTO));
    }

    /**
     * Deletes the authenticated user's account.
     *
     * @return 204 No Content if successful
     */
    @Operation(summary = "Delete authenticated user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount() {
        userService.markUserForDeletion();
        return ResponseEntity.noContent().build();
    }
}
