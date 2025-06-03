package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * Business operations interface for managing {@link User} entities.
 * <p>
 * Defines core use cases related to user creation, updates, soft deletion,
 * and lookup operations. This layer encapsulates persistence coordination
 * and domain enforcement such as soft deletion and entity wiring.
 * </p>
 *
 * <h3>Design Notes:</h3>
 * <ul>
 *   <li>Assumes that the service layer has already validated all business rules,
 *       such as username/email availability, user ownership, and patch logic.</li>
 *   <li>This layer is not defensive and will not repeat validation performed upstream.</li>
 *   <li>Intended for use by the service layer only; not exposed to controllers or external code.</li>
 * </ul>
 */
public interface UserBO {

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user ID
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserById(Long userId);

    /**
     * Retrieves a user by their unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserByUsername(String username);

    /**
     * Retrieves a user by their unique email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found, otherwise empty
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Retrieves all users with the given role.
     *
     * @param role the role to filter by
     * @return a list of users matching the given role
     */
    List<User> getUsersByRole(Role role);

    /**
     * Retrieves all user records in the system.
     * <p>
     * Excludes soft-deleted users. May return a large dataset; typically reserved
     * for administrative or diagnostic workflows.
     * </p>
     *
     * @return a list of all active {@link User} entities
     */
    List<User> getAllUsers();

    /**
     * Creates a new user.
     * <p>
     * Assumes the entity is fully prepared and validated by the service layer.
     * </p>
     *
     * @param user the prepared user entity to persist
     * @return the saved {@link User} with an assigned ID
     */
    User createUser(User user);

    /**
     * Updates an existing user.
     * <p>
     * Assumes all patching and validation have been performed upstream.
     * </p>
     *
     * @param user the updated user entity with a valid ID
     * @return the updated and saved {@link User}
     */
    User updateUser(User user);

    /**
     * Soft-deletes a user by marking them as deleted and inactive.
     * <p>
     * This is a logical deletion used for deactivation and audit trails.
     * Assumes the user has already been retrieved and validated by the service layer.
     * </p>
     *
     * @param user the user entity to mark as deleted and inactive
     */
    void deleteUser(User user);


    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username to check
     * @return {@code true} if a user with the username exists, otherwise {@code false}
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email the email address to check
     * @return {@code true} if a user with the email exists, otherwise {@code false}
     */
    boolean existsByEmail(String email);

    /**
     * Returns the number of users who are not soft-deleted.
     *
     * @return count of active (non-deleted) users
     */
    long countActiveUsers();

    /**
     * Returns the total number of user records, including soft-deleted users.
     *
     * @return total user count
     */
    long countUsers();
}
