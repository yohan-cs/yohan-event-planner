package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link RefreshToken} entities with comprehensive token lifecycle support.
 * 
 * <p>This repository provides sophisticated data access functionality for refresh token management,
 * implementing secure token rotation, revocation, and cleanup operations. It supports the dual-token
 * authentication strategy by enabling long-lived refresh tokens that can generate new access tokens
 * while maintaining security through automatic token rotation and revocation capabilities.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Token Lookup</strong>: Secure token retrieval by hash with validation</li>
 *   <li><strong>User Management</strong>: User-scoped token operations and revocation</li>
 *   <li><strong>Lifecycle Management</strong>: Token creation, rotation, and cleanup</li>
 *   <li><strong>Security Operations</strong>: Revocation, expiry, and validation queries</li>
 * </ul>
 * 
 * <h2>Token Security Model</h2>
 * <p>Comprehensive security implementation for refresh tokens:</p>
 * <ul>
 *   <li><strong>Hash-based Storage</strong>: Tokens stored as secure hashes, never in plaintext</li>
 *   <li><strong>Automatic Rotation</strong>: Each refresh generates new token and revokes old</li>
 *   <li><strong>Revocation Support</strong>: Immediate invalidation of compromised tokens</li>
 *   <li><strong>Expiry Management</strong>: Time-based automatic token invalidation</li>
 * </ul>
 * 
 * <h2>Token Lifecycle Operations</h2>
 * <p>Complete token management capabilities:</p>
 * 
 * <h3>Token Creation and Validation</h3>
 * <ul>
 *   <li><strong>Secure Lookup</strong>: {@link #findByTokenHash(String)} for token validation</li>
 *   <li><strong>Existence Checks</strong>: {@link #existsByTokenHash(String)} for duplicate prevention</li>
 *   <li><strong>Validation Queries</strong>: {@link #findAllValidTokens(Instant)} for active tokens</li>
 * </ul>
 * 
 * <h3>User-Scoped Management</h3>
 * <ul>
 *   <li><strong>Active Tokens</strong>: {@link #findByUserIdAndIsRevokedFalse(Long)} for user sessions</li>
 *   <li><strong>Token Counting</strong>: {@link #countActiveTokensByUserId(Long, Instant)} for limits</li>
 *   <li><strong>Mass Revocation</strong>: {@link #revokeAllByUserId(Long)} for security events</li>
 * </ul>
 * 
 * <h3>Security Operations</h3>
 * <ul>
 *   <li><strong>Individual Revocation</strong>: {@link #revokeByTokenHash(String)} for logout</li>
 *   <li><strong>Cleanup Operations</strong>: {@link #deleteExpiredTokens(Instant)} for maintenance</li>
 *   <li><strong>Revoked Cleanup</strong>: {@link #deleteRevokedTokensOlderThan(Instant)} for storage</li>
 * </ul>
 * 
 * <h2>Authentication Flow Integration</h2>
 * <p>Support for secure authentication workflows:</p>
 * <ul>
 *   <li><strong>Login Process</strong>: Create new refresh tokens with secure hashing</li>
 *   <li><strong>Token Refresh</strong>: Validate and rotate tokens automatically</li>
 *   <li><strong>Logout Process</strong>: Revoke specific tokens for clean logout</li>
 *   <li><strong>Security Events</strong>: Mass revocation for compromised accounts</li>
 * </ul>
 * 
 * <h2>Token Rotation Strategy</h2>
 * <p>Implementation of secure token rotation:</p>
 * <ul>
 *   <li><strong>Automatic Rotation</strong>: Each refresh operation creates new token</li>
 *   <li><strong>Old Token Revocation</strong>: Previous tokens automatically invalidated</li>
 *   <li><strong>Rotation Tracking</strong>: Maintain audit trail of token generations</li>
 *   <li><strong>Security Benefits</strong>: Reduce exposure window for token theft</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Hash Indexing</strong>: Efficient token lookup via hash indexes</li>
 *   <li><strong>User ID Indexing</strong>: Fast user-scoped queries</li>
 *   <li><strong>Batch Operations</strong>: Efficient bulk revocation and cleanup</li>
 *   <li><strong>Expiry Queries</strong>: Optimized time-based filtering</li>
 * </ul>
 * 
 * <h2>Cleanup and Maintenance</h2>
 * <p>Automated database hygiene operations:</p>
 * <ul>
 *   <li><strong>Expired Token Removal</strong>: Automatic cleanup of expired tokens</li>
 *   <li><strong>Revoked Token Cleanup</strong>: Remove old revoked tokens to save space</li>
 *   <li><strong>Storage Optimization</strong>: Maintain optimal database performance</li>
 *   <li><strong>Audit Trail</strong>: Configurable retention for security auditing</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <p>Comprehensive security implementation:</p>
 * <ul>
 *   <li><strong>Hash Security</strong>: Tokens never stored in plaintext</li>
 *   <li><strong>Timing Attack Prevention</strong>: Consistent query timing</li>
 *   <li><strong>Token Uniqueness</strong>: Prevent duplicate token generation</li>
 *   <li><strong>Revocation Integrity</strong>: Ensure revoked tokens cannot be reused</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>RefreshTokenService</strong>: Primary service layer integration</li>
 *   <li><strong>AuthService</strong>: Authentication and token rotation support</li>
 *   <li><strong>Security Framework</strong>: JWT token validation and renewal</li>
 *   <li><strong>Cleanup Jobs</strong>: Scheduled maintenance operations</li>
 * </ul>
 * 
 * <h2>Rate Limiting Support</h2>
 * <p>Enable rate limiting and abuse prevention:</p>
 * <ul>
 *   <li><strong>Active Token Counting</strong>: Prevent excessive token generation</li>
 *   <li><strong>User Session Limits</strong>: Enforce concurrent session limits</li>
 *   <li><strong>Abuse Detection</strong>: Identify suspicious token patterns</li>
 *   <li><strong>Security Monitoring</strong>: Support for authentication analytics</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Atomic Operations</strong>: Ensure consistent token state changes</li>
 *   <li><strong>User Relationships</strong>: Maintain proper user-token associations</li>
 *   <li><strong>State Integrity</strong>: Reliable revocation and expiry handling</li>
 *   <li><strong>Cleanup Accuracy</strong>: Precise removal of invalid tokens</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Hash Storage</strong>: All tokens are stored as secure hashes</li>
 *   <li><strong>Automatic Rotation</strong>: Token rotation handled at service layer</li>
 *   <li><strong>Security Critical</strong>: Repository operations directly impact authentication security</li>
 *   <li><strong>Performance Impact</strong>: Token operations should be optimized for frequent use</li>
 * </ul>
 * 
 * @see RefreshToken
 * @see com.yohan.event_planner.service.RefreshTokenService
 * @see com.yohan.event_planner.service.AuthService
 * @see com.yohan.event_planner.domain.User
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 2.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its secure hash value.
     * 
     * <p>This method is used during token validation and refresh operations.
     * The token hash is used instead of plaintext for security.</p>
     * 
     * @param tokenHash the secure hash of the token to find
     * @return an Optional containing the token if found, empty otherwise
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds all active (non-revoked) refresh tokens for a specific user.
     * 
     * <p>This method returns tokens that are not revoked, regardless of expiry.
     * Used for user session management and concurrent session limiting.</p>
     * 
     * @param userId the ID of the user whose tokens to retrieve
     * @return list of active refresh tokens for the user
     */
    List<RefreshToken> findByUserIdAndIsRevokedFalse(Long userId);

    /**
     * Revokes all refresh tokens for a specific user.
     * 
     * <p>This operation is used during security events such as password changes,
     * account compromise, or forced logout. It marks all user tokens as revoked.</p>
     * 
     * @param userId the ID of the user whose tokens should be revoked
     * @return the number of tokens that were revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.userId = :userId")
    int revokeAllByUserId(@Param("userId") Long userId);

    /**
     * Revokes a specific refresh token by its hash.
     * 
     * <p>This operation is used during normal logout to invalidate the specific
     * token being used, while leaving other user sessions active.</p>
     * 
     * @param tokenHash the hash of the token to revoke
     * @return the number of tokens that were revoked (should be 0 or 1)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.tokenHash = :tokenHash")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Deletes all refresh tokens that have expired.
     * 
     * <p>This cleanup operation should be run periodically to remove tokens
     * that are no longer valid due to expiration. Helps maintain database performance.</p>
     * 
     * @param now the current timestamp to compare expiry dates against
     * @return the number of expired tokens that were deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Deletes revoked refresh tokens that are older than the specified cutoff date.
     * 
     * <p>This cleanup operation removes old revoked tokens while maintaining recent
     * ones for audit purposes. Helps manage database storage efficiently.</p>
     * 
     * @param cutoffDate the cutoff date before which revoked tokens should be deleted
     * @return the number of old revoked tokens that were deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true AND rt.createdAt < :cutoffDate")
    int deleteRevokedTokensOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Counts the number of active refresh tokens for a specific user.
     * 
     * <p>Returns tokens that are not revoked and not expired. Used for implementing
     * rate limiting and concurrent session limits.</p>
     * 
     * @param userId the ID of the user to count tokens for
     * @param now the current timestamp to check expiry against
     * @return the count of active tokens for the user
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.isRevoked = false AND rt.expiryDate > :now")
    long countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Finds all valid (non-revoked and non-expired) refresh tokens.
     * 
     * <p>This method is primarily used for administrative purposes and system
     * monitoring to understand the current active token population.</p>
     * 
     * @param now the current timestamp to check expiry against
     * @return list of all valid refresh tokens in the system
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.isRevoked = false AND rt.expiryDate > :now")
    List<RefreshToken> findAllValidTokens(@Param("now") Instant now);

    /**
     * Checks if a refresh token exists with the specified hash.
     * 
     * <p>Used to prevent duplicate token generation and for efficient
     * existence checking without retrieving the full entity.</p>
     * 
     * @param tokenHash the hash of the token to check
     * @return true if a token with the hash exists, false otherwise
     */
    boolean existsByTokenHash(String tokenHash);
}