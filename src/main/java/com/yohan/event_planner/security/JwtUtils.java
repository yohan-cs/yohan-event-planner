package com.yohan.event_planner.security;

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

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

/**
 * Utility class for handling JWT-related operations such as generation, validation,
 * and parsing. This class is configured as a Spring component and reads its configuration
 * from application properties.
 *
 * <p>
 * Uses the JJWT library and a cached HMAC-SHA secret key. Tokens include a subject
 * (username), issued-at timestamp, and expiration timestamp.
 * </p>
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private SecretKey secretKey;

    /**
     * Initializes and caches the HMAC secret key after dependency injection.
     * This method is invoked automatically by Spring via {@link PostConstruct}.
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
     * Extracts the JWT token string from the Authorization header of an HTTP request.
     * Expects the token to be in the form: {@code Authorization: Bearer <token>}
     *
     * @param request the HTTP request containing the Authorization header
     * @return the JWT token string, or null if not present or improperly formatted
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization Header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Generates a new JWT for the given authenticated user.
     *
     * @param customUserDetails the authenticated user for whom the token is generated
     * @return a signed JWT token string
     */
    public String generateToken(CustomUserDetails customUserDetails) {
        String username = customUserDetails.getUsername();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Extracts the username (subject) from the given JWT token.
     *
     * @param token the JWT token string
     * @return the username contained in the token
     * @throws io.jsonwebtoken.JwtException if the token is invalid or signature fails
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates the integrity and expiration of the given JWT token.
     * Logs specific error messages if the token is malformed, expired, unsupported, or empty.
     *
     * @param authToken the JWT token to validate
     * @return true if the token is valid; false otherwise
     */
    public boolean validateJwtToken(String authToken) {
        try {
            logger.debug("Validating JWT token...");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
