package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecurringEventService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private EventService eventService;
    @Autowired private RecurringEventService recurringEventService;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        var auth = testDataHelper.registerAndLoginUserWithUser("myevents");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class SearchEventsTests {

        @Test
        void testSearchEvents_Success() throws Exception {
            // Arrange
            Event testEvent = testDataHelper.createAndPersistCompletedEvent(user);

            // Act & Assert
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10")
                            .param("sortDescending", "true")
                            .param("includeIncompletePastEvents", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(testEvent.getId()))
                    .andExpect(jsonPath("$.content[0].name").value(testEvent.getName()))
                    .andExpect(jsonPath("$.content[0].creatorUsername").value(user.getUsername()));
        }

        @Test
        void testSearchEvents_WithLabelFilter() throws Exception {
            // Arrange
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            Event testEvent = testDataHelper.createAndPersistCompletedEvent(user);
            testEvent.setLabel(label);
            testDataHelper.saveAndFlush(testEvent);

            // Act & Assert
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", label.getId().toString())
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(testEvent.getId()))
                    .andExpect(jsonPath("$.content[0].label.name").value(label.getName()));
        }

        @Test
        void testSearchEvents_WithPastOnlyFilter() throws Exception {
            // Arrange
            Event pastEvent = testDataHelper.createAndPersistCompletedEvent(user);
            Event futureEvent = testDataHelper.createAndPersistFutureEvent(user);

            // Act & Assert - Only past events should be returned
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(pastEvent.getId()));
        }

        @Test
        void testSearchEvents_WithFutureOnlyFilter() throws Exception {
            // Arrange
            Event pastEvent = testDataHelper.createAndPersistCompletedEvent(user);
            Event futureEvent = testDataHelper.createAndPersistFutureEvent(user);

            // Act & Assert - Only future events should be returned
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "FUTURE_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(futureEvent.getId()));
        }

        @Test
        void testSearchEvents_WithCustomTimeFilter() throws Exception {
            // Arrange
            Event event1 = testDataHelper.createAndPersistCompletedEvent(user);
            Event event2 = testDataHelper.createAndPersistFutureEvent(user);
            
            // Use a time range that includes both events
            String startTime = "2020-01-01T00:00:00Z";
            String endTime = "2030-12-31T23:59:59Z";

            // Act & Assert
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "CUSTOM")
                            .param("start", startTime)
                            .param("end", endTime)
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void testSearchEvents_WithPagination() throws Exception {
            // Arrange - Create 5 events
            for (int i = 0; i < 5; i++) {
                testDataHelper.createAndPersistCompletedEvent(user);
            }

            // Act & Assert - First page with 2 items
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalPages").value(3));

            // Second page
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "1")
                            .param("pageSize", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        void testSearchEvents_WithSorting() throws Exception {
            // Arrange
            Event event1 = testDataHelper.createAndPersistCompletedEvent(user);
            Event event2 = testDataHelper.createAndPersistCompletedEvent(user);

            // Act & Assert - Descending order (default)
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("sortDescending", "true")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));

            // Ascending order
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("sortDescending", "false")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        void testSearchEvents_WithMultipleFilters() throws Exception {
            // Arrange
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            Event eventWithLabel = testDataHelper.createAndPersistCompletedEvent(user);
            eventWithLabel.setLabel(label);
            testDataHelper.saveAndFlush(eventWithLabel);
            
            // Event without the label
            testDataHelper.createAndPersistCompletedEvent(user);

            // Act & Assert
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", label.getId().toString())
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "5")
                            .param("sortDescending", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(eventWithLabel.getId()));
        }

        @Test
        void testSearchEvents_WithIncludeIncompletePastEvents() throws Exception {
            // Arrange
            Event completedEvent = testDataHelper.createAndPersistCompletedEvent(user);

            // Act & Assert - Without incomplete events
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("includeIncompletePastEvents", "false")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));

            // With incomplete events
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("includeIncompletePastEvents", "true")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void testSearchEvents_NoResults() throws Exception {
            // Arrange - Create event with specific label
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            Event event = testDataHelper.createAndPersistCompletedEvent(user);
            event.setLabel(label);
            testDataHelper.saveAndFlush(event);

            // Act & Assert - Search for different label that doesn't exist
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", "99999")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void testSearchEvents_UnauthorizedAccess() throws Exception {
            // Act & Assert - No JWT token
            mockMvc.perform(get("/search/events")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testSearchEvents_InvalidLabelId() throws Exception {
            // Act & Assert - Search with invalid label ID
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", "invalid")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testSearchEvents_BoundaryDateTesting() throws Exception {
            // Arrange - Create events at exact boundary times
            Event pastEvent = testDataHelper.createAndPersistCompletedEvent(user);
            Event futureEvent = testDataHelper.createAndPersistFutureEvent(user);
            
            // Test PAST_ONLY boundary - should include past events but not future
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(pastEvent.getId()));

            // Test FUTURE_ONLY boundary - should include future events but not past
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "FUTURE_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(futureEvent.getId()));
        }

        @Test
        void testSearchEvents_InvalidTimeRanges() throws Exception {
            // Test when startDate > endDate in CUSTOM filter
            String invalidStart = "2025-12-31T23:59:59Z"; // Future date
            String invalidEnd = "2020-01-01T00:00:00Z";   // Past date

            // Act & Assert - Should return bad request for invalid time range
            mockMvc.perform(get("/search/events")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "CUSTOM")
                            .param("start", invalidStart)
                            .param("end", invalidEnd)
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    class SearchRecurringEventsTests {

        @Test
        void testSearchRecurringEvents_Success() throws Exception {
            // Arrange
            RecurringEvent testRecurringEvent = testDataHelper.createAndPersistConfirmedRecurringEvent(user);

            // Act & Assert
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10")
                            .param("sortDescending", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(testRecurringEvent.getId()))
                    .andExpect(jsonPath("$.content[0].name").value(testRecurringEvent.getName()))
                    .andExpect(jsonPath("$.content[0].creatorUsername").value(user.getUsername()));
        }

        @Test
        void testSearchRecurringEvents_WithLabelFilter() throws Exception {
            // Arrange
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            RecurringEvent testRecurringEvent = testDataHelper.createAndPersistConfirmedRecurringEvent(user);
            testRecurringEvent.setLabel(label);
            testDataHelper.saveAndFlush(testRecurringEvent);

            // Act & Assert
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", label.getId().toString())
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(testRecurringEvent.getId()))
                    .andExpect(jsonPath("$.content[0].label.name").value(label.getName()));
        }

        @Test
        void testSearchRecurringEvents_WithPastOnlyFilter() throws Exception {
            // Arrange
            RecurringEvent pastRecurringEvent = testDataHelper.createAndPersistPastRecurringEvent(user);
            RecurringEvent futureRecurringEvent = testDataHelper.createAndPersistFutureRecurringEvent(user);

            // Act & Assert - Only past recurring events should be returned
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(pastRecurringEvent.getId()));
        }

        @Test
        void testSearchRecurringEvents_WithFutureOnlyFilter() throws Exception {
            // Arrange
            RecurringEvent pastRecurringEvent = testDataHelper.createAndPersistPastRecurringEvent(user);
            RecurringEvent futureRecurringEvent = testDataHelper.createAndPersistFutureRecurringEvent(user);

            // Act & Assert - Only future recurring events should be returned
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "FUTURE_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(futureRecurringEvent.getId()));
        }

        @Test
        void testSearchRecurringEvents_WithCustomTimeFilter() throws Exception {
            // Arrange
            RecurringEvent recurringEvent1 = testDataHelper.createAndPersistPastRecurringEvent(user);
            RecurringEvent recurringEvent2 = testDataHelper.createAndPersistFutureRecurringEvent(user);
            
            // Use a time range that includes both recurring events
            String startDate = "2020-01-01";
            String endDate = "2030-12-31";

            // Act & Assert
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "CUSTOM")
                            .param("startDate", startDate)
                            .param("endDate", endDate)
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void testSearchRecurringEvents_WithPagination() throws Exception {
            // Arrange - Create 5 recurring events
            for (int i = 0; i < 5; i++) {
                testDataHelper.createAndPersistConfirmedRecurringEvent(user);
            }

            // Act & Assert - First page with 2 items
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalPages").value(3));

            // Second page
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "1")
                            .param("pageSize", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        void testSearchRecurringEvents_WithSorting() throws Exception {
            // Arrange
            RecurringEvent recurringEvent1 = testDataHelper.createAndPersistConfirmedRecurringEvent(user);
            RecurringEvent recurringEvent2 = testDataHelper.createAndPersistConfirmedRecurringEvent(user);

            // Act & Assert - Descending order (default)
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("sortDescending", "true")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));

            // Ascending order
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "ALL")
                            .param("sortDescending", "false")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        void testSearchRecurringEvents_WithMultipleFilters() throws Exception {
            // Arrange
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            RecurringEvent recurringEventWithLabel = testDataHelper.createAndPersistPastRecurringEvent(user);
            recurringEventWithLabel.setLabel(label);
            testDataHelper.saveAndFlush(recurringEventWithLabel);
            
            // Recurring event without the label
            testDataHelper.createAndPersistPastRecurringEvent(user);

            // Act & Assert
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", label.getId().toString())
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "5")
                            .param("sortDescending", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(recurringEventWithLabel.getId()));
        }

        @Test
        void testSearchRecurringEvents_NoResults() throws Exception {
            // Arrange - Create recurring event with specific label
            var label = testDataHelper.createAndPersistLabel(user, "Work");
            RecurringEvent recurringEvent = testDataHelper.createAndPersistConfirmedRecurringEvent(user);
            recurringEvent.setLabel(label);
            testDataHelper.saveAndFlush(recurringEvent);

            // Act & Assert - Search for different label that doesn't exist
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", "99999")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void testSearchRecurringEvents_UnauthorizedAccess() throws Exception {
            // Act & Assert - No JWT token
            mockMvc.perform(get("/search/recurringevents")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testSearchRecurringEvents_InvalidLabelId() throws Exception {
            // Act & Assert - Search with invalid label ID
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("labelId", "invalid")
                            .param("timeFilter", "ALL")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testSearchRecurringEvents_BoundaryDateTesting() throws Exception {
            // Arrange - Create recurring events at exact boundary times
            RecurringEvent pastRecurringEvent = testDataHelper.createAndPersistPastRecurringEvent(user);
            RecurringEvent futureRecurringEvent = testDataHelper.createAndPersistFutureRecurringEvent(user);
            
            // Test PAST_ONLY boundary - should include past recurring events but not future
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "PAST_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(pastRecurringEvent.getId()));

            // Test FUTURE_ONLY boundary - should include future recurring events but not past
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "FUTURE_ONLY")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(futureRecurringEvent.getId()));
        }

        @Test
        void testSearchRecurringEvents_InvalidTimeRanges() throws Exception {
            // Test when startDate > endDate in CUSTOM filter
            String invalidStartDate = "2025-12-31"; // Future date
            String invalidEndDate = "2020-01-01";   // Past date

            // Act & Assert - Should return bad request for invalid time range
            mockMvc.perform(get("/search/recurringevents")
                            .header("Authorization", "Bearer " + jwt)
                            .param("timeFilter", "CUSTOM")
                            .param("startDate", invalidStartDate)
                            .param("endDate", invalidEndDate)
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isBadRequest());
        }

    }
}
