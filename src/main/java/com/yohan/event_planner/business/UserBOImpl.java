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
 * <p>
 * Coordinates persistence and domain behavior for {@link User} entities.
 * Delegates database access to {@link UserRepository} and ensures consistency
 * for queries by excluding users who are pending deletion.
 * </p>
 *
 * <h3>Design Assumptions:</h3>
 * <ul>
 *   <li>Service layer is responsible for all validation, ownership, and DTO mapping.</li>
 *   <li>This class is not defensive—inputs are assumed to be valid and complete.</li>
 *   <li>{@code isPendingDeletion} determines whether a user is considered active.</li>
 * </ul>
 *
 * <p>
 * All read operations (e.g., {@code getUserByUsername}, {@code getAllUsers}, etc.)
 * automatically exclude users who are pending deletion.
 * </p>
 *
 * <h3>Deletion Lifecycle:</h3>
 * <ul>
 *   <li>Calling {@link #deleteUser(User)} marks the user as pending deletion via domain logic.</li>
 *   <li>Users are not removed from the database immediately; a scheduled job performs hard deletion
 *       after the grace period has elapsed.</li>
 *   <li>Use {@link #getUsersScheduledForDeletion()} to retrieve users eligible for final removal.</li>
 * </ul>
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
        return userRepository.findById(userId);
    }

    /**
     * {@inheritDoc}
     */
    public Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user by username {}", username);
        return userRepository.findByUsernameAndIsPendingDeletionFalse(username);
    }

    /**
     * {@inheritDoc}
     */
    public Optional<User> getUserByEmail(String email) {
        logger.debug("Fetching user by email {}", email);
        return userRepository.findByEmailAndIsPendingDeletionFalse(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUsersByRole(Role role) {
        logger.debug("Fetching users by role {}", role);
        return userRepository.findAllByRolesAndIsPendingDeletionFalse(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAllByIsPendingDeletionFalse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User createUser(User user) {
        logger.info("Creating new user with username {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     */
    public User updateUser(User user) {
        logger.info("Updating user with ID {}", user.getId());
        return userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markUserForDeletion(User user) {
        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(user));
        user.markForDeletion(now);
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUsername(String username) {
        logger.debug("Checking if username {} exists", username);
        return userRepository.existsByUsername(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByEmail(String email) {
        logger.debug("Checking if email {} exists", email);
        return userRepository.existsByEmail(email);
    }

}
