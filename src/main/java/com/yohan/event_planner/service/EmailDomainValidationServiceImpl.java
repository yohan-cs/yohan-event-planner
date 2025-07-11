package com.yohan.event_planner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Implementation of email domain validation service for blocking fake and disposable emails.
 *
 * <p>
 * This service maintains blocklists of known disposable email providers and implements
 * pattern-based detection for suspicious domains. It helps prevent fake account creation
 * and maintains the quality of user registrations by blocking obviously invalid or
 * temporary email addresses.
 * </p>
 *
 * <h2>Validation Strategy</h2>
 * <ul>
 *   <li><strong>Static Blocklist</strong>: Known disposable email providers</li>
 *   <li><strong>Pattern Matching</strong>: Suspicious domain patterns</li>
 *   <li><strong>Format Validation</strong>: Basic domain structure checks</li>
 *   <li><strong>Testing Domain Detection</strong>: Common test/placeholder domains</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Fast Lookup</strong>: HashSet-based domain checking for O(1) performance</li>
 *   <li><strong>Compiled Patterns</strong>: Pre-compiled regex for efficient pattern matching</li>
 *   <li><strong>Memory Efficient</strong>: Compact blocklist storage</li>
 *   <li><strong>Thread Safe</strong>: Immutable data structures for concurrent access</li>
 * </ul>
 *
 * @see EmailDomainValidationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
@Service
public class EmailDomainValidationServiceImpl implements EmailDomainValidationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailDomainValidationServiceImpl.class);

    /**
     * Set of known disposable email domains.
     * This list includes popular temporary email services that are commonly used for fake registrations.
     */
    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
        // Popular disposable email services
        "10minutemail.com", "10minutemail.net", "tempmail.org", "guerrillamail.com",
        "mailinator.com", "maildrop.cc", "temp-mail.org", "yopmail.com",
        "throwaway.email", "getnada.com", "sharklasers.com", "guerrillamailblock.com",
        
        // Additional temporary email providers
        "tempail.com", "dispostable.com", "fakeinbox.com", "mailnesia.com",
        "trashmail.com", "emailondeck.com", "spamgourmet.com", "mytrashmail.com",
        "tempinbox.com", "10mail.org", "20minutemail.com", "emailtemporanea.net",
        
        // Testing and placeholder domains (should not be used for real accounts)
        "example.com", "example.org", "example.net", "test.com", "test.org",
        "localhost.com", "invalid.com", "domain.com", "email.com",
        
        // Suspicious patterns commonly used
        "tempemailaddress.com", "tempemail.com", "temp-email.com", "fakeemail.com",
        "nomail.com", "noemail.com", "disposableemail.com", "temporaryemail.com"
    );

    /**
     * Pattern for detecting suspicious domain structures.
     * This regex looks for domains that are likely to be fake or auto-generated.
     */
    private static final Pattern SUSPICIOUS_DOMAIN_PATTERN = Pattern.compile(
        "^[a-z0-9]{8,}\\.(com|org|net)$|" +     // Long random alphanumeric domains
        "^[a-z]{1,3}[0-9]{4,}\\.(com|org|net)$|" + // Short prefix with many numbers
        "^temp[a-z0-9]*\\.|" +                   // Starts with "temp"
        "^fake[a-z0-9]*\\.|" +                   // Starts with "fake"  
        "^test[a-z0-9]*\\.|" +                   // Starts with "test"
        "^mail[a-z0-9]*\\.|" +                   // Starts with "mail" (often temp services)
        "\\.(tk|ml|ga|cf)$",                     // Free TLDs often used for spam
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern for validating basic domain format.
     */
    private static final Pattern VALID_DOMAIN_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$"
    );

    @Override
    public boolean isEmailDomainValid(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            logger.debug("Email validation failed: null or empty email address");
            return false;
        }

        String domain = extractDomain(emailAddress.trim().toLowerCase());
        if (domain == null) {
            logger.debug("Email validation failed: could not extract domain from '{}'", emailAddress);
            return false;
        }

        // Check if domain format is valid
        if (!VALID_DOMAIN_PATTERN.matcher(domain).matches()) {
            logger.debug("Email validation failed: invalid domain format '{}'", domain);
            return false;
        }

        // Check against disposable email blocklist
        if (isDisposableEmailDomain(domain)) {
            logger.info("Email validation failed: disposable email domain '{}' blocked", domain);
            return false;
        }

        // Check for suspicious patterns
        if (isSuspiciousDomain(domain)) {
            logger.info("Email validation failed: suspicious domain pattern '{}' detected", domain);
            return false;
        }

        logger.debug("Email domain validation passed for '{}'", domain);
        return true;
    }

    @Override
    public String extractDomain(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            return null;
        }

        String trimmed = emailAddress.trim();
        int atIndex = trimmed.lastIndexOf('@');
        
        if (atIndex <= 0 || atIndex >= trimmed.length() - 1) {
            return null; // No @ symbol or @ is at start/end
        }

        return trimmed.substring(atIndex + 1).toLowerCase();
    }

    @Override
    public boolean isDisposableEmailDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        String normalizedDomain = domain.trim().toLowerCase();
        boolean isDisposable = DISPOSABLE_EMAIL_DOMAINS.contains(normalizedDomain);
        
        if (isDisposable) {
            logger.debug("Domain '{}' identified as disposable email provider", normalizedDomain);
        }
        
        return isDisposable;
    }

    @Override
    public boolean isSuspiciousDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }

        String normalizedDomain = domain.trim().toLowerCase();
        boolean isSuspicious = SUSPICIOUS_DOMAIN_PATTERN.matcher(normalizedDomain).find();
        
        if (isSuspicious) {
            logger.debug("Domain '{}' matches suspicious pattern", normalizedDomain);
        }
        
        return isSuspicious;
    }

    @Override
    public String getValidationFailureReason(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            return "Email address is null or empty";
        }

        String domain = extractDomain(emailAddress.trim().toLowerCase());
        if (domain == null) {
            return "Invalid email format: could not extract domain";
        }

        if (!VALID_DOMAIN_PATTERN.matcher(domain).matches()) {
            return "Invalid domain format: " + domain;
        }

        if (isDisposableEmailDomain(domain)) {
            return "Disposable email domains are not allowed: " + domain;
        }

        if (isSuspiciousDomain(domain)) {
            return "Suspicious domain pattern detected: " + domain;
        }

        return null; // No validation failure
    }
}