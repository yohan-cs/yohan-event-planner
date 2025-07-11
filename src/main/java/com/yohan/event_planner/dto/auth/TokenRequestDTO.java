package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Shared request DTO for operations requiring a token parameter.
 *
 * <p>
 * This DTO is used across multiple authentication endpoints that require a token,
 * eliminating the need for separate, identical DTOs for each operation. It provides
 * a unified approach to token-based requests with consistent validation.
 * </p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Token Refresh</strong>: Refresh access tokens using refresh tokens</li>
 *   <li><strong>Logout</strong>: Revoke refresh tokens during logout</li>
 *   <li><strong>Future Token Operations</strong>: Extensible for additional token-based endpoints</li>
 * </ul>
 *
 * <h2>Design Benefits</h2>
 * <ul>
 *   <li><strong>DRY Compliance</strong>: Single DTO for all token-based requests</li>
 *   <li><strong>Consistent Validation</strong>: Uniform validation across token operations</li>
 *   <li><strong>Reduced Maintenance</strong>: Fewer DTOs to maintain and update</li>
 *   <li><strong>Extensible Design</strong>: Easy to add new token-based operations</li>
 * </ul>
 *
 * <h2>Validation</h2>
 * <ul>
 *   <li><strong>Required Token</strong>: Token field is mandatory and cannot be blank</li>
 *   <li><strong>Format Agnostic</strong>: Works with any token format (JWT, opaque, etc.)</li>
 *   <li><strong>Security Aware</strong>: Validation helps prevent empty token attacks</li>
 * </ul>
 *
 * @param token the token value required for the operation (refresh token, access token, etc.)
 *
 * @see com.yohan.event_planner.service.AuthService
 * @see RefreshTokenResponseDTO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 2.0.0
 */
public record TokenRequestDTO(

        /** The token required for the operation. */
        @NotBlank(message = "Token is required")
        String token

) {}