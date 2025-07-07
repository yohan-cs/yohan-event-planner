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
@Import(TestConfig.class)
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
            EventCreateDTO dto = new EventCreateDTO(
                    "New Event",
                    ZonedDateTime.now().plusDays(1),
                    ZonedDateTime.now().plusDays(1).plusHours(1),
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
