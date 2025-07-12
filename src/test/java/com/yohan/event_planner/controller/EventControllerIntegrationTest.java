package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.repository.EventRepository;
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

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.yohan.event_planner.constants.ApplicationConstants.JWT_BEARER_PREFIX;
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
class EventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private EventRepository eventRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        var auth = testDataHelper.registerAndLoginUserWithUser("eventcontroller");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class CreateEventTests {

        @Test
        void testCreateEvent_ShouldCreateAndReturnEvent() throws Exception {
            ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z");
            EventCreateDTO dto = new EventCreateDTO(
                    "New Event",
                    fixedTime.plusDays(1),
                    fixedTime.plusDays(1).plusHours(1),
                    "Test description",
                    null,
                    false // isDraft set to false for a confirmed scheduled event
            );

            mockMvc.perform(post("/events")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Event"))
                    .andExpect(jsonPath("$.description").value("Test description"))
                    .andExpect(jsonPath("$.unconfirmed").value(false)); // Optionally assert unconfirmed = false
        }
    }

    @Nested
    class GetEventTests {

        @Test
        void testGetEvent_ShouldReturnEvent() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Existing Event");

            mockMvc.perform(get("/events/{id}", event.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(event.getId()))
                    .andExpect(jsonPath("$.name").value("Existing Event"));
        }
    }

    @Nested
    class UpdateEventTests {

        @Test
        void testUpdateEvent_ShouldUpdateEvent() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Old Event");

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Event"),
                    null,
                    null,
                    Optional.of("Updated description"),
                    null,
                    null
            );

            mockMvc.perform(patch("/events/{id}", event.getId())
                            .header("Authorization", JWT_BEARER_PREFIX + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Event"))
                    .andExpect(jsonPath("$.description").value("Updated description"));
        }
    }

    @Nested
    class DeleteEventTests {

        @Test
        void testDeleteEvent_ShouldDeleteEvent() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Delete Event");

            mockMvc.perform(delete("/events/{id}", event.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            assertThat(eventRepository.findById(event.getId())).isEmpty();
        }
    }

    @Nested
    class ConfirmEventTests {

        @Test
        void testConfirmEvent_ShouldConfirmDraftEvent() throws Exception {
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);

            mockMvc.perform(post("/events/{id}/confirm", draftEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unconfirmed").value(false));
        }
    }

    @Nested
    class SecurityTests {

        @Test
        void testGetEvent_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Test Event");

