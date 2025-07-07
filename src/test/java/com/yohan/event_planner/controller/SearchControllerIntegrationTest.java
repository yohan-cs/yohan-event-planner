package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
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

    }

    @Nested
    class SearchRecurringEventsTests {



    }
}
