package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.time.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of the {@link UserBO} interface.
 *
 * <p><strong>Architectural Role:</strong> This component sits in the Business Object layer,
 * coordinating persistence and domain behavior for {@link User} entities. It delegates 
 * database access to {@link UserRepository} and ensures consistency for queries by 
 * excluding users who are pending deletion.</p>
 *
 * <p><strong>Design Assumptions:</strong>
 * <ul>
 *   <li>Service layer is responsible for all validation, ownership, and DTO mapping</li>
 *   <li>This class is not defensive—inputs are assumed to be valid and complete</li>
 *   <li>{@code isPendingDeletion} determines whether a user is considered active</li>
 * </ul></p>
 *
 * <p><strong>Query Behavior:</strong> Most read operations (e.g., {@code getUserByUsername}, 
 * {@code getUserByEmail}, {@code getAllUsers}, {@code getUsersByRole}) automatically exclude 
 * users who are pending deletion. The exception is {@code getUserById}, which returns
 * users regardless of deletion status.</p>
 *
 * <p><strong>Deletion Lifecycle:</strong>
 * <ol>
 *   <li>Calling {@link #markUserForDeletion(User)} marks the user as pending deletion 
 *       via domain logic and schedules deletion 30 days in the future</li>
 *   <li>Users are not removed from the database immediately; a scheduled job performs 
 *       hard deletion after the grace period has elapsed</li>
 *   <li>During the grace period, users are excluded from most queries but remain 
 *       accessible via {@link #getUserById(Long)}</li>
 *   <li>The scheduled job directly queries the repository for users eligible for final removal</li>
 * </ol></p>
 * 
 * <p><strong>Dependencies:</strong>
 * <ul>
 *   <li>{@link UserRepository} - Database access and persistence operations</li>
 *   <li>{@link ClockProvider} - Timezone-aware time operations for deletion scheduling</li>
 * </ul></p>
 */
@Service
public class UserBOImpl implements UserBO {

    private static final Logger logger = LoggerFactory.getLogger(UserBOImpl.class);
    private final UserRepository userRepository;
    private final ClockProvider clockProvider;

    public UserBOImpl(UserRepository userRepository, ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.clockProvider = clockProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserById(Long userId) {
        logger.debug("Fetching user by ID {}", userId);
        Optional<User> result = userRepository.findById(userId);
        if (result.isPresent()) {
            logger.debug("Found user with ID {}", userId);
        } else {
            logger.debug("No user found with ID {}", userId);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user by username {}", username);
        Optional<User> result = userRepository.findByUsernameAndIsPendingDeletionFalse(username);
        if (result.isPresent()) {
            logger.debug("Found user with username {}", username);
        } else {
            logger.debug("No user found with username {}", username);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserByEmail(String email) {
        logger.debug("Fetching user by email {}", email);
        Optional<User> result = userRepository.findByEmailAndIsPendingDeletionFalse(email);
        if (result.isPresent()) {
            logger.debug("Found user with email {}", email);
        } else {
            logger.debug("No user found with email {}", email);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUsersByRole(Role role) {
        logger.debug("Fetching users by role {}", role);
        List<User> result = userRepository.findAllByRolesAndIsPendingDeletionFalse(role);
        logger.debug("Found {} users with role {}", result.size(), role);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        List<User> result = userRepository.findAllByIsPendingDeletionFalse();
        logger.debug("Found {} total users", result.size());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User createUser(User user) {
        logger.info("Creating new user with username {}", user.getUsername());
        User savedUser = userRepository.save(user);
        logger.info("Successfully created user with ID {} and username {}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User updateUser(User user) {
        logger.info("Updating user with ID {}", user.getId());
        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user with ID {}", updatedUser.getId());
        return updatedUser;
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * <ol>
     *   <li>Gets the current time in the user's timezone using the clock provider</li>
     *   <li>Delegates to the domain object's markForDeletion method to set flags and calculate deletion date</li>
     *   <li>Persists the updated user entity with the new deletion state</li>
     * </ol></p>
     */
    @Override
    public void markUserForDeletion(User user) {
        logger.info("Marking user ID {} for deletion", user.getId());
        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(user));
        user.markForDeletion(now);
        userRepository.save(user);
        logger.info("Successfully marked user ID {} for deletion, scheduled for {}", user.getId(), user.getScheduledDeletionDate().orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUsername(String username) {
        logger.debug("Checking if username {} exists", username);
        boolean exists = userRepository.existsByUsername(username);
        logger.debug("Username {} existence check result: {}", username, exists);
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByEmail(String email) {
        logger.debug("Checking if email {} exists", email);
        boolean exists = userRepository.existsByEmail(email);
        logger.debug("Email {} existence check result: {}", email, exists);
        return exists;
    }

}
