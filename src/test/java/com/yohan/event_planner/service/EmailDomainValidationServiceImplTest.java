package com.yohan.event_planner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for EmailDomainValidationServiceImpl.
 * Tests all validation scenarios, edge cases, and security patterns.
 */
public class EmailDomainValidationServiceImplTest {

    private EmailDomainValidationServiceImpl emailDomainValidationService;

    @BeforeEach
    void setUp() {
        emailDomainValidationService = new EmailDomainValidationServiceImpl();
    }

    @Nested
    class IsEmailDomainValidTests {

        @Test
        void testIsEmailDomainValid_withValidDomains() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isEmailDomainValid("user@gmail.com"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("test@yahoo.com"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("admin@microsoft.com"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("support@company.org"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("info@business.net"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("contact@university.edu"));
        }

        @Test
        void testIsEmailDomainValid_withNullOrEmptyEmails() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid(null));
            assertFalse(emailDomainValidationService.isEmailDomainValid(""));
            assertFalse(emailDomainValidationService.isEmailDomainValid("   "));
        }

        @Test
        void testIsEmailDomainValid_withMalformedEmails() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid("invalid-email"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("@domain.com"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@@domain.com"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain@com"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "user@10minutemail.com", "test@guerrillamail.com", "fake@mailinator.com",
            "temp@tempmail.org", "user@yopmail.com", "test@throwaway.email",
            "admin@example.com", "user@test.com", "test@localhost.com"
        })
        void testIsEmailDomainValid_withDisposableEmails(String email) {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "user@abcdefgh1234.com", "test@ab1234567.org", "user@temp123.com",
            "admin@fake456domain.net", "test@test789.com", "user@mail123.com",
            "contact@randomdomain.tk", "info@spamsite.ml"
        })
        void testIsEmailDomainValid_withSuspiciousPatterns(String email) {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid(email));
        }

