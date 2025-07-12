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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
 * Utility class for handling JWT-related operations such as generation, validation,
 * and parsing. Reads its configuration from Spring application properties.
 *
 * <p>
 * Uses the JJWT library and a cached HMAC-SHA key for signing and verifying tokens.
 * Tokens include the user ID as the subject, an issued-at timestamp, and an expiration timestamp.
 * </p>
 *
 * <p>
 * This class throws {@link UnauthorizedException} when tokens are missing or invalid.
 * </p>
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
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Initializes the HMAC signing key and password encoder after dependency injection.
     * Called automatically by Spring after all properties are injected.
     */
    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.passwordEncoder = new BCryptPasswordEncoder();
        logger.debug("JWT secret key and password encoder initialized and cached.");
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
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
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

            return Long.valueOf(subject);
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
    }

    /**
     * Generates a secure opaque refresh token.
     * Uses UUID v4 for cryptographically secure random token generation.
     *
     * @return a secure opaque refresh token string
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
     */
    public String hashRefreshToken(String refreshToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Failed to hash refresh token: {}", e.getMessage());
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

    /**
     * Validates a refresh token against its stored hash.
     * Uses HMAC-SHA256 to compare the raw token with the stored hash.
     *
     * @param refreshToken the raw refresh token to validate
     * @param hashedToken the stored hash to compare against
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
