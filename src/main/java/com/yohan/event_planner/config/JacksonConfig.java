package com.yohan.event_planner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Jackson JSON serialization and deserialization in the Event Planner application.
 *
 * <p>
 * This configuration customizes the default Jackson ObjectMapper to properly handle modern Java types
 * and provide consistent JSON formatting across all REST API endpoints. It is particularly important
 * for an event management system that relies heavily on temporal data and optional fields.
 * </p>
 *
 * <h2>Key Modules Registered</h2>
 * <ul>
 *   <li><strong>JavaTimeModule</strong>: Enables proper serialization of Java 8+ date/time types</li>
 *   <li><strong>Jdk8Module</strong>: Provides support for Java 8 features like Optional</li>
 * </ul>
 *
 * <h2>Serialization Behavior</h2>
 * <ul>
 *   <li><strong>ISO 8601 Dates</strong>: All temporal types are serialized as ISO 8601 strings</li>
 *   <li><strong>No Timestamp Format</strong>: Avoids numeric timestamp representation for clarity</li>
 *   <li><strong>Optional Support</strong>: Properly handles Optional fields in DTOs</li>
 *   <li><strong>Timezone Preservation</strong>: Maintains timezone information in serialized dates</li>
 * </ul>
 *
 * <h2>Event Planner Specific Benefits</h2>
 * <ul>
 *   <li><strong>Timezone Consistency</strong>: Critical for multi-timezone event scheduling</li>
 *   <li><strong>API Standardization</strong>: Consistent date formats across all endpoints</li>
 *   <li><strong>Frontend Compatibility</strong>: ISO 8601 strings are easily parsed by JavaScript</li>
 *   <li><strong>Database Alignment</strong>: Matches the UTC storage strategy used in the database</li>
 * </ul>
 *
 * <h2>Supported Date/Time Types</h2>
 * <ul>
 *   <li><strong>LocalDateTime</strong>: For timezone-agnostic timestamps</li>
 *   <li><strong>ZonedDateTime</strong>: For timezone-aware event scheduling</li>
 *   <li><strong>LocalDate</strong>: For date-only fields like event dates</li>
 *   <li><strong>LocalTime</strong>: For time-only fields like duration</li>
 *   <li><strong>Instant</strong>: For precise UTC timestamps</li>
 * </ul>
 *
 * @see com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
 * @see com.fasterxml.jackson.datatype.jdk8.Jdk8Module
 * @see com.fasterxml.jackson.databind.ObjectMapper
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates and configures a custom Jackson ObjectMapper with support for modern Java types.
     *
     * <p>
     * The ObjectMapper is configured with modules that enable proper handling of Java 8+
     * date/time types and Optional values. This ensures consistent JSON serialization
     * across all REST API responses and request parsing.
     * </p>
     *
     * <h3>Module Configuration</h3>
     * <ul>
     *   <li><strong>JavaTimeModule</strong>: Registers serializers/deserializers for JSR-310 types</li>
     *   <li><strong>Jdk8Module</strong>: Adds support for Optional, OptionalInt, etc.</li>
     * </ul>
     *
     * <h3>Serialization Settings</h3>
     * <ul>
     *   <li><strong>WRITE_DATES_AS_TIMESTAMPS = false</strong>: Uses ISO 8601 string format</li>
     *   <li><strong>Default Inclusion</strong>: Includes all non-null values in JSON output</li>
     * </ul>
     *
     * <h3>Example JSON Output</h3>
     * <pre>{@code
     * {
     *   "id": 123,
     *   "title": "Team Meeting",
     *   "startTime": "2024-12-25T10:30:00Z",
     *   "endTime": "2024-12-25T11:30:00Z",
     *   "timezone": "America/New_York",
     *   "description": null  // Optional fields are included when present
     * }
     * }</pre>
     *
     * <h3>Error Handling</h3>
     * <p>
     * Invalid date/time strings in JSON requests will result in proper Jackson
     * deserialization errors, which are handled by the global exception handler
     * and returned as appropriate HTTP 400 responses.
     * </p>
     *
     * @return a configured ObjectMapper with support for modern Java types
     * @see com.yohan.event_planner.exception.GlobalExceptionHandler
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register modules for modern Java type support
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        
        // Configure date serialization to use ISO 8601 strings instead of timestamps
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        return mapper;
    }
}
