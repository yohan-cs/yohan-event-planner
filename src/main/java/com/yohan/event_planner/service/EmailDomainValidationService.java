package com.yohan.event_planner.service;

/**
 * Service interface for validating email domains against blocklists and suspicious patterns.
 *
 * <p>
 * This service provides email domain validation functionality to prevent registration
 * with disposable email addresses, suspicious domains, and other potentially problematic
 * email providers. It helps maintain account quality and reduces fake registrations.
 * </p>
 *
 * <h2>Validation Categories</h2>
 * <ul>
 *   <li><strong>Disposable Email Detection</strong>: Blocks temporary email services</li>
 *   <li><strong>Suspicious Domain Patterns</strong>: Detects obviously fake domain patterns</li>
 *   <li><strong>Known Spam Domains</strong>: Blocks domains associated with spam</li>
 *   <li><strong>Invalid Domain Format</strong>: Validates proper domain structure</li>
 * </ul>
 *
 * <h2>Security Benefits</h2>
 * <ul>
 *   <li><strong>Fake Account Prevention</strong>: Reduces registrations with invalid emails</li>
 *   <li><strong>Username Protection</strong>: Prevents squatting with temporary emails</li>
 *   <li><strong>Quality Assurance</strong>: Ensures users provide legitimate contact info</li>
 *   <li><strong>Spam Reduction</strong>: Blocks email addresses from known spam domains</li>
 * </ul>
 *
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
public interface EmailDomainValidationService {

    /**
     * Validates if the given email domain is acceptable for registration.
     *
     * <p>
     * This method checks the email domain against various blocklists and validation
     * rules to determine if it should be allowed for account registration. It helps
     * prevent fake accounts and maintains the quality of user registrations.
     * </p>
     *
     * <h3>Validation Checks</h3>
     * <ul>
     *   <li><strong>Disposable Email Check</strong>: Against known temporary email providers</li>
     *   <li><strong>Domain Format Validation</strong>: Ensures proper domain structure</li>
     *   <li><strong>Suspicious Pattern Detection</strong>: Identifies obviously fake patterns</li>
     *   <li><strong>Spam Domain Check</strong>: Against known spam/abuse domains</li>
     * </ul>
     *
     * <h3>Common Blocked Domains</h3>
     * <ul>
     *   <li>10minutemail.com, guerrillamail.com (disposable email)</li>
     *   <li>example.com, test.com (testing/placeholder domains)</li>
     *   <li>Domains with suspicious patterns (random strings, etc.)</li>
     * </ul>
     *
     * @param emailAddress the full email address to validate
     * @return true if the email domain is acceptable, false if it should be blocked
     */
    boolean isEmailDomainValid(String emailAddress);

    /**
     * Extracts the domain portion from an email address.
     *
     * <p>
     * This method safely extracts the domain part from an email address,
     * handling edge cases and malformed inputs gracefully.
     * </p>
     *
     * @param emailAddress the email address to extract domain from
     * @return the domain portion of the email, or null if invalid
     */
    String extractDomain(String emailAddress);

    /**
     * Checks if a domain is in the disposable email blocklist.
     *
     * <p>
     * This method specifically checks against known disposable/temporary
     * email service providers that are commonly used for fake registrations.
     * </p>
     *
     * @param domain the domain to check
     * @return true if the domain is a known disposable email provider
     */
    boolean isDisposableEmailDomain(String domain);

    /**
     * Checks if a domain matches suspicious patterns.
     *
     * <p>
     * This method looks for patterns commonly associated with fake or
     * automatically generated domains, such as random character sequences
     * or obvious placeholder patterns.
     * </p>
     *
     * @param domain the domain to check
     * @return true if the domain matches suspicious patterns
     */
    boolean isSuspiciousDomain(String domain);

    /**
     * Gets the reason why an email domain was rejected.
     *
     * <p>
     * This method provides detailed information about why a specific email
     * domain was considered invalid, which can be useful for logging,
     * debugging, or providing user feedback.
     * </p>
     *
     * @param emailAddress the email address that was validated
     * @return a human-readable reason for rejection, or null if valid
     */
    String getValidationFailureReason(String emailAddress);
}