package com.yohan.event_planner.service;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.UserInitializer;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserHeaderResponseDTO;
import com.yohan.event_planner.dto.UserHeaderUpdateDTO;
import com.yohan.event_planner.dto.UserProfileResponseDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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
    private final UserInitializer userInitializer;
    private final PasswordBO passwordBO;
    private final BadgeService badgeService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;

    public UserServiceImpl(
            UserBO userBO,
            UserMapper userMapper,
            UserPatchHandler userPatchHandler,
            UserInitializer userInitializer,
            PasswordBO passwordBO,
            BadgeService badgeService,
            AuthenticatedUserProvider authenticatedUserProvider,
            OwnershipValidator ownershipValidator
    ) {
        this.userBO = userBO;
        this.userMapper = userMapper;
        this.userPatchHandler = userPatchHandler;
        this.userInitializer = userInitializer;
        this.passwordBO = passwordBO;
        this.badgeService = badgeService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ownershipValidator = ownershipValidator;
    }


    @Override
    public UserResponseDTO getUserSettings() {
        User currentUser = authenticatedUserProvider.getCurrentUser();

        return userMapper.toResponseDTO(currentUser);
    }

    @Override
    @Transactional
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

        // Delegate to UserInitializer instead of userBO directly
        User initializedUser = userInitializer.initializeUser(user);

        return userMapper.toResponseDTO(initializedUser);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserSettings(UserUpdateDTO dto) {
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
     * Marks the currently authenticated user for deletion after a grace period.
     */
    @Override
    @Transactional
    public void markUserForDeletion() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        userBO.markUserForDeletion(currentUser);
    }

    /**
     * Cancels the deletion of the currently authenticated user, if previously marked.
     */
    @Override
    @Transactional
    public void reactivateCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        currentUser.unmarkForDeletion(); // domain logic clears timestamp
        userBO.updateUser(currentUser); // persist
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(String username, Long viewerId) {
        User user = userBO.getUserByUsername(username.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(username));

        boolean isSelf = viewerId != null && user.getId().equals(viewerId);

        UserHeaderResponseDTO header = getUserHeader(user);
        List<BadgeResponseDTO> badges = badgeService.getBadgesByUser(user.getId());

        return new UserProfileResponseDTO(isSelf, header, badges);
    }

    @Override
    @Transactional
    public UserHeaderResponseDTO updateUserHeader(Long userId, UserHeaderUpdateDTO dto) {
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ownershipValidator.validateUserOwnership(user.getId(), userId);

        boolean changed = false;

        if (!Objects.equals(dto.bio(), user.getBio())) {
            user.setBio(dto.bio());
            changed = true;
        }

        if (!Objects.equals(dto.profilePictureUrl(), user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(dto.profilePictureUrl());
            changed = true;
        }

        if (changed) {
            user = userBO.updateUser(user);
        }

        return getUserHeader(user);
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

    private UserHeaderResponseDTO getUserHeader(User user) {
        return new UserHeaderResponseDTO(
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePictureUrl()
        );
    }
}
