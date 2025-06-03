package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.*;

import java.util.List;

/**
 * Service interface for managing user operations and business logic.
 *
 * <p>
 * Provides high-level access to core user flows, including identity lookups,
 * account creation, updates, deletions, and validation for uniqueness.
 * This is the main orchestrator for user-related concerns.
 * </p>
 */
public interface UserService {

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId the ID of the user to retrieve
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if the user is not found
     */
    UserResponseDTO getUserById(Long userId);

    /**
     * Retrieves the currently authenticated user's profile.
     *
     * @return the current user's profile as a {@link UserResponseDTO}
     * @throws UserNotFoundException if the current user does not exist
     */
    UserResponseDTO getCurrentUser();

    /**
     * Retrieves a user by their username (case-insensitive).
     *
     * @param username the username to retrieve
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if no user is found
     */
    UserResponseDTO getUserByUsername(String username);

    /**
     * Retrieves a user by their email address (case-insensitive).
     *
     * @param email the email address to retrieve
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if no user is found
     */
    UserResponseDTO getUserByEmail(String email);

    /**
     * Retrieves all users assigned a specific role.
     *
     * @param role the role to filter by
     * @return a list of matching {@link UserResponseDTO}s (may be empty)
     */
    List<UserResponseDTO> getUsersByRole(Role role);

    /**
     * Retrieves all users in the system using pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return a list of {@link UserResponseDTO}s (may be empty)
     */
    List<UserResponseDTO> getAllUsers(int page, int size);

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
     * Applies a partial update to a specific user.
     * <p>
     * Requires that the current user is authorized to modify the target user.
     * </p>
     *
     * @param userId the ID of the user to update
     * @param dto    the patch payload
     * @return the updated {@link UserResponseDTO}
     * @throws UserNotFoundException  if the user does not exist
     * @throws UserOwnershipException if the current user is unauthorized
     * @throws UsernameException      if the new username is taken
     * @throws EmailException         if the new email is taken
     */
    UserResponseDTO updateUser(Long userId, UserUpdateDTO dto);

    /**
     * Applies a partial update to the currently authenticated user.
     *
     * @param dto the patch payload
     * @return the updated {@link UserResponseDTO}
     * @throws UserNotFoundException if the current user does not exist
     * @throws UsernameException     if the new username is taken
     * @throws EmailException        if the new email is taken
     */
    UserResponseDTO updateCurrentUser(UserUpdateDTO dto);

    /**
     * Soft-deletes a user.
     * <p>
     * Requires that the current user is authorized to perform the deletion.
     * </p>
     *
     * @param userId the ID of the user to delete
     * @throws UserNotFoundException  if the user does not exist
     * @throws UserOwnershipException if the current user is unauthorized
     */
    void deleteUser(Long userId);

    /**
     * Soft-deletes the currently authenticated user.
     *
     * @throws UserNotFoundException if the user does not exist
     */
    void deleteCurrentUser();

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

    /**
     * Returns the number of users who are not soft-deleted.
     *
     * @return count of active (non-deleted) users
     */
    long countActiveUsers();

    /**
     * Returns the total number of users in the system.
     *
     * @return the user count
     */
    long countUsers();

}
