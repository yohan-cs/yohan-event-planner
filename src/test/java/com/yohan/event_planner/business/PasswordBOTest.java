package com.yohan.event_planner.business;

import com.yohan.event_planner.util.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    @Nested
    class EdgeCaseTests {

        @Test
        void testEncryptPassword_EmptyString() {
            // Arrange
            String emptyPassword = "";

            // Act
            String hashedPassword = passwordBO.encryptPassword(emptyPassword);

            // Assert
            assertNotNull(hashedPassword, "Empty password should still produce a hash");
            assertTrue(hashedPassword.startsWith("$2a$"), "Empty password hash should be valid bcrypt format");
            assertEquals(60, hashedPassword.length(), "Empty password hash should have standard bcrypt length");
            assertTrue(passwordBO.isMatch(emptyPassword, hashedPassword), "Empty password should match its hash");
        }

        @Test
        void testEncryptPassword_SpecialCharacters() {
            // Arrange
            String specialPassword = "P@ssw0rd!@#$%^&*()";

            // Act
            String hashedPassword = passwordBO.encryptPassword(specialPassword);

            // Assert
            assertNotNull(hashedPassword, "Password with special characters should produce a hash");
            assertTrue(hashedPassword.startsWith("$2a$"), "Special character password hash should be valid bcrypt format");
            assertEquals(60, hashedPassword.length(), "Special character password hash should have standard bcrypt length");
            assertTrue(passwordBO.isMatch(specialPassword, hashedPassword), "Password with special characters should match its hash");
        }

        @Test
        void testEncryptPassword_UnicodeCharacters() {
            // Arrange
            String unicodePassword = "パスワード123";

            // Act
            String hashedPassword = passwordBO.encryptPassword(unicodePassword);

            // Assert
            assertNotNull(hashedPassword, "Unicode password should produce a hash");
            assertTrue(hashedPassword.startsWith("$2a$"), "Unicode password hash should be valid bcrypt format");
            assertEquals(60, hashedPassword.length(), "Unicode password hash should have standard bcrypt length");
            assertTrue(passwordBO.isMatch(unicodePassword, hashedPassword), "Unicode password should match its hash");
        }

        @Test
        void testEncryptPassword_VeryLongPassword() {
            // Arrange - BCrypt limit is 72 bytes, so use 70 to be safe
            String longPassword = "a".repeat(70);

            // Act
            String hashedPassword = passwordBO.encryptPassword(longPassword);

            // Assert
            assertNotNull(hashedPassword, "Long password should produce a hash");
            assertTrue(hashedPassword.startsWith("$2a$"), "Long password hash should be valid bcrypt format");
            assertEquals(60, hashedPassword.length(), "Long password hash should have standard bcrypt length");
            assertTrue(passwordBO.isMatch(longPassword, hashedPassword), "Long password should match its hash");
        }

        @Test
        void testIsMatch_EmptyHashedPassword() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;
            String emptyHash = "";

            // Act
            boolean result = passwordBO.isMatch(rawPassword, emptyHash);

            // Assert
            assertFalse(result, "Password should not match empty hash");
        }

        @Test
        void testIsMatch_InvalidHashFormat() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;
            String invalidHash = "not-a-valid-bcrypt-hash";

            // Act
            boolean result = passwordBO.isMatch(rawPassword, invalidHash);

            // Assert
            assertFalse(result, "Password should not match invalid hash format");
        }

        @Test
        void testIsMatch_DifferentValidHash() {
            // Arrange
            String rawPassword = TestConstants.VALID_PASSWORD;
            String differentPassword = "DifferentPassword123!";
            String differentHash = passwordBO.encryptPassword(differentPassword);

            // Act
            boolean result = passwordBO.isMatch(rawPassword, differentHash);

            // Assert
            assertFalse(result, "Password should not match hash of different password");
        }
    }

    @Nested
    class SecurityTests {

        @Test
        void testEncryptPassword_ProducesDifferentHashesForSameInput() {
            // Arrange
            String password = TestConstants.VALID_PASSWORD;

            // Act
            String hash1 = passwordBO.encryptPassword(password);
            String hash2 = passwordBO.encryptPassword(password);

            // Assert
            assertNotEquals(hash1, hash2, "Same password should produce different hashes due to salt randomization");
            assertTrue(passwordBO.isMatch(password, hash1), "Password should match first hash");
            assertTrue(passwordBO.isMatch(password, hash2), "Password should match second hash");
        }

        @Test
        void testEncryptPassword_ConsistentLength() {
            // Arrange
            String shortPassword = "abc";
            String mediumPassword = TestConstants.VALID_PASSWORD;
            String longPassword = "a".repeat(70);

            // Act
            String shortHash = passwordBO.encryptPassword(shortPassword);
            String mediumHash = passwordBO.encryptPassword(mediumPassword);
            String longHash = passwordBO.encryptPassword(longPassword);

            // Assert
            assertEquals(60, shortHash.length(), "Short password hash should have standard bcrypt length");
            assertEquals(60, mediumHash.length(), "Medium password hash should have standard bcrypt length");
            assertEquals(60, longHash.length(), "Long password hash should have standard bcrypt length");
        }

        @Test
        void testEncryptPassword_AllHashesHaveValidBcryptFormat() {
            // Arrange
            String[] testPasswords = {
                "",
                "a",
                TestConstants.VALID_PASSWORD,
                "P@ssw0rd!@#$%^&*()",
                "パスワード123",
                "a".repeat(70), // BCrypt's input limit is 72 bytes, use 70 to be safe
                "Mixed123!@#ÄÖÜäöüß"
            };

            for (String password : testPasswords) {
                // Act
                String hash = passwordBO.encryptPassword(password);

                // Assert
                assertTrue(hash.startsWith("$2a$"), "Hash should start with BCrypt identifier for password: '" + password + "'");
                assertEquals(60, hash.length(), "Hash should be 60 characters for password: '" + password + "'");
                assertTrue(passwordBO.isMatch(password, hash), "Password should match its hash for: '" + password + "'");
            }
        }

        @Test
        void testIsMatch_CaseSensitive() {
            // Arrange
            String password = "TestPassword123";
            String uppercasePassword = password.toUpperCase();
            String lowercasePassword = password.toLowerCase();
            String hash = passwordBO.encryptPassword(password);

            // Act & Assert
            assertTrue(passwordBO.isMatch(password, hash), "Exact password should match");
            assertFalse(passwordBO.isMatch(uppercasePassword, hash), "Uppercase version should not match");
            assertFalse(passwordBO.isMatch(lowercasePassword, hash), "Lowercase version should not match");
        }

        @Test
        void testIsMatch_WhitespaceSignificant() {
            // Arrange
            String password = "password123";
            String passwordWithSpaces = " password123 ";
            String passwordWithTabs = "\tpassword123\t";
            String hash = passwordBO.encryptPassword(password);

            // Act & Assert
            assertTrue(passwordBO.isMatch(password, hash), "Exact password should match");
            assertFalse(passwordBO.isMatch(passwordWithSpaces, hash), "Password with leading/trailing spaces should not match");
            assertFalse(passwordBO.isMatch(passwordWithTabs, hash), "Password with leading/trailing tabs should not match");
        }
    }
}
