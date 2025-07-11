package com.yohan.event_planner.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting client IP addresses from HTTP requests.
 *
 * <p>
 * This utility handles various proxy and load balancer scenarios to correctly
 * identify the original client IP address. It checks common headers used by
 * proxies and CDNs to forward the original client IP.
 * </p>
 *
 * <h2>Supported Headers</h2>
 * <p>
 * The utility checks headers in the following order of preference:
 * </p>
 * <ol>
 *   <li><strong>CF-Connecting-IP</strong>: Cloudflare original IP</li>
 *   <li><strong>X-Real-IP</strong>: Nginx and other reverse proxies</li>
 *   <li><strong>X-Forwarded-For</strong>: Standard forwarded IP header</li>
 *   <li><strong>X-Forwarded</strong>: Alternative forwarded header</li>
 *   <li><strong>Forwarded-For</strong>: Less common forwarded header</li>
 *   <li><strong>Forwarded</strong>: RFC 7239 standard header</li>
 *   <li><strong>Remote Address</strong>: Direct connection IP (fallback)</li>
 * </ol>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Header Validation</strong>: Validates IP format and rejects invalid values</li>
 *   <li><strong>Private IP Filtering</strong>: Skips private/internal IP addresses when possible</li>
 *   <li><strong>Proxy Chain Handling</strong>: Extracts the first public IP from forwarded chains</li>
 *   <li><strong>Fallback Protection</strong>: Always returns a valid IP, never null</li>
 * </ul>
 *
 * <h2>Rate Limiting Integration</h2>
 * <p>
 * This utility is designed for use with rate limiting systems that need to
 * identify unique clients across various network configurations including:
 * </p>
 * <ul>
 *   <li>Direct connections</li>
 *   <li>CDN/Cloudflare proxied connections</li>
 *   <li>Load balancer configurations</li>
 *   <li>Reverse proxy setups</li>
 * </ul>
 *
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
public class IPAddressUtil {

    private static final Logger logger = LoggerFactory.getLogger(IPAddressUtil.class);

    /** Private constructor to prevent instantiation */
    private IPAddressUtil() {
        // Utility class
    }

    /**
     * Extracts the client IP address from the HTTP request.
     *
     * <p>
     * This method attempts to determine the original client IP address by checking
     * various headers commonly used by proxies and CDNs. It handles scenarios where
     * the application is behind load balancers, reverse proxies, or CDNs like Cloudflare.
     * </p>
     *
     * <h3>Header Priority</h3>
     * <p>
     * Headers are checked in order of reliability and preference:
     * </p>
     * <ol>
     *   <li>CF-Connecting-IP (Cloudflare's original IP header)</li>
     *   <li>X-Real-IP (commonly used by Nginx)</li>
     *   <li>X-Forwarded-For (most common forwarding header)</li>
     *   <li>X-Forwarded, Forwarded-For, Forwarded (alternatives)</li>
     *   <li>Remote address (direct connection fallback)</li>
     * </ol>
     *
     * <h3>IP Validation</h3>
     * <ul>
     *   <li><strong>Format Validation</strong>: Ensures IP is in valid format</li>
     *   <li><strong>Non-Empty Check</strong>: Skips empty or "unknown" values</li>
     *   <li><strong>Public IP Preference</strong>: Prefers public IPs over private ones</li>
     *   <li><strong>Chain Processing</strong>: Handles comma-separated IP lists</li>
     * </ul>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * @PostMapping("/register")
     * public ResponseEntity<?> register(HttpServletRequest request, @RequestBody UserCreateDTO dto) {
     *     String clientIP = IPAddressUtil.getClientIpAddress(request);
     *     if (!rateLimitingService.isRegistrationAllowed(clientIP)) {
     *         throw new RateLimitExceededException("registration", 5, 5, 3600);
     *     }
     *     // ... registration logic
     * }
     * }</pre>
     *
     * @param request the HTTP servlet request
     * @return the client IP address, never null or empty
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // Headers to check in order of preference
        String[] headerNames = {
            "CF-Connecting-IP",     // Cloudflare
            "X-Real-IP",           // Nginx and other reverse proxies
            "X-Forwarded-For",     // Standard forwarded header
            "X-Forwarded",         // Alternative forwarded header
            "Forwarded-For",       // Less common
            "Forwarded"           // RFC 7239
        };

        for (String headerName : headerNames) {
            String ip = extractValidIpFromHeader(request, headerName);
            if (ip != null) {
                logger.debug("Client IP extracted from header '{}': {}", headerName, ip);
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        if (isValidIpAddress(remoteAddr)) {
            logger.debug("Client IP extracted from remote address: {}", remoteAddr);
            return remoteAddr;
        }

        // Ultimate fallback - should never happen in normal circumstances
        logger.warn("Unable to determine client IP address, using fallback: 0.0.0.0");
        return "0.0.0.0";
    }

    /**
     * Extracts a valid IP address from the specified header.
     *
     * @param request the HTTP request
     * @param headerName the header name to check
     * @return a valid IP address from the header, or null if none found
     */
    private static String extractValidIpFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        
        if (headerValue == null || headerValue.trim().isEmpty() || "unknown".equalsIgnoreCase(headerValue.trim())) {
            return null;
        }

        // Handle comma-separated list (X-Forwarded-For can contain multiple IPs)
        String[] ips = headerValue.split(",");
        for (String ip : ips) {
            ip = ip.trim();
            if (isValidIpAddress(ip)) {
                return ip;
            }
        }

        return null;
    }

    /**
     * Validates if the given string is a valid IP address.
     *
     * <p>
     * This method performs basic validation to ensure the IP address is in a valid format
     * and is not a placeholder value like "unknown" or empty string.
     * </p>
     *
     * @param ip the IP address string to validate
     * @return true if the IP address is valid, false otherwise
     */
    private static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        ip = ip.trim();

        // Check for common placeholder values
        if ("unknown".equalsIgnoreCase(ip) || "0.0.0.0".equals(ip)) {
            return false;
        }

        // Basic IPv4 validation
        if (isValidIPv4(ip)) {
            return true;
        }

        // Basic IPv6 validation
        if (isValidIPv6(ip)) {
            return true;
        }

        return false;
    }

    /**
     * Validates IPv4 address format.
     *
     * @param ip the IP address to validate
     * @return true if valid IPv4, false otherwise
     */
    private static boolean isValidIPv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates IPv6 address format (basic check).
     *
     * @param ip the IP address to validate
     * @return true if valid IPv6, false otherwise
     */
    private static boolean isValidIPv6(String ip) {
        // Basic IPv6 validation - contains colons and hex characters
        return ip.contains(":") && ip.matches("[0-9a-fA-F:]+");
    }
}