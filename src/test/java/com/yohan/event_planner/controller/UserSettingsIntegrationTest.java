package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.service.UserService;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserSettingsIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired
    private UserRepository userRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Use TestDataHelper to register and login the user
        var auth = testDataHelper.registerAndLoginUserWithUser("settings");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    private void simulatePastDeletionDate(User user) {
        try {
            Field field = User.class.getDeclaredField("scheduledDeletionDate");
            field.setAccessible(true);

            // Set to 48 hours before a fixed time
            ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z");
            ZonedDateTime dateInPast = fixedTime.minusHours(48);
            field.set(user, dateInPast);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set scheduledDeletionDate via reflection", e);
        }
    }

    @Nested
    class GetSettingsTests {

        @Test
        void testGetSettings_ShouldReturnUserProfile() throws Exception {
            mockMvc.perform(get("/settings")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.timezone").value(user.getTimezone()));
        }

        @Test
        void testUnauthorizedGetSettings_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/settings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetSettings_MalformedJWT_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/settings")
                            .header("Authorization", "Bearer malformed.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetSettings_EmptyAuthorizationHeader_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/settings")
                            .header("Authorization", ""))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    class UpdateSettingsTests {

        @Test
        void testUpdateSettings_ShouldUpdateUserProfile() throws Exception {
            UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, "UpdatedFirst", null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("UpdatedFirst"));
        }

        @Test
        void testUnauthorizedUpdateSettings_ShouldReturnUnauthorized() throws Exception {
            UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, "UpdatedFirst", null, null);

            mockMvc.perform(patch("/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testUpdateSettings_InvalidData_ShouldReturnBadRequest() throws Exception {
            // Sending an invalid update (e.g., empty username)
            UserUpdateDTO invalidUpdateDTO = new UserUpdateDTO("", null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUpdateDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_AllFieldsNull_ShouldReturnUnchangedProfile() throws Exception {
            // Test PATCH with all null fields - should return current profile unchanged
            UserUpdateDTO allNullUpdateDTO = new UserUpdateDTO(null, null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(allNullUpdateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                    .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                    .andExpect(jsonPath("$.timezone").value(user.getTimezone()));
        }

        @Test
        void testUpdateSettings_DuplicateUsername_ShouldReturnConflict() throws Exception {
            // Create another user to test username conflict
            var otherAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            
            // Try to update current user's username to the other user's username
            UserUpdateDTO conflictUpdateDTO = new UserUpdateDTO(otherAuth.user().getUsername(), null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(conflictUpdateDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        void testUpdateSettings_DuplicateEmail_ShouldReturnConflict() throws Exception {
            // Create another user to test email conflict
            var otherAuth = testDataHelper.registerAndLoginUserWithUser("emailtest");
            
            // Try to update current user's email to the other user's email
            UserUpdateDTO conflictUpdateDTO = new UserUpdateDTO(null, null, otherAuth.user().getEmail(), null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(conflictUpdateDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        void testUpdateSettings_InvalidTimezone_ShouldReturnBadRequest() throws Exception {
            // Test with invalid timezone
            UserUpdateDTO invalidTimezoneDTO = new UserUpdateDTO(null, null, null, null, null, "Invalid/Timezone");

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidTimezoneDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_PasswordUpdate_ShouldSucceed() throws Exception {
            UserUpdateDTO passwordUpdate = new UserUpdateDTO(null, "newValidPassword123", null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()));
        }

        @Test
        void testUpdateSettings_SamePassword_ShouldReturnConflict() throws Exception {
            // Try to update to current password - should return 409
            // Use the password from TestConstants.VALID_PASSWORD
            UserUpdateDTO samePasswordUpdate = new UserUpdateDTO(null, "BuckusIsDope42!", null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(samePasswordUpdate)))
                    .andExpect(status().isConflict());
        }

        @Test
        void testUpdateSettings_InvalidPasswordLength_ShouldReturnBadRequest() throws Exception {
            UserUpdateDTO shortPassword = new UserUpdateDTO(null, "short", null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shortPassword)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_MultipleFields_ShouldUpdateAll() throws Exception {
            UserUpdateDTO multiUpdate = new UserUpdateDTO(
                "newusername123", null, "new@email.com", "NewFirst", "NewLast", "America/Chicago"
            );

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(multiUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("newusername123"))
                    .andExpect(jsonPath("$.email").value("new@email.com"))
                    .andExpect(jsonPath("$.firstName").value("NewFirst"))
                    .andExpect(jsonPath("$.lastName").value("NewLast"))
                    .andExpect(jsonPath("$.timezone").value("America/Chicago"));
        }

        @Test
        void testUpdateSettings_PartialFailure_ShouldRejectAll() throws Exception {
            // Create another user to test conflict scenario
            var otherAuth = testDataHelper.registerAndLoginUserWithUser("conflictuser");
            
            // Try to update multiple fields where one will conflict (atomic failure)
            UserUpdateDTO conflictUpdate = new UserUpdateDTO(
                otherAuth.user().getUsername(), // This will conflict
                null,
                "valid@email.com", // This is valid
                "ValidFirst", // This is valid
                null, 
                null
            );

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(conflictUpdate)))
                    .andExpect(status().isConflict());

            // Verify no fields were updated (atomic semantics)
            mockMvc.perform(get("/settings")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.firstName").value(user.getFirstName()));
        }

        @Test
        void testUpdateSettings_UsernameWithSpecialChars_ShouldReturnBadRequest() throws Exception {
            UserUpdateDTO invalidUsername = new UserUpdateDTO("user@name", null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUsername)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_InvalidEmailFormat_ShouldReturnBadRequest() throws Exception {
            UserUpdateDTO invalidEmail = new UserUpdateDTO(null, null, "not-an-email", null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidEmail)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_CaseInsensitiveUsername_ShouldHandleCorrectly() throws Exception {
            // Create another user to test case insensitive uniqueness
            var otherAuth = testDataHelper.registerAndLoginUserWithUser("casetest");
            
            // Try to update to the same username but different case - should conflict
            UserUpdateDTO caseConflict = new UserUpdateDTO(otherAuth.user().getUsername().toUpperCase(), null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(caseConflict)))
                    .andExpect(status().isConflict());
        }

        @Test
        void testUpdateSettings_MaxLengthFields_ShouldSucceed() throws Exception {
            // Test maximum allowed field lengths based on ApplicationConstants
            String maxUsername = "a".repeat(30); // USERNAME_MAX_LENGTH
            String maxFirstName = "A".repeat(50); // SHORT_NAME_MAX_LENGTH  
            String maxLastName = "L".repeat(50); // SHORT_NAME_MAX_LENGTH

            UserUpdateDTO maxLengthUpdate = new UserUpdateDTO(
                maxUsername, null, null, maxFirstName, maxLastName, null
            );

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(maxLengthUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(maxUsername))
                    .andExpect(jsonPath("$.firstName").value(maxFirstName))
                    .andExpect(jsonPath("$.lastName").value(maxLastName));
        }

        @Test
        void testUpdateSettings_ExceedsMaxLength_ShouldReturnBadRequest() throws Exception {
            // Test fields exceeding limits
            String tooLongUsername = "a".repeat(31); // Exceeds USERNAME_MAX_LENGTH (30)
            UserUpdateDTO exceedsLimits = new UserUpdateDTO(tooLongUsername, null, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(exceedsLimits)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_ExceedsMaxPasswordLength_ShouldReturnBadRequest() throws Exception {
            // Test password exceeding limit
            String tooLongPassword = "a".repeat(73); // Exceeds PASSWORD_MAX_LENGTH (72)
            UserUpdateDTO exceedsPasswordLimit = new UserUpdateDTO(null, tooLongPassword, null, null, null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(exceedsPasswordLimit)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateSettings_MalformedJWT_ShouldReturnUnauthorized() throws Exception {
            UserUpdateDTO updateDTO = new UserUpdateDTO(null, null, null, "TestFirst", null, null);

            mockMvc.perform(patch("/settings")
                            .header("Authorization", "Bearer malformed.jwt.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    class DeleteAccountTests {

        @Test
        void testSoftDeleteUser_ShouldMarkForDeletion() throws Exception {
            // Perform soft delete
            mockMvc.perform(delete("/settings")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // DB validation (post-delete) using the repository directly
            Optional<User> deletedUser = userRepository.findById(user.getId());
            assertThat(deletedUser).isPresent();
            assertThat(deletedUser.get().isPendingDeletion()).isTrue();
        }

        @Test
        void testHardDeleteEligibility_ShouldMarkForHardDeletion() throws Exception {
            // Arrange: mark user for deletion with a time far enough in the past
            ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z");
            ZonedDateTime deletionDateInPast = fixedTime.minusDays(31); // exceeds 30-day grace period
            user.markForDeletion(deletionDateInPast);

            // Persist the change to the database
            userRepository.save(user);
            userRepository.flush(); // Ensures immediate persistence

            // Act: fetch users eligible for hard deletion
            ZonedDateTime now = fixedTime;
            List<User> eligibleForHardDeletion = userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(now);

            // Assert: user should now be eligible for hard deletion
            assertThat(eligibleForHardDeletion)
                    .anyMatch(u -> u.getId().equals(user.getId()));
        }

        @Test
        void testUnauthorizedDeleteSettings_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(delete("/settings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testSoftDeleteInvalidUser_ShouldReturnUnauthorized() throws Exception {
            // Using an invalid JWT token
            String invalidJwt = "invalid-jwt-token";

            // Expecting a 401 Unauthorized since the JWT is invalid
            mockMvc.perform(delete("/settings")
                            .header("Authorization", "Bearer " + invalidJwt))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testSoftDeleteUserTwice_ShouldReturnNoContentOnce() throws Exception {
            mockMvc.perform(delete("/settings")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // Try deleting again and expect same result (no side effects)
            mockMvc.perform(delete("/settings")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
        }

        @Test
        void testDeleteAccount_MalformedJWT_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(delete("/settings")
                            .header("Authorization", "Bearer malformed.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testDeleteAccount_EmptyAuthorizationHeader_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(delete("/settings")
                            .header("Authorization", ""))
                    .andExpect(status().isUnauthorized());
        }

    }
}
