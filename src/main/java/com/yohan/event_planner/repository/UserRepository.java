package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>
 * Extends {@link JpaRepository} to provide standard CRUD operations,
 * along with custom query methods for retrieving users by username,
 * email, and role, as well as existence checks.
 * </p>
 *
 * <p>
 * This repository does not enforce any access control or authorization.
 * All such concerns must be handled at the service or business layer.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Retrieves a user by their unique identifier.
     * <p>
     * Method redeclared for explicit documentation purposes.
     * </p>
     *
     * @param id the ID of the user
     * @return an {@link Optional} containing the found user, or empty if not found
     */
    @Override
    Optional<User> findById(Long id);

    /**
     * Retrieves a user by their unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the found user, or empty if none found
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves a user by their unique email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the found user, or empty if none found
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrieves all users assigned the specified role.
     * <p>
     * Matches users where the given role is present in their {@code roles} set.
     * </p>
     *
     * @param role the role to filter users by
     * @return a list of users having the given role
     */
    List<User> findAllByRoles(Role role);

    /**
     * Deletes the user entity with the specified ID.
     *
     * @param id the ID of the user to delete
     */
    @Override
    void deleteById(Long id);

    /**
     * Checks if a user exists with the specified username.
     *
     * @param username the username to check
     * @return {@code true} if a user with the username exists, {@code false} otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the specified email address.
     *
     * @param email the email address to check
     * @return {@code true} if a user with the email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Saves the given user entity.
     * <p>
     * Can be used for both creating new users and updating existing ones.
     * </p>
     *
     * @param user the user entity to save
     * @param <S>  subtype of {@link User}
     * @return the saved user entity
     */
    @Override
    <S extends User> S save(S user);

    /**
     * Counts the number of users who have not been soft-deleted.
     *
     * @return the total number of users with {@code deleted = false}
     */
    long countByDeletedFalse();
}
