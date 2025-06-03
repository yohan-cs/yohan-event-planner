package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of the {@link UserBO} interface.
 *
 * <p>
 * Provides data access and coordination logic for managing {@link User} entities.
 * This implementation delegates persistence operations to the {@link UserRepository},
 * and is responsible for domain-level tasks such as soft deletion.
 * </p>
 *
 * <p>
 * Assumes all validation and authorization logic is handled upstream
 * in the service layer. This class is not defensive and expects well-formed inputs.
 * </p>
 *
 * <p>
 * Soft-deleted users are automatically excluded in {@link #getUserByUsername(String)},
 * {@link #getUsersByRole(Role)}, {@link #getAllUsers()}, and {@link #countActiveUsers()}
 * to ensure consistent reads across the application.
 * </p>
 */
@Service
public class UserBOImpl implements UserBO {

    private static final Logger logger = LoggerFactory.getLogger(UserBOImpl.class);
    private final UserRepository userRepository;

    public UserBOImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    @Override
    public Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user by username {}", username);
        return userRepository.findByUsername(username)
                .filter(user -> !user.isDeleted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserByEmail(String email) {
        logger.debug("Fetching user by email {}", email);
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDeleted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUsersByRole(Role role) {
        logger.debug("Fetching users by role {}", role);
        return userRepository.findAllByRoles(role).stream()
                .filter(user -> !user.isDeleted())
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .toList();
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
    @Override
    public User updateUser(User user) {
        logger.info("Updating user with ID {}", user.getId());
        return userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUser(User user) {
        logger.info("Soft deleting user with ID {}", user.getId());
        user.setDeleted(true);
        user.setActive(false);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public long countUsers() {
        logger.debug("Counting all users including deleted");
        return userRepository.count();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countActiveUsers() {
        logger.debug("Counting active (non-deleted) users");
        return userRepository.countByDeletedFalse();
    }
}
