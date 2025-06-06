package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user accounts.
 *
 * <p>
 * Provides endpoints for retrieving, updating, and deleting the authenticated user's account,
 * as well as looking up user profiles by username.
 * </p>
 */
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    /**
     * Constructs a new {@code UserController} with the given service.
     *
     * @param userService the service used to manage user-related operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
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
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    /**
     * Retrieves a user profile by username.
     *
     * @param username the username to look up
     * @return the user profile
     */
    @Operation(summary = "Get a public user profile by username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    /**
     * Updates the authenticated user's profile using the provided patch data.
     *
     * @param updateDTO the fields to update (may include first name, last name, timezone, etc.)
     * @return the updated user profile as a {@link UserResponseDTO}
     */
    @Operation(summary = "Update authenticated user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(@RequestBody @Valid UserUpdateDTO updateDTO) {
        return ResponseEntity.ok(userService.updateCurrentUser(updateDTO));
    }

    /**
     * Deletes the authenticated user's account (soft delete).
     *
     * @return 204 No Content if successful
     */
    @Operation(summary = "Delete authenticated user's account (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
