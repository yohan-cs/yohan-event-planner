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

    UserProfileResponseDTO getUserProfile(String username, Long viewerId);

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
