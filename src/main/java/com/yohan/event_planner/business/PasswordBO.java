package com.yohan.event_planner.business;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The PasswordBO class handles password encryption and comparison.
 * It uses a {@link PasswordEncoder} implementation to hash passwords
 * and verify if a raw password matches the hashed version.
 *
 * <p>It is a business object used to isolate password-related logic,
 * ensuring that passwords are securely hashed before storage and compared
 * securely during authentication.</p>
 */
@Service
public class PasswordBO {

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a PasswordBO instance with the provided {@link PasswordEncoder}.
     * The PasswordEncoder is used for hashing and comparing passwords.
     *
     * @param passwordEncoder the {@link PasswordEncoder} to use for password operations.
     */
    public PasswordBO(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Encrypts the raw password and returns the hashed version.
     * This method uses the provided {@link PasswordEncoder} to hash the password.
     *
     * @param rawPassword the raw password to encrypt.
     * @return the hashed version of the password.
     */
    public String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Checks if the raw password matches the provided hashed password.
     * This method uses the {@link PasswordEncoder} to compare the raw password with the hashed one.
     *
     * @param rawPassword the raw password entered by the user.
     * @param hashedPassword the hashed password stored in the system.
     * @return {@code true} if the raw password matches the hashed password, {@code false} otherwise.
     */
    public boolean isMatch(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
