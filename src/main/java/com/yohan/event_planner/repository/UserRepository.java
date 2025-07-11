package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities with comprehensive query support.
 * 
 * <p>This repository provides sophisticated data access functionality for user management,
 * including unique constraint queries, role-based filtering, deletion lifecycle management,
 * and existence verification. It supports the complete user lifecycle from registration
 * through soft deletion and permanent cleanup operations.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Identity Queries</strong>: Find users by username, email, and ID</li>
 *   <li><strong>Uniqueness Verification</strong>: Check username and email availability</li>
 *   <li><strong>Role-based Queries</strong>: Filter users by assigned roles</li>
 *   <li><strong>Deletion Lifecycle</strong>: Manage soft deletion and cleanup processes</li>
 *   <li><strong>Active User Filtering</strong>: Exclude pending deletion users from results</li>
 * </ul>
 * 
 * <h2>Identity and Uniqueness Management</h2>
 * <p>Comprehensive support for user identity constraints:</p>
 * <ul>
 *   <li><strong>Username Uniqueness</strong>: Ensure globally unique usernames</li>
 *   <li><strong>Email Uniqueness</strong>: Prevent duplicate email registrations</li>
 *   <li><strong>Existence Checks</strong>: Efficient availability verification</li>
 *   <li><strong>Case Sensitivity</strong>: Handle username and email case requirements</li>
 * </ul>
 * 
 * <h2>Soft Deletion Support</h2>
 * <p>Advanced user lifecycle management:</p>
 * <ul>
 *   <li><strong>Pending Deletion State</strong>: Mark users for delayed deletion</li>
 *   <li><strong>Active User Filtering</strong>: Exclude deleted users from standard queries</li>
 *   <li><strong>Scheduled Cleanup</strong>: Find users eligible for permanent deletion</li>
 *   <li><strong>Restoration Support</strong>: Query patterns support user reactivation</li>
 * </ul>
 * 
 * <h2>Role-based Access Patterns</h2>
 * <p>Support for role-based user management:</p>
 * <ul>
 *   <li><strong>Role Filtering</strong>: Query users by assigned roles</li>
 *   <li><strong>Administrative Queries</strong>: Support for admin functionality</li>
 *   <li><strong>Multi-role Support</strong>: Handle users with multiple roles</li>
 *   <li><strong>Role Validation</strong>: Support role-based authorization</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Utilize database indexes for username/email</li>
 *   <li><strong>Existence Checks</strong>: Efficient boolean queries without data transfer</li>
 *   <li><strong>Filtered Queries</strong>: Automatic filtering of deleted users</li>
 *   <li><strong>Batch Operations</strong>: Support bulk operations where applicable</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <p>Repository design supports security enforcement:</p>
 * <ul>
 *   <li><strong>No Built-in Authorization</strong>: Security enforced at service layer</li>
 *   <li><strong>Privacy Support</strong>: Query patterns respect user privacy needs</li>
 *   <li><strong>Deletion Privacy</strong>: Automatic exclusion of deleted users</li>
 *   <li><strong>Data Integrity</strong>: Maintain referential integrity constraints</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>UserService</strong>: Primary service layer integration</li>
 *   <li><strong>AuthService</strong>: Authentication and registration support</li>
 *   <li><strong>Security Framework</strong>: User lookup for authentication</li>
 *   <li><strong>Cleanup Jobs</strong>: Scheduled deletion processing</li>
 * </ul>
 * 
 * <h2>Query Patterns</h2>
 * <p>Common query patterns supported:</p>
 * <ul>
 *   <li><strong>Lookup by Identity</strong>: Username and email-based queries</li>
 *   <li><strong>Registration Validation</strong>: Uniqueness verification</li>
 *   <li><strong>Active User Lists</strong>: Filtered user collections</li>
 *   <li><strong>Cleanup Operations</strong>: Scheduled deletion processing</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Unique Constraints</strong>: Enforce username and email uniqueness</li>
 *   <li><strong>State Consistency</strong>: Maintain deletion state integrity</li>
 *   <li><strong>Role Consistency</strong>: Proper role assignment handling</li>
 *   <li><strong>Timestamp Accuracy</strong>: Reliable deletion scheduling</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Service Layer Security</strong>: Authorization must be enforced at service layer</li>
 *   <li><strong>Soft Deletion</strong>: Default queries should exclude pending deletion users</li>
 *   <li><strong>Case Sensitivity</strong>: Username/email comparisons are case-sensitive</li>
 *   <li><strong>Null Safety</strong>: Proper handling of optional parameters</li>
 * </ul>
 * 
 * @see User
 * @see UserService
 * @see AuthService
 * @see Role
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
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
     * Retrieves a user by their unique username, excluding pending deletion users.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the found user, or empty if none found
     */
    Optional<User> findByUsernameAndIsPendingDeletionFalse(String username);

    /**
     * Retrieves a user by their unique email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the found user, or empty if none found
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrieves a user by their unique email address, excluding pending deletion users.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the found user, or empty if none found
     */
    Optional<User> findByEmailAndIsPendingDeletionFalse(String email);

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
     * Retrieves all users assigned the specified role, excluding pending deletion users.
     *
     * @param role the role to filter users by
     * @return a list of users having the given role
     */
    List<User> findAllByRolesAndIsPendingDeletionFalse(Role role);

    /**
     * Retrieves all users excluding pending deletion users.
     *
     * @return a list of all active users
     */
    List<User> findAllByIsPendingDeletionFalse();

    /**
     * Retrieves all users pending deletion with scheduled deletion date before the given time.
     *
     * @param cutoffTime the cutoff time for scheduled deletion
     * @return a list of users eligible for permanent deletion
     */
    List<User> findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(ZonedDateTime cutoffTime);

    /**
     * Retrieves all users who have not verified their email and were created before the given time.
     *
     * @param cutoffTime the cutoff time for unverified account cleanup
     * @return a list of unverified users eligible for cleanup
     */
    List<User> findAllByEmailVerifiedFalseAndCreatedAtBefore(ZonedDateTime cutoffTime);

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
}
