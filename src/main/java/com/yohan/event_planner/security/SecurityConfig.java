package com.yohan.event_planner.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for stateless JWT-based authentication.
 *
 * <p>
 * This configuration:
 * <ul>
 *     <li>Disables form login and HTTP Basic authentication</li>
 *     <li>Disables CSRF protection entirely (safe for stateless JWT APIs)</li>
 *     <li>Allows unauthenticated access to {@code /auth/**} endpoints</li>
 *     <li>Requires authentication for all other requests</li>
 *     <li>Registers a custom JWT filter before the default username/password authentication filter</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(AuthTokenFilter authTokenFilter,
                          AuthEntryPointJwt authEntryPointJwt,
                          UserDetailsServiceImpl userDetailsService) {
        this.authTokenFilter = authTokenFilter;
        this.authEntryPointJwt = authEntryPointJwt;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Defines the security filter chain:
     * <ul>
     *     <li>CSRF disabled for stateless API</li>
     *     <li>JWT filter added before username/password filter</li>
     *     <li>{@code /auth/**} is publicly accessible</li>
     *     <li>All other endpoints require authentication</li>
     * </ul>
     *
     * @param http the HTTP security builder
     * @return the configured filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(requests ->
                requests.requestMatchers("/auth/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
        );

        http.exceptionHandling(exception ->
                exception.authenticationEntryPoint(authEntryPointJwt)
        );

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides the authentication manager bean used for login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder used for password hashing and verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
