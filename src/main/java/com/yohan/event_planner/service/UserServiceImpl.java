package com.yohan.event_planner.service;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME;

/**
 * Implementation of the {@link com.yohan.event_planner.service.UserService} interface.
 *
 * <p>
 * Coordinates business logic and validations related to user management,
 * including user creation, retrieval, update, deletion, and uniqueness checks.
 * Uses {@link AuthenticatedUserProvider} to access current user context
 * and {@link OwnershipValidator} to enforce ownership constraints.
 * </p>
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserBO userBO;
    private final UserMapper userMapper;
    private final UserPatchHandler userPatchHandler;
    private final PasswordBO passwordBO;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserServiceImpl(
            UserBO userBO,
            UserMapper userMapper,
            UserPatchHandler userPatchHandler,
            PasswordBO passwordBO,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userBO = userBO;
        this.userMapper = userMapper;
        this.userPatchHandler = userPatchHandler;
        this.passwordBO = passwordBO;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId the ID of the user to retrieve
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Override
    public UserResponseDTO getUserById(Long userId) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Retrieves a user by username (case-insensitive).
     *
     * @param username the username to search for
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if no user with the given username exists
     */
    @Override
    public UserResponseDTO getUserByUsername(String username) {
        User user = userBO.getUserByUsername(username.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(username));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Retrieves a user by email (case-insensitive).
     *
     * @param email the email to search for
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userBO.getUserByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Retrieves all users with the specified role.
     *
     * @param role the role to filter users by
     * @return a list of matching {@link UserResponseDTO}s; empty list if none
     */
    @Override
    public List<UserResponseDTO> getUsersByRole(Role role) {
        return userBO.getUsersByRole(role).stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves all users in the system.
     *
     * <p>Note: Paging is not yet implemented; provided parameters are currently ignored.</p>
     *
     * @param page the (ignored) page index
     * @param size the (ignored) page size
     * @return a list of all users as {@link UserResponseDTO}s
     */
    @Override
    public List<UserResponseDTO> getAllUsers(int page, int size) {
        return userBO.getAllUsers().stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves the currently authenticated user's profile.
     *
     * @return the corresponding {@link UserResponseDTO}
     * @throws UserNotFoundException if the current user does not exist
     */
    @Override
    public UserResponseDTO getCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        return userMapper.toResponseDTO(currentUser);
    }

    /**
     * Creates a new user with the specified details.
     *
     * <p>
     * Usernames and emails are normalized to lowercase.
     * Passwords are encrypted before storage.
     * </p>
     *
     * @param dto the DTO containing user creation data
     * @return the corresponding {@link UserResponseDTO}
     * @throws UsernameException if the username is already taken
     * @throws EmailException if the email is already taken
     */
    @Override
    public UserResponseDTO createUser(UserCreateDTO dto) {
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

        return userMapper.toResponseDTO(userBO.createUser(user));
    }

    /**
     * Applies a partial update to the specified user.
     *
     * <p>
     * Validates ownership and uniqueness of updated fields before applying changes.
     * </p>
     *
     * @param userId the ID of the user to update
     * @param dto the patch data DTO
     * @return the updated {@link UserResponseDTO}
     * @throws UserNotFoundException if the user does not exist
     * @throws UsernameException if the username is already taken
     * @throws EmailException if the email is already taken
     */
    @Override
    public UserResponseDTO updateUser(Long userId, UserUpdateDTO dto) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Long currentUserId = authenticatedUserProvider.getCurrentUser().getId();
        ownershipValidator.validateUserOwnership(currentUserId, userId);

        if (dto.username() != null) {
            validateUsernameAvailability(dto.username(), userId);
        }

        if (dto.email() != null) {
            validateEmailAvailability(dto.email(), userId);
        }

        boolean changed = userPatchHandler.applyPatch(user, dto);
        return userMapper.toResponseDTO(changed ? userBO.updateUser(user) : user);
    }

    /**
     * Applies a partial update to the currently authenticated user.
     *
     * @param dto the patch data DTO
     * @return the updated {@link UserResponseDTO}
     * @throws UserNotFoundException if the user does not exist
     * @throws UsernameException if the username is already taken
     * @throws EmailException if the email is already taken
     */
    @Override
    public UserResponseDTO updateCurrentUser(UserUpdateDTO dto) {
        User currentUser = authenticatedUserProvider.getCurrentUser();

        if (dto.username() != null) {
            validateUsernameAvailability(dto.username(), currentUser.getId());
        }

        if (dto.email() != null) {
            validateEmailAvailability(dto.email(), currentUser.getId());
        }

        boolean changed = userPatchHandler.applyPatch(currentUser, dto);
        return userMapper.toResponseDTO(changed ? userBO.updateUser(currentUser) : currentUser);
    }

    /**
     * Soft-deletes the user with the specified ID.
     *
     * @param userId the ID of the user to delete
     * @throws UserNotFoundException if the user does not exist
     */
    @Override
    public void deleteUser(Long userId) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Long currentUserId = authenticatedUserProvider.getCurrentUser().getId();
        ownershipValidator.validateUserOwnership(currentUserId, userId);

        userBO.deleteUser(user);
    }

    /**
     * Soft-deletes the currently authenticated user.
     *
     * @throws UserNotFoundException if the user does not exist
     */
    @Override
    public void deleteCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        userBO.deleteUser(currentUser);
    }

    /**
     * Checks whether a username exists (case-insensitive).
     *
     * @param username the username to check
     * @return {@code true} if the username is taken; otherwise {@code false}
     */
    @Override
    public boolean existsByUsername(String username) {
        return userBO.existsByUsername(username.toLowerCase());
    }

    /**
     * Checks whether an email address exists (case-insensitive).
     *
     * @param email the email address to check
     * @return {@code true} if the email is taken; otherwise {@code false}
     */
    @Override
    public boolean existsByEmail(String email) {
        return userBO.existsByEmail(email.toLowerCase());
    }

    /**
     * Returns the number of users who are not soft-deleted.
     *
     * @return count of active (non-deleted) users
     */
    @Override
    public long countActiveUsers() {
        return userBO.countActiveUsers();
    }

    /**
     * Returns the total number of users.
     *
     * @return user count
     */
    @Override
    public long countUsers() {
        return userBO.countUsers();
    }

    /**
     * Validates that the provided username is not already taken by another user.
     *
     * @param username the username to validate
     * @param userId the ID of the user being updated
     * @throws UsernameException if the username is already in use
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
     * Validates that the provided email is not already taken by another user.
     *
     * @param email the email to validate
     * @param userId the ID of the user being updated
     * @throws EmailException if the email is already in use
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
