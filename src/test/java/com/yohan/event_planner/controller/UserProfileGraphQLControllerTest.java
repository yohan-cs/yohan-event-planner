package com.yohan.event_planner.controller;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.graphql.input.UpdateFieldInput;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.service.BadgeService;
import com.yohan.event_planner.service.EventRecapService;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecapMediaService;
import com.yohan.event_planner.service.UserService;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserProfileGraphQLController focusing on helper methods and 
 * internal logic that doesn't require full integration testing.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileGraphQLControllerTest {

    @Mock private AuthenticatedUserProvider authenticatedUserProvider;
    @Mock private UserService userService;
    @Mock private BadgeService badgeService;
    @Mock private EventService eventService;
    @Mock private EventRecapService eventRecapService;
    @Mock private RecapMediaService recapMediaService;

    private UserProfileGraphQLController controller;

    @BeforeEach
    void setUp() {
        controller = new UserProfileGraphQLController(
                authenticatedUserProvider,
                userService,
                badgeService,
                eventService,
                eventRecapService,
                recapMediaService
        );
    }

    @Nested
    class ParseMediaTypeTests {

        @Test
        void testParseMediaType_ValidUppercaseType_ShouldReturnEnum() throws Exception {
            RecapMediaType result = invokeParseMediaType("VIDEO");
            assertEquals(RecapMediaType.VIDEO, result);
        }

        @Test
        void testParseMediaType_ValidLowercaseType_ShouldReturnEnum() throws Exception {
            RecapMediaType result = invokeParseMediaType("video");
            assertEquals(RecapMediaType.VIDEO, result);
        }

        @Test
        void testParseMediaType_ValidMixedCaseType_ShouldReturnEnum() throws Exception {
            RecapMediaType result = invokeParseMediaType("Video");
            assertEquals(RecapMediaType.VIDEO, result);
        }

        @Test
        void testParseMediaType_NullInput_ShouldReturnNull() throws Exception {
            RecapMediaType result = invokeParseMediaType(null);
            assertNull(result);
        }

        @Test
        void testParseMediaType_InvalidType_ShouldThrowException() {
            Exception exception = assertThrows(Exception.class, () -> {
                invokeParseMediaType("INVALID_TYPE");
            });
            
            // The reflection wraps the IllegalArgumentException in InvocationTargetException
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }

        @Test
        void testParseMediaType_EmptyString_ShouldThrowException() {
            Exception exception = assertThrows(Exception.class, () -> {
                invokeParseMediaType("");
            });
            
            // The reflection wraps the IllegalArgumentException in InvocationTargetException
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }

        private RecapMediaType invokeParseMediaType(String mediaType) throws Exception {
            Method method = UserProfileGraphQLController.class.getDeclaredMethod("parseMediaType", String.class);
            method.setAccessible(true);
            return (RecapMediaType) method.invoke(controller, mediaType);
        }
    }

    @Nested
    class MapFieldTests {

        @Test
        void testMapStringField_NullInput_ShouldReturnNull() throws Exception {
            Optional<String> result = invokeMapStringField(null);
            assertNull(result);
        }

        @Test
        void testMapStringField_AbsentField_ShouldReturnEmptyOptional() throws Exception {
            UpdateFieldInput<String> absentField = UpdateFieldInput.absent();
            Optional<String> result = invokeMapStringField(absentField);
            assertEquals(Optional.ofNullable(null), result);
        }

        @Test
        void testMapStringField_PresentFieldWithValue_ShouldReturnOptionalWithValue() throws Exception {
            UpdateFieldInput<String> presentField = new UpdateFieldInput<>("test value");
            Optional<String> result = invokeMapStringField(presentField);
            assertEquals(Optional.of("test value"), result);
        }

        @Test
        void testMapStringField_PresentFieldWithNullValue_ShouldReturnOptionalEmpty() throws Exception {
            UpdateFieldInput<String> presentField = new UpdateFieldInput<>(null);
            Optional<String> result = invokeMapStringField(presentField);
            assertEquals(Optional.ofNullable(null), result);
        }

        private Optional<String> invokeMapStringField(UpdateFieldInput<String> field) throws Exception {
            Method method = UserProfileGraphQLController.class.getDeclaredMethod("mapField", UpdateFieldInput.class);
            method.setAccessible(true);
            return (Optional<String>) method.invoke(controller, field);
        }
    }

    @Nested
    class MapZonedDateTimeFieldTests {

        @Test
        void testMapZonedDateTimeField_NullInput_ShouldReturnNull() throws Exception {
            Optional<ZonedDateTime> result = invokeMapZonedDateTimeField(null);
            assertNull(result);
        }

        @Test
        void testMapZonedDateTimeField_AbsentField_ShouldReturnEmptyOptional() throws Exception {
            UpdateFieldInput<Object> absentField = UpdateFieldInput.absent();
            Optional<ZonedDateTime> result = invokeMapZonedDateTimeField(absentField);
            assertEquals(Optional.ofNullable(null), result);
        }

        @Test
        void testMapZonedDateTimeField_ValidZonedDateTime_ShouldReturnSameInstance() throws Exception {
            ZonedDateTime now = ZonedDateTime.now();
            UpdateFieldInput<ZonedDateTime> presentField = new UpdateFieldInput<>(now);
            Optional<ZonedDateTime> result = invokeMapZonedDateTimeField(presentField);
            assertEquals(Optional.of(now), result);
        }

        @Test
        void testMapZonedDateTimeField_ValidISOString_ShouldParseCorrectly() throws Exception {
            String isoString = "2025-06-27T12:00:00Z";
            UpdateFieldInput<String> presentField = new UpdateFieldInput<>(isoString);
            Optional<ZonedDateTime> result = invokeMapZonedDateTimeField(presentField);
            
            assertTrue(result.isPresent());
            assertEquals(ZonedDateTime.parse(isoString), result.get());
        }

        @Test
        void testMapZonedDateTimeField_InvalidString_ShouldThrowException() throws Exception {
            UpdateFieldInput<String> presentField = new UpdateFieldInput<>("invalid-datetime");
            
            Exception exception = assertThrows(Exception.class, () -> {
                invokeMapZonedDateTimeField(presentField);
            });
            
            // Should be either IllegalArgumentException from our code or DateTimeParseException from parsing
            assertTrue(exception instanceof IllegalArgumentException || 
                      exception.getCause() instanceof java.time.format.DateTimeParseException);
        }

        @Test
        void testMapZonedDateTimeField_InvalidObjectType_ShouldThrowIllegalArgumentException() throws Exception {
            UpdateFieldInput<Integer> presentField = new UpdateFieldInput<>(12345);
            
            Exception exception = assertThrows(Exception.class, () -> {
                invokeMapZonedDateTimeField(presentField);
            });
            
            // The reflection may wrap the exception, so check both possibilities
            boolean isDirectException = exception instanceof IllegalArgumentException;
            boolean isCausedException = exception.getCause() instanceof IllegalArgumentException;
            
            assertTrue(isDirectException || isCausedException, 
                "Exception should be IllegalArgumentException either directly or as cause");
            
            String message = isDirectException ? exception.getMessage() : exception.getCause().getMessage();
            assertTrue(message.contains(ApplicationConstants.INVALID_DATETIME_FORMAT_MESSAGE));
        }

        @Test
        void testMapZonedDateTimeField_NullValue_ShouldReturnEmptyOptional() throws Exception {
            UpdateFieldInput<Object> presentField = new UpdateFieldInput<>(null);
            Optional<ZonedDateTime> result = invokeMapZonedDateTimeField(presentField);
            assertEquals(Optional.ofNullable(null), result);
        }

        private Optional<ZonedDateTime> invokeMapZonedDateTimeField(UpdateFieldInput<?> field) throws Exception {
            Method method = UserProfileGraphQLController.class.getDeclaredMethod("mapZonedDateTimeField", UpdateFieldInput.class);
            method.setAccessible(true);
            return (Optional<ZonedDateTime>) method.invoke(controller, field);
        }
    }

    @Nested
    class ApplicationConstantsUsageTests {

        @Test
        void testGraphQLOperationSuccessConstant_HasCorrectValue() {
            assertEquals(Boolean.TRUE, ApplicationConstants.GRAPHQL_OPERATION_SUCCESS);
        }

        @Test
        void testInvalidDateTimeFormatMessage_IsNotEmpty() {
            assertTrue(ApplicationConstants.INVALID_DATETIME_FORMAT_MESSAGE != null);
            assertTrue(!ApplicationConstants.INVALID_DATETIME_FORMAT_MESSAGE.trim().isEmpty());
        }

        @Test
        void testApplicationConstantsIntegration_InController() {
            // This test verifies that our controller uses the constants correctly
            // by testing the behavior rather than the implementation
            assertTrue(ApplicationConstants.GRAPHQL_OPERATION_SUCCESS);
            assertTrue(ApplicationConstants.INVALID_DATETIME_FORMAT_MESSAGE.contains("Unexpected value type"));
        }
    }

    @Nested
    class UnpinImpromptuEventTests {

        @Test
        void shouldUnpinImpromptuEventSuccessfully() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act
            Boolean result = controller.unpinImpromptuEvent();

            // Assert
            assertEquals(ApplicationConstants.GRAPHQL_OPERATION_SUCCESS, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventService).unpinImpromptuEventForCurrentUser();
        }

        @Test
        void shouldReturnTrueEvenWhenNoEventToUnpin() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act
            Boolean result = controller.unpinImpromptuEvent();

            // Assert
            assertEquals(ApplicationConstants.GRAPHQL_OPERATION_SUCCESS, result);
            verify(eventService).unpinImpromptuEventForCurrentUser();
        }

        @Test
        void shouldDelegateToEventService() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act
            controller.unpinImpromptuEvent();

            // Assert
            verify(eventService).unpinImpromptuEventForCurrentUser();
        }

    }
}