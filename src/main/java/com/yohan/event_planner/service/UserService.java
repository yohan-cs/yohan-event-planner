package com.yohan.event_planner.service;

import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.*;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link User} entities.
 *
 * <p>
 * Provides core operations for creating, updating, deleting, and retrieving users,
 * as well as utilities for checking uniqueness and filtering by roles or credentials.
 * All user-specific business logic and validation is enforced through the service layer.
 * </p>
 */
public interface UserService {

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId the ID of the user to retrieve
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> getUserById(Long userId);

    /**
     * Retrieves a user by their username (case-insensitive).
     * <p>
     * The provided username is normalized to lowercase before lookup.
     * </p>
     *
     * @param username the username to search
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Retrieves a user by their email address (case-insensitive).
     * <p>
     * The provided email is normalized to lowercase before lookup.
     * </p>
     *
     * @param email the email to search
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Retrieves all users assigned a specific role.
     *
     * @param role the role to filter by (e.g., {@link Role#ADMIN})
     * @return a list of users with the specified role, or an empty list if none match
     */
    List<User> getUsersByRole(Role role);

    /**
     * Retrieves all users in the system.
     *
     * @return a list of all users
     */
    List<User> getAllUsers(int page, int size); // keeping signature for interface stability in V1

    /**
     * Creates a new user from the provided data transfer object (DTO).
     * <p>
     * Performs uniqueness checks for both username and email (case-insensitive),
     * encrypts the password, and persists the user.
     * </p>
     *
     * @param dto the user creation payload, including raw password
     * @return the created {@link User}
     * @throws UsernameException if the username is already in use
     * @throws EmailException    if the email is already in use
     */
    User createUser(UserCreateDTO dto);

    /**
     * Applies partial updates to an existing user.
     * <p>
     * Fields that are non-null in the DTO will be applied via {@link UserPatchHandler}.
     * Performs validation for ownership, and checks for updated username/email conflicts.
     * </p>
     *
     * @param userId the ID of the user being updated
     * @param dto    the update payload
     * @return the updated {@link User}
     * @throws UserNotFoundException  if the user does not exist
     * @throws UserOwnershipException if the current user is not authorized
     * @throws UsernameException      if the new username is taken
     * @throws EmailException         if the new email is taken
     */
    User updateUser(Long userId, UserUpdateDTO dto);

    /**
     * Soft-deletes a user by their ID.
     * <p>
     * Enforces ownership rules to ensure only the user or an admin can delete the account.
     * The deletion is logical — the user is marked as {@code deleted = true} and {@code active = false},
     * ensuring they can no longer authenticate or appear in public queries.
     * </p>
     *
     * @param userId the ID of the user to delete
     * @throws UserNotFoundException  if the user does not exist
     * @throws UserOwnershipException if the current user is not authorized
     */
    void deleteUser(Long userId);

    /**
     * Checks whether a user exists with the given username (case-insensitive).
     * <p>
     * Used to enforce uniqueness before creating or updating a user.
     * </p>
     *
     * @param username the username to check
     * @return {@code true} if a user exists, {@code false} otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a user exists with the given email (case-insensitive).
     * <p>
     * Used to enforce uniqueness before creating or updating a user.
     * </p>
     *
     * @param email the email to check
     * @return {@code true} if a user exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Returns the total number of users in the system.
     *
     * @return the user count
     */
    long countUsers();
}
