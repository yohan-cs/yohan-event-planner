package com.yohan.event_planner.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity representing an email verification token for user account activation.
 *
 * <p>
 * This entity stores secure, single-use tokens that allow users to verify their email addresses
 * after registration. Tokens have a limited lifespan and are automatically invalidated after
 * use or expiry to maintain security.
 * </p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Single Use</strong>: Tokens are invalidated immediately after successful verification</li>
 *   <li><strong>Time-Limited</strong>: Tokens expire after a configurable duration (default: 24 hours)</li>
 *   <li><strong>Secure Generation</strong>: Uses cryptographically secure random token generation</li>
 *   <li><strong>User Association</strong>: Each token is tied to a specific user account</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li><strong>Generation</strong>: Created automatically when user registers</li>
 *   <li><strong>Email Delivery</strong>: Token sent to user's provided email address</li>
 *   <li><strong>Validation</strong>: Token verified when user clicks verification link</li>
 *   <li><strong>Activation</strong>: User account activated and token marked as used</li>
 * </ol>
 *
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "email_verification_tokens", indexes = {
    @Index(name = "idx_email_verification_token", columnList = "token"),
    @Index(name = "idx_email_verification_user_id", columnList = "user_id"),
    @Index(name = "idx_email_verification_expiry", columnList = "expiry_date")
})
public class EmailVerificationToken {

    /**
     * Primary key for the email verification token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The secure token string used for email verification.
     * This should be a cryptographically secure random string.
     */
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    /**
     * The user account associated with this email verification token.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Timestamp when this token was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp when this token expires and becomes invalid.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Flag indicating whether this token has been used for email verification.
     * Used tokens cannot be reused for security purposes.
     */
    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    /**
     * Default constructor for JPA.
     */
    public EmailVerificationToken() {}

    /**
     * Creates a new email verification token for the specified user.
     *
     * @param token the secure token string
     * @param user the user account this token is for
     * @param createdAt when this token was created
     * @param expiryDate when this token expires
     */
    public EmailVerificationToken(String token, User user, Instant createdAt, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.createdAt = createdAt;
        this.expiryDate = expiryDate;
        this.used = false;
    }

    /**
     * Checks if this token is currently valid (not used and not expired).
     *
     * @param currentTime the current timestamp to check against
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid(Instant currentTime) {
        return !used && currentTime.isBefore(expiryDate);
    }

    /**
     * Marks this token as used, preventing further email verification with this token.
     */
    public void markAsUsed() {
        this.used = true;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}