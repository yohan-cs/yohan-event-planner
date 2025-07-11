package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.repository.BadgeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class BadgeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestDataHelper testDataHelper;
    @Autowired
    private BadgeRepository badgeRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login a user using TestDataHelper
        var auth = testDataHelper.registerAndLoginUserWithUser("badgecontroller");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class CreateBadgeTests {

        @Test
        void testCreateBadge_ShouldCreateAndReturnBadge() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("New Badge", null);

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Badge"))
                    .andExpect(jsonPath("$.sortOrder").value(0));
        }

    }

    @Nested
    class GetBadgeTests {

        @Test
        void testGetBadge_ShouldReturnBadge() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Test Badge");

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testBadge.getId()))
                    .andExpect(jsonPath("$.name").value(testBadge.getName())); // Assert the correct badge name
        }

    }

    @Nested
    class UpdateBadgeTests {

        @Test
        void testUpdateBadge_ShouldUpdateBadge() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Old Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Badge Name");

            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Badge Name"));
        }

        @Test
        void testUnauthorizedUpdateBadge_ShouldReturnUnauthorized() throws Exception {
            // Create and persist a badge using TestDataHelper
            Badge userBadge = testDataHelper.createAndPersistBadge(user, "User Badge");

            // Create an update DTO with a new name
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Badge Name");

            // Try updating the badge without providing an authorization token
            mockMvc.perform(patch("/badges/{id}", userBadge.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized since no token is provided
        }

    }

    @Nested
    class DeleteBadgeTests {

        @Test
        void testDeleteBadge_ShouldDeleteBadge() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Badge To Delete");

            // Perform the DELETE request
            mockMvc.perform(delete("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // Verify the badge is deleted from the repository
            assertThat(badgeRepository.findById(testBadge.getId())).isEmpty();
        }

        @Test
        void testUnauthorizedDeleteBadge_ShouldReturnUnauthorized() throws Exception {
            // Create and persist a badge using TestDataHelper
            Badge userBadge = testDataHelper.createAndPersistBadge(user, "User Badge");

            // Try deleting the badge without providing an authorization token
            mockMvc.perform(delete("/badges/{id}", userBadge.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized since no token is provided
        }

    }
}
