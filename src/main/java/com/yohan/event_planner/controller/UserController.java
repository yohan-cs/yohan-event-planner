package com.yohan.event_planner.controller;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.SecurityService;
import com.yohan.event_planner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the current authenticated user and public user profiles.
 * <p>
 * Provides endpoints for retrieving, updating, and deleting the current user's account,
 * as well as looking up public profiles by username.
 * </p>
 *
 * <p>Note: This controller is scoped for the initial version. No follower/block/profile-privacy features are included.</p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final SecurityService securityService;
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(SecurityService securityService,
                          UserService userService,
                          UserMapper userMapper) {
        this.securityService = securityService;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // region GET /users/me

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return the user's profile as a {@link UserResponseDTO}
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile() {
        User user = securityService.getAuthenticatedUser();
        return ResponseEntity.ok(userMapper.toResponseDTO(user));
    }

    // endregion

    // region PATCH /users/me

    /**
     * Updates the authenticated user's profile using the provided patch data.
     * Assumes authentication is handled by Spring Security and the user is authorized.
     *
     * @param updateDTO the fields to update (may include first name, last name, timezone, etc.)
     * @return the updated user profile as a {@link UserResponseDTO}
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(@RequestBody @Valid UserUpdateDTO updateDTO) {
        Long userId = securityService.requireCurrentUserId();
        User updated = userService.updateUser(userId, updateDTO);
        return ResponseEntity.ok(userMapper.toResponseDTO(updated));
    }

    // endregion

    // region DELETE /users/me

    /**
     * Deletes the authenticated user's account (soft delete).
     *
     * @return 204 No Content if successful
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        Long userId = securityService.requireCurrentUserId();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // endregion

    // region GET /users/{username}

    /**
     * Retrieves a public user profile by username.
     *
     * @param username the username to look up
     * @return the public user profile
     * @throws UserNotFoundException if no such user exists or is deleted
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(username));

        return ResponseEntity.ok(userMapper.toResponseDTO(user));
    }

    // endregion
}
