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

        @Test
        void testCreateDuplicateLabel_ShouldReturn400() throws Exception {
            // Create initial label
            testDataHelper.createAndPersistLabelWithExactName(user, "Duplicate Name");

            // Try to create another label with same name
            LabelCreateDTO dto = new LabelCreateDTO("Duplicate Name");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithBlankName_ShouldReturn400() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithNullName_ShouldReturn400() throws Exception {
            // Create JSON manually to include null value
            String jsonWithNull = "{\"name\": null}";
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNull))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithWhitespaceOnlyName_ShouldReturn400() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("   ");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithValidMinimumLength_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("A"); // 1 character
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("A"));
        }

        @Test
        void testCreateLabelWithoutAuth_ShouldReturn401() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("New Label");
            mockMvc.perform(post("/labels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
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

        @Test
        void testGetNonExistentLabel_ShouldReturn404() throws Exception {
            Long nonExistentId = 99999L;
            mockMvc.perform(get("/labels/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testGetLabelWithoutAuth_ShouldReturn401() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "test");
            mockMvc.perform(get("/labels/{id}", testLabel.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetOtherUsersLabel_ShouldReturn403() throws Exception {
            // Create another user and their label
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Label otherUserLabel = testDataHelper.createAndPersistLabel(otherUserAuth.user(), "other-label");

            // Try to access other user's label with current user's JWT
            mockMvc.perform(get("/labels/{id}", otherUserLabel.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
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
        void testUpdateNonExistentLabel_ShouldReturn404() throws Exception {
            Long nonExistentId = 99999L;
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Label");
            mockMvc.perform(patch("/labels/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testUpdateLabelToDuplicateName_ShouldReturn400() throws Exception {
            // Create two labels
            Label label1 = testDataHelper.createAndPersistLabelWithExactName(user, "Label One");
            testDataHelper.createAndPersistLabelWithExactName(user, "Label Two");

            // Try to update label1 to have same name as label2
            LabelUpdateDTO dto = new LabelUpdateDTO("Label Two");
            mockMvc.perform(patch("/labels/{id}", label1.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateOtherUsersLabel_ShouldReturn403() throws Exception {
            // Create another user and their label
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser2");
            Label otherUserLabel = testDataHelper.createAndPersistLabel(otherUserAuth.user(), "other-label");

            // Try to update other user's label
            LabelUpdateDTO dto = new LabelUpdateDTO("Hacked Label");
            mockMvc.perform(patch("/labels/{id}", otherUserLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void testUpdateSystemManagedLabel_ShouldReturn403() throws Exception {
            // Try to update the user's "Unlabeled" label
            LabelUpdateDTO dto = new LabelUpdateDTO("Modified System Label");
            mockMvc.perform(patch("/labels/{id}", user.getUnlabeled().getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void testUpdateLabelWithTooLongName_ShouldReturn400() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Test Label");
            // Create a name that's 101 characters (exceeds 100 char limit)
            String tooLongName = "A".repeat(101);
            LabelUpdateDTO dto = new LabelUpdateDTO(tooLongName);
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateLabelWithValidMaximumLength_ShouldSucceed() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Test Label");
            // Create a name that's exactly 100 characters (at the limit)
            String maxLengthName = "A".repeat(100);
            LabelUpdateDTO dto = new LabelUpdateDTO(maxLengthName);
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(maxLengthName));
        }

        @Test
        void testUpdateLabelWithWhitespaceOnlyName_ShouldReturn400() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Test Label");
            LabelUpdateDTO dto = new LabelUpdateDTO("   ");
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateLabelWithNullName_ShouldReturn400() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Test Label");
            // Create JSON manually to include null value
            String jsonWithNull = "{\"name\": null}";
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNull))
                    .andExpect(status().isBadRequest());
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
        void testDeleteNonExistentLabel_ShouldReturn404() throws Exception {
            Long nonExistentId = 99999L;
            mockMvc.perform(delete("/labels/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testDeleteOtherUsersLabel_ShouldReturn403() throws Exception {
            // Create another user and their label
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser3");
            Label otherUserLabel = testDataHelper.createAndPersistLabel(otherUserAuth.user(), "other-label");

            // Try to delete other user's label
            mockMvc.perform(delete("/labels/{id}", otherUserLabel.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
        }

        @Test
        void testDeleteSystemManagedLabel_ShouldReturn403() throws Exception {
            // Try to delete the user's "Unlabeled" label
            mockMvc.perform(delete("/labels/{id}", user.getUnlabeled().getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
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

    @Nested
    class MalformedRequestTests {

        @Test
        void testCreateLabelWithMalformedJson_ShouldReturn400() throws Exception {
            String malformedJson = "{\"name\": \"Test Label\", }";
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithWrongContentType_ShouldReturn415() throws Exception {
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain text"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void testUpdateLabelWithEmptyBody_ShouldReturn400() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Test Label");
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateLabelWithMissingContentType_ShouldReturn415() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Test Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void testCreateLabelWithInvalidJsonStructure_ShouldReturn400() throws Exception {
            String invalidJson = "{\"wrongField\": \"Test Label\"}";
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class JwtTokenEdgeCaseTests {

        @Test
        void testCreateLabelWithMalformedToken_ShouldReturn401() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Test Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer malformed.jwt.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateLabelWithInvalidTokenFormat_ShouldReturn401() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Test Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "InvalidFormat " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateLabelWithEmptyToken_ShouldReturn401() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Test Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateLabelWithMissingBearerPrefix_ShouldReturn401() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Test Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PathVariableValidationTests {

        @Test
        void testGetLabelWithNonNumericId_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/labels/abc")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetLabelWithNegativeId_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/labels/-1")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testGetLabelWithZeroId_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/labels/0")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testUpdateLabelWithNonNumericId_ShouldReturn400() throws Exception {
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Label");
            mockMvc.perform(patch("/labels/invalid")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testDeleteLabelWithNonNumericId_ShouldReturn400() throws Exception {
            mockMvc.perform(delete("/labels/xyz")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UnicodeAndSpecialCharacterTests {

        @Test
        void testCreateLabelWithEmoji_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Work 🎉");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Work 🎉"));
        }

        @Test
        void testCreateLabelWithUnicodeCharacters_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Tâches importantes");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Tâches importantes"));
        }

        @Test
        void testCreateLabelWithSpecialCharacters_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Client/Project & Tasks");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Client/Project & Tasks"));
        }

        @Test
        void testCreateLabelWithMixedLanguageCharacters_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("日本語 English Français");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("日本語 English Français"));
        }

        @Test
        void testUpdateLabelWithUnicodeCharacters_ShouldSucceed() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Original Name");
            LabelUpdateDTO dto = new LabelUpdateDTO("Nuevos nombres 🌟");
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Nuevos nombres 🌟"));
        }
    }

    @Nested
    class WhitespaceTrimmingTests {

        @Test
        void testCreateLabelTrimsLeadingWhitespace_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("   Trimmed Label");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Trimmed Label"));
        }

        @Test
        void testCreateLabelTrimsTrailingWhitespace_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("Trimmed Label   ");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Trimmed Label"));
        }

        @Test
        void testCreateLabelTrimsBothSidesWhitespace_ShouldSucceed() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("   Trimmed Label   ");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Trimmed Label"));
        }

        @Test
        void testUpdateLabelTrimsWhitespace_ShouldSucceed() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Original Label");
            LabelUpdateDTO dto = new LabelUpdateDTO("   Updated Label   ");
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Label"));
        }

        @Test
        void testCreateLabelWithOnlyWhitespaceAfterTrimming_ShouldReturn400() throws Exception {
            LabelCreateDTO dto = new LabelCreateDTO("     ");
            mockMvc.perform(post("/labels")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateLabelWithOnlyWhitespaceAfterTrimming_ShouldReturn400() throws Exception {
            Label testLabel = testDataHelper.createAndPersistLabel(user, "Original Label");
            LabelUpdateDTO dto = new LabelUpdateDTO("     ");
            mockMvc.perform(patch("/labels/{id}", testLabel.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
}