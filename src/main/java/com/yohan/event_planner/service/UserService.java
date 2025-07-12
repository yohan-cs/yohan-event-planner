package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserHeaderResponseDTO;
import com.yohan.event_planner.dto.UserHeaderUpdateDTO;
import com.yohan.event_planner.dto.UserProfileResponseDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;

/**
 * Service interface for managing user operations and business logic.
 *
 * <p>
 * Provides high-level access to core user flows, including identity lookups,
 * account creation, updates, deletion scheduling, and validation for uniqueness.
 * This is the main orchestrator for user-related concerns.
 * </p>
 */
public interface UserService {

    /**
     * Retrieves the current authenticated user's settings and preferences.
     *
     * @return {@link UserResponseDTO} containing user settings and preferences
     * @throws UserNotFoundException if no authenticated user is found
     */
    UserResponseDTO getUserSettings();

    /**
     * Creates a new user with the provided details.
     *
     * @param dto the user creation payload
     * @return the created {@link UserResponseDTO}
     * @throws UsernameException if the username is taken
     * @throws EmailException    if the email is taken
     */
    UserResponseDTO createUser(UserCreateDTO dto);

    /**
     * Updates the current authenticated user's settings with the provided changes.
     *
     * <p>Performs validation for username and email uniqueness if those fields are being updated.
     * Only applies changes for non-null fields in the DTO. Uses atomic patch operations
     * to ensure data consistency.</p>
     *
     * @param dto the user update payload containing fields to modify
     * @return {@link UserResponseDTO} with the updated user information
     * @throws UsernameException if the new username is already taken
     * @throws EmailException if the new email is already taken
     * @throws UserNotFoundException if the current user does not exist
     */
    UserResponseDTO updateUserSettings(UserUpdateDTO dto);

    /**
     * Marks the currently authenticated user for deletion after a grace period.
     *
     * <p>
     * This action disables the account and schedules it for permanent deletion.
     * </p>
     *
     * @throws UserNotFoundException if the current user does not exist
     */
    void markUserForDeletion();

    /**
     * Cancels the pending deletion of the currently authenticated user.
     *
     * <p>
     * This action restores the user's account to active status.
     * </p>
     *
     * @throws UserNotFoundException if the current user does not exist
     */
    void reactivateCurrentUser();

    /**
     * Retrieves a user's public profile information by username.
     *
     * <p>Returns profile header information and user badges. The {@code isSelf} flag
     * indicates whether the viewer is looking at their own profile, which may affect
     * what information is displayed on the client side.</p>
     *
     * @param username the username of the profile to retrieve (case-insensitive)
     * @param viewerId the ID of the user viewing the profile (null for anonymous)
     * @return {@link UserProfileResponseDTO} containing profile header and badges
     * @throws UserNotFoundException if the requested username does not exist
     */
    UserProfileResponseDTO getUserProfile(String username, Long viewerId);

    /**
     * Updates a user's profile header information (bio and profile picture).
     *
     * <p>Validates ownership to ensure users can only modify their own profiles.
     * Only persists changes if the provided values differ from current values.</p>
     *
     * @param userId the ID of the user whose header to update
     * @param input the header update payload containing bio and profile picture URL
     * @return {@link UserHeaderResponseDTO} with the updated header information
     * @throws UserNotFoundException if the user does not exist
     * @throws RuntimeException if ownership validation fails
     */
    UserHeaderResponseDTO updateUserHeader(Long userId, UserHeaderUpdateDTO input);

    /**
     * Checks whether a username exists (case-insensitive).
     *
     * @param username the username to check
     * @return {@code true} if the username is taken; otherwise {@code false}
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email address exists (case-insensitive).
     *
     * @param email the email address to check
     * @return {@code true} if the email is taken; otherwise {@code false}
     */
    boolean existsByEmail(String email);
}
