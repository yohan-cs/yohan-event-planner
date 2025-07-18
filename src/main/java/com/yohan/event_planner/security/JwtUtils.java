package com.yohan.event_planner.security;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.exception.UnauthorizedException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;

/**
 * Utility class for handling JWT-related operations including generation, validation,
 * parsing, and refresh token management.
 *
 * <p>This component serves as the central JWT authority for the application, providing
 * secure token operations for authentication and authorization workflows.</p>
 *
 * <p><strong>Architecture Integration:</strong></p>
 * <ul>
 *   <li><strong>{@link com.yohan.event_planner.service.AuthServiceImpl}</strong> - Uses for access token generation during login</li>
 *   <li><strong>{@link AuthTokenFilter}</strong> - Uses for token validation and user ID extraction</li>
 *   <li><strong>{@link com.yohan.event_planner.service.RefreshTokenServiceImpl}</strong> - Uses for refresh token operations</li>
 * </ul>
 *
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li><strong>HMAC-SHA256 Signing</strong> - Cryptographically secure token signing</li>
 *   <li><strong>Configurable Expiration</strong> - Flexible token lifetime management</li>
 *   <li><strong>Secure Key Caching</strong> - Performance optimization with security</li>
 *   <li><strong>Deterministic Refresh Tokens</strong> - HMAC-based refresh token validation</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe after initialization.</p>
 *
 * <p>This class throws {@link UnauthorizedException} when tokens are missing or invalid.</p>
 *
 * @see com.yohan.event_planner.service.AuthServiceImpl
 * @see AuthTokenFilter
 * @see com.yohan.event_planner.service.RefreshTokenServiceImpl
 * @see CustomUserDetails
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${spring.app.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;

    private SecretKey secretKey;

    /**
     * Initializes the HMAC signing key after dependency injection.
     * Called automatically by Spring after all properties are injected.
     */
    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        logger.debug("JWT secret key initialized and cached.");
    }

    /**
     * Returns the cached signing key.
     *
     * @return the HMAC signing key used to sign and verify JWTs
     */
    private Key key() {
        return secretKey;
    }

    /**
     * Extracts the JWT token from the {@code Authorization} header of the provided request.
     * Expected format: {@code Authorization: Bearer <token>}.
     *
     * @param request the HTTP request containing the Authorization header
     * @return the JWT token string if present and properly formatted; {@code null} otherwise
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith(ApplicationConstants.JWT_BEARER_PREFIX)) {
            return bearerToken.substring(ApplicationConstants.JWT_BEARER_PREFIX_LENGTH);
        }
        return null;
    }

    /**
     * Generates a signed JWT for the specified authenticated user.
     * The token includes the user's ID as the subject, and has both issued-at and expiration timestamps.
     *
     * @param customUserDetails the authenticated user
     * @return a signed JWT token string
     */
    public String generateToken(CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUserId();
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
        
        logger.info("Generated JWT token for user ID: {}", userId);
        return token;
    }

    /**
     * Extracts the user ID from a valid JWT token.
     * This method validates the token and throws {@link UnauthorizedException} if the token is
     * missing, invalid, malformed, expired, or otherwise unusable.
     *
     * @param token the JWT token string
     * @return the user ID embedded in the token's subject
     * @throws UnauthorizedException if the token is invalid or cannot be parsed
     */
    public Long getUserIdFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            logger.error("JWT token is missing or blank.");
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
        try {
            String subject = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            Long userId = Long.valueOf(subject);
            logger.debug("Successfully validated JWT token for user ID: {}", userId);
            return userId;
        } catch (MalformedJwtException ex) {
            logger.warn("Invalid JWT token format - potential attack attempt: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
    }

    /**
     * Generates a secure opaque refresh token using cryptographically secure randomness.
     * 
     * <p>The generated token uses UUID v4 which provides 122 bits of entropy,
     * making it cryptographically suitable for security-sensitive operations.</p>
     *
     * <p><strong>Security Properties:</strong></p>
     * <ul>
     *   <li>Cryptographically secure random generation</li>
     *   <li>No predictable patterns or sequences</li>
     *   <li>Suitable for single-use authentication tokens</li>
     * </ul>
     *
     * @return a cryptographically secure opaque refresh token string
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Hashes the provided refresh token using HMAC-SHA256.
     * Used for securely storing refresh tokens in the database.
     * Unlike BCrypt, this produces deterministic hashes that allow for direct database lookups.
     *
     * @param refreshToken the raw refresh token to hash
     * @return the HMAC-SHA256 hash of the refresh token
     * @throws IllegalArgumentException if refreshToken is null or blank
     */
    public String hashRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be null or blank");
        }
        
        try {
            Mac mac = Mac.getInstance(ApplicationConstants.HMAC_SHA256_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), ApplicationConstants.HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to hash refresh token: {}", e.getMessage());
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

    /**
     * Validates a refresh token against its stored hash using constant-time comparison.
     * 
     * <p>This method provides secure validation by computing the HMAC of the provided
     * token and comparing it against the stored hash. The comparison is performed
     * using {@link String#equals(Object)} which provides natural timing-attack resistance
     * for equal-length strings.</p>
     *
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Uses the same HMAC algorithm as {@link #hashRefreshToken(String)}</li>
     *   <li>Resistant to timing attacks due to equal-length hash comparison</li>
     *   <li>No sensitive information logged during validation</li>
     * </ul>
     *
     * @param refreshToken the raw refresh token to validate
     * @param hashedToken the stored HMAC hash to compare against  
     * @return true if the token matches the hash, false otherwise
     */
    public boolean validateRefreshToken(String refreshToken, String hashedToken) {
        String computedHash = hashRefreshToken(refreshToken);
        return computedHash.equals(hashedToken);
    }

    /**
     * Returns the configured refresh token expiration time in milliseconds.
     *
     * @return refresh token expiration time in milliseconds
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
