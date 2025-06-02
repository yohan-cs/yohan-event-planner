package com.yohan.event_planner.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

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
            String rawPassword = "userPassword123";

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
            String rawPassword = "userPassword123";
            String hashedPassword = passwordBO.encryptPassword(rawPassword);
            boolean result = passwordBO.isMatch(rawPassword, hashedPassword);
            assertTrue(result, "The password should match the hashed password.");
        }

        @Test
        void testIsMatchFailure() {
            String rawPassword = "userPassword123";
            String wrongPassword = "wrongPassword123";
            String hashedPassword = passwordBO.encryptPassword(rawPassword);
            boolean result = passwordBO.isMatch(wrongPassword, hashedPassword);
            assertFalse(result, "The password should not match the incorrect hashed password.");
        }
    }
}
