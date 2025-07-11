package com.yohan.event_planner.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void testConfigurationAnnotation_isPresent() {
        // Assert
        assertTrue(OpenApiConfig.class.isAnnotationPresent(
            org.springframework.context.annotation.Configuration.class),
            "OpenApiConfig should have @Configuration annotation");
    }

    @Test
    void testOpenAPIDefinitionAnnotation_isPresent() {
        // Assert
        assertTrue(OpenApiConfig.class.isAnnotationPresent(OpenAPIDefinition.class),
            "OpenApiConfig should have @OpenAPIDefinition annotation");
    }

    @Test
    void testSecuritySchemeAnnotation_isPresent() {
        // Assert
        assertTrue(OpenApiConfig.class.isAnnotationPresent(SecurityScheme.class),
            "OpenApiConfig should have @SecurityScheme annotation");
    }

    @Test
    void testOpenAPIDefinition_hasCorrectTitle() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        Info info = definition.info();

        // Assert
        assertEquals("Event Planner API", info.title(),
            "API title should be 'Event Planner API'");
    }

    @Test
    void testOpenAPIDefinition_hasCorrectVersion() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        Info info = definition.info();

        // Assert
        assertEquals("2.0.0", info.version(),
            "API version should be '2.0.0'");
    }

    @Test
    void testOpenAPIDefinition_hasNonEmptyDescription() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        Info info = definition.info();

        // Assert
        assertNotNull(info.description(), "API description should not be null");
        assertFalse(info.description().trim().isEmpty(), "API description should not be empty");
        assertTrue(info.description().contains("event management"),
            "Description should mention event management");
    }

    @Test
    void testOpenAPIDefinition_hasContactInformation() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        var contact = definition.info().contact();

        // Assert
        assertNotNull(contact.name(), "Contact name should not be null");
        assertNotNull(contact.email(), "Contact email should not be null");
        assertFalse(contact.name().trim().isEmpty(), "Contact name should not be empty");
        assertFalse(contact.email().trim().isEmpty(), "Contact email should not be empty");
    }

    @Test
    void testOpenAPIDefinition_hasServers() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        var servers = definition.servers();

        // Assert
        assertTrue(servers.length >= 1, "Should have at least one server defined");
        assertNotNull(servers[0].url(), "Server URL should not be null");
        assertFalse(servers[0].url().trim().isEmpty(), "Server URL should not be empty");
    }

    @Test
    void testOpenAPIDefinition_hasTags() {
        // Act
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        Tag[] tags = definition.tags();

        // Assert
        assertTrue(tags.length >= 5, "Should have multiple API tags defined");
        
        // Check for expected tags
        boolean hasAuthTag = false;
        boolean hasEventsTag = false;
        for (Tag tag : tags) {
            if ("Authentication".equals(tag.name())) {
                hasAuthTag = true;
            }
            if ("Events".equals(tag.name())) {
                hasEventsTag = true;
            }
        }
        
        assertTrue(hasAuthTag, "Should have Authentication tag");
        assertTrue(hasEventsTag, "Should have Events tag");
    }

    @Test
    void testSecurityScheme_hasCorrectName() {
        // Act
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        // Assert
        assertEquals("Bearer Authentication", scheme.name(),
            "Security scheme name should be 'Bearer Authentication'");
    }

    @Test
    void testSecurityScheme_isBearerType() {
        // Act
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        // Assert
        assertEquals(io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP, scheme.type(),
            "Security scheme should be HTTP type");
        assertEquals("bearer", scheme.scheme(),
            "Security scheme should be bearer");
        assertEquals("JWT", scheme.bearerFormat(),
            "Bearer format should be JWT");
    }

    @Test
    void testCanBeInstantiated() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            OpenApiConfig config = new OpenApiConfig();
            assertNotNull(config, "OpenApiConfig should be instantiable");
        });
    }
}