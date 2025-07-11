package com.yohan.event_planner.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlazePersistenceConfigTest {

    private BlazePersistenceConfig blazePersistenceConfig;

    @BeforeEach
    void setUp() {
        blazePersistenceConfig = new BlazePersistenceConfig();
    }

    @Test
    void testConfigurationAnnotation_isPresent() {
        // Assert
        assertTrue(BlazePersistenceConfig.class.isAnnotationPresent(
            org.springframework.context.annotation.Configuration.class),
            "BlazePersistenceConfig should have @Configuration annotation");
    }

    @Test
    void testCriteriaBuilderFactoryMethod_hasCorrectAnnotations() throws NoSuchMethodException {
        // Act
        var method = BlazePersistenceConfig.class.getMethod("criteriaBuilderFactory");

        // Assert
        assertTrue(method.isAnnotationPresent(
            org.springframework.context.annotation.Bean.class),
            "criteriaBuilderFactory() method should have @Bean annotation");
    }

    @Test
    void testEntityManagerFactoryField_hasAutowiredAnnotation() throws NoSuchFieldException {
        // Act
        var field = BlazePersistenceConfig.class.getDeclaredField("entityManagerFactory");

        // Assert
        assertTrue(field.isAnnotationPresent(
            org.springframework.beans.factory.annotation.Autowired.class),
            "entityManagerFactory field should have @Autowired annotation");
    }

    @Test
    void testCanBeInstantiated() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            BlazePersistenceConfig config = new BlazePersistenceConfig();
            assertNotNull(config, "BlazePersistenceConfig should be instantiable");
        });
    }
}