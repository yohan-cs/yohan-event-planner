package com.yohan.event_planner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.BadgeRepository;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.repository.RecapMediaRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.time.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

@TestConfiguration
public class TestConfig {

    @Bean
    public MockMvc mockMvc(WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Bean
    public TestAuthUtils testAuthUtils(JwtUtils jwtUtils, MockMvc mockMvc, ObjectMapper objectMapper) {
        return new TestAuthUtils(jwtUtils, mockMvc, objectMapper);
    }

    @Bean
    public TestDataHelper testDataHelper(
            UserRepository userRepository,
            EventRepository eventRepository,
            LabelRepository labelRepository,
            BadgeRepository badgeRepository,
            RecurringEventRepository recurringEventRepository,
            EventRecapRepository eventRecapRepository,
            RecapMediaRepository recapMediaRepository,
            LabelTimeBucketRepository labelTimeBucketRepository,
            TestAuthUtils testAuthUtils,
            Clock clock
    ) {
        return new TestDataHelper(userRepository, eventRepository, labelRepository, badgeRepository,
                recurringEventRepository, eventRecapRepository, recapMediaRepository, labelTimeBucketRepository, testAuthUtils, clock);
    }

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse("2025-06-27T12:00:00Z"), ZoneOffset.UTC);
    }
}
