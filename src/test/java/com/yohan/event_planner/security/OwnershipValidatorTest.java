package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipValidatorTest {

    private SecurityService securityService;
    private OwnershipValidator ownershipValidator;

    @BeforeEach
    void setUp() {
        securityService = mock(SecurityService.class);
        ownershipValidator = new OwnershipValidator(securityService);
    }

    @Nested
    class ValidateEventOwnershipTests {

    }

    @Nested
    class ValidateUserOwnershipTests {

        @Test
        void testValidateUserOwnership_validOwner_doesNotThrow() {
            // Mocks
            when(securityService.requireCurrentUserId()).thenReturn(TestConstants.USER_ID);

            // Act + Assert
            assertDoesNotThrow(() -> ownershipValidator.validateUserOwnership(TestConstants.USER_ID));
        }

        @Test
        void testValidateUserOwnership_invalidOwner_throwsUserOwnershipException() {
            // Mocks
            when(securityService.requireCurrentUserId()).thenReturn(99L);

            // Act + Assert
            assertThrows(UserOwnershipException.class,
                    () -> ownershipValidator.validateUserOwnership(TestConstants.USER_ID));
        }
    }

    @Nested
    class GetCurrentUserTests {

        @Test
        void testGetCurrentUser_delegatesToSecurityService() {
            // Arrange
            User expectedUser = TestUtils.createUserEntityWithId();

            // Mocks
            when(securityService.getAuthenticatedUser()).thenReturn(expectedUser);

            // Act
            User result = ownershipValidator.getCurrentUser();

            // Assert
            assertEquals(expectedUser, result);
            verify(securityService).getAuthenticatedUser();
        }
    }



}
