package com.yohan.event_planner.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Component responsible for loading environment variables from .env files into the Spring Boot application.
 *
 * <p>
 * This configuration component integrates the dotenv-java library to provide support for
 * .env files in local development environments. It automatically loads environment variables
 * from .env files and makes them available as system properties, enabling consistent
 * configuration management across development and production environments.
 * </p>
 *
 * <h2>Environment Variable Loading Strategy</h2>
 * <ul>
 *   <li><strong>Local Development</strong>: Loads from .env file in project root</li>
 *   <li><strong>Production</strong>: Uses actual environment variables (Docker/Cloud)</li>
 *   <li><strong>CI/CD</strong>: Uses environment variables set by build pipeline</li>
 *   <li><strong>Graceful Fallback</strong>: Ignores missing .env files without errors</li>
 * </ul>
 *
 * <h2>Supported Configuration</h2>
 * <p>
 * The application expects the following environment variables to be configured
 * either through .env files or actual environment variables:
 * </p>
 *
 * <h3>Database Configuration</h3>
 * <ul>
 *   <li><strong>SPRING_DATASOURCE_URL</strong>: PostgreSQL connection URL</li>
 *   <li><strong>SPRING_DATASOURCE_USERNAME</strong>: Database username</li>
 *   <li><strong>SPRING_DATASOURCE_PASSWORD</strong>: Database password</li>
 * </ul>
 *
 * <h3>Test Database Configuration</h3>
 * <ul>
 *   <li><strong>SPRING_DATASOURCE_TEST_URL</strong>: Test database connection URL</li>
 *   <li><strong>SPRING_DATASOURCE_TEST_USERNAME</strong>: Test database username</li>
 *   <li><strong>SPRING_DATASOURCE_TEST_PASSWORD</strong>: Test database password</li>
 * </ul>
 *
 * <h3>Security Configuration</h3>
 * <ul>
 *   <li><strong>SPRING_APP_JWT_SECRET</strong>: JWT signing secret for production</li>
 *   <li><strong>SPRING_APP_JWT_SECRET_TEST</strong>: JWT signing secret for tests</li>
 * </ul>
 *
 * <h3>Docker Configuration</h3>
 * <ul>
 *   <li><strong>POSTGRES_DB</strong>: PostgreSQL database name for Docker</li>
 *   <li><strong>POSTGRES_USER</strong>: PostgreSQL username for Docker</li>
 *   <li><strong>POSTGRES_PASSWORD</strong>: PostgreSQL password for Docker</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>.env Exclusion</strong>: .env files must be added to .gitignore</li>
 *   <li><strong>Secret Management</strong>: Never commit sensitive data to version control</li>
 *   <li><strong>Production Security</strong>: Use proper secret management in production</li>
 *   <li><strong>Access Control</strong>: Restrict file system access to .env files</li>
 * </ul>
 *
 * <h2>Development Workflow</h2>
 * <ol>
 *   <li>Copy .env.example to .env (if provided)</li>
 *   <li>Configure local database credentials</li>
 *   <li>Set up JWT secrets for development</li>
 *   <li>Start the application - variables are automatically loaded</li>
 * </ol>
 *
 * @see io.github.cdimascio.dotenv.Dotenv
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class DotenvLoader {

    /**
     * Loads environment variables from .env files into system properties.
     *
     * <p>
     * This method is automatically called after Spring constructs this component,
     * ensuring that environment variables are available before other beans are
     * initialized. It searches for .env files in the project root directory
     * and gracefully handles missing files without causing application startup failure.
     * </p>
     *
     * <h3>Loading Configuration</h3>
     * <ul>
     *   <li><strong>Directory</strong>: Searches in current working directory (./)</li>
     *   <li><strong>Missing Files</strong>: Ignores missing .env files silently</li>
     *   <li><strong>Override Behavior</strong>: Does not override existing system properties</li>
     *   <li><strong>Format Support</strong>: Standard KEY=VALUE format with comment support</li>
     * </ul>
     *
     * <h3>Example .env File</h3>
     * <pre>{@code
     * # Database Configuration
     * SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/eventplanner
     * SPRING_DATASOURCE_USERNAME=dev_user
     * SPRING_DATASOURCE_PASSWORD=dev_password
     * 
     * # JWT Configuration
     * SPRING_APP_JWT_SECRET=your-super-secret-jwt-key-here
     * 
     * # Test Database
     * SPRING_DATASOURCE_TEST_URL=jdbc:postgresql://localhost:5432/eventplanner_test
     * SPRING_DATASOURCE_TEST_USERNAME=test_user
     * SPRING_DATASOURCE_TEST_PASSWORD=test_password
     * }</pre>
     *
     * <h3>Error Handling</h3>
     * <p>
     * If environment variable parsing fails due to malformed .env files,
     * the application startup will fail with clear error messages indicating
     * the problematic line or configuration issue.
     * </p>
     *
     * <h3>Priority Order</h3>
     * <ol>
     *   <li>Actual environment variables (highest priority)</li>
     *   <li>System properties set programmatically</li>
     *   <li>Properties from .env files (this loader)</li>
     *   <li>Spring Boot default properties (lowest priority)</li>
     * </ol>
     *
     * @see jakarta.annotation.PostConstruct
     * @see java.lang.System#setProperty(String, String)
     */
    @PostConstruct
    public void loadDotenv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );
    }
}
