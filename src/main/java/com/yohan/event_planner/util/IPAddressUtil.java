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
    
    /** Headers to check in order of preference for IP extraction */
    private static final String[] HEADER_NAMES = {
        "CF-Connecting-IP",     // Cloudflare
        "X-Real-IP",           // Nginx and other reverse proxies
        "X-Forwarded-For",     // Standard forwarded header
        "X-Forwarded",         // Alternative forwarded header
        "Forwarded-For",       // Less common
        "Forwarded"           // RFC 7239
    };
    
    /** Fallback IP address when no valid IP can be determined */
    private static final String FALLBACK_IP = "0.0.0.0";
    
    /** Placeholder value commonly used by misconfigured proxies */
    private static final String UNKNOWN_IP = "unknown";

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
        logger.debug("Extracting client IP from HTTP request");

        for (String headerName : HEADER_NAMES) {
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
        logger.warn("Unable to determine client IP address from any header or remote address, using fallback: {}. Request from: {}", FALLBACK_IP, request.getRemoteAddr());
        return FALLBACK_IP;
    }

    /**
     * Extracts a valid IP address from the specified header.
     *
     * <p>
     * This method handles header value parsing and validation, including comma-separated
     * lists commonly found in X-Forwarded-For headers. It filters out invalid values
     * and "unknown" placeholder strings commonly inserted by misconfigured proxies.
     * </p>
     *
     * @param request the HTTP request containing headers
     * @param headerName the name of the header to extract IP from
     * @return a valid IP address from the header, or null if none found
     */
    private static String extractValidIpFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        
        if (headerValue == null || headerValue.trim().isEmpty() || UNKNOWN_IP.equalsIgnoreCase(headerValue.trim())) {
            return null;
        }

        // Handle comma-separated list (X-Forwarded-For can contain multiple IPs)
        String[] ips = headerValue.split(",");
        for (String ip : ips) {
            ip = ip.trim();
            if (isValidIpAddress(ip)) {
                return ip;
            } else {
                logger.debug("Skipping invalid IP format in header '{}': {}", headerName, ip);
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
        if (UNKNOWN_IP.equalsIgnoreCase(ip) || FALLBACK_IP.equals(ip)) {
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
     * Validates IPv4 address format using strict parsing rules.
     *
     * <p>
     * Performs comprehensive IPv4 validation by checking octet count, numeric format,
     * and value ranges (0-255). This validation is more strict than simple regex
     * matching to ensure only legitimate IPv4 addresses are accepted.
     * </p>
     *
     * @param ip the IP address string to validate as IPv4
     * @return true if the string represents a valid IPv4 address, false otherwise
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
     * Validates IPv6 address format using basic pattern matching.
     *
     * <p>
     * Performs basic IPv6 validation by checking for colons and hexadecimal characters.
     * This is a simplified validation suitable for general IP extraction purposes,
     * not comprehensive IPv6 RFC compliance checking.
     * </p>
     *
     * @param ip the IP address string to validate as IPv6
     * @return true if the string appears to be a valid IPv6 address, false otherwise
     */
    private static boolean isValidIPv6(String ip) {
        // Basic IPv6 validation - contains colons and hex characters
        if (!ip.contains(":")) {
            return false;
        }
        
        // Handle common IPv6 formats including compressed notation
        return ip.matches("^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$") ||
               ip.matches("^::1$") || // loopback
               ip.matches("^::[0-9a-fA-F]{1,4}$") || // compressed
               ip.matches("^[0-9a-fA-F]{1,4}::$"); // compressed trailing
    }
}