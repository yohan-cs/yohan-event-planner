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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Management", description = "User profile and account management")
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
        return ResponseEntity.ok(userService.getUserSettings());
    }

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
        return ResponseEntity.ok(userService.updateUserSettings(updateDTO));
    }

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
        userService.markUserForDeletion();
        return ResponseEntity.noContent().build();
    }
}
