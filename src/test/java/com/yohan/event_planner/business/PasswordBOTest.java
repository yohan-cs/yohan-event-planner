package com.yohan.event_planner.business;

import com.yohan.event_planner.util.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordBOTest {

    private PasswordBO passwordBO;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Set up the PasswordEncoder to use BCryptPasswordEncoder
        passwordEncoder = new BCryptPasswordEncoder();
        passwordBO = new PasswordBO(passwordEncoder);
    }

    @Nested
    class EncryptPasswordTests {

        @Test
        void testEncryptPassword() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;

            // Act
            String hashedPassword = passwordBO.encryptPassword(rawPassword);

            // Assert
            assertTrue(hashedPassword.startsWith("$2a$"), "The hashed password should be a valid bcrypt hash.");
            assertEquals(60, hashedPassword.length(), "The hashed password should have a length of 60 characters.");
        }
    }

    @Nested
    class isMatchTests {

        @Test
        void testIsMatchSuccess() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;
            String hashedPassword = passwordBO.encryptPassword(rawPassword);

            // Act
            boolean result = passwordBO.isMatch(rawPassword, hashedPassword);

            // Assert
            assertTrue(result, "The password should match the hashed password.");
        }

        @Test
        void testIsMatchFailure() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;
            String wrongPassword = "wrongPassword123";
            String hashedPassword = passwordBO.encryptPassword(rawPassword);

            // Act
            boolean result = passwordBO.isMatch(wrongPassword, hashedPassword);

            // Assert
            assertFalse(result, "The password should not match the incorrect hashed password.");
        }
    }
}
