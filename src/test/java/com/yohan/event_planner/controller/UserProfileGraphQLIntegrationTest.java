package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.exception.GraphQLExceptionHandler;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import com.yohan.event_planner.util.TestUtils;
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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yohan.event_planner.domain.enums.RecapMediaType.VIDEO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Import({GraphQLExceptionHandler.class, TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
class UserProfileGraphQLIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private TestDataHelper testDataHelper;

    @BeforeEach
    void setUp() {
    }

    // region --- Helper Methods ---

    private String buildUpdateBadgeMutation(Long badgeId, String name) {
        StringBuilder inputBuilder = new StringBuilder("{");

        // Only append the name if it is not null
        if (name != null) {
            inputBuilder.append("\"name\":\"").append(name).append("\",");
        }

        if (inputBuilder.charAt(inputBuilder.length() - 1) == ',') {
            inputBuilder.deleteCharAt(inputBuilder.length() - 1);
        }

        inputBuilder.append("}");

        // Return the mutation with the updated variables
        return String.format("""
        {
          "query": "mutation($id: ID!, $input: UpdateBadgeInput!) { updateBadge(id: $id, input: $input) { id name labels { id name } } }",
          "variables": {
            "id": %d,
            "input": %s
          }
        }
        """, badgeId, inputBuilder.toString());
    }

    private String buildUpdateUserHeaderMutation(String bio, String profilePictureUrl) throws Exception {
        Map<String, Object> inputMap = new LinkedHashMap<>();
        if (bio != null) inputMap.put("bio", bio);
        if (profilePictureUrl != null) inputMap.put("profilePictureUrl", profilePictureUrl);

        Map<String, Object> payload = Map.of(
                "query", "mutation($input: UpdateUserHeaderInput!) { updateUserHeader(input: $input) { username firstName lastName bio profilePictureUrl } }",
                "variables", Map.of("input", inputMap)
        );

        return objectMapper.writeValueAsString(payload);
    }

    private String buildDeleteBadgeMutation(Long badgeId) {
        return String.format("""
    {
      "query": "mutation($id: ID!) { deleteBadge(id: $id) }",
      "variables": {
        "id": "%d"
      }
    }
    """, badgeId);
    }

    private String buildReorderBadgesMutation(Long... badgeIds) {
        if (badgeIds == null || badgeIds.length == 0) {
            throw new IllegalArgumentException("At least one badge ID must be provided");
        }

        String ids = Arrays.stream(badgeIds)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        if (ids.isBlank()) {
            throw new IllegalArgumentException("Badge IDs must not contain only nulls");
        }

        return String.format("""
            {
              "query": "mutation($ids: [ID!]!) { reorderBadges(ids: $ids) }",
              "variables": {
                "ids": [%s]
              }
            }
            """, ids);
    }

    private String buildReorderBadgeLabelsMutation(Long badgeId, List<Long> labelOrder) {
        String ids = labelOrder.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        return String.format("""
            {
              "query": "mutation($badgeId: ID!, $labelOrder: [ID!]!) { reorderBadgeLabels(badgeId: $badgeId, labelOrder: $labelOrder) }",
              "variables": {
                "badgeId": %d,
                "labelOrder": [%s]
              }
            }
            """, badgeId, ids);
    }

    private String buildWeekViewQuery(String username, String anchorDate) {
        return String.format("""
            {
              "query": "query($username: String!, $anchorDate: Date!) { userProfile(username: $username) { weekView(anchorDate: $anchorDate) { days { date events { id name } } } } }",
              "variables": {
                "username": "%s",
                "anchorDate": "%s"
              }
            }
            """, username, anchorDate);
    }

    private String buildUpdateEventMutation(Long eventId, Map<String, Object> inputFields) throws Exception {
        Map<String, Object> variables = Map.of(
                "id", eventId,
                "input", inputFields
        );

        Map<String, Object> payload = Map.of(
                "query", """
                mutation($id: ID!, $input: UpdateEventInput!) {
                    updateEvent(id: $id, input: $input) {
                        id name description isCompleted unconfirmed isVirtual label { id }
                    }
                }
            """,
                "variables", variables
        );

        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> buildUpdateEventInputFields(
            String name,
            String description,
            Boolean isCompleted,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            String labelId
    ) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (name != null) map.put("name", wrapField(name));
        if (description != null) map.put("description", wrapField(description));
        if (isCompleted != null) map.put("isCompleted", wrapField(isCompleted));
        if (startTime != null) {
            String isoStartTime = startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            map.put("startTime", wrapField(isoStartTime));
        }
        if (endTime != null) {
            String isoEndTime = endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            map.put("endTime", wrapField(isoEndTime));
        }
        if (labelId != null) map.put("labelId", wrapField(labelId));
        return map;
    }

    private String buildDeleteEventMutation(Long eventId) {
        return String.format("""
        {
          "query": "mutation($id: ID!) { deleteEvent(id: $id) }",
          "variables": {
            "id": "%d"
          }
        }
        """, eventId);
    }

    private String buildAddEventRecapMutation(Long eventId, String recapName, String notes, Boolean isUnconfirmed, List<Map<String, Object>> media) throws Exception {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("eventId", eventId.toString());
        if (recapName != null) input.put("recapName", recapName);
        if (notes != null) input.put("notes", notes);
        if (isUnconfirmed != null) input.put("isUnconfirmed", isUnconfirmed);
        if (media != null) input.put("media", media);

        Map<String, Object> payload = Map.of(
                "query", """
                mutation($input: AddEventRecapInput!) {
                    addEventRecap(input: $input) {
                        id eventName username notes media { id mediaUrl mediaType }
                    }
                }
            """,
                "variables", Map.of("input", input)
        );

        return objectMapper.writeValueAsString(payload);
    }

    private String buildUpdateEventRecapMutation(Long eventId, String notes, List<Map<String, Object>> media) throws Exception {
        Map<String, Object> input = new LinkedHashMap<>();
        if (notes != null) input.put("notes", notes);
        if (media != null) input.put("media", media);

        Map<String, Object> payload = Map.of(
                "query", """
                mutation($eventId: ID!, $input: UpdateEventRecapInput!) {
                    updateEventRecap(eventId: $eventId, input: $input) {
                        id notes media { id mediaUrl mediaType }
                    }
                }
            """,
                "variables", Map.of(
                        "eventId", eventId,
                        "input", input
                )
        );

        return objectMapper.writeValueAsString(payload);
    }

    private String buildConfirmEventRecapMutation(Long eventId) {
        return String.format("""
        {
          "query": "mutation($eventId: ID!) { confirmEventRecap(eventId: $eventId) { id notes } }",
          "variables": {
            "eventId": "%d"
          }
        }
        """, eventId);
    }

    private String buildDeleteEventRecapMutation(Long eventId) {
        return String.format("""
        {
          "query": "mutation($eventId: ID!) { deleteEventRecap(eventId: $eventId) }",
          "variables": {
            "eventId": "%d"
          }
        }
        """, eventId);
    }

    private String buildEventRecapQuery(Long eventId) {
        return String.format("""
        {
          "query": "query($eventId: ID!) { eventRecap(eventId: $eventId) { id eventName username notes media { id mediaUrl mediaType } } }",
          "variables": {
            "eventId": "%d"
          }
        }
        """, eventId);
    }

    private String buildAddRecapMediaMutation(Long recapId, String mediaUrl, String mediaType, Integer durationSeconds, Integer mediaOrder) {
        return String.format("""
    {
      "query": "mutation($recapId: ID!, $input: CreateRecapMediaInput!) { addRecapMedia(recapId: $recapId, input: $input) { id mediaUrl mediaType durationSeconds mediaOrder } }",
      "variables": {
        "recapId": %d,
        "input": {
          "mediaUrl": "%s",
          "mediaType": "%s",
          "durationSeconds": %d,
          "mediaOrder": %d
        }
      }
    }
    """, recapId, mediaUrl, mediaType, durationSeconds != null ? durationSeconds : 0, mediaOrder != null ? mediaOrder : 0);
    }

    private String buildUpdateRecapMediaMutation(Long mediaId, String mediaUrl, String mediaType, int durationSeconds) {
        return String.format("""
        {
          "query": "mutation UpdateRecapMedia($mediaId: ID!, $input: UpdateRecapMediaInput!) { updateRecapMedia(mediaId: $mediaId, input: $input) { id mediaUrl mediaType durationSeconds } }",
          "variables": {
            "mediaId": "%d",
            "input": {
              "mediaUrl": "%s",
              "mediaType": "%s",
              "durationSeconds": %d
            }
          }
        }
        """, mediaId, mediaUrl, mediaType, durationSeconds);
    }

    private String buildDeleteRecapMediaMutation(Long mediaId) {
        return String.format("""
        {
          "query": "mutation($mediaId: ID!) { deleteRecapMedia(mediaId: $mediaId) }",
          "variables": {
            "mediaId": %d
          }
        }
        """, mediaId);
    }

    private String buildReorderRecapMediaMutation(Long recapId, List<Long> mediaOrder) {
        String ids = mediaOrder.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format("""
        {
          "query": "mutation($recapId: ID!, $mediaOrder: [ID!]!) { reorderRecapMedia(recapId: $recapId, mediaOrder: $mediaOrder) }",
          "variables": {
            "recapId": %d,
            "mediaOrder": [%s]
          }
        }
        """, recapId, ids);
    }

    private void performAndAssertUserHeaderUpdate(String jwt, UserCreateDTO dto, String expectedBio, String expectedPicUrl) throws Exception {
        String mutation = buildUpdateUserHeaderMutation(expectedBio, expectedPicUrl);

        var resultActions = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(mutation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateUserHeader.username").value(dto.username()))
                .andExpect(jsonPath("$.data.updateUserHeader.firstName").value(dto.firstName()))
                .andExpect(jsonPath("$.data.updateUserHeader.lastName").value(dto.lastName()));

        if (expectedBio != null) {
            resultActions.andExpect(jsonPath("$.data.updateUserHeader.bio").value(expectedBio));
        } else {
            resultActions.andExpect(jsonPath("$.data.updateUserHeader.bio").doesNotExist());
        }

        if (expectedPicUrl != null) {
            resultActions.andExpect(jsonPath("$.data.updateUserHeader.profilePictureUrl").value(expectedPicUrl));
        } else {
            resultActions.andExpect(jsonPath("$.data.updateUserHeader.profilePictureUrl").doesNotExist());
        }
    }

    private void performAndAssertBadgeUpdate(String mutation, String jwt, Long badgeId, String expectedName) throws Exception {
        var action = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(mutation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateBadge.id").value(badgeId.toString()));

        // Assert the name if it is provided
        if (expectedName != null) {
            action.andExpect(jsonPath("$.data.updateBadge.name").value(expectedName));
        }
    }

    private void performAndAssertWeekViewSuccess(String jwt, String graphqlQuery) throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(graphqlQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userProfile.weekView.days").isArray())
                .andExpect(jsonPath("$.data.userProfile.weekView.days.length()").value(7));
    }

    private void performAndAssertEventUpdate(String jwt, String mutation, Long expectedId, Map<String, Object> expectedFields) throws Exception {
        var resultActions = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content(mutation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateEvent.id").value(expectedId.toString()));

        if (expectedFields != null) {
            for (var entry : expectedFields.entrySet()) {
                String jsonPath = "$.data.updateEvent." + entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    resultActions.andExpect(jsonPath(jsonPath).doesNotExist());
                } else if (value instanceof Boolean boolVal) {
                    resultActions.andExpect(jsonPath(jsonPath).value(boolVal));
                } else {
                    resultActions.andExpect(jsonPath(jsonPath).value(value.toString()));
                }
            }
        }
    }


    private void performAndAssertUnauthorized(String graphqlQuery) throws Exception {
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphqlQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_ACCESS"))
                .andExpect(jsonPath("$.errors[0].extensions.status").value(401));
    }

    private record AuthResult(String jwt, User user) {}

    private Map<String, Object> wrapField(Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (value instanceof ZonedDateTime zdt) {
            map.put("value", zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else if (value instanceof LocalDateTime ldt) {
            map.put("value", ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            map.put("value", value);
        }
        return map;
    }

    private String formatMediaType(Object mediaType) {
        if (mediaType == null) return "null";
        if (mediaType instanceof RecapMediaType type) {
            return type.name(); // no quotes for enum
        }
        // For invalid String input (testing schema error), return raw without quotes to trigger validation
        return mediaType.toString();
    }

    // endregion

    @Nested
    class UpdateUserHeaderTests {

        @Test
        void testUpdateUserHeader_AllFields_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateall");

            String mutation = buildUpdateUserHeaderMutation("This is my bio", "https://example.com/pic.jpg");

            performAndAssertUserHeaderUpdate(auth.jwt(), TestUtils.createValidRegisterPayload("updateall"), "This is my bio", "https://example.com/pic.jpg");
        }

        @Test
        void testUpdateUserHeader_PartialUpdate_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updatepartial");

            String mutation = buildUpdateUserHeaderMutation("Just updated bio", null);

            performAndAssertUserHeaderUpdate(auth.jwt(), TestUtils.createValidRegisterPayload("updatepartial"), "Just updated bio", null);
        }

        @Test
        void testUpdateUserHeader_EmptyInput_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("emptyinput");

            String mutation = buildUpdateUserHeaderMutation(null, null);

            performAndAssertUserHeaderUpdate(auth.jwt(), TestUtils.createValidRegisterPayload("emptyinput"), null, null);
        }

        @Test
        void testUpdateUserHeader_Unauthorized_ShouldReturnError() throws Exception {
            String mutation = buildUpdateUserHeaderMutation("Attempted update", null);
            performAndAssertUnauthorized(mutation);
        }


        @Test
        void testUpdateUserHeader_MissingInput_ShouldReturnError() throws Exception {
            String graphqlMutation = """
                {
                  "query": "mutation { updateUserHeader { username } }"
                }
                """;

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(graphqlMutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message").value(
                            org.hamcrest.Matchers.containsString("Missing field argument 'input'")
                    ))
                    .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
        }

        @Test
        void testUpdateUserHeader_IdempotentRepeatedUpdate_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("idempotent");

            String mutation = buildUpdateUserHeaderMutation("Consistent bio", "https://example.com/consistent.jpg");

            performAndAssertUserHeaderUpdate(auth.jwt(), TestUtils.createValidRegisterPayload("idempotent"), "Consistent bio", "https://example.com/consistent.jpg");
            performAndAssertUserHeaderUpdate(auth.jwt(), TestUtils.createValidRegisterPayload("idempotent"), "Consistent bio", "https://example.com/consistent.jpg");
        }

    }

    @Nested
    class UpdateBadgeTests {

        @Test
        void testUpdateBadge_Name_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("badgeupdate");

            // Create and persist a label
            var label = testDataHelper.createAndPersistLabel(auth.user(), "Test Label");

            // Create and persist a badge
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Initial Badge");

            // Build the mutation for badge name change (no label update)
            String mutation = buildUpdateBadgeMutation(badge.getId(), "Updated Badge Name");

            // Perform the mutation and assert the badge name change
            performAndAssertBadgeUpdate(mutation, auth.jwt(), badge.getId(), "Updated Badge Name");
        }

        @Test
        void testUpdateBadge_Unauthorized_ShouldReturnError() throws Exception {
            String mutation = buildUpdateBadgeMutation(1L, "Hacked");
            performAndAssertUnauthorized(mutation);
        }

        @Test
        void testUpdateBadge_NonexistentBadge_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("nonexistentbadge");

            String mutation = buildUpdateBadgeMutation(9999L, "Should Fail");

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("BADGE_NOT_FOUND"));
        }
    }

    @Nested
    class DeleteBadgeTests {

        @Test
        void testDeleteBadge_Success() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deletesuccess");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Badge to Delete");

            String mutation = buildDeleteBadgeMutation(badge.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleteBadge").value(true));
        }

        @Test
        void testDeleteBadge_Nonexistent_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deletenonexistent");

            String mutation = buildDeleteBadgeMutation(9999L);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("BADGE_NOT_FOUND"));
        }

        @Test
        void testDeleteBadge_Unauthorized_ShouldReturnError() throws Exception {
            String mutation = buildDeleteBadgeMutation(1L); // Any dummy ID
            performAndAssertUnauthorized(mutation);
        }

        @Test
        void testUpdateBadge_EmptyInput_ShouldReturnUnchanged() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("badgeempty");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Initial Badge");

            String mutation = buildUpdateBadgeMutation(badge.getId(), null);

            performAndAssertBadgeUpdate(mutation, auth.jwt(), badge.getId(), "Initial Badge");
        }

        @Test
        void testDeleteBadge_OtherUsersBadge_ShouldReturnForbidden() throws Exception {
            var owner = testDataHelper.registerAndLoginUserWithUser("badgeowner");
            var other = testDataHelper.registerAndLoginUserWithUser("badgeattacker");
            var badge = testDataHelper.createAndPersistBadge(owner.user(), "Protected Badge");

            String mutation = buildDeleteBadgeMutation(badge.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + other.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_BADGE_ACCESS"));
        }

    }

    @Nested
    class ReorderBadgeTests {

        @Test
        void testReorderBadges_Success() throws Exception {
            // Register and login user
            var auth = testDataHelper.registerAndLoginUserWithUser("reordersuccess");

            var badge1 = testDataHelper.createAndPersistBadge(auth.user(), "Badge 1");
            var badge2 = testDataHelper.createAndPersistBadge(auth.user(), "Badge 2");

            List<Long> badgeIds = Arrays.asList(badge1.getId(), badge2.getId());

            // Reverse the order of badge IDs
            List<Long> reversedBadgeIds = new ArrayList<>(badgeIds);
            Collections.reverse(reversedBadgeIds);

            // Create the mutation with the reversed badge IDs
            String mutation = buildReorderBadgesMutation(reversedBadgeIds.toArray(new Long[0]));

            // Perform the mutation and assert the success
            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reorderBadges").value(true));
        }

        @Test
        void testReorderBadges_NonexistentBadge_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("reordernonexistent");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Existing Badge");

            String mutation = buildReorderBadgesMutation(badge.getId(), 9999L);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("BADGE_NOT_FOUND"));
        }

        @Test
        void testReorderBadges_UnauthorizedBadge_ShouldReturnError() throws Exception {
            var user1 = testDataHelper.registerAndLoginUserWithUser("owner");
            var user2 = testDataHelper.registerAndLoginUserWithUser("attacker");
            var badge = testDataHelper.createAndPersistBadge(user1.user(), "Protected Badge");

            String mutation = buildReorderBadgesMutation(badge.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + user2.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_BADGE_ACCESS"));
        }

        @Test
        void testReorderBadges_Unauthenticated_ShouldReturnError() throws Exception {
            String mutation = buildReorderBadgesMutation(1L, 2L);
            performAndAssertUnauthorized(mutation);
        }
    }

    @Nested
    class ReorderBadgeLabelTests {

        @Test
        void testReorderBadgeLabels_Success() throws Exception {
            // Register and login user
            var auth = testDataHelper.registerAndLoginUserWithUser("reorderlabels");

            // Create labels using TestDataHelper
            var label1 = testDataHelper.createAndPersistLabel(auth.user(), "Label 1");
            var label2 = testDataHelper.createAndPersistLabel(auth.user(), "Label 2");

            // Create badge and add labels using TestDataHelper
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Badge with labels");

            // Add labels to the badge and set the label order
            badge.addLabelIds(Set.of(label1.getId(), label2.getId()));
            badge.setLabelOrder(List.of(label1.getId(), label2.getId()));

            testDataHelper.saveAndFlush(badge);

            // New order: swap labels
            String mutation = buildReorderBadgeLabelsMutation(badge.getId(), List.of(label2.getId(), label1.getId()));

            // Perform the mutation and assert the success
            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reorderBadgeLabels").value(true));
        }

        @Test
        void testReorderBadgeLabels_InvalidLabelId_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("invalidlabel");

            // Create and persist label and badge
            var label = testDataHelper.createAndPersistLabel(auth.user(), "Label");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Badge");

            // Add label to badge and set label order
            badge.addLabelIds(Set.of(label.getId()));
            badge.setLabelOrder(List.of(label.getId()));

            // Save badge to the database
            testDataHelper.saveAndFlush(badge);

            // Include a label ID that doesn't belong to this badge
            String mutation = buildReorderBadgeLabelsMutation(badge.getId(), List.of(label.getId(), 9999L));

            // Perform the GraphQL request and check the response
            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INCOMPLETE_BADGE_LABEL_REORDER_LIST"));
        }

        @Test
        void testReorderBadgeLabels_MissingLabel_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("missinglabel");

            // Create and persist label and badge
            var label1 = testDataHelper.createAndPersistLabel(auth.user(), "Label 1");
            var label2 = testDataHelper.createAndPersistLabel(auth.user(), "Label 2");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Badge");

            // Add labels to badge and set label order
            badge.addLabelIds(Set.of(label1.getId(), label2.getId()));
            badge.setLabelOrder(List.of(label1.getId(), label2.getId()));

            // Save badge to the database
            testDataHelper.saveAndFlush(badge);

            // Provide only one of the two labels in the reorder list
            String mutation = buildReorderBadgeLabelsMutation(badge.getId(), List.of(label1.getId()));

            // Perform the GraphQL request and check the response
            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INCOMPLETE_BADGE_LABEL_REORDER_LIST"));
        }

        @Test
        void testReorderBadgeLabels_Unauthorized_ShouldReturnError() throws Exception {
            var user1 = testDataHelper.registerAndLoginUserWithUser("ownerlabel");
            var user2 = testDataHelper.registerAndLoginUserWithUser("attackerlabel");

            // Create and persist label and badge
            var label = testDataHelper.createAndPersistLabel(user1.user(), "Protected Label");
            var badge = testDataHelper.createAndPersistBadge(user1.user(), "Protected Badge");

            // Add label to badge and set label order
            badge.addLabelIds(Set.of(label.getId()));
            badge.setLabelOrder(List.of(label.getId()));

            // Save badge to the database
            testDataHelper.saveAndFlush(badge);

            // Build mutation to reorder badge labels
            String mutation = buildReorderBadgeLabelsMutation(badge.getId(), List.of(label.getId()));

            // Perform the GraphQL request and check the response for unauthorized access
            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + user2.jwt()) // Attacker user
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_BADGE_ACCESS"));
        }

        @Test
        void testReorderBadgeLabels_Unauthenticated_ShouldReturnError() throws Exception {
            String mutation = buildReorderBadgeLabelsMutation(1L, List.of(1L));
            performAndAssertUnauthorized(mutation);
        }
    }

    @Nested
    class WeekViewTests {

        @Test
        void testWeekView_ValidAnchorDate_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("weekviewvalid");
            String query = buildWeekViewQuery(auth.user().getUsername(), "2025-06-23");
            performAndAssertWeekViewSuccess(auth.jwt(), query);
        }

        @Test
        void testWeekView_TodayAsAnchor_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("weekviewtoday");
            String fixedDate = "2025-06-29"; // Use fixed date instead of dynamic "today"
            String query = buildWeekViewQuery(auth.user().getUsername(), fixedDate);
            performAndAssertWeekViewSuccess(auth.jwt(), query);
        }

        @Test
        void testWeekView_Unauthenticated_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("weekviewunauth");
            String query = buildWeekViewQuery(auth.user().getUsername(), "2025-06-23");
            performAndAssertUnauthorized(query);
        }

        @Test
        void testWeekView_InvalidAnchorFormat_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("weekviewinvalid");
            String query = buildWeekViewQuery(auth.user().getUsername(), "invalid-date");

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message")
                            .value(org.hamcrest.Matchers.containsString(
                                    "Variable 'anchorDate' has an invalid value: Invalid ISO-8601 Date value: invalid-date")))
                    .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError"));
        }
    }

    @Nested
    class UpdateEventTests {

        @Test
        void testUpdateEvent_AllFields_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventall");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            // ✅ Use past times relative to your fixed clock (2025-06-27T12:00:00Z)
            Map<String, Object> input = buildUpdateEventInputFields(
                    "Updated Event",
                    "Updated description",
                    true, // isCompleted = true (allowed since event is in the past)
                    ZonedDateTime.parse("2025-06-25T10:00:00Z"),
                    ZonedDateTime.parse("2025-06-25T11:00:00Z"),
                    null
            );

            String mutation = buildUpdateEventMutation(event.getId(), input);

            Map<String, Object> expected = Map.of(
                    "name", "Updated Event",
                    "description", "Updated description",
                    "isCompleted", true
            );

            performAndAssertEventUpdate(auth.jwt(), mutation, event.getId(), expected);
        }

        @Test
        void testUpdateEvent_PartialUpdate_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventpartial");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = buildUpdateEventInputFields(
                    "Partial Update Event", // name
                    null,                   // description
                    null,                   // isCompleted
                    null,                   // startTime
                    null,                   // endTime
                    null                    // labelId
            );

            String mutation = buildUpdateEventMutation(event.getId(), input);

            Map<String, Object> expected = Map.of(
                    "name", "Partial Update Event"
            );

            performAndAssertEventUpdate(auth.jwt(), mutation, event.getId(), expected);
        }

        @Test
        void testUpdateEvent_ClearDescription_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventclear");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("description", wrapField(null));  // allows null safely

            String mutation = buildUpdateEventMutation(event.getId(), input);

            Map<String, Object> expected = new LinkedHashMap<>();
            expected.put("description", null);

            performAndAssertEventUpdate(auth.jwt(), mutation, event.getId(), expected);
        }

        @Test
        void testUpdateEvent_Unauthorized_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventunauth");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", wrapField("Hacked Event"));

            String mutation = buildUpdateEventMutation(event.getId(), input);

            performAndAssertUnauthorized(mutation);
        }

        @Test
        void testUpdateEvent_Nonexistent_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventmissing");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", wrapField("Should Fail"));

            String mutation = buildUpdateEventMutation(9999L, input);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("EVENT_NOT_FOUND"));
        }

        @Test
        void testUpdateEvent_InvalidTimeFormat_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventbadtime");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("startTime", wrapField("not-a-time"));

            String mutation = buildUpdateEventMutation(event.getId(), input);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message").value(org.hamcrest.Matchers.containsString("Invalid ISO-8601 DateTime value")))
                    .andExpect(jsonPath("$.errors[0].extensions.classification").value("ValidationError")); // likely "ValidationError" for scalar coercion errors
        }

        @Test
        void testUpdateEvent_EndBeforeStart_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventendbeforestart");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("startTime", wrapField("2025-07-01T12:00:00Z"));
            input.put("endTime", wrapField("2025-07-01T11:00:00Z"));

            String mutation = buildUpdateEventMutation(event.getId(), input);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INVALID_EVENT_TIME"));
        }

        @Test
        void testUpdateEvent_ClearLabel_ShouldAssignUnlabeled() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updateeventclearlabel");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Initial Event");

            Map<String, Object> input = new LinkedHashMap<>();
            input.put("labelId", wrapField(null));  // Clear label

            String mutation = buildUpdateEventMutation(event.getId(), input);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updateEvent.label.id").value(auth.user().getUnlabeled().getId().toString()));
        }
    }

    @Nested
    class DeleteEventTests {

        @Test
        void testDeleteEvent_Success() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleteeventsuccess");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Event to Delete");

            String mutation = buildDeleteEventMutation(event.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleteEvent").value(true));
        }

        @Test
        void testDeleteEvent_Nonexistent_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleteeventmissing");

            String mutation = buildDeleteEventMutation(999999L);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("EVENT_NOT_FOUND"));
        }

        @Test
        void testDeleteEvent_Unauthorized_ShouldReturnError() throws Exception {
            var authOwner = testDataHelper.registerAndLoginUserWithUser("deleteeventowner");
            var authOther = testDataHelper.registerAndLoginUserWithUser("deleteeventother");
            var event = testDataHelper.createAndPersistScheduledEvent(authOwner.user(), "Protected Event");

            String mutation = buildDeleteEventMutation(event.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + authOther.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("USER_OWNERSHIP_VIOLATION"));
        }

        @Test
        void testDeleteEvent_Unauthenticated_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleteeventunauth");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Event No Auth");

            String mutation = buildDeleteEventMutation(event.getId());

            performAndAssertUnauthorized(mutation);
        }
    }

    @Nested
    class AddEventRecapTests {

        @Test
        void testAddEventRecap_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("addrecap");
            var event = testDataHelper.createAndPersistCompletedEvent(auth.user());

            String mutation = buildAddEventRecapMutation(event.getId(), "My Recap", "Great event recap notes.", false, null);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.addEventRecap.id").exists())
                    .andExpect(jsonPath("$.data.addEventRecap.notes").value("Great event recap notes."));
        }

        @Test
        void testAddEventRecap_Unauthorized_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("unauthrecap");
            var event = testDataHelper.createAndPersistCompletedEvent(auth.user());

            String mutation = buildAddEventRecapMutation(event.getId(), "Unauthorized Recap", "Notes", false, null);

            performAndAssertUnauthorized(mutation);
        }
    }

    @Nested
    class UpdateEventRecapTests {

        @Test
        void testUpdateEventRecap_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updaterecap");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Recap Update", "Initial notes");

            String mutation = buildUpdateEventRecapMutation(event.getId(), "Updated recap notes.", null);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updateEventRecap.notes").value("Updated recap notes."));
        }
    }

    @Nested
    class ConfirmEventRecapTests {

        @Test
        void testConfirmEventRecap_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("confirmrecap");
            var event = testDataHelper.createAndPersistCompletedEventWithUnconfirmedRecap(auth.user(), "Event to Confirm Recap");

            String mutation = buildConfirmEventRecapMutation(event.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.confirmEventRecap.id").exists());
        }
    }

    @Nested
    class DeleteEventRecapTests {

        @Test
        void testDeleteEventRecap_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleterecap");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event to Delete Recap", "Some notes");

            String mutation = buildDeleteEventRecapMutation(event.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleteEventRecap").value(true));
        }
    }

    @Nested
    class GetEventRecapTests {

        @Test
        void testGetEventRecap_ShouldReturnRecap() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("getrecap");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event with Recap", "My recap notes");

            String query = buildEventRecapQuery(event.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.eventRecap.notes").value("My recap notes"));
        }
    }

    @Nested
    class AddRecapMediaTests {

        @Test
        void testAddRecapMedia_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("addrecapmedia");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Recap Media", "Initial recap notes");
            var recap = event.getRecap();

            String mutation = buildAddRecapMediaMutation(recap.getId(), "https://example.com/media.mp4", "VIDEO", 120, 1);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.addRecapMedia.id").exists())
                    .andExpect(jsonPath("$.data.addRecapMedia.mediaUrl").value("https://example.com/media.mp4"))
                    .andExpect(jsonPath("$.data.addRecapMedia.mediaType").value("VIDEO"))
                    .andExpect(jsonPath("$.data.addRecapMedia.durationSeconds").value(120))
                    .andExpect(jsonPath("$.data.addRecapMedia.mediaOrder").value(1));
        }

        @Test
        void testAddRecapMedia_Unauthorized_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("unauthaddrecapmedia");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event Unauthorized", "Notes");
            var recap = event.getRecap();

            String mutation = buildAddRecapMediaMutation(recap.getId(), "https://example.com/unauth.mp4", "VIDEO", 60, 2);

            performAndAssertUnauthorized(mutation);
        }

        @Test
        void testAddRecapMedia_NonexistentRecap_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("nonexistrecapmedia");

            String mutation = buildAddRecapMediaMutation(99999L, "https://example.com/missing.mp4", "VIDEO", 45, 3);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("EVENT_RECAP_NOT_FOUND"));
        }
    }

    @Nested
    class UpdateRecapMediaTests {

        @Test
        void testUpdateRecapMedia_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updaterecapmedia_success");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Recap Media Update", "Initial recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            String mutation = buildUpdateRecapMediaMutation(media.getId(), "https://example.com/updated.mp4", "VIDEO", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updateRecapMedia.id").value(media.getId().toString()))
                    .andExpect(jsonPath("$.data.updateRecapMedia.mediaUrl").value("https://example.com/updated.mp4"))
                    .andExpect(jsonPath("$.data.updateRecapMedia.mediaType").value("VIDEO"))
                    .andExpect(jsonPath("$.data.updateRecapMedia.durationSeconds").value(150));
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_Unauthorized() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updaterecapmedia_unauth");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Unauthorized Update", "Recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            // Use a valid media type value to pass schema parsing
            String mutation = buildUpdateRecapMediaMutation(media.getId(), "https://example.com/updated.mp4", "VIDEO", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mutation)) // no Authorization header to trigger unauthorized
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_ACCESS"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(401));
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_NotFound() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("updaterecapmedia_notfound");

            String mutation = buildUpdateRecapMediaMutation(999999L, "https://example.com/updated.mp4", "VIDEO", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("RECAP_MEDIA_NOT_FOUND"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(404));
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_InvalidMediaType() throws Exception {
            // Use shorter username to pass 30 char validation
            String usernameSuffix = "urmi_" + System.currentTimeMillis();
            var auth = testDataHelper.registerAndLoginUserWithUser(usernameSuffix);

            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Invalid MediaType", "Recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            String mutation = buildUpdateRecapMediaMutation(media.getId(), "https://example.com/updated.mp4", "INVALID_TYPE", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message").exists());
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_Forbidden_OtherUsersMedia() throws Exception {
            var ownerAuth = testDataHelper.registerAndLoginUserWithUser("urmi_owner_" + System.currentTimeMillis());
            var otherAuth = testDataHelper.registerAndLoginUserWithUser("urmi_other_" + System.currentTimeMillis());

            var event = testDataHelper.createAndPersistCompletedEventWithRecap(ownerAuth.user(), "Owner's event", "Recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            String mutation = buildUpdateRecapMediaMutation(media.getId(), "https://example.com/updated.mp4", "VIDEO", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + otherAuth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("USER_OWNERSHIP_VIOLATION"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(403));
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_InvalidUrl() throws Exception {
            String usernameSuffix = "urmiinvurl" + System.currentTimeMillis(); // shorter prefix
            var auth = testDataHelper.registerAndLoginUserWithUser(usernameSuffix);

            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event", "Notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            String mutation = buildUpdateRecapMediaMutation(media.getId(), "invalid-url", "VIDEO", 150);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INVALID_URL"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(400));
        }

        @Test
        void testUpdateRecapMedia_ShouldFail_NegativeDuration() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("urmi_negdur_" + System.currentTimeMillis());
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event", "Notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/original.mp4", VIDEO, 100, 1);

            String mutation = buildUpdateRecapMediaMutation(media.getId(), "https://example.com/updated.mp4", "VIDEO", -10);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INVALID_DURATION"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(400));
        }
    }

    @Nested
    class DeleteRecapMediaTests {

        @Test
        void testDeleteRecapMedia_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleterecapmedia_success");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for Delete Media", "Recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/media.mp4", VIDEO, 120, 1);

            String mutation = buildDeleteRecapMediaMutation(media.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleteRecapMedia").value(true));
        }

        @Test
        void testDeleteRecapMedia_ShouldFail_Unauthorized() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleterecapmedia_unauth");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event Unauthorized Delete", "Notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/media.mp4", VIDEO, 100, 1);

            String mutation = buildDeleteRecapMediaMutation(media.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mutation)) // No Authorization header
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("UNAUTHORIZED_ACCESS"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(401));
        }

        @Test
        void testDeleteRecapMedia_ShouldFail_NotFound() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("deleterecapmedia_notfound");

            String mutation = buildDeleteRecapMediaMutation(999999L); // Non-existent media ID

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("RECAP_MEDIA_NOT_FOUND"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(404));
        }

        @Test
        void testDeleteRecapMedia_ShouldFail_Forbidden_OtherUsersMedia() throws Exception {
            // Short prefixes to avoid username > 30 char validation failure
            String ownerSuffix = "drmo_" + System.currentTimeMillis();
            String otherSuffix = "drmu_" + System.currentTimeMillis();

            var ownerAuth = testDataHelper.registerAndLoginUserWithUser(ownerSuffix);
            var otherAuth = testDataHelper.registerAndLoginUserWithUser(otherSuffix);

            var event = testDataHelper.createAndPersistCompletedEventWithRecap(ownerAuth.user(), "Owner's Event", "Recap notes");
            var recap = event.getRecap();
            var media = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/media.mp4", VIDEO, 100, 1);

            String mutation = buildDeleteRecapMediaMutation(media.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + otherAuth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("USER_OWNERSHIP_VIOLATION"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(403));
        }
    }

    @Nested
    class ReorderRecapMediaTests {

        @Test
        void testReorderRecapMedia_ShouldSucceed() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("rrms_" + System.currentTimeMillis());
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for reorder", "Recap notes");
            var recap = event.getRecap();

            // Create multiple media entries
            var media1 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/1.mp4", VIDEO, 60, 1);
            var media2 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/2.mp4", VIDEO, 70, 2);

            // Reverse their order in mutation
            String mutation = buildReorderRecapMediaMutation(recap.getId(), List.of(media2.getId(), media1.getId()));

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reorderRecapMedia").value(true));
        }

        @Test
        void testReorderRecapMedia_Unauthorized_ShouldReturnError() throws Exception {
            String usernameSuffix = "rrmu_" + System.currentTimeMillis();
            var auth = testDataHelper.registerAndLoginUserWithUser(usernameSuffix);
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for unauthorized reorder", "Recap notes");
            var recap = event.getRecap();

            var media1 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/1.mp4", VIDEO, 60, 1);

            String mutation = buildReorderRecapMediaMutation(recap.getId(), List.of(media1.getId()));

            performAndAssertUnauthorized(mutation);
        }

        @Test
        void testReorderRecapMedia_NonexistentRecap_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("rrmnr_" + System.currentTimeMillis());

            String mutation = buildReorderRecapMediaMutation(999999L, List.of(1L));

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("EVENT_RECAP_NOT_FOUND"));
        }

        @Test
        void testReorderRecapMedia_IncompleteList_ShouldReturnError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("rrmil_" + System.currentTimeMillis());
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Event for incomplete reorder", "Recap notes");
            var recap = event.getRecap();

            var media1 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/1.mp4", VIDEO, 60, 1);
            var media2 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/2.mp4", VIDEO, 70, 2);

            // Omit one media ID
            String mutation = buildReorderRecapMediaMutation(recap.getId(), List.of(media1.getId()));

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("INCOMPLETE_RECAP_MEDIA_REORDER_LIST"));
        }

        @Test
        void testReorderRecapMedia_ShouldFail_Forbidden_OtherUsersRecap() throws Exception {
            String ownerSuffix = "rrm_owner_" + System.currentTimeMillis();
            String otherSuffix = "rrm_other_" + System.currentTimeMillis();

            var ownerAuth = testDataHelper.registerAndLoginUserWithUser(ownerSuffix);
            var otherAuth = testDataHelper.registerAndLoginUserWithUser(otherSuffix);

            var event = testDataHelper.createAndPersistCompletedEventWithRecap(ownerAuth.user(), "Owner's Event", "Recap notes");
            var recap = event.getRecap();
            var media1 = testDataHelper.createAndPersistRecapMedia(recap, "https://example.com/1.mp4", VIDEO, 60, 1);

            String mutation = buildReorderRecapMediaMutation(recap.getId(), List.of(media1.getId()));

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + otherAuth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors[0].extensions.errorCode").value("USER_OWNERSHIP_VIOLATION"))
                    .andExpect(jsonPath("$.errors[0].extensions.status").value(403));
        }
    }

    @Nested
    class ApplicationConstantsIntegrationTests {

        @Test
        void testSuccessfulBooleanOperations_ShouldReturnApplicationConstant() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("constants_test");
            var badge = testDataHelper.createAndPersistBadge(auth.user(), "Test Badge");

            String mutation = buildDeleteBadgeMutation(badge.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deleteBadge").value(true));
        }

        @Test
        void testReorderOperations_ShouldReturnApplicationConstant() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("constants_reorder");
            var badge1 = testDataHelper.createAndPersistBadge(auth.user(), "Badge 1");
            var badge2 = testDataHelper.createAndPersistBadge(auth.user(), "Badge 2");

            String mutation = buildReorderBadgesMutation(badge2.getId(), badge1.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reorderBadges").value(true));
        }
    }

    @Nested
    class EnhancedErrorHandlingTests {

        @Test
        void testInvalidMediaType_WithImprovedErrorHandling() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("error_test");
            var event = testDataHelper.createAndPersistCompletedEventWithRecap(auth.user(), "Test Event", "Notes");
            var recap = event.getRecap();

            // Test with completely invalid media type to trigger enum parsing error
            String mutation = String.format("""
                {
                  "query": "mutation($recapId: ID!, $input: CreateRecapMediaInput!) { addRecapMedia(recapId: $recapId, input: $input) { id mediaType } }",
                  "variables": {
                    "recapId": %d,
                    "input": {
                      "mediaUrl": "https://example.com/test.mp4",
                      "mediaType": "COMPLETELY_INVALID_TYPE",
                      "durationSeconds": 60,
                      "mediaOrder": 1
                    }
                  }
                }
                """, recap.getId());

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message").exists());
        }

        @Test
        void testMalformedDateTimeInput_ShouldProvideUsefulError() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("datetime_error");
            var event = testDataHelper.createAndPersistScheduledEvent(auth.user(), "Test Event");

            // Test with completely malformed datetime
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("startTime", wrapField("this-is-not-a-datetime"));

            String mutation = buildUpdateEventMutation(event.getId(), input);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0].message").exists());
        }
    }

    @Nested
    class BoundaryConditionTests {

        @Test
        void testAddEventRecap_WithMaximumMediaCount() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("boundary_media");
            var event = testDataHelper.createAndPersistCompletedEvent(auth.user());

            // Create a large list of media items to test boundary conditions
            List<Map<String, Object>> mediaList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Map<String, Object> mediaItem = Map.of(
                        "mediaUrl", "https://example.com/media" + i + ".mp4",
                        "mediaType", "VIDEO",
                        "durationSeconds", 60 + i,
                        "mediaOrder", i + 1
                );
                mediaList.add(mediaItem);
            }

            String mutation = buildAddEventRecapMutation(event.getId(), "Large Media Recap", 
                "Testing with many media items", false, mediaList);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.addEventRecap.id").exists())
                    .andExpect(jsonPath("$.data.addEventRecap.media").isArray());
        }

        @Test
        void testReorderBadges_WithEmptyList() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("empty_reorder");

            // Test with empty list - should handle gracefully
            String mutation = """
                {
                  "query": "mutation($ids: [ID!]!) { reorderBadges(ids: $ids) }",
                  "variables": {
                    "ids": []
                  }
                }
                """;

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reorderBadges").value(true));
        }

        @Test
        void testUpdateUserHeader_WithVeryLongBio() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("long_bio");

            // Test with bio approaching maximum length
            String longBio = "A".repeat(950); // Just under the 1000 char limit
            String mutation = buildUpdateUserHeaderMutation(longBio, null);

            mockMvc.perform(post("/graphql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + auth.jwt())
                            .content(mutation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updateUserHeader.bio").value(longBio));
        }
    }

}
