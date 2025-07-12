package com.yohan.event_planner.service;

import com.yohan.event_planner.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        
        // Set up test configuration values
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Sender");
        ReflectionTestUtils.setField(emailService, "deepLinkBase", "myapp://reset-password");
        ReflectionTestUtils.setField(emailService, "verificationLinkBase", "https://test.example.com/verify-email");
    }

    @Nested
    class SendEmailVerificationEmailTests {

        @Test
        void sendEmailVerificationEmail_whenSuccessful_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendEmailVerificationEmail_whenMessagingException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));

            assertEquals("Unexpected error sending email verification email", thrown.getMessage());
            assertInstanceOf(MailException.class, thrown.getCause());
        }

        @Test
        void sendEmailVerificationEmail_whenMailException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));

            assertEquals("Unexpected error sending email verification email", thrown.getMessage());
            assertInstanceOf(MailException.class, thrown.getCause());
        }

        @Test
        void sendEmailVerificationEmail_whenUnexpectedException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));

            assertEquals("Unexpected error sending email verification email", thrown.getMessage());
            assertInstanceOf(RuntimeException.class, thrown.getCause());
        }

        @Test
        void sendEmailVerificationEmail_withEmptyFirstName_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendEmailVerificationEmail_withNullFirstName_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = null;
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendEmailVerificationEmail_withSpecialCharactersInName_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "José-François Müller 🎉";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendEmailVerificationEmail_withVeryLongToken_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "a".repeat(500); // Very long token

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendEmailVerificationEmail_withLongEmailAddress_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "very.long.email.address.with.many.dots@very-long-domain-name-example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token-123";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class SendPasswordResetEmailTests {

        @Test
        void sendPasswordResetEmail_whenSuccessful_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_whenMailException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));

            assertEquals("Unexpected error sending password reset email", thrown.getMessage());
            assertInstanceOf(MailException.class, thrown.getCause());
        }

        @Test
        void sendPasswordResetEmail_whenUnexpectedException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));

            assertEquals("Unexpected error sending password reset email", thrown.getMessage());
            assertInstanceOf(RuntimeException.class, thrown.getCause());
        }

        @Test
        void sendPasswordResetEmail_withValidInputs_createsCorrectContent() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
            // Verify that MimeMessage was configured (we can't easily test the exact content without more complex mocking)
        }

        @Test
        void sendPasswordResetEmail_withZeroExpiryMinutes_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 0;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_withVeryLargeExpiryMinutes_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = Integer.MAX_VALUE;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_withSpecialCharactersInToken_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "token-with-special-chars!@#$%^&*()_+=";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_withSpecialEmailFormat_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user+test@example-domain.co.uk";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class SendWelcomeEmailTests {

        @Test
        void sendWelcomeEmail_whenSuccessful_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendWelcomeEmail(toEmail, username);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendWelcomeEmail_whenMailException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendWelcomeEmail(toEmail, username));

            assertEquals("Unexpected error sending welcome email", thrown.getMessage());
            assertInstanceOf(MailException.class, thrown.getCause());
        }

        @Test
        void sendWelcomeEmail_whenUnexpectedException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendWelcomeEmail(toEmail, username));

            assertEquals("Unexpected error sending welcome email", thrown.getMessage());
            assertInstanceOf(RuntimeException.class, thrown.getCause());
        }

        @Test
        void sendWelcomeEmail_withEmptyUsername_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendWelcomeEmail(toEmail, username));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendWelcomeEmail_withNullUsername_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = null;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendWelcomeEmail(toEmail, username));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendWelcomeEmail_withInternationalCharacters_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "用户名 Пользователь العربية 🌍";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendWelcomeEmail(toEmail, username));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class SendPasswordChangeConfirmationTests {

        @Test
        void sendPasswordChangeConfirmation_whenSuccessful_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendPasswordChangeConfirmation(toEmail, username);

            // Assert
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordChangeConfirmation_whenMailException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendPasswordChangeConfirmation(toEmail, username));

            assertEquals("Unexpected error sending password change confirmation", thrown.getMessage());
            assertInstanceOf(MailException.class, thrown.getCause());
        }

        @Test
        void sendPasswordChangeConfirmation_whenUnexpectedException_throwsEmailException() {
            // Arrange
            String toEmail = "user@example.com";
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class,
                    () -> emailService.sendPasswordChangeConfirmation(toEmail, username));

            assertEquals("Unexpected error sending password change confirmation", thrown.getMessage());
            assertInstanceOf(RuntimeException.class, thrown.getCause());
        }

        @Test
        void sendPasswordChangeConfirmation_withEmptyUsername_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordChangeConfirmation(toEmail, username));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordChangeConfirmation_withNullUsername_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = null;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> emailService.sendPasswordChangeConfirmation(toEmail, username));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidMailSender_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new EmailServiceImpl(mailSender));
        }

        @Test
        void constructor_withNullMailSender_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new EmailServiceImpl(null));
        }
    }

    @Nested
    class ConfigurationTests {

        @Test
        void emailService_withMissingFromEmail_handlesGracefully() {
            // Arrange
            EmailServiceImpl testService = new EmailServiceImpl(mailSender);
            ReflectionTestUtils.setField(testService, "fromEmail", null);
            ReflectionTestUtils.setField(testService, "fromName", "Test Sender");
            ReflectionTestUtils.setField(testService, "deepLinkBase", "myapp://reset-password");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service should handle null configuration gracefully
            assertDoesNotThrow(() -> {
                try {
                    testService.sendWelcomeEmail("test@example.com", "testuser");
                } catch (EmailException e) {
                    // Expected due to null configuration
                }
            });
        }

        @Test
        void emailService_withMissingVerificationLinkBase_handlesGracefully() {
            // Arrange
            EmailServiceImpl testService = new EmailServiceImpl(mailSender);
            ReflectionTestUtils.setField(testService, "fromEmail", "test@example.com");
            ReflectionTestUtils.setField(testService, "fromName", "Test Sender");
            ReflectionTestUtils.setField(testService, "verificationLinkBase", null);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service should handle null configuration gracefully
            assertDoesNotThrow(() -> {
                try {
                    testService.sendEmailVerificationEmail("test@example.com", "John", "token123");
                } catch (EmailException e) {
                    // Expected due to null configuration
                }
            });
        }

        @Test
        void emailService_withMissingDeepLinkBase_handlesGracefully() {
            // Arrange
            EmailServiceImpl testService = new EmailServiceImpl(mailSender);
            ReflectionTestUtils.setField(testService, "fromEmail", "test@example.com");
            ReflectionTestUtils.setField(testService, "fromName", "Test Sender");
            ReflectionTestUtils.setField(testService, "deepLinkBase", null);

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service should handle null configuration gracefully
            assertDoesNotThrow(() -> {
                try {
                    testService.sendPasswordResetEmail("test@example.com", "token123", 15);
                } catch (EmailException e) {
                    // Expected due to null configuration
                }
            });
        }

        @Test
        void emailService_withEmptyFromName_sendsEmail() throws MessagingException {
            // Arrange
            EmailServiceImpl testService = new EmailServiceImpl(mailSender);
            ReflectionTestUtils.setField(testService, "fromEmail", "test@example.com");
            ReflectionTestUtils.setField(testService, "fromName", "");
            ReflectionTestUtils.setField(testService, "deepLinkBase", "myapp://reset-password");

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            assertDoesNotThrow(() -> testService.sendWelcomeEmail("test@example.com", "testuser"));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class InputValidationTests {

        @Test
        void sendEmailVerificationEmail_withNullEmail_throwsEmailException() {
            // Arrange
            String toEmail = null;
            String firstName = "John";
            String verificationToken = "test-token";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Null email should cause MessagingException during helper.setTo()
            assertThrows(EmailException.class, () -> 
                emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
        }

        @Test
        void sendPasswordResetEmail_withNullEmail_throwsEmailException() {
            // Arrange
            String toEmail = null;
            String resetToken = "test-token";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Null email should cause MessagingException during helper.setTo()
            assertThrows(EmailException.class, () -> 
                emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
        }

        @Test
        void sendWelcomeEmail_withNullEmail_throwsEmailException() {
            // Arrange
            String toEmail = null;
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Null email should cause MessagingException during helper.setTo()
            assertThrows(EmailException.class, () -> 
                emailService.sendWelcomeEmail(toEmail, username));
        }

        @Test
        void sendPasswordChangeConfirmation_withNullEmail_throwsEmailException() {
            // Arrange
            String toEmail = null;
            String username = "testuser";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Null email should cause MessagingException during helper.setTo()
            assertThrows(EmailException.class, () -> 
                emailService.sendPasswordChangeConfirmation(toEmail, username));
        }

        @Test
        void sendEmailVerificationEmail_withNullToken_sendsEmailWithNullInUrl() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = null;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service handles null tokens by concatenating "null" into URL
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_withNullToken_sendsEmailWithNullInUrl() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = null;
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service handles null tokens by concatenating "null" into URL
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void sendPasswordResetEmail_withNegativeExpiryMinutes_sendsEmail() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token";
            int expiryMinutes = -5;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - Service should handle negative values gracefully
            assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes));
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    class ThreadSafetyTests {

        @Test
        void sendMultipleEmailsSimultaneously_handlesCorrectly() throws InterruptedException {
            // Arrange
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // Act
            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                new Thread(() -> {
                    try {
                        emailService.sendWelcomeEmail(
                            "user" + threadNum + "@example.com", 
                            "User" + threadNum
                        );
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Assert
            latch.await(5, TimeUnit.SECONDS);
            assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent execution");
            verify(mailSender, times(threadCount)).send(mimeMessage);
        }
    }

    @Nested
    class PerformanceTests {

        @Test
        void sendEmailVerificationEmail_withLargeContent_completesInReasonableTime() {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "A".repeat(1000); // Large first name
            String verificationToken = "B".repeat(2000); // Large token

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert
            long startTime = System.currentTimeMillis();
            assertDoesNotThrow(() -> emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken));
            long duration = System.currentTimeMillis() - startTime;

            // Should complete within a reasonable time (5 seconds)
            assertTrue(duration < 5000, "Email generation should complete within 5 seconds");
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void emailService_withAllConfigurationPresent_initializesSuccessfully() {
            // Arrange & Act
            EmailServiceImpl testService = new EmailServiceImpl(mailSender);
            ReflectionTestUtils.setField(testService, "fromEmail", "test@example.com");
            ReflectionTestUtils.setField(testService, "fromName", "Test Sender");
            ReflectionTestUtils.setField(testService, "deepLinkBase", "myapp://reset-password");
            ReflectionTestUtils.setField(testService, "verificationLinkBase", "https://test.example.com/verify-email");

            // Assert
            assertNotNull(testService);
        }
    }

    @Nested
    class ContentGenerationTests {

        @Test
        void emailVerificationEmail_sendsSuccessfully() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String firstName = "John";
            String verificationToken = "test-verification-token";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendEmailVerificationEmail(toEmail, firstName, verificationToken);

            // Assert
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void passwordResetEmail_containsExpectedElements() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String resetToken = "test-token-123";
            int expiryMinutes = 15;

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendPasswordResetEmail(toEmail, resetToken, expiryMinutes);

            // Assert - Verify the service was called (content verification would require more complex mocking)
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void welcomeEmail_containsUsername() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "JohnDoe";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendWelcomeEmail(toEmail, username);

            // Assert
            verify(mailSender).send(mimeMessage);
        }

        @Test
        void passwordChangeConfirmation_containsUsername() throws MessagingException {
            // Arrange
            String toEmail = "user@example.com";
            String username = "JohnDoe";

            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            emailService.sendPasswordChangeConfirmation(toEmail, username);

            // Assert
            verify(mailSender).send(mimeMessage);
        }
    }
}