package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.BadgeRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserToolsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private BadgeRepository badgeRepository;
    @Autowired private LabelRepository labelRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login the user
        var auth = testDataHelper.registerAndLoginUserWithUser("usertools");
        this.jwt = auth.jwt();
        this.user = auth.user();

        // Create test badges and labels
        testDataHelper.createAndPersistBadge(user, "Badge 1");
        testDataHelper.createAndPersistBadge(user, "Badge 2");
        testDataHelper.createAndPersistLabel(user, "Label 1");
        testDataHelper.createAndPersistLabel(user, "Label 2");
    }

    // region GetUserToolsTests

    @Nested
    class GetUserToolsTests {

        @Test
        void testGetUserTools_ShouldReturnBadgesAndLabels() throws Exception {
            mockMvc.perform(get("/usertools")
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.badges").isArray())
                    .andExpect(jsonPath("$.badges.length()").value(2))
                    .andExpect(jsonPath("$.badges[0].name").value("Badge 1"))
                    .andExpect(jsonPath("$.badges[1].name").value("Badge 2"))
                    .andExpect(jsonPath("$.labels").isArray())
                    .andExpect(jsonPath("$.labels.length()").value(2))
                    // Check that the label name contains the expected string, ignoring the dynamic timestamp
                    .andExpect(jsonPath("$.labels[0].name").value(containsString("Label 1")))
                    .andExpect(jsonPath("$.labels[1].name").value(containsString("Label 2")));
        }


        @Test
        void testUnauthorizedGetUserTools_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/usertools")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }
    // endregion

}