        @Test
        void testIsEmailDomainValid_withInvalidDomainFormats() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@.com"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain."));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@-domain.com"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain-.com"));
        }

        @Test
        void testIsEmailDomainValid_caseInsensitivity() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isEmailDomainValid("USER@GMAIL.COM"));
            assertTrue(emailDomainValidationService.isEmailDomainValid("Test@Yahoo.Com"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("USER@TEMPMAIL.ORG"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("Test@Example.Com"));
        }

        @Test
        void testIsEmailDomainValid_withWhitespace() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isEmailDomainValid("  user@gmail.com  "));
            assertFalse(emailDomainValidationService.isEmailDomainValid("  user@tempmail.org  "));
        }

        @Test
        void testIsEmailDomainValid_withRFCLengthLimits() {
            // Arrange - Test RFC 5321 email length limit (254 characters)
            String longEmail = "a".repeat(244) + "@gmail.com"; // 254 chars total - should pass
            String tooLongEmail = "a".repeat(245) + "@gmail.com"; // 255 chars total - should fail
            
            // Arrange - Test subdomain support and label length limits
            // Valid subdomains
            String validSubdomain = "mail.google.com";
            String deepSubdomain = "help.support.microsoft.com";
            String maxLabelDomain = "a".repeat(61) + ".example.com"; // 61-char label (max allowed)
            
            // Invalid - label too long (64+ chars violates RFC 1035)
            String tooLongLabelDomain = "a".repeat(64) + ".com"; // 64-char label exceeds limit
            
            // Act & Assert
            assertTrue(emailDomainValidationService.isEmailDomainValid(longEmail));
            assertFalse(emailDomainValidationService.isEmailDomainValid(tooLongEmail));
            
            // Test subdomain support
            assertTrue(emailDomainValidationService.isEmailDomainValid("user@" + validSubdomain));
            assertTrue(emailDomainValidationService.isEmailDomainValid("admin@" + deepSubdomain));
            assertTrue(emailDomainValidationService.isEmailDomainValid("test@" + maxLabelDomain));
            
            // Test label length enforcement
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@" + tooLongLabelDomain));
        }
    }

    @Nested
    class ExtractDomainTests {

        @Test
        void testExtractDomain_withValidEmails() {
            // Arrange & Act & Assert
            assertEquals("gmail.com", emailDomainValidationService.extractDomain("user@gmail.com"));
            assertEquals("company.org", emailDomainValidationService.extractDomain("admin@company.org"));
            assertEquals("subdomain.example.net", emailDomainValidationService.extractDomain("test@subdomain.example.net"));
            assertEquals("mail.google.com", emailDomainValidationService.extractDomain("user@mail.google.com"));
            assertEquals("help.support.microsoft.com", emailDomainValidationService.extractDomain("admin@help.support.microsoft.com"));
        }

        @Test
        void testExtractDomain_withNullOrEmptyEmails() {
            // Arrange & Act & Assert
            assertNull(emailDomainValidationService.extractDomain(null));
            assertNull(emailDomainValidationService.extractDomain(""));
            assertNull(emailDomainValidationService.extractDomain("   "));
        }

        @Test
        void testExtractDomain_withMalformedEmails() {
            // Arrange & Act & Assert
            assertNull(emailDomainValidationService.extractDomain("invalid-email"));
            assertNull(emailDomainValidationService.extractDomain("@domain.com"));
            assertNull(emailDomainValidationService.extractDomain("user@"));
        }

        @Test
        void testExtractDomain_withMultipleAtSymbols() {
            // Arrange & Act & Assert - Uses lastIndexOf, so should extract correctly
            assertEquals("domain.com", emailDomainValidationService.extractDomain("user@company@domain.com"));
            assertEquals("final.org", emailDomainValidationService.extractDomain("test@first@second@final.org"));
        }

        @Test
        void testExtractDomain_caseNormalization() {
            // Arrange & Act & Assert
            assertEquals("gmail.com", emailDomainValidationService.extractDomain("USER@GMAIL.COM"));
            assertEquals("yahoo.com", emailDomainValidationService.extractDomain("Test@Yahoo.Com"));
        }

        @Test
        void testExtractDomain_withWhitespace() {
            // Arrange & Act & Assert
            assertEquals("gmail.com", emailDomainValidationService.extractDomain("  user@gmail.com  "));
            assertEquals("company.org", emailDomainValidationService.extractDomain("\ttest@company.org\n"));
        }
    }

    @Nested
    class IsDisposableEmailDomainTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "10minutemail.com", "guerrillamail.com", "mailinator.com", "tempmail.org",
            "yopmail.com", "throwaway.email", "example.com", "test.com", "localhost.com"
        })
        void testIsDisposableEmailDomain_withKnownDisposableDomains(String domain) {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isDisposableEmailDomain(domain));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "gmail.com", "yahoo.com", "microsoft.com", "company.org", 
            "university.edu", "business.net", "legitimate.co.uk"
        })
        void testIsDisposableEmailDomain_withLegitmateDomains(String domain) {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isDisposableEmailDomain(domain));
        }

        @Test
        void testIsDisposableEmailDomain_withNullOrEmptyDomains() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isDisposableEmailDomain(null));
            assertFalse(emailDomainValidationService.isDisposableEmailDomain(""));
            assertFalse(emailDomainValidationService.isDisposableEmailDomain("   "));
        }

        @Test
        void testIsDisposableEmailDomain_caseInsensitivity() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isDisposableEmailDomain("TEMPMAIL.ORG"));
            assertTrue(emailDomainValidationService.isDisposableEmailDomain("Example.COM"));
            assertTrue(emailDomainValidationService.isDisposableEmailDomain("GuerrillaMAIL.com"));
        }

        @Test
        void testIsDisposableEmailDomain_withWhitespace() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isDisposableEmailDomain("  tempmail.org  "));
            assertTrue(emailDomainValidationService.isDisposableEmailDomain("\texample.com\n"));
        }
    }

    @Nested
    class IsSuspiciousDomainTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "abcdefgh1234.com", "randomtext456.org", "longrandom789domain.net"
        })
        void testIsSuspiciousDomain_withLongRandomDomains(String domain) {
            // Arrange & Act & Assert - 12+ characters followed by common TLD
            assertTrue(emailDomainValidationService.isSuspiciousDomain(domain));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ab12345.com", "xyz98765.org", "a123456789.net"
        })
        void testIsSuspiciousDomain_withShortPrefixManyNumbers(String domain) {
            // Arrange & Act & Assert - 1-3 letters followed by 4+ numbers
            assertTrue(emailDomainValidationService.isSuspiciousDomain(domain));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "temp123.com", "fake456.org", "test789.net", "mail999.com",
            "tempservice.org", "fakesite.com", "testdomain.net", "mail123.com"
        })
        void testIsSuspiciousDomain_withSuspiciousPrefixes(String domain) {
            // Arrange & Act & Assert - Starts with temp, fake, test, or mail followed by numbers
            assertTrue(emailDomainValidationService.isSuspiciousDomain(domain));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "spamsite.tk", "fakeedomain.ml", "randomstuff.ga", "suspicious.cf"
        })
        void testIsSuspiciousDomain_withFreeTLDs(String domain) {
            // Arrange & Act & Assert - Free TLDs often used for spam
            assertTrue(emailDomainValidationService.isSuspiciousDomain(domain));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "gmail.com", "yahoo.com", "microsoft.com", "company.org", 
            "university.edu", "business.net", "legitimate.co.uk", "short.com"
        })
        void testIsSuspiciousDomain_withLegitmateDomains(String domain) {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isSuspiciousDomain(domain));
        }

        @Test
        void testIsSuspiciousDomain_withNullOrEmptyDomains() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isSuspiciousDomain(null));
            assertFalse(emailDomainValidationService.isSuspiciousDomain(""));
            assertFalse(emailDomainValidationService.isSuspiciousDomain("   "));
        }

        @Test
        void testIsSuspiciousDomain_caseInsensitivity() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isSuspiciousDomain("TEMP123.COM"));
            assertTrue(emailDomainValidationService.isSuspiciousDomain("Fake456.ORG"));
            assertTrue(emailDomainValidationService.isSuspiciousDomain("RANDOMDOMAIN.TK"));
        }

        @Test
        void testIsSuspiciousDomain_withWhitespace() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.isSuspiciousDomain("  temp123.com  "));
            assertTrue(emailDomainValidationService.isSuspiciousDomain("\tfake456.org\n"));
        }
    }

    @Nested
    class GetValidationFailureReasonTests {

        @Test
        void testGetValidationFailureReason_withValidEmails() {
            // Arrange & Act & Assert
            assertNull(emailDomainValidationService.getValidationFailureReason("user@gmail.com"));
            assertNull(emailDomainValidationService.getValidationFailureReason("admin@company.org"));
            assertNull(emailDomainValidationService.getValidationFailureReason("test@university.edu"));
        }

        @Test
        void testGetValidationFailureReason_withNullOrEmptyEmails() {
            // Arrange & Act & Assert
            assertEquals("Email address is null or empty", 
                        emailDomainValidationService.getValidationFailureReason(null));
            assertEquals("Email address is null or empty", 
                        emailDomainValidationService.getValidationFailureReason(""));
            assertEquals("Email address is null or empty", 
                        emailDomainValidationService.getValidationFailureReason("   "));
        }

        @Test
        void testGetValidationFailureReason_withMalformedEmails() {
            // Arrange & Act & Assert
            assertEquals("Invalid email format: could not extract domain", 
                        emailDomainValidationService.getValidationFailureReason("invalid-email"));
            assertEquals("Invalid email format: could not extract domain", 
                        emailDomainValidationService.getValidationFailureReason("@domain.com"));
            assertEquals("Invalid email format: could not extract domain", 
                        emailDomainValidationService.getValidationFailureReason("user@"));
        }

        @Test
        void testGetValidationFailureReason_withInvalidDomainFormats() {
            // Arrange & Act & Assert
            assertTrue(emailDomainValidationService.getValidationFailureReason("user@.com")
                      .startsWith("Invalid domain format:"));
            assertTrue(emailDomainValidationService.getValidationFailureReason("user@domain.")
                      .startsWith("Invalid domain format:"));
            assertTrue(emailDomainValidationService.getValidationFailureReason("user@-domain.com")
                      .startsWith("Invalid domain format:"));
        }

        @Test
        void testGetValidationFailureReason_withDisposableEmails() {
            // Arrange & Act & Assert
            assertEquals("Disposable email domains are not allowed: tempmail.org", 
                        emailDomainValidationService.getValidationFailureReason("user@tempmail.org"));
            assertEquals("Disposable email domains are not allowed: example.com", 
                        emailDomainValidationService.getValidationFailureReason("admin@example.com"));
        }

        @Test
        void testGetValidationFailureReason_withSuspiciousDomains() {
            // Arrange & Act & Assert
            assertEquals("Suspicious domain pattern detected: temp123.com", 
                        emailDomainValidationService.getValidationFailureReason("user@temp123.com"));
            assertEquals("Suspicious domain pattern detected: abcdefgh1234.org", 
                        emailDomainValidationService.getValidationFailureReason("test@abcdefgh1234.org"));
        }

        @Test
        void testGetValidationFailureReason_withRFCLengthViolations() {
            // Arrange - Create emails that violate RFC length limits
            String tooLongEmail = "a".repeat(245) + "@gmail.com"; // 255 chars total
            
            // Create domain with invalid label length (should hit format validation)
            String tooLongLabelDomain = "a".repeat(64) + ".com"; // 64-char label (max is 63)
            String emailWithBadFormat = "user@" + tooLongLabelDomain;
            
            // Create valid emails with subdomains
            String validSubdomainEmail = "user@mail.google.com";
            String maxValidDomain = "a".repeat(61) + ".example.com"; // 61-char label (valid)
            String validEmail = "user@" + maxValidDomain;
            
            // Act
            String longEmailReason = emailDomainValidationService.getValidationFailureReason(tooLongEmail);
            String badFormatReason = emailDomainValidationService.getValidationFailureReason(emailWithBadFormat);
            String validSubdomainReason = emailDomainValidationService.getValidationFailureReason(validSubdomainEmail);
            String validReason = emailDomainValidationService.getValidationFailureReason(validEmail);
            
            // Assert
            assertTrue(longEmailReason.startsWith("Email address too long:"), 
                      "Expected email length error but got: " + longEmailReason);
            assertTrue(badFormatReason.startsWith("Invalid domain format:"), 
                      "Expected domain format error but got: " + badFormatReason);
            assertNull(validSubdomainReason, "Valid subdomain email should return null but got: " + validSubdomainReason);
            assertNull(validReason, "Valid email should return null but got: " + validReason);
        }

        @Test
        void testGetValidationFailureReason_priorityOrder() {
            // Arrange & Act & Assert - Email length should be checked before domain extraction
            String tooLongEmail = "a".repeat(245) + "@tempmail.org"; // 259 chars total
            assertTrue(emailDomainValidationService.getValidationFailureReason(tooLongEmail)
                      .startsWith("Email address too long:"));
            
            // Invalid format should be caught before disposable check
            assertTrue(emailDomainValidationService.getValidationFailureReason("user@.tempmail.org")
                      .startsWith("Invalid domain format:"));
            
            // Domain format valid, but disposable should be caught before suspicious patterns
            assertEquals("Disposable email domains are not allowed: tempmail.org", 
                        emailDomainValidationService.getValidationFailureReason("user@tempmail.org"));
        }
    }

    @Nested
    class PerformanceAndSecurityTests {

        @Test
        void testRegexPerformance_noReDoSVulnerability() {
            // Arrange - Create potentially problematic input for ReDoS
            String maliciousInput = "a".repeat(1000) + "@" + "b".repeat(1000) + ".com";
            
            // Act & Assert - Should complete quickly, not hang
            long startTime = System.currentTimeMillis();
            boolean result = emailDomainValidationService.isEmailDomainValid(maliciousInput);
            long duration = System.currentTimeMillis() - startTime;
            
            // Should complete in reasonable time (< 100ms)
            assertTrue(duration < 100, "Regex processing took too long: " + duration + "ms");
            assertFalse(result); // Should be invalid due to length or pattern
        }

        @Test
        void testLargeEmailHandling() {
            // Arrange - Test with very long email (beyond RFC limits)
            String veryLongLocalPart = "a".repeat(300);
            String veryLongDomain = "b".repeat(300) + ".com";
            String veryLongEmail = veryLongLocalPart + "@" + veryLongDomain;
            
            // Act & Assert - Should handle gracefully
            assertFalse(emailDomainValidationService.isEmailDomainValid(veryLongEmail));
            assertNotNull(emailDomainValidationService.getValidationFailureReason(veryLongEmail));
        }

        @Test
        void testSpecialCharacterHandling() {
            // Arrange & Act & Assert - Test various special characters
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain.com; DROP TABLE users;"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain.com<script>alert('xss')</script>"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("user@domain.com\nEXTRA_CONTENT"));
        }
    }

    @Nested
    class BoundaryValueTests {

        @Test
        void testMinimumValidEmail() {
            // Arrange & Act & Assert - Shortest possible valid email
            assertTrue(emailDomainValidationService.isEmailDomainValid("a@b.co"));
        }

        @Test
        void testShortButInvalidEmails() {
            // Arrange & Act & Assert
            assertFalse(emailDomainValidationService.isEmailDomainValid("a@b"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("@b.co"));
            assertFalse(emailDomainValidationService.isEmailDomainValid("a@"));
        }

        @Test
        void testDomainLengthBoundaries() {
            // Arrange - Test domains at various lengths
            String shortDomain = "ab.co"; // 5 characters
            String mediumDomain = "company.com"; // 11 characters
            String longDomain = "very-long-company-name.organization"; // 35 characters
            
            // Act & Assert
            assertTrue(emailDomainValidationService.isEmailDomainValid("user@" + shortDomain));
            assertTrue(emailDomainValidationService.isEmailDomainValid("user@" + mediumDomain));
            assertTrue(emailDomainValidationService.isEmailDomainValid("user@" + longDomain));
        }
    }
}