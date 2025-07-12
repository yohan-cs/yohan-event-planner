package com.yohan.event_planner.service;

import com.yohan.event_planner.constants.ApplicationConstants;
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
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>PII Protection</strong>: Email addresses are masked in logs to protect user privacy</li>
 *   <li><strong>ReDoS Prevention</strong>: Regex patterns designed to avoid catastrophic backtracking</li>
 *   <li><strong>Input Sanitization</strong>: All inputs normalized and validated before processing</li>
 *   <li><strong>Fail-Safe Design</strong>: Invalid inputs return safe default values</li>
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
     * Suspicious domain pattern components for maintainable regex construction.
     * Each pattern targets different categories of suspicious domains.
     */
    private static final String SUSPICIOUS_PATTERNS = String.join("|",
        "^[a-z0-9]{12,}\\.(com|org|net)$",       // Very long random alphanumeric domains (12+ chars)
        "^[a-z]{1,3}[0-9]{4,}\\.(com|org|net)$", // Short prefix with many numbers  
        "^(temp|fake|test)[a-z0-9]*\\.",          // Suspicious prefixes
        "^mail[0-9]+\\.",                         // Mail with numbers (more specific)
        "\\.(tk|ml|ga|cf)$"                       // Free TLDs often used for spam
    );

    /**
     * Pattern for detecting suspicious domain structures.
     * This regex looks for domains that are likely to be fake or auto-generated.
     */
    private static final Pattern SUSPICIOUS_DOMAIN_PATTERN = Pattern.compile(
        SUSPICIOUS_PATTERNS, Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern for validating basic domain format according to RFC standards.
     * 
     * <p>This pattern enforces:</p>
     * <ul>
     *   <li>Domain must start and end with alphanumeric characters</li>
     *   <li>Can contain hyphens in the middle (but not at start/end)</li>
     *   <li>Maximum 63 characters per label (RFC 1035)</li>
     *   <li>Must have at least one dot and valid TLD (2+ characters)</li>
     *   <li>Supports subdomains (e.g., mail.company.com, subdomain.example.org)</li>
     * </ul>
     */
    private static final Pattern VALID_DOMAIN_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$"
    );

    // Service initialization logging - placed after all field definitions
    static {
        logger.info("EmailDomainValidationService initialized with {} disposable domains and 4 pattern categories", 
                    DISPOSABLE_EMAIL_DOMAINS.size());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Implementation performs validation in the following order:</p>
     * <ol>
     *   <li>Null/empty check</li>
     *   <li>Email length validation (RFC 5321 - max 254 characters)</li>
     *   <li>Domain extraction</li>
     *   <li>Domain length validation (RFC 1035 - max 253 characters)</li>
     *   <li>Domain format validation (RFC compliance)</li>
     *   <li>Disposable email domain check</li>
     *   <li>Suspicious pattern detection</li>
     * </ol>
     * 
     * @param emailAddress the email address to validate
     * @return true if domain passes all validation checks, false otherwise
     */
    @Override
    public boolean isEmailDomainValid(String emailAddress) {
        logger.debug("Validating email domain for address: {}", 
                     emailAddress != null ? maskEmail(emailAddress) : "null");
        
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            logger.debug("Email validation failed: null or empty email address");
            return false;
        }

        String trimmedEmail = emailAddress.trim();
        
        // RFC 5321 compliance: Check total email length
        if (trimmedEmail.length() > ApplicationConstants.MAX_EMAIL_LENGTH) {
            logger.debug("Email validation failed: email too long ({} > {} chars)", 
                        trimmedEmail.length(), ApplicationConstants.MAX_EMAIL_LENGTH);
            return false;
        }

        String domain = extractDomain(trimmedEmail.toLowerCase());
        if (domain == null) {
            logger.debug("Email validation failed: could not extract domain from '{}'", maskEmail(emailAddress));
            return false;
        }
        
        // RFC 1035 compliance: Check domain length
        if (domain.length() > ApplicationConstants.MAX_DOMAIN_LENGTH) {
            logger.debug("Email validation failed: domain too long ({} > {} chars)", 
                        domain.length(), ApplicationConstants.MAX_DOMAIN_LENGTH);
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

    /**
     * {@inheritDoc}
     * 
     * <p>Implementation uses {@code lastIndexOf('@')} to handle edge cases
     * with multiple @ symbols gracefully by extracting from the rightmost @.</p>
     * 
     * @param emailAddress the email address to extract domain from
     * @return the lowercase domain portion, or null if extraction fails
     */
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

    /**
     * {@inheritDoc}
     * 
     * <p>Checks against a curated blocklist of known disposable email providers 
     * including temporary services and testing domains. The blocklist contains
     * popular services like 10minutemail, guerrillamail, tempmail, and testing
     * domains like example.com.</p>
     * 
     * @param domain the domain to check against the blocklist
     * @return true if domain is found in the disposable email blocklist
     */
    @Override
    public boolean isDisposableEmailDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        String normalizedDomain = normalizeDomain(domain);
        boolean isDisposable = DISPOSABLE_EMAIL_DOMAINS.contains(normalizedDomain);
        
        if (isDisposable) {
            logger.debug("Domain '{}' identified as disposable email provider", normalizedDomain);
        }
        
        return isDisposable;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Uses compiled regex patterns to detect:</p>
     * <ul>
     *   <li>Long random alphanumeric domains (8+ chars)</li>
     *   <li>Short prefixes with many numbers (pattern: abc12345)</li>
     *   <li>Suspicious prefixes (temp, fake, test, mail)</li>
     *   <li>Free TLDs commonly used for spam (.tk, .ml, .ga, .cf)</li>
     * </ul>
     * 
     * @param domain the domain to check for suspicious patterns
     * @return true if domain matches any suspicious pattern
     */
    @Override
    public boolean isSuspiciousDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }

        String normalizedDomain = normalizeDomain(domain);
        boolean isSuspicious = SUSPICIOUS_DOMAIN_PATTERN.matcher(normalizedDomain).find();
        
        if (isSuspicious) {
            logger.debug("Domain '{}' matches suspicious pattern", normalizedDomain);
        }
        
        return isSuspicious;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Returns specific failure reasons in priority order:</p>
     * <ol>
     *   <li>"Email address is null or empty"</li>
     *   <li>"Email address too long: [length] characters (max 254)"</li>
     *   <li>"Invalid email format: could not extract domain"</li>
     *   <li>"Domain name too long: [length] characters (max 253)"</li>
     *   <li>"Invalid domain format: [domain]"</li>
     *   <li>"Disposable email domains are not allowed: [domain]"</li>
     *   <li>"Suspicious domain pattern detected: [domain]"</li>
     * </ol>
     * 
     * @param emailAddress the email address that failed validation
     * @return human-readable failure reason, or null if valid
     */
    @Override
    public String getValidationFailureReason(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            return "Email address is null or empty";
        }

        String trimmedEmail = emailAddress.trim();
        
        // RFC 5321 compliance: Check total email length
        if (trimmedEmail.length() > ApplicationConstants.MAX_EMAIL_LENGTH) {
            return "Email address too long: " + trimmedEmail.length() + " characters (max " + ApplicationConstants.MAX_EMAIL_LENGTH + ")";
        }

        String domain = extractDomain(trimmedEmail.toLowerCase());
        if (domain == null) {
            return "Invalid email format: could not extract domain";
        }
        
        // RFC 1035 compliance: Check domain length
        if (domain.length() > ApplicationConstants.MAX_DOMAIN_LENGTH) {
            return "Domain name too long: " + domain.length() + " characters (max " + ApplicationConstants.MAX_DOMAIN_LENGTH + ")";
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
    
    /**
     * Normalizes domain input for consistent validation processing.
     * 
     * @param domain the raw domain string to normalize
     * @return lowercase, trimmed domain string
     */
    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase();
    }
    
    /**
     * Masks email address for secure logging while preserving traceability.
     * Protects PII by showing only first character and domain.
     * 
     * @param email the email address to mask
     * @return masked email in format "a***@domain.com" or "***" if invalid
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***@" + email.substring(atIndex + 1);
    }
}