package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom implementation of {@link UserDetails} that wraps the application's {@link User} domain object.
 *
 * <p>
 * This class is used by Spring Security to perform authentication and authorization.
 * It provides user credentials and authorities (roles) as required by the framework.
 * </p>
 *
 * <p>
 * All account-related flags (non-expired, non-locked, credentials non-expired, enabled)
 * are currently hardcoded for simplicity, except {@code isEnabled()}, which delegates to
 * the {@code User#isActive()} flag.
 * </p>
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Constructs a {@code CustomUserDetails} instance by wrapping a domain {@code User}.
     *
     * @param user the user entity from the domain layer
     */
    public CustomUserDetails(User user) {
        this.user = user;
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
     * Maps each {@link Role} to a {@link SimpleGrantedAuthority} using its string representation.
     *
     * @return a collection of granted authorities (e.g., ROLE_USER, ROLE_ADMIN)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles()
                .stream()
                .map(Role::getAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
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
        return !user.isPendingDeletion();
    }
}
