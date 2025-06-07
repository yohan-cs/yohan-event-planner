package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.util.TestAuthUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_START;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EventFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private EventRepository eventRepository;
    @Autowired private JwtUtils jwtUtils;
    private TestAuthUtils testAuthUtils;

    @BeforeEach
    void setUp() {
        testAuthUtils = new TestAuthUtils(jwtUtils, mockMvc, objectMapper);
    }

    @Test
    void testEventLifecycleFlow() throws Exception {
        // Register + Login user
        String suffix = "event1";
        RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload(suffix);
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create event
        EventCreateDTO createDTO = TestUtils.createTimedEventCreateDTO();

        MvcResult createResult = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(createDTO.name()))
                .andExpect(jsonPath("$.description").value(createDTO.description()))
                .andReturn();

        EventResponseDTO createdEvent = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), EventResponseDTO.class
        );
        Long eventId = createdEvent.id();

        // Get event by ID
        mockMvc.perform(get("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.name").value(createDTO.name()));

        // Get events for current user
        mockMvc.perform(get("/events/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId));

        // Update event (name only)
        EventUpdateDTO updateDTO = new EventUpdateDTO(
                "Updated Event Name", null, null, null
        );

        mockMvc.perform(patch("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Event Name"));

        // Verify update in DB
        Optional<Event> updatedEventOpt = eventRepository.findById(eventId);
        assertThat(updatedEventOpt).isPresent();
        assertThat(updatedEventOpt.get().getName()).isEqualTo("Updated Event Name");

        // Delete event
        mockMvc.perform(delete("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // Verify hard delete from DB
        Optional<Event> deletedEventOpt = eventRepository.findById(eventId);
        assertThat(deletedEventOpt).isEmpty();

        // Verify GET returns 404
        mockMvc.perform(get("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateAndDeleteForbiddenForOtherUser() throws Exception {
        // Register and login user A
        String suffixA = "owner";
        RegisterRequestDTO registerA = TestUtils.createValidRegisterPayload(suffixA);
        String jwtA = testAuthUtils.registerAndLoginUser(suffixA);

        // Create event as user A
        EventCreateDTO eventDTO = TestUtils.createTimedEventCreateDTO();
        MvcResult result = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        Long eventId = objectMapper.readValue(result.getResponse().getContentAsString(), EventResponseDTO.class).id();

        // Register and login user B
        String suffixB = "intruder";
        String jwtB = testAuthUtils.registerAndLoginUser(suffixB);

        // Try to PATCH the event as user B
        EventUpdateDTO updateDTO = new EventUpdateDTO("Hacked!", null, null, null);

        mockMvc.perform(patch("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());

        // Try to DELETE the event as user B
        mockMvc.perform(delete("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUntimedEventLifecycleFlow() throws Exception {
        // Register and login
        String suffix = "untimed1";
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create untimed event
        EventCreateDTO createDTO = TestUtils.createUntimedEventCreateDTO();
        MvcResult createResult = mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(createDTO.name()))
                .andExpect(jsonPath("$.description").value(createDTO.description()))
                .andExpect(jsonPath("$.endTimeUtc").doesNotExist())  // Untimed
                .andReturn();

        EventResponseDTO created = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventResponseDTO.class);
        Long eventId = created.id();

        // GET by ID
        mockMvc.perform(get("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTimeUtc").doesNotExist());

        // PATCH — set end time
        EventUpdateDTO addEndTimeDTO = new EventUpdateDTO(null, null, Optional.of(createDTO.startTime().plusHours(2)), null);
        mockMvc.perform(patch("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addEndTimeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTimeUtc").exists());

        // PATCH — remove end time again
        EventUpdateDTO removeEndTimeDTO = new EventUpdateDTO(null, null, Optional.empty(), null);
        mockMvc.perform(patch("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(removeEndTimeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTimeUtc").doesNotExist());

        // DELETE
        mockMvc.perform(delete("/events/" + eventId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // Confirm deletion
        assertThat(eventRepository.findById(eventId)).isEmpty();
    }

    @Test
    void testUntimedEvent_conflictWithExisting_throwsConflict() throws Exception {
        // Register and login
        String suffix = "untimed2";
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create the first untimed event
        EventCreateDTO firstDTO = TestUtils.createUntimedEventCreateDTO();
        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstDTO)))
                .andExpect(status().isCreated());

        // Attempt to create a second untimed event at the same start time
        EventCreateDTO conflictingDTO = new EventCreateDTO(
                "Conflicting Untimed Event",
                firstDTO.startTime(),
                null,  // no end time
                "Should trigger conflict"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUntimedEvent_followedByTimedEvent_sameStartTime_allowed() throws Exception {
        // Register and login
        String suffix = "untimed3";
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create the first untimed event
        EventCreateDTO untimedDTO = TestUtils.createUntimedEventCreateDTO();
        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(untimedDTO)))
                .andExpect(status().isCreated());

        // Create a second event at the same start time, but with an end time
        EventCreateDTO timedDTO = new EventCreateDTO(
                "Timed Event",
                untimedDTO.startTime(),
                untimedDTO.startTime().plusHours(2),
                "Should NOT trigger conflict"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timedDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void testTimedEvent_followedByUntimedEvent_sameStartTime_conflict() throws Exception {
        // Register and login
        String suffix = "untimed4";
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create the first timed event
        EventCreateDTO timedDTO = new EventCreateDTO(
                "Timed Event",
                VALID_EVENT_START,
                VALID_EVENT_START.plusHours(2),
                "Timed description"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timedDTO)))
                .andExpect(status().isCreated());

        // Try to create an untimed event at the same start time
        EventCreateDTO untimedDTO = new EventCreateDTO(
                "Untimed Event",
                VALID_EVENT_START,
                null,
                "Untimed description"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(untimedDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUntimedAndTimedEvents_differentStartTime_noConflict() throws Exception {
        // Register and login
        String suffix = "untimed4";
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // Create untimed event at 7 AM
        EventCreateDTO untimedDTO = new EventCreateDTO(
                "Untimed",
                VALID_EVENT_START, // 7:00 AM
                null,
                "Untimed desc"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(untimedDTO)))
                .andExpect(status().isCreated());

        // Create timed event at 8 AM–9 AM → no conflict
        EventCreateDTO timedDTO = new EventCreateDTO(
                "Timed",
                VALID_EVENT_START.plusHours(1),       // 8:00 AM
                VALID_EVENT_START.plusHours(2),       // 9:00 AM
                "Timed desc"
        );

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(timedDTO)))
                .andExpect(status().isCreated());
    }

}
