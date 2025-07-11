package com.yohan.event_planner.dto.auth;

/**
 * Response DTO returned upon successful user registration with automatic login.
 *
 * <p>
 * This DTO provides both registration confirmation and immediate authentication tokens,
 * enabling a seamless user experience where newly registered users are automatically
 * logged in without requiring a separate login step. This approach aligns with modern
 * application UX patterns and reduces client-side complexity.
 * </p>
 *
 * <h2>Auto-Login Benefits</h2>
 * <ul>
 *   <li><strong>Seamless UX</strong>: Users immediately access the application after registration</li>
 *   <li><strong>Reduced Friction</strong>: Eliminates the need for a separate login step</li>
 *   <li><strong>Consistent Flow</strong>: Registration and login return similar token structures</li>
 *   <li><strong>Modern Pattern</strong>: Follows contemporary application registration patterns</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Fresh Tokens</strong>: Generates brand new authentication tokens for the user</li>
 *   <li><strong>Standard Security</strong>: Same security model as regular login tokens</li>
 *   <li><strong>Token Rotation</strong>: Refresh tokens follow the same rotation strategy</li>
 *   <li><strong>Session Management</strong>: Full session management capabilities from registration</li>
 * </ul>
 *
 * @param token JWT access token for immediate API authentication
 * @param refreshToken opaque refresh token for session renewal
 * @param userId unique identifier of the newly created user
 * @param username the registered username
 * @param email the registered email address
 * @param timezone the user's preferred timezone
 * @param message confirmation message about successful registration and login
 *
 * @see LoginResponseDTO
 * @see com.yohan.event_planner.dto.UserCreateDTO
 * @see com.yohan.event_planner.service.AuthService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 2.0.0
 */
public record RegisterResponseDTO(
        
        /** JWT access token for immediate authentication. */
        String token,
        
        /** Opaque refresh token for session management. */
        String refreshToken,
        
        /** Unique ID of the newly created user. */
        Long userId,
        
        /** Registered username. */
        String username,
        
        /** Registered email address. */
        String email,
        
        /** User's preferred time zone. */
        String timezone,
        
        /** Confirmation message about successful registration. */
        String message
) {}