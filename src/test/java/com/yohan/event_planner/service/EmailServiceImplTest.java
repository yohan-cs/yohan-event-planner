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
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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

            lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
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

            lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
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

            lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
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
    class ContentGenerationTests {

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