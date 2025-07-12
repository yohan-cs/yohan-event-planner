package com.yohan.event_planner.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a refresh token for secure JWT token renewal.
 *
 * <p>
 * This entity stores secure tokens that allow users to obtain new access tokens
 * without re-authentication. Refresh tokens have longer lifespans than access tokens
 * but include security mechanisms for revocation and expiry management.
 * </p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Secure Storage</strong>: Only token hashes are stored, never plaintext tokens</li>
 *   <li><strong>Revocation Support</strong>: Tokens can be explicitly revoked for security</li>
 *   <li><strong>Time-Limited</strong>: Tokens expire after a configurable duration</li>
 *   <li><strong>User Association</strong>: Each token is tied to a specific user account</li>
 * </ul>
 *
 * <h2>Token Lifecycle</h2>
 * <ol>
 *   <li><strong>Generation</strong>: Created during user login or token refresh</li>
 *   <li><strong>Storage</strong>: Hash stored securely in database</li>
 *   <li><strong>Usage</strong>: Used to generate new access tokens before expiry</li>
 *   <li><strong>Invalidation</strong>: Revoked on logout, security events, or expiry</li>
 * </ol>
 *
 * <h2>Validation Logic</h2>
 * <p>A refresh token is considered valid only when:</p>
 * <ul>
 *   <li>It has not been explicitly revoked ({@code isRevoked = false})</li>
 *   <li>Current time is before the expiry date</li>
 *   <li>Both conditions must be true for {@link #isValid()} to return true</li>
 * </ul>
 *
 * <h2>Database Design</h2>
 * <p>Key design decisions include:</p>
 * <ul>
 *   <li><strong>Token Hash Storage</strong>: Only hashed tokens stored for security</li>
 *   <li><strong>Unique Constraint</strong>: Ensures no duplicate token hashes</li>
 *   <li><strong>Dual User Reference</strong>: Both direct userId and JPA relationship</li>
 *   <li><strong>Timestamp Tracking</strong>: Creation time tracked for auditing</li>
 * </ul>
 *
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 * @see User
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    /** Primary key for the refresh token. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * Secure hash of the refresh token.
     * The plaintext token is never stored for security purposes.
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    /** 
     * ID of the user account associated with this refresh token.
     * Used for direct queries and as foreign key reference.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 
     * Timestamp when this refresh token expires and becomes invalid.
     * Used in conjunction with revocation status to determine token validity.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /** 
     * Timestamp when this refresh token was created.
     * Automatically set by Hibernate during entity creation.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 
     * Flag indicating whether this token has been explicitly revoked.
     * Revoked tokens cannot be used even if they haven't expired.
     */
    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked = false;

    /** 
     * JPA relationship to the user entity.
     * Uses the same user_id column but provides object access.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * Default constructor for JPA.
     */
    public RefreshToken() {}

    /**
     * Creates a new refresh token with the specified parameters.
     *
     * @param tokenHash the secure hash of the refresh token
     * @param userId the ID of the user this token belongs to
     * @param expiryDate when this token expires
     */
    public RefreshToken(String tokenHash, Long userId, Instant expiryDate) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiryDate = expiryDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Checks if this refresh token has expired.
     *
     * @return true if the current time is after the expiry date
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    /**
     * Checks if this refresh token is valid for use.
     * A token is valid if it has not been revoked and has not expired.
     *
     * @return true if the token can be used for generating new access tokens
     */
    public boolean isValid() {
        return !isRevoked && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        
        // If both have IDs, compare only by ID
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        
        // If either has null ID, they can't be equal (entities not yet persisted)
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hashCode(id) : 0;
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiryDate=" + expiryDate +
                ", createdAt=" + createdAt +
                ", isRevoked=" + isRevoked +
                '}';
    }
}