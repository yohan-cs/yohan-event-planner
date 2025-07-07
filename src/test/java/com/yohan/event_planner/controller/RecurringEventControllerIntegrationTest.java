package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.repository.UserRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_DESCRIPTION;
import static com.yohan.event_planner.util.TestConstants.VALID_WEEKLY_RECURRENCE_RULE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class RecurringEventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private UserRepository userRepository;
    @Autowired private RecurringEventRepository recurringEventRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        var auth = testDataHelper.registerAndLoginUserWithUser("recurringeventcontroller");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class CreateRecurringEventTests {

        @Test
        void testCreateRecurringEvent_ShouldCreateAndReturnRecurringEvent() throws Exception {

            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    "New Recurring Event",
                    LocalTime.of(9, 0),          // Start Time: 9:00 AM
                    LocalTime.of(10, 0),         // End Time: 10:00 AM
                    LocalDate.of(2025, 7, 3),    // Start Date: July 3, 2025 (hardcoded)
                    LocalDate.of(2025, 8, 3),    // End Date: August 3, 2025 (hardcoded)
                    "Test description",          // Description
                    null,                        // Label (no label provided)
                    VALID_WEEKLY_RECURRENCE_RULE, // Example recurrence rule
                    null,                        // Any other parameters (e.g., time zone) can be null
                    false                        // isDraft set to false for a confirmed scheduled event
            );

            mockMvc.perform(post("/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Recurring Event"))
                    .andExpect(jsonPath("$.description").value("Test description"))
                    .andExpect(jsonPath("$.unconfirmed").value(false));
        }

        @Test
        void testCreateRecurringEvent_Unauthenticated_ShouldReturn401() throws Exception {
            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    "New Recurring Event",
                    LocalTime.of(9, 0),          // Start Time: 9:00 AM
                    LocalTime.of(10, 0),         // End Time: 10:00 AM
                    LocalDate.of(2025, 7, 3),    // Start Date: July 3, 2025
                    LocalDate.of(2025, 8, 3),    // End Date: August 3, 2025
                    "Test description",          // Description
                    null,                        // Label (no label provided)
                    VALID_WEEKLY_RECURRENCE_RULE, // Example recurrence rule
                    null,                        // Any other parameters (e.g., time zone) can be null
                    false                        // isDraft set to false for a confirmed scheduled event
            );

            mockMvc.perform(post("/recurringevents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    class GetRecurringEventTests {

        @Test
        void testGetRecurringEvent_ShouldReturnRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Existing Recurring Event");

            mockMvc.perform(get("/recurringevents/{id}", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(recurringEvent.getId()))
                    .andExpect(jsonPath("$.name").value("Existing Recurring Event"));
        }
    }

    @Nested
    class UpdateRecurringEventTests {

        @Test
        void testUpdateRecurringEvent_ShouldUpdateRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Old Recurring Event");

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of("Updated Recurring Event"),  // Updated name
                    null,                                          // No change to startTime
                    null,                          // No change to endTime
                    null,                          // No change to startDate
                    null,                         // No change to endDate
                    Optional.of("Updated description"),    // Updated description
                    null,                         // No change to labelId
                    null,                         // No change to recurrenceRule
                    null                           // No change to unconfirmed status
            );

            mockMvc.perform(patch("/recurringevents/{id}", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))  // Send update request
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Recurring Event"))
                    .andExpect(jsonPath("$.description").value("Updated description"));
        }

        @Test
        void testDeleteRecurringEvent_ShouldReturn404IfEventNotFound() throws Exception {
            // Arrange
            Long nonExistentEventId = 999L; // Non-existent event ID

            // Act & Assert
            mockMvc.perform(delete("/recurringevents/{id}", nonExistentEventId)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound()); // Expecting 404 Not Found
        }

        @Test
        void testUpdateRecurringEvent_ShouldReturnOkIfNoChangesProvided() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Old Recurring Event");

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,  // No change to name
                    null,               // No change to startTime
                    null,               // No change to endTime
                    null,               // No change to startDate
                    null,               // No change to endDate
                    null,   // No change to description
                    null,               // No change to labelId
                    null,               // No change to recurrenceRule
                    null                // No change to unconfirmed status
            );

            mockMvc.perform(patch("/recurringevents/{id}", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk()) // Expecting 200 OK as no changes are applied, but the request is valid
                    .andExpect(jsonPath("$.name").value("Old Recurring Event")) // Ensure the event name remains unchanged
                    .andExpect(jsonPath("$.description").value(VALID_EVENT_DESCRIPTION)); // Ensure the description remains unchanged
        }

    }

    @Nested
    class DeleteRecurringEventTests {

        @Test
        void testDeleteRecurringEvent_ShouldDeleteRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Delete Recurring Event");

            mockMvc.perform(delete("/recurringevents/{id}", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            assertThat(recurringEventRepository.findById(recurringEvent.getId())).isEmpty();
        }
    }

    @Nested
    class ConfirmRecurringEventsTests {

        @Test
        void testConfirmRecurringEvent_ShouldReturn404IfEventNotFound() throws Exception {
            // Arrange
            Long nonExistentEventId = 999L; // Non-existent event ID

            // Act & Assert
            mockMvc.perform(post("/recurringevents/{id}/confirm", nonExistentEventId)
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()); // Expecting 404 Not Found
        }


        @Test
        void testConfirmRecurringEvent_ShouldReturn401IfNotAuthenticated() throws Exception {
            // Arrange
            Long eventId = 1L; // Existing event ID, but no authentication token

            // Act & Assert
            mockMvc.perform(post("/recurringevents/{id}/confirm", eventId)
                            .contentType(MediaType.APPLICATION_JSON)) // No Authorization header
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized
        }

        @Test
        void testConfirmRecurringEvent_ShouldReturn403IfNotAuthorized() throws Exception {
            // Arrange
            Long eventId = 1L; // Existing event ID
            String invalidJwt = "InvalidJwtToken"; // An invalid or expired JWT token

            // Act & Assert
            mockMvc.perform(post("/recurringevents/{id}/confirm", eventId)
                            .header("Authorization", "Bearer " + invalidJwt) // Invalid token
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized due to invalid/expired token
        }

        @Test
        void testConfirmRecurringEvent_ShouldReturn401IfNoAuthHeader() throws Exception {
            // Arrange
            Long eventId = 1L; // Existing event ID, but no authentication token

            // Act & Assert
            mockMvc.perform(post("/recurringevents/{id}/confirm", eventId)
                            .contentType(MediaType.APPLICATION_JSON)) // No Authorization header
                    .andExpect(status().isUnauthorized()); // Expecting 401 Unauthorized
        }

        @Test
        void testConfirmRecurringEvent_ShouldReturn409IfAlreadyConfirmed() throws Exception {
            // Arrange
            // Use TestDataHelper to create and persist a recurring event
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Recurring Event");

            // Act & Assert
            mockMvc.perform(post("/recurringevents/{id}/confirm", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict()); // Expecting 409 Conflict for already confirmed event
        }


    }

    @Nested
    class SkipDaysManagementTests {

        @Test
        void testAddSkipDays_ShouldAddSkipDaysToRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Recurring Event with Skip Days");

            Set<LocalDate> skipDays = Set.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

            mockMvc.perform(post("/recurringevents/{id}/skipdays", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(skipDays)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skipDays").isArray())
                    .andExpect(jsonPath("$.skipDays.length()").value(2)); // Verify that skip days were added
        }

        @Test
        void testRemoveSkipDays_ShouldRemoveSkipDaysFromRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Recurring Event with Skip Days");

            Set<LocalDate> skipDaysToRemove = Set.of(LocalDate.now().plusDays(1));

            mockMvc.perform(delete("/recurringevents/{id}/skipdays", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(skipDaysToRemove)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skipDays").isArray())
                    .andExpect(jsonPath("$.skipDays.length()").value(0)); // Verify that skip day was removed
        }

        @Test
        void testAddMultipleSkipDays_ShouldAddAllSkipDaysToRecurringEvent() throws Exception {
            // Create recurring event using TestDataHelper
            RecurringEvent recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Recurring Event with Multiple Skip Days");

            Set<LocalDate> skipDays = Set.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LocalDate.now().plusDays(3));

            mockMvc.perform(post("/recurringevents/{id}/skipdays", recurringEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(skipDays)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skipDays").isArray())
                    .andExpect(jsonPath("$.skipDays.length()").value(3)); // Verify that all skip days were added
        }
    }
}
