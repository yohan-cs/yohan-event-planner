package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.exception.UserNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserDetailsService} used by Spring Security to retrieve user information
 * during the authentication process.
 *
 * <p>
 * This class delegates to the {@link UserBO} to fetch a {@link com.yohan.event_planner.domain.User}
 * by username and wraps it in a {@link CustomUserDetails} object compatible with Spring Security.
 * </p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserBO userBO;

    /**
     * Constructs the user details service with the given business layer dependency.
     *
     * @param userBO the business object responsible for user retrieval
     */
    public UserDetailsServiceImpl(UserBO userBO) {
        this.userBO = userBO;
    }

    /**
     * Loads a user by their username and converts the domain user into Spring Security's
     * {@link UserDetails} representation.
     *
     * @param username the username identifying the user
     * @return a {@link CustomUserDetails} wrapping the domain user
     * @throws UserNotFoundException if no user with the given username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        return userBO.getUserByUsername(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
