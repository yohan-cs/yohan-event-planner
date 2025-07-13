package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.UserInitializer;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
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
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class UserServiceImplTest {

    private UserBO userBO;
    private UserMapper userMapper;
    private UserPatchHandler userPatchHandler;
    private UserInitializer userInitializer;
    private PasswordBO passwordBO;
    private BadgeService badgeService;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private OwnershipValidator ownershipValidator;
    private EventBO eventBO;
    private EventResponseDTOFactory eventResponseDTOFactory;
    private Clock fixedClock;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userBO = mock(UserBO.class);
        userMapper = mock(UserMapper.class);
        userPatchHandler = mock(UserPatchHandler.class);
        userInitializer = mock(UserInitializer.class);
        passwordBO = mock(PasswordBO.class);
        badgeService = mock(BadgeService.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        ownershipValidator = mock(OwnershipValidator.class);
        eventBO = mock(EventBO.class);
        eventResponseDTOFactory = mock(EventResponseDTOFactory.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        userService = new UserServiceImpl(userBO, eventBO, userMapper, userPatchHandler, userInitializer, passwordBO, badgeService, eventResponseDTOFactory, authenticatedUserProvider, ownershipValidator);
    }

    @Nested
    class GetUserSettingsTests {

        @Test
        void testGetUserSettingsSuccess() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();  // A valid user
            UserResponseDTO responseDTO = TestUtils.createValidUserResponseDTO();  // The expected DTO response

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);  // Mock that current user is returned
            when(userMapper.toResponseDTO(currentUser)).thenReturn(responseDTO);  // Mock the DTO mapping

            // Act
            UserResponseDTO result = userService.getUserSettings();

            // Assert
            assertEquals(responseDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userMapper).toResponseDTO(currentUser);
        }

        @Test
        void testGetUserSettingsFailure_UserNotAuthenticated() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenThrow(new UserNotFoundException("User not authenticated"));

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.getUserSettings());

            verify(authenticatedUserProvider).getCurrentUser();
            verify(userMapper, never()).toResponseDTO(any());
        }

    }


    @Nested
    class CreateUserTests {

        @Test
        void testCreateUserSuccess() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            User entityToCreate = TestUtils.createValidUserEntity();
            User initializedEntity = TestUtils.createValidUserEntityWithId();
            UserResponseDTO responseDTO = TestUtils.createValidUserResponseDTO();

            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(false);
            when(userMapper.toEntity(dto)).thenReturn(entityToCreate);
            when(passwordBO.encryptPassword(dto.password())).thenReturn(TestConstants.VALID_PASSWORD);
            when(userInitializer.initializeUser(entityToCreate)).thenReturn(initializedEntity);
            when(userMapper.toResponseDTO(initializedEntity)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.createUser(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userMapper).toEntity(dto);
            verify(passwordBO).encryptPassword(dto.password());
            verify(userInitializer).initializeUser(entityToCreate);
            verify(userMapper).toResponseDTO(initializedEntity);
        }

        @Test
        void testCreateUserFailure_UsernameConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(any());
            verify(userMapper, never()).toEntity(any());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_EmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(true);

            // Act + Assert
            assertThrows(EmailException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userMapper, never()).toEntity(any());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_UsernameAndEmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(any());
            verify(userMapper, never()).toEntity(any());
            verify(userBO, never()).createUser(any());
        }
    }

    @Nested
    class UpdateUserSettingsTests {

        @Test
        void testUpdateUserSettingsSuccess() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO("newName", null, null, null, null, "Asia/Seoul");
            User currentUser = TestUtils.createValidUserEntityWithId();
            User updatedUser = TestUtils.createValidUserEntityWithId();
            updatedUser.setUsername("newname");
            updatedUser.setTimezone("Asia/Seoul");
            UserResponseDTO responseDTO = TestUtils.createValidUserResponseDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByUsername("newname")).thenReturn(Optional.empty());
            when(userPatchHandler.applyPatch(currentUser, dto)).thenReturn(true);
            when(userBO.updateUser(currentUser)).thenReturn(updatedUser);
            when(userMapper.toResponseDTO(updatedUser)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.updateUserSettings(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByUsername("newname");
            verify(userPatchHandler).applyPatch(currentUser, dto);
            verify(userBO).updateUser(currentUser);
            verify(userMapper).toResponseDTO(updatedUser);
        }

        @Test
        void testUpdateUserSettingsFailure_UsernameConflict() {
            // Arrange
            String newUsername = TestConstants.VALID_USERNAME;
            UserUpdateDTO dto = new UserUpdateDTO(newUsername, null, null, null, null, null);
            User currentUser = TestUtils.createValidUserEntityWithId();
            User existingUser = TestUtils.createValidUserEntityWithId(999L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByUsername(newUsername.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.updateUserSettings(dto));
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }

        @Test
        void testUpdateUserSettingsFailure_EmailConflict() {
            // Arrange
            String newEmail = TestConstants.VALID_EMAIL;
            UserUpdateDTO dto = new UserUpdateDTO(null, null, newEmail, null, null, null);
            User currentUser = TestUtils.createValidUserEntityWithId();
            User existingUser = TestUtils.createValidUserEntityWithId(999L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByEmail(newEmail.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(EmailException.class, () -> userService.updateUserSettings(dto));
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByEmail(newEmail.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }

        @Test
        void testUpdateUserSettingsNoOp() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO(null, null, null, null, null, null);
            User currentUser = TestUtils.createValidUserEntityWithId();
            UserResponseDTO responseDTO = TestUtils.createValidUserResponseDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userPatchHandler.applyPatch(currentUser, dto)).thenReturn(false);
            when(userMapper.toResponseDTO(currentUser)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.updateUserSettings(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO, never()).updateUser(any());
            verify(userMapper).toResponseDTO(currentUser);
        }
    }

    @Nested
    class MarkUserForDeletionTests {

        @Test
        void testMarkCurrentUserForDeletion_callsBOWithCurrentUser() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act
            userService.markUserForDeletion();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).markUserForDeletion(currentUser);
        }

    }

    @Nested
    class ReactivateCurrentUserTests {

        @Test
        void testReactivateCurrentUser_ClearsPendingDeletionAndDate() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            currentUser.markForDeletion(ZonedDateTime.now(fixedClock)); // sets deletion flag + date
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act
            userService.reactivateCurrentUser();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).updateUser(currentUser);
            assertFalse(currentUser.isPendingDeletion());
            assertTrue(currentUser.getScheduledDeletionDate().isEmpty());
        }
    }

    @Nested
    class GetUserProfileTests {

        @Test
        void testGetUserProfile_SelfView_ReturnsCorrectDTO() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);

            UserHeaderResponseDTO header = new UserHeaderResponseDTO(
                    username, user.getFirstName(), user.getLastName(), user.getBio(), user.getProfilePictureUrl()
            );

            List<BadgeResponseDTO> badges = List.of(
                    mock(BadgeResponseDTO.class)
            );

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(header.username(), result.header().username());
            assertEquals(header.firstName(), result.header().firstName());
            assertEquals(header.lastName(), result.header().lastName());
            assertEquals(header.bio(), result.header().bio());
            assertEquals(header.profilePictureUrl(), result.header().profilePictureUrl());
            assertEquals(badges, result.badges());

            verify(userBO).getUserByUsername(username.toLowerCase());
            verify(badgeService).getBadgesByUser(user.getId());
        }

        @Test
        void testGetUserProfile_SelfView_WithPinnedEvent_IncludesPinnedEvent() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);
            
            // Create a pinned impromptu event
            Event pinnedEvent = Event.createImpromptuEvent(ZonedDateTime.now(fixedClock), user);
            user.setPinnedImpromptuEvent(pinnedEvent);
            EventResponseDTO pinnedEventDTO = mock(EventResponseDTO.class);

            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);
            when(eventResponseDTOFactory.createFromEvent(pinnedEvent)).thenReturn(pinnedEventDTO);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(pinnedEventDTO, result.pinnedImpromptuEvent());
            verify(eventResponseDTOFactory).createFromEvent(pinnedEvent);
        }

        @Test
        void testGetUserProfile_SelfView_WithInvalidPinnedEvent_CleansUpAndReturnsNull() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);
            
            // Create an invalid pinned event (not impromptu or completed)
            Event invalidPinnedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            invalidPinnedEvent.setCompleted(true); // completed events should be auto-unpinned
            user.setPinnedImpromptuEvent(invalidPinnedEvent);

            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(null, result.pinnedImpromptuEvent());
            verify(userBO).updateUser(user); // Should clean up invalid pinned event
        }

        @Test
        void testGetUserProfile_SelfView_WithNoPinnedEvent_ReturnsNull() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);

            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(null, result.pinnedImpromptuEvent());
            verify(userBO, never()).updateUser(any(User.class));
        }

        @Test
        void testGetUserProfile_OtherView_ReturnsCorrectDTO() {
            // Arrange
            String username = "testuser";
            Long userId = 1L;
            Long viewerId = 999L;
            User user = TestUtils.createValidUserEntityWithId(userId);
            user.setUsername(username);

            UserHeaderResponseDTO header = new UserHeaderResponseDTO(
                    username, user.getFirstName(), user.getLastName(), user.getBio(), user.getProfilePictureUrl()
            );

            List<BadgeResponseDTO> badges = List.of(
                    mock(BadgeResponseDTO.class)
            );

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertFalse(result.isSelf());
            assertEquals(header.username(), result.header().username());
            assertEquals(header.firstName(), result.header().firstName());
            assertEquals(header.lastName(), result.header().lastName());
            assertEquals(header.bio(), result.header().bio());
            assertEquals(header.profilePictureUrl(), result.header().profilePictureUrl());
            assertEquals(badges, result.badges());

            verify(userBO).getUserByUsername(username.toLowerCase());
            verify(badgeService).getBadgesByUser(user.getId());
        }

        @Test
        void testGetUserProfile_OtherView_WithPinnedEvent_DoesNotIncludePinnedEvent() {
            // Arrange
            String username = "testuser";
            Long userId = 1L;
            Long viewerId = 999L; // Different viewer
            User user = TestUtils.createValidUserEntityWithId(userId);
            user.setUsername(username);
            
            // User has a pinned event, but viewer is not the owner
            Event pinnedEvent = Event.createImpromptuEvent(ZonedDateTime.now(fixedClock), user);
            user.setPinnedImpromptuEvent(pinnedEvent);

            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertFalse(result.isSelf());
            assertEquals(null, result.pinnedImpromptuEvent()); // Should not include pinned event for other viewers
            verify(eventResponseDTOFactory, never()).createFromEvent(any(Event.class)); // Should not convert pinned event
        }

        @Test
        void testGetUserProfile_UserNotFound_ThrowsException() {
            // Arrange
            String username = "nonexistent";
            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.getUserProfile(username, 1L));
            verify(userBO).getUserByUsername(username.toLowerCase());
            verifyNoInteractions(badgeService);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void testGetUserProfile_SelfView_InvalidPinnedEventTypes_PerformsCleanup() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);
            
            // Test different invalid pinned event scenarios
            Event nonImpromptuEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            nonImpromptuEvent.setUnconfirmed(false); // confirmed but not impromptu
            user.setPinnedImpromptuEvent(nonImpromptuEvent);

            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(null, result.pinnedImpromptuEvent());
            verify(userBO).updateUser(user); // Should clean up invalid event
        }

        @Test
        void testGetUserProfile_SelfView_ValidImpromptuUnconfirmedEvent_IncludesEvent() {
            // Arrange
            String username = "testuser";
            Long viewerId = 1L;
            User user = TestUtils.createValidUserEntityWithId(viewerId);
            user.setUsername(username);
            
            // Create a valid pinned impromptu event (unconfirmed, impromptu, not completed)
            Event validPinnedEvent = Event.createImpromptuEvent(ZonedDateTime.now(fixedClock), user);
            validPinnedEvent.setUnconfirmed(true);
            validPinnedEvent.setCompleted(false);
            user.setPinnedImpromptuEvent(validPinnedEvent);
            
            EventResponseDTO pinnedEventDTO = mock(EventResponseDTO.class);
            List<BadgeResponseDTO> badges = List.of(mock(BadgeResponseDTO.class));

            when(userBO.getUserByUsername(username.toLowerCase())).thenReturn(Optional.of(user));
            when(badgeService.getBadgesByUser(user.getId())).thenReturn(badges);
            when(eventResponseDTOFactory.createFromEvent(validPinnedEvent)).thenReturn(pinnedEventDTO);

            // Act
            UserProfileResponseDTO result = userService.getUserProfile(username, viewerId);

            // Assert
            assertTrue(result.isSelf());
            assertEquals(pinnedEventDTO, result.pinnedImpromptuEvent());
            verify(userBO, never()).updateUser(any(User.class)); // Should not clean up valid event
            verify(eventResponseDTOFactory).createFromEvent(validPinnedEvent);
        }

    }

    @Nested
    class UpdateUserHeaderTests {

        @Test
        void testUpdateUserHeader_SuccessfullyUpdatesBioAndPicture() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            User user = TestUtils.createValidUserEntityWithId(userId);
            UserHeaderUpdateDTO input = new UserHeaderUpdateDTO("new bio", "https://new.picture/url.png");

            when(userBO.getUserById(userId)).thenReturn(Optional.of(user));
            doNothing().when(ownershipValidator).validateUserOwnership(user.getId(), userId);
            when(userBO.updateUser(user)).thenReturn(user);

            // Act
            UserHeaderResponseDTO result = userService.updateUserHeader(userId, input);

            // Assert
            assertEquals(user.getUsername(), result.username());
            assertEquals("new bio", result.bio());
            assertEquals("https://new.picture/url.png", result.profilePictureUrl());

            verify(userBO).getUserById(userId);
            verify(ownershipValidator).validateUserOwnership(user.getId(), userId);
            verify(userBO).updateUser(user);
        }

        @Test
        void testUpdateUserHeader_OnlyUpdatesBio() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            User user = TestUtils.createValidUserEntityWithId(userId);
            user.setProfilePictureUrl("https://same.picture.png");

            UserHeaderUpdateDTO input = new UserHeaderUpdateDTO("updated bio", "https://same.picture.png");

            when(userBO.getUserById(userId)).thenReturn(Optional.of(user));
            doNothing().when(ownershipValidator).validateUserOwnership(user.getId(), userId);
            when(userBO.updateUser(user)).thenReturn(user);

            // Act
            UserHeaderResponseDTO result = userService.updateUserHeader(userId, input);

            // Assert
            assertEquals("updated bio", result.bio());
            verify(userBO).updateUser(user);
        }

        @Test
        void testUpdateUserHeader_NoChanges_MakesNoUpdateCall() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            User user = TestUtils.createValidUserEntityWithId(userId);
            user.setBio("same bio");
            user.setProfilePictureUrl("https://same.url");

            UserHeaderUpdateDTO input = new UserHeaderUpdateDTO("same bio", "https://same.url");

            when(userBO.getUserById(userId)).thenReturn(Optional.of(user));
            doNothing().when(ownershipValidator).validateUserOwnership(user.getId(), userId);

            // Act
            UserHeaderResponseDTO result = userService.updateUserHeader(userId, input);

            // Assert
            assertEquals("same bio", result.bio());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void testUpdateUserHeader_UserNotFound_ThrowsException() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            UserHeaderUpdateDTO input = new UserHeaderUpdateDTO("bio", "url");

            when(userBO.getUserById(userId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.updateUserHeader(userId, input));
            verify(userBO).getUserById(userId);
            verify(ownershipValidator, never()).validateUserOwnership(any(), any());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void testUpdateUserHeader_ValidatesOwnership() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            User user = TestUtils.createValidUserEntityWithId(userId);
            UserHeaderUpdateDTO input = new UserHeaderUpdateDTO("bio", "pic");

            when(userBO.getUserById(userId)).thenReturn(Optional.of(user));
            doThrow(new RuntimeException("Ownership failed")).when(ownershipValidator)
                    .validateUserOwnership(user.getId(), userId);

            // Act + Assert
            assertThrows(RuntimeException.class, () -> userService.updateUserHeader(userId, input));
            verify(userBO).getUserById(userId);
            verify(ownershipValidator).validateUserOwnership(user.getId(), userId);
            verify(userBO, never()).updateUser(any());
        }

    }

    @Nested
    class ExistsByUsernameTests {

        @Test
        void testExistsByUsername_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String storedUsername = "storedUsername";
            when(userBO.existsByUsername(storedUsername.toLowerCase())).thenReturn(true);

            // Act
            boolean result = userService.existsByUsername(storedUsername);

            // Assert
            assertTrue(result);
            verify(userBO).existsByUsername(storedUsername.toLowerCase());
        }

        @Test
        void testExistsByUsername_ReturnsFalse() {
            // Arrange
            String input = "nonexistent";
            when(userBO.existsByUsername(input)).thenReturn(false);

            // Act
            boolean result = userService.existsByUsername(input);

            // Assert
            assertFalse(result);
            verify(userBO).existsByUsername(input);
        }
    }

    @Nested
    class ExistsByEmailTests {

        @Test
        void testExistsByEmail_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String inputEmail = "TestEmail@Example.com";
            when(userBO.existsByEmail(inputEmail.toLowerCase())).thenReturn(true);

            // Act
            boolean result = userService.existsByEmail(inputEmail);

            // Assert
            assertTrue(result);
            verify(userBO).existsByEmail(inputEmail.toLowerCase());
        }

        @Test
        void testExistsByEmail_ReturnsFalse() {
            // Arrange
            String inputEmail = "nonexistent@example.com";
            when(userBO.existsByEmail(inputEmail.toLowerCase())).thenReturn(false);

            // Act
            boolean result = userService.existsByEmail(inputEmail);

            // Assert
            assertFalse(result);
            verify(userBO).existsByEmail(inputEmail.toLowerCase());
        }
    }
}