package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MyEventsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private EventRepository eventRepository;
    @Autowired private RecurringEventRepository recurringEventRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        var auth = testDataHelper.registerAndLoginUserWithUser("myevents");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class GetMyEventsTests {

        @Test
        void testGetMyEvents_ShouldReturnConfirmedEventsAndRecurringEvents() throws Exception {
            // Arrange
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Confirmed Event");
            RecurringEvent confirmedRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Confirmed Recurring Event");

            // Act + Assert
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events[0].id").value(confirmedEvent.getId()))
                    .andExpect(jsonPath("$.recurringEvents").isArray())
                    .andExpect(jsonPath("$.recurringEvents[0].id").value(confirmedRecurringEvent.getId()));
        }

        @Test
        void testUnauthorizedGetMyEvents_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/myevents"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetMyEvents_ReturnsEmptyListsWhenNoData() throws Exception {
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events").isEmpty())
                    .andExpect(jsonPath("$.recurringEvents").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isEmpty());
        }

        @Test
        void testGetMyEvents_WithPaginationParameters() throws Exception {
            // Arrange
            Event event1 = testDataHelper.createAndPersistScheduledEvent(user, "Event 1");
            Event event2 = testDataHelper.createAndPersistScheduledEvent(user, "Event 2");
            
            ZonedDateTime startCursor = event1.getStartTime().minusHours(1);
            ZonedDateTime endCursor = event1.getEndTime().plusHours(1);
            
            // Act + Assert
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("startTimeCursor", startCursor.toString())
                            .param("endTimeCursor", endCursor.toString())
                            .param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyEvents_WithCustomLimit() throws Exception {
            // Arrange - create multiple events
            for (int i = 0; i < 5; i++) {
                testDataHelper.createAndPersistScheduledEvent(user, "Event " + i);
            }
            
            // Act + Assert with limit of 2
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyEvents_WithInvalidLimitParameters() throws Exception {
            // Test negative limit - should return 400 Bad Request due to service validation
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithZeroLimit() throws Exception {
            // Test zero limit - should return 400 Bad Request due to service validation
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithExtremelyLargeLimit() throws Exception {
            // Test very large limit - should be handled gracefully
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "10000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyDrafts_ReturnsEmptyListsWhenNoDrafts() throws Exception {
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.eventDrafts").isEmpty())
                    .andExpect(jsonPath("$.recurringEventDrafts").isArray())
                    .andExpect(jsonPath("$.recurringEventDrafts").isEmpty());
        }

        @Test
        void testGetMyEvents_ResponseStructureValidation() throws Exception {
            // Arrange
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Test Event");
            
            // Act + Assert - Validate complete response structure
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events[0].id").value(confirmedEvent.getId()))
                    .andExpect(jsonPath("$.events[0].name").value("Test Event"))
                    .andExpect(jsonPath("$.events[0].startTimeUtc").exists())
                    .andExpect(jsonPath("$.events[0].endTimeUtc").exists())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyEvents_WithMalformedDateCursors() throws Exception {
            // Test invalid date format for startTimeCursor
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("startTimeCursor", "invalid-date-format"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithMalformedEndTimeCursor() throws Exception {
            // Test invalid date format for endTimeCursor
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("endTimeCursor", "not-a-valid-timestamp"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithLargeDataset() throws Exception {
            // Arrange - Create many events to test pagination performance
            for (int i = 0; i < 50; i++) {
                testDataHelper.createAndPersistScheduledEvent(user, "Event " + i);
            }
            for (int i = 0; i < 25; i++) {
                testDataHelper.createAndPersistRecurringEvent(user, "Recurring Event " + i);
            }
            
            // Act + Assert - Should handle large dataset efficiently
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyEvents_WithMixedConfirmedAndUnconfirmedEvents() throws Exception {
            // Arrange - Create both confirmed and unconfirmed events
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Confirmed Event");
            
            Event unconfirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Unconfirmed Event");
            unconfirmedEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(unconfirmedEvent);
            
            RecurringEvent confirmedRecurring = testDataHelper.createAndPersistRecurringEvent(user, "Confirmed Recurring");
            
            RecurringEvent unconfirmedRecurring = testDataHelper.createAndPersistRecurringEvent(user, "Unconfirmed Recurring");
            unconfirmedRecurring.setUnconfirmed(true);
            testDataHelper.saveAndFlush(unconfirmedRecurring);
            
            // Act + Assert - Should only return confirmed events
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events").value(org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.events[0].id").value(confirmedEvent.getId()))
                    .andExpect(jsonPath("$.recurringEvents").isArray())
                    .andExpect(jsonPath("$.recurringEvents").value(org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.recurringEvents[0].id").value(confirmedRecurring.getId()));
        }

    }

    @Nested
    class GetMyDraftsTests {

        @Test
        void testGetMyDrafts_ShouldReturnEventAndRecurringEventDrafts() throws Exception {
            // Arrange
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);

            RecurringEvent draftRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring Event");
            draftRecurringEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurringEvent);

            // Act + Assert
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.eventDrafts[0].id").value(draftEvent.getId()))
                    .andExpect(jsonPath("$.recurringEventDrafts").isArray())
                    .andExpect(jsonPath("$.recurringEventDrafts[0].id").value(draftRecurringEvent.getId()));
        }

        @Test
        void testUnauthorizedGetMyDrafts_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/myevents/drafts"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UserIsolationTests {

        @Test
        void testGetMyEvents_UserIsolation() throws Exception {
            // Arrange - Create another user with events
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Event");
            
            // Create event for current user
            Event myEvent = testDataHelper.createAndPersistScheduledEvent(user, "My Event");
            
            // Act + Assert - Should only see own events
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events").value(org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.events[0].id").value(myEvent.getId()))
                    .andExpect(jsonPath("$.events[0].name").value("My Event"));
        }

        @Test
        void testGetMyDrafts_UserIsolation() throws Exception {
            // Arrange - Create another user with drafts
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Event otherUserDraft = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Draft");
            otherUserDraft.setUnconfirmed(true);
            testDataHelper.saveAndFlush(otherUserDraft);
            
            // Create draft for current user
            Event myDraft = testDataHelper.createAndPersistScheduledEvent(user, "My Draft");
            myDraft.setUnconfirmed(true);
            testDataHelper.saveAndFlush(myDraft);
            
            // Act + Assert - Should only see own drafts
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.eventDrafts").value(org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.eventDrafts[0].id").value(myDraft.getId()))
                    .andExpect(jsonPath("$.eventDrafts[0].name").value("My Draft"));
        }
    }

    @Nested
    class DeleteMyDraftsTests {

        @Test
        void testDeleteAllDrafts_ShouldDeleteEventAndRecurringEventDrafts() throws Exception {
            // Arrange
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);

            RecurringEvent draftRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring Event");
            draftRecurringEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurringEvent);

            // Act
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // Assert
            List<Event> remainingDraftEvents = eventRepository.findAll().stream()
                    .filter(Event::isUnconfirmed)
                    .toList();
            List<RecurringEvent> remainingDraftRecurringEvents = recurringEventRepository.findAll().stream()
                    .filter(RecurringEvent::isUnconfirmed)
                    .toList();

            assertThat(remainingDraftEvents).isEmpty();
            assertThat(remainingDraftRecurringEvents).isEmpty();
        }

        @Test
        void testUnauthorizedDeleteAllDrafts_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(delete("/myevents/drafts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testDeleteAllDrafts_WhenNoDraftsExist_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
        }

        @Test
        void testDeleteAllDrafts_WithMixedConfirmedAndUnconfirmed() throws Exception {
            // Arrange - Create both confirmed and unconfirmed events
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Confirmed Event");
            
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);
            
            // Act
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
            
            // Assert - Confirmed event should remain, draft should be deleted
            List<Event> remainingEvents = eventRepository.findAll().stream()
                    .filter(e -> e.getCreator().getId().equals(user.getId()))
                    .toList();
            
            assertThat(remainingEvents).hasSize(1);
            assertThat(remainingEvents.get(0).getId()).isEqualTo(confirmedEvent.getId());
            assertThat(remainingEvents.get(0).isUnconfirmed()).isFalse();
        }

        @Test
        void testDeleteAllDrafts_UserIsolation() throws Exception {
            // Arrange - Create drafts for multiple users
            var otherUserAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Event otherUserDraft = testDataHelper.createAndPersistScheduledEvent(otherUserAuth.user(), "Other User Draft");
            otherUserDraft.setUnconfirmed(true);
            testDataHelper.saveAndFlush(otherUserDraft);
            
            Event myDraft = testDataHelper.createAndPersistScheduledEvent(user, "My Draft");
            myDraft.setUnconfirmed(true);
            testDataHelper.saveAndFlush(myDraft);
            
            // Act - Delete drafts for current user only
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
            
            // Assert - Other user's draft should remain
            List<Event> remainingDrafts = eventRepository.findAll().stream()
                    .filter(Event::isUnconfirmed)
                    .toList();
            
            assertThat(remainingDrafts).hasSize(1);
            assertThat(remainingDrafts.get(0).getId()).isEqualTo(otherUserDraft.getId());
        }

        @Test
        void testDeleteAllDrafts_WithOnlyEventDrafts() throws Exception {
            // Arrange - Create only event drafts (no recurring event drafts)
            Event draftEvent1 = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event 1");
            draftEvent1.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent1);
            
            Event draftEvent2 = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event 2");
            draftEvent2.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent2);
            
            // Act
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
            
            // Assert - All event drafts should be deleted
            List<Event> remainingDrafts = eventRepository.findAll().stream()
                    .filter(Event::isUnconfirmed)
                    .toList();
            
            assertThat(remainingDrafts).isEmpty();
        }

        @Test
        void testDeleteAllDrafts_WithOnlyRecurringEventDrafts() throws Exception {
            // Arrange - Create only recurring event drafts (no regular event drafts)
            RecurringEvent draftRecurring1 = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring 1");
            draftRecurring1.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurring1);
            
            RecurringEvent draftRecurring2 = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring 2");
            draftRecurring2.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurring2);
            
            // Act
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
            
            // Assert - All recurring event drafts should be deleted
            List<RecurringEvent> remainingRecurringDrafts = recurringEventRepository.findAll().stream()
                    .filter(RecurringEvent::isUnconfirmed)
                    .toList();
            
            assertThat(remainingRecurringDrafts).isEmpty();
        }
    }

    @Nested
    class ContentTypeAndHeaderTests {

        @Test
        void testGetMyEvents_WithoutAcceptHeader() throws Exception {
            // Test without explicit Accept header - should default to JSON
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyEvents_WithJsonAcceptHeader() throws Exception {
            // Test with explicit JSON Accept header
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .header("Accept", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }

        @Test
        void testGetMyDrafts_WithJsonAcceptHeader() throws Exception {
            // Test drafts endpoint with JSON Accept header
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt)
                            .header("Accept", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.recurringEventDrafts").isArray());
        }

        @Test
        void testDeleteAllDrafts_WithJsonContentType() throws Exception {
            // Test delete with JSON Content-Type (even though no body)
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt)
                            .header("Content-Type", "application/json"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void testGetMyEvents_WithNonNumericLimit() throws Exception {
            // Test non-numeric limit parameter
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", "not-a-number"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithEmptyStringLimit() throws Exception {
            // Test empty string limit parameter
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("limit", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testGetMyEvents_WithNullCursors() throws Exception {
            // Test with explicitly null cursor parameters (should be handled gracefully)
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("startTimeCursor", "")
                            .param("endTimeCursor", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isArray());
        }
    }
}
