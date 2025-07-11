package com.yohan.event_planner.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI 3.0 documentation and Swagger UI integration.
 *
 * <p>
 * This configuration provides comprehensive API documentation for the Event Planner application
 * using OpenAPI 3.0 specification. It defines the overall API structure, authentication schemes,
 * server configurations, and categorizes endpoints with meaningful tags for better organization.
 * </p>
 *
 * <h2>Documentation Features</h2>
 * <ul>
 *   <li><strong>Interactive Documentation</strong>: Full Swagger UI with request/response examples</li>
 *   <li><strong>Authentication Testing</strong>: Built-in JWT token testing capability</li>
 *   <li><strong>Schema Validation</strong>: Automatic request/response validation</li>
 *   <li><strong>API Versioning</strong>: Clear version management and compatibility tracking</li>
 * </ul>
 *
 * <h2>Security Documentation</h2>
 * <p>
 * The configuration includes JWT Bearer token authentication documentation,
 * enabling developers to easily test authenticated endpoints through the Swagger UI.
 * The security scheme is automatically applied to all protected endpoints.
 * </p>
 *
 * <h2>Endpoint Organization</h2>
 * <p>
 * API endpoints are organized into logical tags that correspond to different
 * functional areas of the application, making it easy for developers to navigate
 * and understand the available functionality.
 * </p>
 *
 * <h3>Available Endpoint Categories</h3>
 * <ul>
 *   <li><strong>Authentication</strong>: Login, registration, token management</li>
 *   <li><strong>Events</strong>: Core event CRUD operations and management</li>
 *   <li><strong>Recurring Events</strong>: Complex recurrence pattern management</li>
 *   <li><strong>Calendar</strong>: Calendar views and visual analytics</li>
 *   <li><strong>My Events</strong>: Personal event management and drafts</li>
 *   <li><strong>Search</strong>: Advanced filtering and search capabilities</li>
 *   <li><strong>Labels</strong>: Event categorization and organization</li>
 *   <li><strong>Badges</strong>: Time analytics and achievement tracking</li>
 *   <li><strong>User Management</strong>: Profile and account operations</li>
 *   <li><strong>User Tools</strong>: Utility endpoints and aggregated data</li>
 * </ul>
 *
 * <h2>Access Information</h2>
 * <ul>
 *   <li><strong>Swagger UI</strong>: http://localhost:8080/swagger-ui/index.html</li>
 *   <li><strong>OpenAPI JSON</strong>: http://localhost:8080/v3/api-docs</li>
 *   <li><strong>OpenAPI YAML</strong>: http://localhost:8080/v3/api-docs.yaml</li>
 * </ul>
 *
 * <h2>Integration Benefits</h2>
 * <ul>
 *   <li><strong>Frontend Development</strong>: Auto-generated client SDKs</li>
 *   <li><strong>API Testing</strong>: Built-in testing tools and validation</li>
 *   <li><strong>Documentation Maintenance</strong>: Self-updating documentation</li>
 *   <li><strong>Team Collaboration</strong>: Shared understanding of API contracts</li>
 * </ul>
 *
 * @see io.swagger.v3.oas.annotations.OpenAPIDefinition
 * @see io.swagger.v3.oas.annotations.security.SecurityScheme
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Event Planner API",
                version = "2.0.0",
                description = """
                        A comprehensive event management system with advanced scheduling capabilities.
                        
                        ## Key Features
                        - **Advanced Event Management**: Scheduled, impromptu, untimed, and draft events
                        - **Recurring Events**: Complex patterns with infinite recurrence support
                        - **Multi-timezone Support**: Events stored in UTC with timezone metadata
                        - **Time Analytics**: Badge-based time tracking with comprehensive statistics
                        - **Media Management**: Event recap attachments with ordering
                        - **Calendar Analytics**: Visual productivity tracking with label-specific insights
                        - **Dual Authentication**: JWT access tokens with refresh token rotation
                        
                        ## Authentication
                        Most endpoints require authentication using JWT Bearer tokens. Use the `/auth/login` endpoint to obtain tokens.
                        
                        ## Rate Limiting
                        API requests are subject to rate limiting. Check response headers for current limits.
                        """,
                contact = @Contact(
                        name = "Event Planner API Team",
                        email = "api-support@eventplanner.com",
                        url = "https://eventplanner.com/support"
                ),
                license = @License(
                        name = "All Rights Reserved",
                        url = "https://eventplanner.com/license"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Development Server"),
                @Server(url = "https://api.eventplanner.com", description = "Production Server")
        },
        tags = {
                @Tag(name = "Authentication", description = "User authentication and token management"),
                @Tag(name = "Events", description = "Core event management operations"),
                @Tag(name = "Recurring Events", description = "Recurring event patterns and management"),
                @Tag(name = "Calendar", description = "Calendar views and analytics"),
                @Tag(name = "My Events", description = "Personalized event views and draft management"),
                @Tag(name = "Search", description = "Advanced search and filtering capabilities"),
                @Tag(name = "Labels", description = "Event categorization and organization"),
                @Tag(name = "Badges", description = "Multi-label collections and time analytics"),
                @Tag(name = "User Management", description = "User profile and account management"),
                @Tag(name = "User Tools", description = "User utilities and aggregated data")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token authentication. Obtain tokens via /auth/login endpoint."
)
public class OpenApiConfig {
    
    /**
     * This configuration class is annotation-driven and does not require bean definitions.
     * 
     * <p>
     * The OpenAPI specification is entirely configured through annotations above,
     * which are automatically processed by the springdoc-openapi library to generate
     * the complete API documentation and Swagger UI interface.
     * </p>
     * 
     * <h3>Automatic Processing</h3>
     * <ul>
     *   <li><strong>API Discovery</strong>: Scans all controller endpoints automatically</li>
     *   <li><strong>Schema Generation</strong>: Creates JSON schemas from DTO classes</li>
     *   <li><strong>Security Integration</strong>: Applies authentication to protected endpoints</li>
     *   <li><strong>UI Generation</strong>: Builds interactive Swagger UI from specification</li>
     * </ul>
     */
}
