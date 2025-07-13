package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserDetailsService} used by Spring Security to retrieve user information
 * during the authentication process.
 *
 * <h2>Architecture Integration</h2>
 * <p>
 * This service acts as a bridge between Spring Security's authentication framework and the
 * application's domain layer. It delegates to {@link UserBO} for user retrieval and wraps
 * domain {@link User} entities in {@link CustomUserDetails} objects compatible with Spring Security.
 * </p>
 *
 * <h2>Authentication Strategy</h2>
 * <p>
 * The service implements an email-as-username authentication strategy, improving user experience
 * by allowing login with email addresses while maintaining compatibility with Spring Security's
 * username-based authentication model.
 * </p>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>{@link SecurityConfig}</strong> - Registered as the user details service for authentication</li>
 *   <li><strong>{@link AuthTokenFilter}</strong> - Used for JWT-based user loading during token validation</li>
 *   <li><strong>{@link CustomUserDetails}</strong> - Returned wrapper providing Spring Security compatibility</li>
 *   <li><strong>{@link UserBO}</strong> - Business layer dependency for user retrieval operations</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Soft Deletion Support</strong> - Integrates with domain soft deletion semantics</li>
 *   <li><strong>Role Mapping</strong> - Automatically converts domain roles to Spring Security authorities</li>
 *   <li><strong>Exception Translation</strong> - Converts domain exceptions to Spring Security exceptions</li>
 * </ul>
 *
 * @see UserDetailsService
 * @see SecurityConfig
 * @see AuthTokenFilter
 * @see CustomUserDetails
 * @see UserBO
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    private final UserBO userBO;

    /**
     * Constructs the user details service with the given business layer dependency.
     *
     * <p>
     * This constructor establishes the connection to the business layer, ensuring proper
     * separation of concerns between Spring Security integration and domain logic.
     * The {@link UserBO} handles all user retrieval operations including soft deletion
     * semantics and business rules.
     * </p>
     *
     * <p>
     * <strong>Dependency Injection:</strong> This service is designed to be managed by
     * the Spring container and typically injected into {@link SecurityConfig} for
     * authentication manager configuration.
     * </p>
     *
     * @param userBO the business object responsible for user retrieval and domain logic
     */
    public UserDetailsServiceImpl(UserBO userBO) {
        this.userBO = userBO;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Loads a user by their email address and converts the domain {@link User} into Spring Security's
     * {@link UserDetails} representation.
     * </p>
     *
     * <h3>Email-as-Username Strategy</h3>
     * <p>
     * Despite the method name being "loadUserByUsername", this implementation uses the parameter
     * as an email address for user lookup. This provides better user experience by allowing
     * authentication with email addresses while maintaining compatibility with Spring Security's
     * username-based authentication contracts.
     * </p>
     *
     * <h3>Integration with Business Layer</h3>
     * <p>
     * This method delegates to {@link UserBO#getUserByEmail(String)} which handles:
     * </p>
     * <ul>
     *   <li>Database queries with proper soft deletion semantics</li>
     *   <li>Business rule enforcement</li>
     *   <li>Domain entity validation</li>
     * </ul>
     *
     * @param username the email address identifying the user (despite parameter name)
     * @return a {@link CustomUserDetails} wrapping the domain user with Spring Security compatibility
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Note: Spring Security requires this method name, but we're using the parameter as email
        String email = username;
        logger.debug("Loading user by email for authentication: {}", email);
        
        User user = userBO.getUserByEmail(email)
                .orElseThrow(() -> {
                    logger.debug("User not found for email: {}", email);
                    return new UsernameNotFoundException("User with email " + email + " not found");
                });
        
        logger.debug("Successfully loaded user {} (ID: {}) for authentication", user.getUsername(), user.getId());
        return new CustomUserDetails(user);
    }


    /**
     * Loads a user by their unique identifier for JWT-based authentication.
     *
     * <p>
     * This method is specifically designed for JWT authentication flows where the user ID
     * is extracted from the token and used to load user details. Unlike the standard
     * {@link #loadUserByUsername(String)} method, this bypasses email lookup and directly
     * retrieves the user by their primary key.
     * </p>
     *
     * <p>
     * <strong>Usage Context:</strong> Primarily used by {@link AuthTokenFilter} during
     * JWT token validation to establish the security context for authenticated requests.
     * </p>
     *
     * @param userId the unique identifier of the user to load
     * @return a {@link CustomUserDetails} wrapping the domain user
     * @throws UserNotFoundException if no user with the given ID exists
     */
    public UserDetails loadUserByUserId(Long userId) {
        logger.debug("Loading user by ID for JWT authentication: {}", userId);
        
        return userBO.getUserById(userId)
                .map(user -> {
                    logger.debug("Successfully loaded user {} (ID: {}) for JWT authentication", user.getUsername(), userId);
                    return new CustomUserDetails(user);
                })
                .orElseThrow(() -> {
                    logger.debug("User not found for ID: {}", userId);
                    return new UserNotFoundException(userId);
                });
    }
}
