package com.yohan.event_planner.config;

import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.ClockProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public ClockProvider clockProvider(Clock clock) {
        return new ClockProviderImpl(clock);
    }
    
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
