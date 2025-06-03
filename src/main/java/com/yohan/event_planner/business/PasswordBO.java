package com.yohan.event_planner.business;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business object responsible for password encryption and comparison.
 *
 * <p>
 * This component centralizes password-related logic to ensure consistent and secure handling
 * of user credentials. It uses a {@link PasswordEncoder} to:
 * <ul>
 *   <li>Hash raw passwords before storing them</li>
 *   <li>Verify raw password input against hashed values during authentication</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class performs no validation or formatting and assumes all inputs have been checked upstream.
 * </p>
 */
@Service
public class PasswordBO {

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a {@code PasswordBO} instance with the provided {@link PasswordEncoder}.
     *
     * @param passwordEncoder the encoder used to hash and verify passwords
     */
    public PasswordBO(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Encrypts a raw password using the configured {@link PasswordEncoder}.
     *
     * @param rawPassword the raw password to encrypt
     * @return the hashed (encoded) version of the password
     */
    public String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies whether the given raw password matches the stored hashed password.
     *
     * @param rawPassword the plain-text password to check
     * @param hashedPassword the previously stored hashed password
     * @return {@code true} if the raw password matches the hashed password; otherwise {@code false}
     */
    public boolean isMatch(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
