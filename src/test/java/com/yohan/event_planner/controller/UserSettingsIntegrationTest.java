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
@Import(TestConfig.class)
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

            // Set to 48 hours in the past, ensuring it's always a fixed interval
            ZonedDateTime dateInPast = ZonedDateTime.now().minusHours(48);
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
            ZonedDateTime deletionDateInPast = ZonedDateTime.now().minusDays(31); // exceeds your 30-day grace period
            user.markForDeletion(deletionDateInPast);

            // Persist the change to the database
            userRepository.save(user);
            userRepository.flush(); // Ensures immediate persistence

            // Act: fetch users eligible for hard deletion
            ZonedDateTime now = ZonedDateTime.now();
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

    }
}
