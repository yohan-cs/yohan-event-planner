package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.PasswordResetToken;
import com.yohan.event_planner.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for managing password reset tokens in the database.
 *
 * <p>
 * This repository provides specialized query methods for password reset token operations,
 * including token validation, cleanup of expired tokens, and user-specific token management.
 * It implements security best practices by ensuring tokens are single-use and time-limited.
 * </p>
 *
 * <h2>Key Operations</h2>
 * <ul>
 *   <li><strong>Token Lookup</strong>: Find tokens by token string for validation</li>
 *   <li><strong>User Management</strong>: Invalidate existing tokens when new ones are created</li>
 *   <li><strong>Cleanup</strong>: Remove expired and used tokens to maintain database hygiene</li>
 *   <li><strong>Security</strong>: Ensure only valid, unused tokens can be retrieved</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Automatic Expiry</strong>: Expired tokens are excluded from active queries</li>
 *   <li><strong>Single Use</strong>: Used tokens cannot be retrieved for reuse</li>
 *   <li><strong>User Isolation</strong>: Operations are scoped to specific users</li>
 *   <li><strong>Cleanup Jobs</strong>: Batch operations for removing stale data</li>
 * </ul>
 *
 * @see PasswordResetToken
 * @see User
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a valid password reset token by its token string.
     *
     * <p>
     * This method returns a token only if it meets all security criteria:
     * the token exists, has not been used, and has not expired. This ensures
     * that only legitimate, active tokens can be used for password resets.
     * </p>
     *
     * @param token the token string to search for
     * @param currentTime the current timestamp to check expiry against
     * @return an Optional containing the valid token, or empty if invalid/not found
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token " +
           "AND prt.used = false AND prt.expiryDate > :currentTime")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, 
                                                @Param("currentTime") Instant currentTime);

    /**
     * Invalidates all existing password reset tokens for a specific user.
     *
     * <p>
     * This method is called when a new password reset token is generated for a user,
     * ensuring that only the most recent token remains valid. This prevents
     * token accumulation and reduces the attack surface for password reset abuse.
     * </p>
     *
     * @param user the user whose tokens should be invalidated
     * @return the number of tokens that were invalidated
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user = :user AND prt.used = false")
    int invalidateAllTokensForUser(@Param("user") User user);

    /**
     * Removes all expired password reset tokens from the database.
     *
     * <p>
     * This cleanup method should be called periodically (e.g., via scheduled job)
     * to remove tokens that are no longer valid due to expiration. This helps
     * maintain database performance and reduces storage requirements.
     * </p>
     *
     * @param currentTime the current timestamp to determine which tokens are expired
     * @return the number of expired tokens that were deleted
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate <= :currentTime")
    int deleteExpiredTokens(@Param("currentTime") Instant currentTime);

    /**
     * Removes all used password reset tokens from the database.
     *
     * <p>
     * This cleanup method removes tokens that have been successfully used for
     * password resets. Used tokens serve no further purpose and can be safely
     * removed to maintain database cleanliness.
     * </p>
     *
     * @return the number of used tokens that were deleted
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.used = true")
    int deleteUsedTokens();

    /**
     * Counts the number of active (unused and non-expired) tokens for a specific user.
     *
     * <p>
     * This method can be used to implement rate limiting for password reset requests,
     * preventing users from generating excessive numbers of reset tokens within
     * a short time period.
     * </p>
     *
     * @param user the user to count active tokens for
     * @param currentTime the current timestamp to check expiry against
     * @return the number of active tokens for the user
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user = :user " +
           "AND prt.used = false AND prt.expiryDate > :currentTime")
    long countActiveTokensForUser(@Param("user") User user, @Param("currentTime") Instant currentTime);

    /**
     * Finds the most recent password reset token for a user, regardless of status.
     *
     * <p>
     * This method can be useful for implementing cooldown periods or rate limiting
     * based on when the last token was generated for a user.
     * </p>
     *
     * @param user the user to find the most recent token for
     * @return an Optional containing the most recent token, or empty if no tokens exist
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user = :user " +
           "ORDER BY prt.createdAt DESC LIMIT 1")
    Optional<PasswordResetToken> findMostRecentTokenForUser(@Param("user") User user);
}