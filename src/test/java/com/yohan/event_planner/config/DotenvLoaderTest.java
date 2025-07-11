package com.yohan.event_planner.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class DotenvLoaderTest {

    @Test
    void testPostConstructAnnotation_isPresent() {
        // Verify that the @PostConstruct annotation is present
        try {
            var method = DotenvLoader.class.getDeclaredMethod("loadDotenv");
            var postConstructAnnotation = method.getAnnotation(jakarta.annotation.PostConstruct.class);
            
            assertNotNull(postConstructAnnotation,
                "loadDotenv method should have @PostConstruct annotation");
        } catch (NoSuchMethodException e) {
            fail("loadDotenv method should exist");
        }
    }

    @Test
    void testComponentAnnotation_isPresent() {
        // Verify that the class has @Component annotation
        var componentAnnotation = DotenvLoader.class.getAnnotation(org.springframework.stereotype.Component.class);
        
        assertNotNull(componentAnnotation, 
            "DotenvLoader should have @Component annotation");
    }

    @Test
    void testCanBeInstantiated() {
        // Test that the class can be instantiated without errors
        assertDoesNotThrow(() -> {
            DotenvLoader loader = new DotenvLoader();
            assertNotNull(loader, "DotenvLoader should be instantiable");
        });
    }
}