package com.yohan.event_planner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonConfigTest {

    private JacksonConfig jacksonConfig;

    @BeforeEach
    void setUp() {
        jacksonConfig = new JacksonConfig();
    }

    @Test
    void testConfigurationAnnotation_isPresent() {
        // Assert
        assertTrue(JacksonConfig.class.isAnnotationPresent(
            org.springframework.context.annotation.Configuration.class),
            "JacksonConfig should have @Configuration annotation");
    }

    @Test
    void testObjectMapperMethod_hasCorrectAnnotations() throws NoSuchMethodException {
        // Act
        var method = JacksonConfig.class.getMethod("objectMapper");

        // Assert
        assertTrue(method.isAnnotationPresent(
            org.springframework.context.annotation.Bean.class),
            "objectMapper() method should have @Bean annotation");
    }

    @Test
    void testObjectMapper_returnsNonNullMapper() {
        // Act
        ObjectMapper result = jacksonConfig.objectMapper();

        // Assert
        assertNotNull(result, "ObjectMapper should not be null");
    }

    @Test
    void testObjectMapper_returnsObjectMapperType() {
        // Act
        ObjectMapper result = jacksonConfig.objectMapper();

        // Assert
        assertInstanceOf(ObjectMapper.class, result,
            "Should return ObjectMapper instance");
    }

    @Test
    void testObjectMapper_hasJavaTimeModuleRegistered() {
        // Act
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Assert
        // Test by checking if JavaTimeModule functionality works
        assertDoesNotThrow(() -> {
            LocalDateTime now = LocalDateTime.now();
            String json = mapper.writeValueAsString(now);
            assertNotNull(json, "Should be able to serialize LocalDateTime");
            assertFalse(json.matches("\\d+"), "Should not be numeric timestamp");
        }, "JavaTimeModule should be registered and working");
    }

    @Test
    void testObjectMapper_hasJdk8ModuleRegistered() {
        // Act
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Assert
        // Test by checking if Jdk8Module functionality works with Optional
        assertDoesNotThrow(() -> {
            Optional<String> optional = Optional.of("test");
            String json = mapper.writeValueAsString(optional);
            assertNotNull(json, "Should be able to serialize Optional");
        }, "Jdk8Module should be registered and working");
    }

    @Test
    void testObjectMapper_writeDatesAsTimestampsDisabled() {
        // Act
        ObjectMapper mapper = jacksonConfig.objectMapper();

        // Assert
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
            "WRITE_DATES_AS_TIMESTAMPS should be disabled for ISO 8601 format");
    }

    @Test
    void testObjectMapper_serializesDateAsIsoString() throws Exception {
        // Arrange
        ObjectMapper mapper = jacksonConfig.objectMapper();
        ZonedDateTime testDate = ZonedDateTime.of(2025, 7, 10, 15, 30, 0, 0, ZoneOffset.UTC);

        // Act
        String json = mapper.writeValueAsString(testDate);

        // Assert
        assertTrue(json.contains("2025-07-10T15:30:00Z") || json.contains("2025-07-10T15:30Z"),
            "Date should be serialized as ISO 8601 string: " + json);
        assertFalse(json.matches("\\d+"), "Date should not be serialized as numeric timestamp");
    }

    @Test
    void testCanBeInstantiated() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            JacksonConfig config = new JacksonConfig();
            assertNotNull(config, "JacksonConfig should be instantiable");
        });
    }
}