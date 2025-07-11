package com.yohan.event_planner.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class GraphQLConfigTest {

    private GraphQLConfig graphQLConfig;

    @BeforeEach
    void setUp() {
        graphQLConfig = new GraphQLConfig();
    }

    @Test
    void testConfigurationAnnotation_isPresent() {
        // Assert
        assertTrue(GraphQLConfig.class.isAnnotationPresent(
            org.springframework.context.annotation.Configuration.class),
            "GraphQLConfig should have @Configuration annotation");
    }

    @Test
    void testRuntimeWiringConfigurerMethod_hasCorrectAnnotations() throws NoSuchMethodException {
        // Act
        var method = GraphQLConfig.class.getMethod("runtimeWiringConfigurer");

        // Assert
        assertTrue(method.isAnnotationPresent(
            org.springframework.context.annotation.Bean.class),
            "runtimeWiringConfigurer() method should have @Bean annotation");
    }

    @Test
    void testRuntimeWiringConfigurer_returnsNonNullConfigurer() {
        // Act
        RuntimeWiringConfigurer result = graphQLConfig.runtimeWiringConfigurer();

        // Assert
        assertNotNull(result, "RuntimeWiringConfigurer should not be null");
    }

    @Test
    void testRuntimeWiringConfigurer_returnsRuntimeWiringConfigurerType() {
        // Act
        RuntimeWiringConfigurer result = graphQLConfig.runtimeWiringConfigurer();

        // Assert
        assertInstanceOf(RuntimeWiringConfigurer.class, result,
            "Should return RuntimeWiringConfigurer instance");
    }

    @Test
    void testCanBeInstantiated() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            GraphQLConfig config = new GraphQLConfig();
            assertNotNull(config, "GraphQLConfig should be instantiable");
        });
    }

    @Test
    void testRuntimeWiringConfigurer_isCallable() {
        // Act
        RuntimeWiringConfigurer configurer = graphQLConfig.runtimeWiringConfigurer();

        // Assert
        assertDoesNotThrow(() -> {
            // The configurer should be callable without throwing exceptions
            // We can't easily test the wiring configuration without a full GraphQL context,
            // but we can verify it doesn't throw on creation
            assertNotNull(configurer, "Configurer should be created without errors");
        });
    }
}