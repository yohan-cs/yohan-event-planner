package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.repository.LabelRepository;
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
class LabelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestDataHelper testDataHelper;
    @Autowired
    private LabelRepository labelRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login a user using TestDataHelper
        var auth = testDataHelper.registerAndLoginUserWithUser("labelcontroller");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class CreateLabelTests {

        @Test
        void testCreateLabel_ShouldCreateAndReturnLabel() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("New Label");

            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Label"))
                    .andExpect(jsonPath("$.creatorUsername").value(user.getUsername()));
        }

    }

    @Nested
    class GetLabelTests {

        @Test
        void testGetLabel_ShouldReturnLabel() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "test");
            // Use the testLabel created in setUp
            mockMvc.perform(get("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testLabel.getId()))
                    .andExpect(jsonPath("$.name").value(testLabel.getName())); // Assert the correct label name
        }

    }

    @Nested
    class UpdateLabelTests {

        @Test
        void testUpdateLabel_ShouldUpdateLabel() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Old Label");
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Label");

            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Label"));
        }

        @Test
        void testUnauthorizedUpdateLabel_ShouldReturnUnauthorized() throws Exception {
            // Create and persist a label using TestDataHelper for the user
            Label userLabel = testDataHelper.createAndPersistLabel(user, "User Label");

            // Create an update DTO with a new name
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Label");

            // Try updating the label without providing an authorization token
            mockMvc.perform(patch("/labels/{id}", userLabel.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized since no token is provided
        }
    }

    @Nested
    class DeleteLabelTests {

        @Test
        void testDeleteLabel_ShouldDeleteLabel() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Label To Delete");

            // Perform the DELETE request
            mockMvc.perform(delete("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // Verify the label is deleted from the repository
            assertThat(labelRepository.findById(testLabel.getId())).isEmpty();
        }

        @Test
        void testUnauthorizedDeleteLabel_ShouldReturnUnauthorized() throws Exception {
            // Create and persist a label using TestDataHelper for the user
            Label userLabel = testDataHelper.createAndPersistLabel(user, "User Label");

            // Try deleting the label without providing an authorization token
            mockMvc.perform(delete("/labels/{id}", userLabel.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized since no token is provided
        }

    }
}
