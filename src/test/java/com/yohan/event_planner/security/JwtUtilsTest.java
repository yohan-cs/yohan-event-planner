package com.yohan.event_planner.security;

import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private CustomUserDetails dummyUserDetails;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();

        // Inject private fields via reflection
        Field secretField = JwtUtils.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtUtils, TestConstants.BASE64_TEST_SECRET);

        Field expirationField = JwtUtils.class.getDeclaredField("jwtExpirationMs");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtils, 3600000L); // 1 hour

        // Trigger @PostConstruct manually
        var initMethod = JwtUtils.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jwtUtils);

        // Set up a dummy user
        dummyUserDetails = TestUtils.createCustomUserDetails();
    }

    @Nested
    class GenerateTokenTests {

        @Test
        void testGenerateTokenAndParseUsername() {
            // Arrange
            String username = dummyUserDetails.getUsername();

            // Act
            String token = jwtUtils.generateToken(dummyUserDetails);
            String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);

            // Assert
            assertNotNull(token, "Generated token should not be null");
            assertEquals(username, extractedUsername, "Extracted username should match the original");
        }
    }

    @Nested
    class ValidateJwtTokenTests {

        @Test
        void testValidateValidToken() {
            // Arrange
            String token = jwtUtils.generateToken(dummyUserDetails);

            // Act
            boolean isValid = jwtUtils.validateJwtToken(token);

            // Assert
            assertTrue(isValid, "Valid JWT token should be considered valid");
        }

        @Test
        void testValidateExpiredToken() {
            // Arrange
            long now = System.currentTimeMillis();
            SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TestConstants.BASE64_TEST_SECRET));
            String expiredToken = Jwts.builder()
                    .subject(dummyUserDetails.getUsername())
                    .issuedAt(new Date(now - 3600000L)) // issued 1 hour ago
                    .expiration(new Date(now - 1800000L)) // expired 30 mins ago
                    .signWith(key)
                    .compact();

            // Act
            boolean isValid = jwtUtils.validateJwtToken(expiredToken);

            // Assert
            assertFalse(isValid, "Expired JWT token should be considered invalid");
        }

        @Test
        void testValidateMalformedToken() {
            // Act + Assert
            boolean isValid = jwtUtils.validateJwtToken("this.is.not.a.jwt");
            assertFalse(isValid, "Malformed token should be considered invalid");
        }

        @Test
        void testValidateEmptyToken() {
            // Act + Assert
            boolean isValid = jwtUtils.validateJwtToken("");
            assertFalse(isValid, "Empty token should be considered invalid");
        }

        @Test
        void testValidateNullToken() {
            // Act + Assert
            boolean isValid = jwtUtils.validateJwtToken(null);
            assertFalse(isValid, "Null token should be considered invalid");
        }
    }
}
