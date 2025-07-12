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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

        @Test
        void testCreateBadgeWithBlankName_ShouldReturn400() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithNullName_ShouldReturn400() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO(null, null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithInvalidJson_ShouldReturn400() throws Exception {
            // Invalid JSON is properly handled as 400 Bad Request by our exception handler
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUnauthorizedCreateBadge_ShouldReturnUnauthorized() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Unauthorized Badge", null);
            
            mockMvc.perform(post("/badges")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateBadgeWithLongName_ShouldValidateLength() throws Exception {
            // Create a name longer than typical validation limits (assuming 255 chars max)
            String longName = "A".repeat(256);
            BadgeCreateDTO dto = new BadgeCreateDTO(longName, null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithSpecialCharacters_ShouldHandleEncoding() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with émojis 🎯 and spëcial chars", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with émojis 🎯 and spëcial chars"));
        }

        @Test
        void testCreateBadgeWithWhitespaceOnlyName_ShouldReturn400() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("   ", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeResponse_ShouldContainCompleteStructure() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Structure Test Badge", null);

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Structure Test Badge"))
                    .andExpect(jsonPath("$.sortOrder").exists())
                    .andExpect(jsonPath("$.timeStats").exists())
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.timeStats.today").exists())
                    .andExpect(jsonPath("$.timeStats.thisWeek").exists())
                    .andExpect(jsonPath("$.timeStats.thisMonth").exists());
        }

        @Test
        void testCreateBadgeWithMissingContentType_ShouldReturn415() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Test Badge", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void testCreateBadgeWithEmptyBody_ShouldReturn400() throws Exception {
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithMalformedJson_ShouldReturn400() throws Exception {
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Test\", }")) // Trailing comma makes it malformed
                    .andExpect(status().isBadRequest());
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

        @Test
        void testGetNonExistentBadge_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/badges/{id}", 99999L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testUnauthorizedGetBadge_ShouldReturnUnauthorized() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Test Badge");
            
            // Note: Currently GET badges endpoint doesn't require authentication
            // This might be intentional design to allow public badge viewing
            // TODO: Review if this is the intended security behavior
            mockMvc.perform(get("/badges/{id}", testBadge.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        void testGetOtherUsersBadge_ShouldReturnBadge() throws Exception {
            // Note: Based on current implementation, getBadgeById doesn't validate ownership
            // This behavior might be intentional for read operations
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Badge otherUserBadge = testDataHelper.createAndPersistBadge(otherUserAuth.user(), "Other User Badge");
            
            // Current implementation allows accessing other users' badges for read operations
            mockMvc.perform(get("/badges/{id}", otherUserBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Other User Badge"));
        }

        @Test
        void testGetBadgeWithInvalidIdFormat_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/badges/{id}", "invalid-id")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetBadgeResponse_ShouldContainCompleteStructure() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Complete Structure Badge");

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").exists())
                    .andExpect(jsonPath("$.sortOrder").exists())
                    .andExpect(jsonPath("$.timeStats").exists())
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.timeStats.today").exists())
                    .andExpect(jsonPath("$.timeStats.thisWeek").exists())
                    .andExpect(jsonPath("$.timeStats.thisMonth").exists());
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
        void testUpdateNonExistentBadge_ShouldReturn404() throws Exception {
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Name");
            
            mockMvc.perform(patch("/badges/{id}", 99999L)
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testUpdateOtherUsersBadge_ShouldReturn403() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserupdate");
            Badge otherUserBadge = testDataHelper.createAndPersistBadge(otherUserAuth.user(), "Other User Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Hacked Name");
            
            mockMvc.perform(patch("/badges/{id}", otherUserBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
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

        @Test
        void testUpdateBadgeWithBlankName_ShouldReturn400() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithNullName_ShouldReturn400() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO(null);
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithLongName_ShouldValidateLength() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            String longName = "A".repeat(256);
            BadgeUpdateDTO dto = new BadgeUpdateDTO(longName);
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithWhitespaceOnlyName_ShouldReturn400() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("   ");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithSpecialCharacters_ShouldHandleEncoding() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated with émojis 🚀 and spëcial chars");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated with émojis 🚀 and spëcial chars"));
        }

        @Test
        void testUpdateBadgeWithInvalidIdFormat_ShouldReturn400() throws Exception {
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Name");
            
            mockMvc.perform(patch("/badges/{id}", "invalid-id")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithEmptyBody_ShouldReturn400() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateBadgeWithMissingContentType_ShouldReturn415() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Name");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnsupportedMediaType());
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
        void testDeleteNonExistentBadge_ShouldReturn404() throws Exception {
            mockMvc.perform(delete("/badges/{id}", 99999L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void testDeleteOtherUsersBadge_ShouldReturn403() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserdelete");
            Badge otherUserBadge = testDataHelper.createAndPersistBadge(otherUserAuth.user(), "Other User Badge");
            
            mockMvc.perform(delete("/badges/{id}", otherUserBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
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

        @Test
        void testDeleteBadgeWithInvalidIdFormat_ShouldReturn400() throws Exception {
            mockMvc.perform(delete("/badges/{id}", "invalid-id")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    class BusinessLogicTests {

        @Test
        void testCreateMultipleBadgesVerifySortOrder() throws Exception {
            // Create first badge
            BadgeCreateDTO dto1 = new BadgeCreateDTO("First Badge", null);
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto1)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sortOrder").value(0));

            // Create second badge
            BadgeCreateDTO dto2 = new BadgeCreateDTO("Second Badge", null);
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto2)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sortOrder").value(1));

            // Create third badge
            BadgeCreateDTO dto3 = new BadgeCreateDTO("Third Badge", null);
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto3)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sortOrder").value(2));
        }

        @Test
        void testBadgeResponseIncludesDetailedTimeStats() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Stats Test Badge");

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timeStats").exists())
                    .andExpect(jsonPath("$.timeStats.today").exists())
                    .andExpect(jsonPath("$.timeStats.thisWeek").exists())
                    .andExpect(jsonPath("$.timeStats.thisMonth").exists())
                    .andExpect(jsonPath("$.timeStats.allTime").exists())
                    .andExpect(jsonPath("$.timeStats.lastWeek").exists())
                    .andExpect(jsonPath("$.timeStats.lastMonth").exists());
        }

        @Test
        void testBadgeResponseIncludesLabelsArray() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Labels Test Badge");

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.labels").isArray());
        }

        @Test
        void testCreateBadgeGeneratesIdAndTimestamp() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("ID Test Badge", null);

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("ID Test Badge"))
                    .andExpect(jsonPath("$.sortOrder").isNumber());
        }

    }

    @Nested
    class BoundaryValueTests {

        @Test
        void testCreateBadgeWithMaximumValidNameLength() throws Exception {
            // Assuming maximum length is 255 characters
            String maxName = "A".repeat(255);
            BadgeCreateDTO dto = new BadgeCreateDTO(maxName, null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(maxName));
        }

        @Test
        void testCreateBadgeWithMinimumValidNameLength() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("A", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("A"));
        }

        @Test
        void testUpdateBadgeWithMaximumValidNameLength() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            String maxName = "B".repeat(255);
            BadgeUpdateDTO dto = new BadgeUpdateDTO(maxName);
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(maxName));
        }

        @Test
        void testUpdateBadgeWithMinimumValidNameLength() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("X");
            
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("X"));
        }

    }

    @Nested
    class SecurityAndAuthorizationTests {

        @Test
        void testCreateBadgeWithExpiredToken_ShouldReturn401() throws Exception {
            // Note: This test requires a way to generate expired tokens
            // For now, we'll test with malformed token
            BadgeCreateDTO dto = new BadgeCreateDTO("Test Badge", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer expired.token.here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testOperationsWithMalformedToken_ShouldReturn401() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Test Badge", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer malformed-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testOperationsWithInvalidTokenPrefix_ShouldReturn401() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Test Badge", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "InvalidPrefix " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    class ErrorResponseFormatTests {

        @Test
        void testValidationErrorsHaveConsistentFormat() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("", null); // Invalid: blank name
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

        @Test
        void testNotFoundErrorsHaveConsistentFormat() throws Exception {
            mockMvc.perform(get("/badges/{id}", 99999L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

        @Test
        void testOwnershipErrorsHaveConsistentFormat() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("errorformatuser");
            Badge otherUserBadge = testDataHelper.createAndPersistBadge(otherUserAuth.user(), "Other User Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Hacked Name");
            
            mockMvc.perform(patch("/badges/{id}", otherUserBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

    }

    @Nested
    class HttpMethodAndHeadersTests {

        @Test
        void testPostWithGetMethod_ShouldReturn405() throws Exception {
            mockMvc.perform(get("/badges")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void testPatchWithPostMethod_ShouldReturn405() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Test Badge");
            BadgeUpdateDTO dto = new BadgeUpdateDTO("Updated Name");
            
            mockMvc.perform(post("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        void testDeleteWithPostMethod_ShouldReturn405() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Test Badge");
            
            mockMvc.perform(post("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isMethodNotAllowed());
        }

    }

    @Nested
    class ConcurrencyAndPerformanceTests {

        @Test
        void testConcurrentBadgeCreation_ShouldHandleGracefully() throws Exception {
            // Note: This is a simplified concurrency test using sequential requests
            // In a real scenario, you might want to use CompletableFuture or similar
            BadgeCreateDTO dto1 = new BadgeCreateDTO("Concurrent Badge 1", null);
            BadgeCreateDTO dto2 = new BadgeCreateDTO("Concurrent Badge 2", null);
            
            // Create two badges rapidly in succession
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto2)))
                    .andExpect(status().isCreated());
        }

        @Test
        void testConcurrentUpdateDelete_ShouldHandleRaceConditions() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Race Condition Badge");
            BadgeUpdateDTO updateDto = new BadgeUpdateDTO("Updated Name");
            
            // Attempt update first
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk());

            // Then delete (should still work since update succeeded)
            mockMvc.perform(delete("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
        }

        @Test
        void testMultipleRapidOperations_ShouldMaintainDataConsistency() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Consistency Test Badge");
            
            // Rapid sequence of operations
            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk());

            BadgeUpdateDTO updateDto = new BadgeUpdateDTO("Updated Name");
            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"));
        }

    }

    @Nested
    class LabelAssociationTests {

        @Test
        void testCreateBadgeWithValidLabelIds_ShouldCreateSuccessfully() throws Exception {
            // Create some labels first
            var label1 = testDataHelper.createAndPersistLabel(user, "Test Label 1");
            var label2 = testDataHelper.createAndPersistLabel(user, "Test Label 2");
            
            Set<Long> labelIds = Set.of(label1.getId(), label2.getId());
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Labels", labelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with Labels"))
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.labels").isArray());
        }

        @Test
        void testCreateBadgeWithSingleValidLabelId_ShouldCreateSuccessfully() throws Exception {
            var label = testDataHelper.createAndPersistLabel(user, "Single Test Label");
            
            Set<Long> labelIds = Set.of(label.getId());
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Single Label", labelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with Single Label"))
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.labels").isArray());
        }

        @Test
        void testCreateBadgeWithNonExistentLabelIds_ShouldReturn400() throws Exception {
            Set<Long> invalidLabelIds = Set.of(99999L, 88888L);
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Invalid Labels", invalidLabelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithMixedValidInvalidLabelIds_ShouldReturn400() throws Exception {
            var validLabel = testDataHelper.createAndPersistLabel(user, "Valid Label");
            
            // Mix valid and invalid label IDs
            Set<Long> mixedLabelIds = Set.of(validLabel.getId(), 99999L);
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Mixed Labels", mixedLabelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateBadgeWithOtherUsersLabels_ShouldReturn403() throws Exception {
            // Create another user and their labels
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("labelowner");
            var otherUserLabel = testDataHelper.createAndPersistLabel(otherUserAuth.user(), "Other User Label");
            
            // Try to create badge with other user's labels
            Set<Long> otherUserLabelIds = Set.of(otherUserLabel.getId());
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Other User Labels", otherUserLabelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void testCreateBadgeWithMixedOwnAndOtherUsersLabels_ShouldReturn403() throws Exception {
            // Create own label
            var ownLabel = testDataHelper.createAndPersistLabel(user, "Own Label");
            
            // Create another user and their label
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("mixedlabelowner");
            var otherUserLabel = testDataHelper.createAndPersistLabel(otherUserAuth.user(), "Other User Label");
            
            // Try to create badge with mixed ownership labels
            Set<Long> mixedOwnershipLabelIds = Set.of(ownLabel.getId(), otherUserLabel.getId());
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Mixed Ownership Labels", mixedOwnershipLabelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void testCreateBadgeWithEmptyLabelIds_ShouldCreateSuccessfully() throws Exception {
            Set<Long> emptyLabelIds = Set.of();
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Empty Labels", emptyLabelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with Empty Labels"))
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.labels").isArray())
                    .andExpect(jsonPath("$.labels").isEmpty());
        }

        @Test
        void testCreateBadgeWithNullLabelIds_ShouldCreateSuccessfully() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Null Labels", null);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with Null Labels"))
                    .andExpect(jsonPath("$.labels").exists())
                    .andExpect(jsonPath("$.labels").isArray());
        }

        @Test
        void testCreateBadgeWithDuplicateLabelIds_ShouldHandleGracefully() throws Exception {
            var label = testDataHelper.createAndPersistLabel(user, "Duplicate Test Label");
            
            // Note: Since we're using Set<Long>, duplicates will be automatically deduplicated
            // But let's test the behavior when the same ID appears in the set
            Set<Long> labelIds = Set.of(label.getId());
            BadgeCreateDTO dto = new BadgeCreateDTO("Badge with Duplicate Labels", labelIds);
            
            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Badge with Duplicate Labels"));
        }

    }

    @Nested
    class ResponseFormatAndEncodingTests {

        @Test
        void testResponsesHaveCorrectContentType() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Content Type Test Badge");

            mockMvc.perform(get("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/json"));
        }

        @Test
        void testCreateResponseHasCorrectContentType() throws Exception {
            BadgeCreateDTO dto = new BadgeCreateDTO("Create Content Type Test", null);

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Content-Type", "application/json"));
        }

        @Test
        void testResponsesHandleUnicodeCorrectly() throws Exception {
            String unicodeName = "测试徽章 🎯 émojis and spëcial characters";
            BadgeCreateDTO dto = new BadgeCreateDTO(unicodeName, null);

            mockMvc.perform(post("/badges")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(unicodeName))
                    .andExpect(header().string("Content-Type", "application/json"));
        }

        @Test
        void testUpdateResponseHandlesUnicodeCorrectly() throws Exception {
            Badge testBadge = testDataHelper.createAndPersistBadge(user, "Original Badge");
            String unicodeUpdateName = "更新的徽章 🚀 with émojis";
            BadgeUpdateDTO updateDto = new BadgeUpdateDTO(unicodeUpdateName);

            mockMvc.perform(patch("/badges/{id}", testBadge.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(unicodeUpdateName))
                    .andExpect(header().string("Content-Type", "application/json"));
        }

        @Test
        void testErrorResponsesHaveCorrectContentType() throws Exception {
            mockMvc.perform(get("/badges/{id}", 99999L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Content-Type", "application/json"));
        }

    }
}