            mockMvc.perform(get("/events/{id}", event.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateEvent_ShouldReturn401_WhenNoJwtToken() throws Exception {
            ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z");
            EventCreateDTO dto = new EventCreateDTO(
                    "New Event",
                    fixedTime.plusDays(1),
                    fixedTime.plusDays(1).plusHours(1),
                    "Test description",
                    null,
                    false
            );

            mockMvc.perform(post("/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testUpdateEvent_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Test Event");
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Event"),
                    null,
                    null,
                    Optional.of("Updated description"),
                    null,
                    null
            );

            mockMvc.perform(patch("/events/{id}", event.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testDeleteEvent_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Test Event");

            mockMvc.perform(delete("/events/{id}", event.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetEventRecap_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Recap notes");

            mockMvc.perform(get("/events/{id}/recap", event.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testCreateEventRecap_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEvent(user);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(
                    event.getId(),
                    "Recap notes",
                    "Recap name",
                    false,
                    null
            );

            mockMvc.perform(post("/events/{id}/recap", event.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testUpdateEventRecap_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Old notes");
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            mockMvc.perform(patch("/events/{id}/recap", event.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testDeleteEventRecap_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Recap notes");

            mockMvc.perform(delete("/events/{id}/recap", event.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testConfirmEventRecap_ShouldReturn401_WhenNoJwtToken() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithUnconfirmedRecap(user, "Event With Unconfirmed Recap");

            mockMvc.perform(post("/events/{id}/recap/confirm", event.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetEvent_ShouldReturn403_WhenNotOwner() throws Exception {
            // Create another user and their event
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Event otherUserEvent = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Event");

            // Try to access other user's event with current user's token
            mockMvc.perform(get("/events/{id}", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testUpdateEvent_ShouldReturn403_WhenNotOwner() throws Exception {
            // Create another user and their event
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("anotheruserupdate");
            Event otherUserEvent = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Event");

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Hacked Event"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Try to update other user's event
            mockMvc.perform(patch("/events/{id}", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testDeleteEvent_ShouldReturn403_WhenNotOwner() throws Exception {
            // Create another user and their event
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("anotheruserdelete");
            Event otherUserEvent = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Event");

            // Try to delete other user's event
            mockMvc.perform(delete("/events/{id}", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testConfirmEvent_ShouldReturn403_WhenNotOwner() throws Exception {
            // Create another user and their draft event
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("anotheruserconfirm");
            Event otherUserEvent = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Draft Event");
            otherUserEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(otherUserEvent);

            // Try to confirm other user's event
            mockMvc.perform(post("/events/{id}/confirm", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }
    }

    @Nested
    class ValidationTests {

        @Test
        void testCreateEvent_ShouldReturn400_WhenInvalidDTO() throws Exception {
            // Test with null name
            EventCreateDTO dto = new EventCreateDTO(
                    null, // Invalid - name is required
                    ZonedDateTime.now().plusDays(1),
                    ZonedDateTime.now().plusDays(1).plusHours(1),
                    "Test description",
                    null,
                    false
            );

            mockMvc.perform(post("/events")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateEvent_ShouldReturn400_WhenInvalidOptionalFields() throws Exception {
            Event event = testDataHelper.createAndPersistScheduledEvent(user, "Test Event");
            
            // Test with empty name in Optional
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of(""), // Invalid - empty name
                    null,
                    null,
                    null,
                    null,
                    null
            );

            mockMvc.perform(patch("/events/{id}", event.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testCreateEvent_ShouldReturn400_WhenInvalidTimeSequence() throws Exception {
            ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z");
            // Invalid - end time before start time
            EventCreateDTO dto = new EventCreateDTO(
                    "Invalid Time Event",
                    fixedTime.plusDays(1),
                    fixedTime.plusDays(1).minusHours(1), // End before start
                    "Test description",
                    null,
                    false
            );

            mockMvc.perform(post("/events")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testCreateEventRecap_ShouldReturn400_WhenNotesExceedMaxLength() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEvent(user);
            
            // Create notes that exceed the maximum length
            String longNotes = "a".repeat(10001); // Assuming max is 10000
            EventRecapCreateDTO dto = new EventRecapCreateDTO(
                    event.getId(),
                    longNotes,
                    "Valid name",
                    false,
                    null
            );

            mockMvc.perform(post("/events/{id}/recap", event.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void testCreateEvent_ShouldReturn400_WhenMalformedJson() throws Exception {
            String malformedJson = "{\"name\": \"Test\", \"startTime\": \"invalid-date\"}";

            mockMvc.perform(post("/events")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ErrorResponseTests {

        @Test
        void testGetEvent_ShouldReturn404_WhenEventNotFound() throws Exception {
            Long nonExistentId = 999999L;

            mockMvc.perform(get("/events/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testUpdateEvent_ShouldReturn404_WhenEventNotFound() throws Exception {
            Long nonExistentId = 999999L;
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Event"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            mockMvc.perform(patch("/events/{id}", nonExistentId)
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testGetEvent_ShouldReturn400_WhenInvalidId() throws Exception {
            // Test with zero ID
            mockMvc.perform(get("/events/{id}", 0L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetEvent_ShouldReturn400_WhenNegativeId() throws Exception {
            // Test with negative ID
            mockMvc.perform(get("/events/{id}", -1L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testUpdateEvent_ShouldReturn400_WhenInvalidId() throws Exception {
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Event"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            mockMvc.perform(patch("/events/{id}", 0L)
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetEventRecap_ShouldReturn400_WhenInvalidId() throws Exception {
            mockMvc.perform(get("/events/{id}/recap", -1L)
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class BusinessRuleTests {

        @Test
        void testConfirmEvent_ShouldReturn409_WhenAlreadyConfirmed() throws Exception {
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Already Confirmed Event");
            // Event is confirmed by default from helper method

            mockMvc.perform(post("/events/{id}/confirm", confirmedEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testCreateEventRecap_ShouldReturn201_WhenEventNotCompleted() throws Exception {
            // Create a scheduled but not completed event
            Event incompleteEvent = testDataHelper.createAndPersistScheduledEvent(user, "Incomplete Event");
            // Ensure the event is not marked as completed
            incompleteEvent.setCompleted(false);
            testDataHelper.saveAndFlush(incompleteEvent);

            EventRecapCreateDTO dto = new EventRecapCreateDTO(
                    incompleteEvent.getId(),
                    "Recap for incomplete event",
                    "Should create draft recap",
                    false,
                    null
            );

            mockMvc.perform(post("/events/{id}/recap", incompleteEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.eventName").value("Incomplete Event"))
                    .andExpect(jsonPath("$.notes").value("Recap for incomplete event"))
                    .andExpect(jsonPath("$.unconfirmed").value(true)); // Should be draft/unconfirmed for incomplete events
        }

        @Test
        void testCreateEventRecap_ShouldReturn409_WhenRecapAlreadyExists() throws Exception {
            Event eventWithRecap = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Existing recap");

            EventRecapCreateDTO dto = new EventRecapCreateDTO(
                    eventWithRecap.getId(),
                    "Duplicate recap",
                    "Should fail",
                    false,
                    null
            );

            mockMvc.perform(post("/events/{id}/recap", eventWithRecap.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testConfirmEventRecap_ShouldReturn409_WhenAlreadyConfirmed() throws Exception {
            // Create event with confirmed recap
            Event eventWithConfirmedRecap = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Confirmed Recap", "Confirmed recap");
            // Recap should be confirmed by default from helper

            mockMvc.perform(post("/events/{id}/recap/confirm", eventWithConfirmedRecap.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }
    }

    @Nested
    class EventRecapAuthorizationTests {

        @Test
        void testGetEventRecap_ShouldReturn403_WhenNotOwner() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserrecap");
            Event otherUserEvent = testDataHelper.createAndPersistCompletedEventWithRecap(
                    otherUserAuth.user(), "Other User Event", "Other user recap");

            mockMvc.perform(get("/events/{id}/recap", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testUpdateEventRecap_ShouldReturn403_WhenNotOwner() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserupdate");
            Event otherUserEvent = testDataHelper.createAndPersistCompletedEventWithRecap(
                    otherUserAuth.user(), "Other User Event", "Other user recap");

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Hacked recap", null);

            mockMvc.perform(patch("/events/{id}/recap", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testDeleteEventRecap_ShouldReturn403_WhenNotOwner() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserdelete");
            Event otherUserEvent = testDataHelper.createAndPersistCompletedEventWithRecap(
                    otherUserAuth.user(), "Other User Event", "Other user recap");

            mockMvc.perform(delete("/events/{id}/recap", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        void testConfirmEventRecap_ShouldReturn403_WhenNotOwner() throws Exception {
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruserconfirm");
            Event otherUserEvent = testDataHelper.createAndPersistCompletedEventWithUnconfirmedRecap(
                    otherUserAuth.user(), "Other User Event");

            mockMvc.perform(post("/events/{id}/recap/confirm", otherUserEvent.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }
    }

    @Nested
    class EventRecapTests {

        @Test
        void testCreateEventRecap_ShouldCreateRecap() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEvent(user);

            EventRecapCreateDTO dto = new EventRecapCreateDTO(
                    event.getId(),
                    "Recap notes",
                    "Recap name",
                    false,
                    null
            );

            mockMvc.perform(post("/events/{id}/recap", event.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.notes").value("Recap notes"));
        }

        @Test
        void testGetEventRecap_ShouldReturnRecap() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Recap notes");

            mockMvc.perform(get("/events/{id}/recap", event.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Recap notes"));
        }

        @Test
        void testUpdateEventRecap_ShouldUpdateRecap() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Old notes");

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            mockMvc.perform(patch("/events/{id}/recap", event.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("Updated notes"));
        }

        @Test
        void testDeleteEventRecap_ShouldDeleteRecap() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithRecap(user, "Event With Recap", "Recap notes");

            mockMvc.perform(delete("/events/{id}/recap", event.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
        }

        @Test
        void testConfirmEventRecap_ShouldConfirmRecap() throws Exception {
            Event event = testDataHelper.createAndPersistCompletedEventWithUnconfirmedRecap(user, "Event With Unconfirmed Recap");

            mockMvc.perform(post("/events/{id}/recap/confirm", event.getId())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unconfirmed").value(false));
        }
    }
}
