package com.yohan.event_planner.service;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME;

/**
 * Implementation of the {@link UserService} interface.
 *
 * <p>
 * This class handles business logic and coordination for user-related operations
 * such as user creation, update, deletion, lookup, and existence checks.
 * All validations, patching, and business rules are handled at this layer.
 * </p>
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserBO userBO;
    private final UserMapper userMapper;
    private final UserPatchHandler userPatchHandler;
    private final PasswordBO passwordBO;
    private final OwnershipValidator ownershipValidator;

    public UserServiceImpl(
            UserBO userBO,
            UserMapper userMapper,
            UserPatchHandler userPatchHandler,
            PasswordBO passwordBO,
            OwnershipValidator ownershipValidator
    ) {
        this.userBO = userBO;
        this.userMapper = userMapper;
        this.userPatchHandler = userPatchHandler;
        this.passwordBO = passwordBO;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserById(Long userId) {
        return userBO.getUserById(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserByUsername(String username) {
        return userBO.getUserByUsername(username.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> getUserByEmail(String email) {
        return userBO.getUserByEmail(email.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUsersByRole(Role role) {
        return userBO.getUsersByRole(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getAllUsers(int page, int size) {
        return userBO.getAllUsers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User createUser(UserCreateDTO dto) {
        String normalizedUsername = dto.username().toLowerCase();
        String normalizedEmail = dto.email().toLowerCase();

        if (existsByUsername(normalizedUsername)) {
            throw new UsernameException(DUPLICATE_USERNAME, dto.username());
        }
        if (existsByEmail(normalizedEmail)) {
            throw new EmailException(DUPLICATE_EMAIL, dto.email());
        }

        User user = userMapper.toEntity(dto);
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setHashedPassword(passwordBO.encryptPassword(dto.password()));

        return userBO.createUser(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User updateUser(Long userId, UserUpdateDTO dto) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ownershipValidator.validateUserOwnership(userId);

        if (dto.username() != null) {
            validateUsernameAvailability(dto.username(), userId);
        }

        if (dto.email() != null) {
            validateEmailAvailability(dto.email(), userId);
        }

        boolean changed = userPatchHandler.applyPatch(user, dto);
        return changed ? userBO.updateUser(user) : user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUser(Long userId) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ownershipValidator.validateUserOwnership(userId);
        userBO.deleteUser(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUsername(String username) {
        return userBO.existsByUsername(username.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByEmail(String email) {
        return userBO.existsByEmail(email.toLowerCase());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countUsers() {
        return userBO.countUsers();
    }

    /**
     * Validates that the given username is available for use.
     *
     * @param username the proposed username
     * @param userId   the ID of the user being updated
     */
    private void validateUsernameAvailability(String username, Long userId) {
        String normalized = username.toLowerCase();
        userBO.getUserByUsername(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new UsernameException(DUPLICATE_USERNAME, normalized);
            }
        });
    }

    /**
     * Validates that the given email is available for use.
     *
     * @param email   the proposed email address
     * @param userId  the ID of the user being updated
     */
    private void validateEmailAvailability(String email, Long userId) {
        String normalized = email.toLowerCase();
        userBO.getUserByEmail(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new EmailException(DUPLICATE_EMAIL, normalized);
            }
        });
    }
}
