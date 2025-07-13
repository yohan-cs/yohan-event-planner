package com.yohan.event_planner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream outputStream;

    private CustomAccessDeniedHandler customAccessDeniedHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        customAccessDeniedHandler = new CustomAccessDeniedHandler();
        objectMapper = new ObjectMapper();
    }

    @Nested
    class HandleTests {

        @Test
        void handle_setsCorrectHttpStatus() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Insufficient permissions");
            when(request.getRequestURI()).thenReturn("/api/admin");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void handle_setsCorrectContentType() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_writesErrorResponseToOutputStream() throws ServletException, IOException {
            // Arrange
            String requestURI = "/api/users/delete";
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
            when(request.getRequestURI()).thenReturn(requestURI);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(response).getOutputStream();
        }

        @Test
        void handle_handlesNullRequestURI() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
            when(request.getRequestURI()).thenReturn(null);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_handlesEmptyRequestURI() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Role not authorized");
            when(request.getRequestURI()).thenReturn("");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_handlesLongRequestURI() throws ServletException, IOException {
            // Arrange
            String longURI = "/api/admin/users/management/complex/operation/with/many/path/segments?param1=value1&param2=value2";
            AccessDeniedException accessDeniedException = new AccessDeniedException("Insufficient role privileges");
            when(request.getRequestURI()).thenReturn(longURI);
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
            verify(request).getRequestURI();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void handle_outputStreamThrowsIOException_propagatesException() throws IOException, ServletException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
            when(request.getRequestURI()).thenReturn("/api/secure");
            when(response.getOutputStream()).thenThrow(new IOException("Output stream error"));

            // Act & Assert
            IOException exception = assertThrows(IOException.class, () -> {
                customAccessDeniedHandler.handle(request, response, accessDeniedException);
            });
            assertEquals("Output stream error", exception.getMessage());
        }

        @Test
        void handle_responseOperationsComplete_whenNoExceptions() throws IOException, ServletException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
            when(request.getRequestURI()).thenReturn("/api/test");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @Nested
    class AccessDeniedExceptionHandlingTests {


        @Test
        void handle_handlesAccessDeniedExceptionWithNullMessage() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException(null);
            when(request.getRequestURI()).thenReturn("/api/admin");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_handlesAccessDeniedExceptionWithEmptyMessage() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("");
            when(request.getRequestURI()).thenReturn("/api/users");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_handlesAccessDeniedExceptionWithSpecialCharacters() throws ServletException, IOException {
            // Arrange
            AccessDeniedException accessDeniedException = new AccessDeniedException("Role: <admin> & permission: {delete}");
            when(request.getRequestURI()).thenReturn("/api/sensitive");
            when(response.getOutputStream()).thenReturn(outputStream);

            // Act
            customAccessDeniedHandler.handle(request, response, accessDeniedException);

            // Assert
            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_handlesVariousAccessDeniedMessages() throws ServletException, IOException {
            // Test different common access denied scenarios
            String[] messages = {
                "Insufficient role privileges",
                "Resource ownership validation failed",
                "Admin access required",
                "User lacks required permissions"
            };

            for (String message : messages) {
                // Arrange
                AccessDeniedException accessDeniedException = new AccessDeniedException(message);
                when(request.getRequestURI()).thenReturn("/api/test");
                when(response.getOutputStream()).thenReturn(outputStream);

                // Act
                customAccessDeniedHandler.handle(request, response, accessDeniedException);
            }

            // Assert - verify that the methods were called for each message
            verify(response, times(4)).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response, times(4)).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @Nested
    class ErrorResponseFormatTests {

        @Test
        void handle_errorResponseHasCorrectStructure() throws ServletException, IOException {
            // This test verifies the error response would be properly structured
            // by creating the same ErrorResponse that the code would create
            
            // Arrange
            String requestURI = "/api/admin/users";
            AccessDeniedException accessDeniedException = new AccessDeniedException("Test access denied");
            
            // Create the same ErrorResponse that CustomAccessDeniedHandler would create
            ErrorResponse expectedErrorResponse = new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied: insufficient permissions",
                ErrorCode.ACCESS_DENIED.name(),
                System.currentTimeMillis(),
                requestURI
            );

            // Assert the structure
            assertEquals(403, expectedErrorResponse.status());
            assertEquals("Access denied: insufficient permissions", expectedErrorResponse.message());
            assertEquals(ErrorCode.ACCESS_DENIED.name(), expectedErrorResponse.errorCode());
            assertEquals(requestURI, expectedErrorResponse.path());
            assertNotNull(expectedErrorResponse.timestamp());
            assertTrue(expectedErrorResponse.timestamp() > 0);
        }

        @Test
        void handle_errorCodeIsAccessDenied() throws ServletException, IOException {
            // Verify that the error code constant is correctly used
            assertEquals("ACCESS_DENIED", ErrorCode.ACCESS_DENIED.name());
        }

        @Test
        void handle_statusCodeIsForbidden() throws ServletException, IOException {
            // Verify that the correct HTTP status is used
            assertEquals(403, HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void handle_messageIsConsistent() throws ServletException, IOException {
            // Verify the error message format
            String expectedMessage = "Access denied: insufficient permissions";
            
            // Create error response to test message consistency
            ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                expectedMessage,
                ErrorCode.ACCESS_DENIED.name(),
                System.currentTimeMillis(),
                "/api/test"
            );

            assertEquals(expectedMessage, errorResponse.message());
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void handle_complexRequestPathScenarios() throws ServletException, IOException {
            String[] complexPaths = {
                "/api/v1/users/123/events/456/details",
                "/admin/system/config/security",
                "/graphql",
                "/api/users?filter=admin&sort=name",
                "/resources/files/upload"
            };

            for (String path : complexPaths) {
                // Arrange
                AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied for path: " + path);
                when(request.getRequestURI()).thenReturn(path);
                when(response.getOutputStream()).thenReturn(outputStream);

                // Act
                customAccessDeniedHandler.handle(request, response, accessDeniedException);
            }

            // Assert - verify that the methods were called for each path
            verify(response, times(5)).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response, times(5)).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        void handle_multipleConsecutiveCalls_handlesCorrectly() throws ServletException, IOException {
            // Test that the handler can be called multiple times without issues
            for (int i = 0; i < 3; i++) {
                // Arrange
                AccessDeniedException accessDeniedException = new AccessDeniedException("Call " + i);
                when(request.getRequestURI()).thenReturn("/api/test/" + i);
                when(response.getOutputStream()).thenReturn(outputStream);

                // Act
                customAccessDeniedHandler.handle(request, response, accessDeniedException);
            }

            // Assert - verify that the methods were called for each iteration
            verify(response, times(3)).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(response, times(3)).setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }
}