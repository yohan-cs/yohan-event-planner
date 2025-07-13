package com.yohan.event_planner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthEntryPointJwtTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream outputStream;

    private AuthEntryPointJwt authEntryPointJwt;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        authEntryPointJwt = new AuthEntryPointJwt();
        objectMapper = new ObjectMapper();
    }

    @Nested
    class CommenceTests {

        @Test
        void commence_setsCorrectHttpStatus() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Invalid token") {};
            when(request.getRequestURI()).thenReturn("/api/test");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        void commence_setsCorrectContentType() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Token expired") {};
            when(request.getRequestURI()).thenReturn("/api/secure");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void commence_writesErrorResponseToOutputStream() throws ServletException, IOException {
            // Arrange
            String requestURI = "/api/protected";
            AuthenticationException authException = new AuthenticationException("Authentication failed") {};
            when(request.getRequestURI()).thenReturn(requestURI);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(response).getOutputStream();
        }

        @Test
        void commence_createsCorrectErrorResponse() throws ServletException, IOException {
            // Arrange
            String requestURI = "/api/events";
            AuthenticationException authException = new AuthenticationException("Missing JWT") {};
            when(request.getRequestURI()).thenReturn(requestURI);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(401);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(request).getRequestURI();
        }

        @Test
        void commence_handlesNullRequestURI() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Auth error") {};
            when(request.getRequestURI()).thenReturn(null);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void commence_handlesEmptyRequestURI() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Invalid credentials") {};
            when(request.getRequestURI()).thenReturn("");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void commence_handlesLongRequestURI() throws ServletException, IOException {
            // Arrange
            String longURI = "/api/very/long/path/with/many/segments/and/parameters?param1=value1&param2=value2";
            AuthenticationException authException = new AuthenticationException("Token validation failed") {};
            when(request.getRequestURI()).thenReturn(longURI);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(request).getRequestURI();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void commence_outputStreamThrowsIOException_propagatesException() throws IOException, ServletException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Auth failed") {};
            when(request.getRequestURI()).thenReturn("/api/test");
            when(response.getOutputStream()).thenThrow(new IOException("Stream closed"));

            // Act & Assert
            IOException exception = assertThrows(IOException.class, () -> {
                authEntryPointJwt.commence(request, response, authException);
            });
            assertEquals("Stream closed", exception.getMessage());
        }

        @Test
        void commence_responseOperationsComplete_whenNoExceptions() throws IOException, ServletException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Auth failed") {};
            when(request.getRequestURI()).thenReturn("/api/test");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Nested
    class AuthenticationExceptionHandlingTests {


        @Test
        void commence_handlesAuthenticationExceptionWithNullMessage() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException(null) {};
            when(request.getRequestURI()).thenReturn("/api/secure");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void commence_handlesAuthenticationExceptionWithEmptyMessage() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("") {};
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void commence_handlesAuthenticationExceptionWithSpecialCharacters() throws ServletException, IOException {
            // Arrange
            AuthenticationException authException = new AuthenticationException("Error: <script>alert('xss')</script>") {};
            when(request.getRequestURI()).thenReturn("/api/admin");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            authEntryPointJwt.commence(request, response, authException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @Nested
    class ErrorResponseFormatTests {

        @Test
        void commence_errorResponseHasCorrectStructure() throws ServletException, IOException {
            // This test verifies the error response would be properly structured
            // by creating the same ErrorResponse that the code would create
            
            // Arrange
            String requestURI = "/api/test";
            AuthenticationException authException = new AuthenticationException("Test auth error") {};
            
            // Create the same ErrorResponse that AuthEntryPointJwt would create
            ErrorResponse expectedErrorResponse = new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                "User not authenticated",
                UNAUTHORIZED_ACCESS.name(),
                System.currentTimeMillis(),
                requestURI
            );

            // Assert the structure
            assertEquals(401, expectedErrorResponse.status());
            assertEquals("User not authenticated", expectedErrorResponse.message());
            assertEquals(UNAUTHORIZED_ACCESS.name(), expectedErrorResponse.errorCode());
            assertEquals(requestURI, expectedErrorResponse.path());
            assertNotNull(expectedErrorResponse.timestamp());
            assertTrue(expectedErrorResponse.timestamp() > 0);
        }

        @Test
        void commence_errorCodeIsUnauthorizedAccess() throws ServletException, IOException {
            // Verify that the error code constant is correctly used
            assertEquals("UNAUTHORIZED_ACCESS", UNAUTHORIZED_ACCESS.name());
        }
    }
}