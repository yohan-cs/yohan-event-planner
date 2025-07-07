package com.yohan.event_planner.security;

import com.yohan.event_planner.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    private final String jwtSecret = Base64.getEncoder().encodeToString("my-super-secret-key-1234567890".getBytes());
    private final long jwtExpirationMs = 3600000L; // 1 hour

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();

        // Use a 32-byte string (256 bits) to meet minimum key length
        String secureKey = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());

        // Inject fields via reflection
        Field secretField = JwtUtils.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtUtils, secureKey);

        Field expirationField = JwtUtils.class.getDeclaredField("jwtExpirationMs");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtils, 3600000L); // 1 hour

        // Call init() manually
        Method initMethod = JwtUtils.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jwtUtils);
    }

    @Nested
    class GetJwtFromHeaderTests {

        @Test
        void testGetJwtFromHeader_validBearerToken_returnsToken() {
            // Arrange
            String token = "abc.def.ghi";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            // Act
            String result = jwtUtils.getJwtFromHeader(request);

            // Assert
            assertEquals(token, result);
        }

        @Test
        void testGetJwtFromHeader_invalidHeader_returnsNull() {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

            // Act
            String result = jwtUtils.getJwtFromHeader(request);

            // Assert
            assertNull(result);
        }

        @Test
        void testGetJwtFromHeader_missingHeader_returnsNull() {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            String result = jwtUtils.getJwtFromHeader(request);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    class GenerateTokenTests {

        @Test
        void testGenerateAndValidateToken_success() {
            // Arrange
            Long userId = 42L;
            CustomUserDetails userDetails = mock(CustomUserDetails.class);
            when(userDetails.getUserId()).thenReturn(userId);

            // Act
            String token = jwtUtils.generateToken(userDetails);
            Long parsedUserId = jwtUtils.getUserIdFromJwtToken(token);

            // Assert
            assertEquals(userId, parsedUserId);
        }
    }

    @Nested
    class GetUserIdFromJwtTokenTests {

        @Test
        void testGetUserIdFromJwtToken_nullToken_throwsUnauthorizedException() {
            // Act + Assert
            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> jwtUtils.getUserIdFromJwtToken(null));
            assertEquals(UNAUTHORIZED_ACCESS, ex.getErrorCode());
        }

        @Test
        void testGetUserIdFromJwtToken_blankToken_throwsUnauthorizedException() {
            // Act + Assert
            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> jwtUtils.getUserIdFromJwtToken(" "));
            assertEquals(UNAUTHORIZED_ACCESS, ex.getErrorCode());
        }

        @Test
        void testGetUserIdFromJwtToken_malformedToken_throwsUnauthorizedException() {
            // Act + Assert
            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> jwtUtils.getUserIdFromJwtToken("not.a.jwt"));
            assertEquals(UNAUTHORIZED_ACCESS, ex.getErrorCode());
        }
    }
}
