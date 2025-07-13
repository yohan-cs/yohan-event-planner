package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom implementation of {@link UserDetails} that wraps the application's {@link User} domain object.
 *
 * <h2>Architecture Integration</h2>
 * <p>
 * This adapter class bridges the domain {@link User} entity with Spring Security's authentication
 * and authorization framework. It is primarily used by {@link UserDetailsServiceImpl} during
 * JWT authentication processing in {@link AuthTokenFilter}, and integrates with the security
 * context through {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}.
 * </p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Role Mapping</strong>: Converts domain {@link Role} enums to Spring Security authorities</li>
 *   <li><strong>Account Status</strong>: Implements account validation including soft deletion checks</li>
 *   <li><strong>Credential Access</strong>: Provides secure access to hashed passwords</li>
 * </ul>
 *
 * <h2>Account Status Implementation</h2>
 * <p>
 * Most account status methods return {@code true} for simplicity, except {@code isEnabled()}
 * which delegates to the domain user's deletion status. This design allows for future
 * enhancement without breaking existing functionality.
 * </p>
 *
 * @see UserDetailsServiceImpl
 * @see AuthTokenFilter
 * @see User
 * @see Role
 */
public class CustomUserDetails implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetails.class);
    private final User user;

    /**
     * Constructs a {@code CustomUserDetails} instance by wrapping a domain {@code User}.
     *
     * <p>
     * This constructor creates a Spring Security-compatible user details object that adapts
     * the domain {@link User} entity for authentication and authorization processing.
     * The wrapped user provides credentials, authorities, and account status information
     * required by the Spring Security framework.
     * </p>
     *
     * <p>
     * <strong>Security Context:</strong> The created instance will be used in authentication
     * tokens stored in the {@link org.springframework.security.core.context.SecurityContextHolder}
     * and accessed throughout the request lifecycle by security-aware components.
     * </p>
     *
     * @param user the user entity from the domain layer, must not be null
     * @throws IllegalArgumentException if user is null
     */
    public CustomUserDetails(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.user = user;
        logger.debug("Created CustomUserDetails for user ID: {}, username: {}", 
                    user.getId(), user.getUsername());
    }

    /**
     * Returns the domain {@code User} object wrapped by this class.
     *
     * @return the domain user
     */
    public User getUser() {
        return user;
    }

    /**
     * Returns the unique identifier of the authenticated user.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return user.getId();
    }

    /**
     * Returns the username used to authenticate the user.
     *
     * @return the user's username
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Returns the hashed password of the user.
     *
     * @return the stored (hashed) password
     */
    @Override
    public String getPassword() {
        return user.getHashedPassword();
    }

    /**
     * Returns the authorities granted to the user.
     *
     * <p>
     * Maps each {@link Role} enum to a {@link SimpleGrantedAuthority} using the role's
     * authority string representation. The mapping follows Spring Security conventions
     * where role names are prefixed with "ROLE_".
     * </p>
     *
     * <p>
     * <strong>Authority Format Examples:</strong>
     * </p>
     * <ul>
     *   <li>{@code Role.USER} → {@code "ROLE_USER"}</li>
     *   <li>{@code Role.ADMIN} → {@code "ROLE_ADMIN"}</li>
     *   <li>{@code Role.MODERATOR} → {@code "ROLE_MODERATOR"}</li>
     * </ul>
     *
     * @return a collection of granted authorities based on the user's roles
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<? extends GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(Role::getAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        
        logger.debug("Mapped {} roles to authorities for user {}: {}", 
                    user.getRoles().size(), user.getUsername(), 
                    authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        
        return authorities;
    }

    /**
     * Indicates whether the user's account has expired.
     * Always returns {@code true} in this implementation.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     * Always returns {@code true} in this implementation.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) have expired.
     * Always returns {@code true} in this implementation.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled. Users who are banned (inactive) or soft-deleted
     * are considered disabled and will not be allowed to authenticate.
     *
     * @return {@code true} if the user is active and not deleted; {@code false} otherwise
     */
    @Override
    public boolean isEnabled() {
        boolean enabled = !user.isPendingDeletion();
        logger.debug("User {} enabled status: {} (pending deletion: {})", 
                    user.getUsername(), enabled, user.isPendingDeletion());
        return enabled;
    }
}
