package com.yohan.event_planner.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IPAddressUtil}.
 *
 * <p>
 * This test suite verifies the IP address extraction functionality across various
 * network configurations and edge cases, ensuring proper security and reliability
 * for rate limiting operations.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class IPAddressUtilTest {

    @Mock
    private HttpServletRequest request;

    @Nested
    @DisplayName("getClientIpAddress")
    class GetClientIpAddressTests {

        @Test
        @DisplayName("should extract IP from CF-Connecting-IP header with highest priority")
        void shouldExtractFromCloudflareHeader() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should extract IP from X-Real-IP when CF-Connecting-IP not available")
        void shouldExtractFromXRealIpHeader() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.2");
        }

        @Test
        @DisplayName("should extract first valid IP from X-Forwarded-For with multiple IPs")
        void shouldExtractFromXForwardedForMultiple() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1, 192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.1");
        }

        @Test
        @DisplayName("should extract IP from X-Forwarded header when others unavailable")
        void shouldExtractFromXForwardedHeader() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn("203.0.113.4");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.4");
        }

        @Test
        @DisplayName("should extract IP from Forwarded-For header")
        void shouldExtractFromForwardedForHeader() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn("203.0.113.5");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.5");
        }

        @Test
        @DisplayName("should extract IP from Forwarded header")
        void shouldExtractFromForwardedHeader() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn("203.0.113.6");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.6");
        }

        @Test
        @DisplayName("should fallback to remote address when headers empty")
        void shouldFallbackToRemoteAddress() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("203.0.113.7");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.7");
        }

        @Test
        @DisplayName("should return fallback IP when all sources invalid")
        void shouldReturnFallbackWhenAllInvalid() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn("");
            when(request.getHeader("X-Forwarded-For")).thenReturn("invalid-ip");
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("0.0.0.0");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("0.0.0.0");
        }

        @Test
        @DisplayName("should skip unknown and empty header values")
        void shouldSkipInvalidHeaderValues() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn("  ");
            when(request.getHeader("X-Forwarded-For")).thenReturn("UNKNOWN, 203.0.113.8");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.8");
        }

        @Test
        @DisplayName("should handle IPv6 addresses")
        void shouldHandleIPv6Addresses() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn("2001:db8::1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("2001:db8::1");
        }

        @Test
        @DisplayName("should skip malformed IPs in comma-separated list")
        void shouldSkipMalformedIpsInList() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("999.999.999.999, invalid, 203.0.113.9");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("203.0.113.9");
        }
    }

    @Nested
    @DisplayName("IPv4 Validation")
    class IPv4ValidationTests {

        @Test
        @DisplayName("should validate correct IPv4 addresses")
        void shouldValidateCorrectIPv4() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.1");
            String result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("192.168.1.1");

            when(request.getHeader("X-Real-IP")).thenReturn("0.0.0.1");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("0.0.0.1");

            when(request.getHeader("X-Real-IP")).thenReturn("255.255.255.255");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("255.255.255.255");
        }

        @Test
        @DisplayName("should reject invalid IPv4 addresses")
        void shouldRejectInvalidIPv4() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("256.1.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            String result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("192.168.1.1");

            when(request.getHeader("X-Real-IP")).thenReturn("192.168.1");
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("192.168.1.2");

            when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.1.1");
            when(request.getRemoteAddr()).thenReturn("192.168.1.3");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("192.168.1.3");
        }

        @Test
        @DisplayName("should reject negative numbers in IPv4")
        void shouldRejectNegativeNumbersInIPv4() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("-1.1.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should reject non-numeric IPv4 octets")
        void shouldRejectNonNumericIPv4Octets() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("192.abc.1.1");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }
    }

    @Nested
    @DisplayName("IPv6 Validation")
    class IPv6ValidationTests {

        @Test
        @DisplayName("should validate correct IPv6 addresses")
        void shouldValidateCorrectIPv6() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("2001:db8:85a3::8a2e:370:7334");
            String result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("2001:db8:85a3::8a2e:370:7334");

            when(request.getHeader("X-Real-IP")).thenReturn("::1");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("::1");

            when(request.getHeader("X-Real-IP")).thenReturn("2001:db8::1");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("2001:db8::1");
        }

        @Test
        @DisplayName("should reject invalid IPv6 addresses")
        void shouldRejectInvalidIPv6() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("not:an:ipv6");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should reject IPv6 without colons")
        void shouldRejectIPv6WithoutColons() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("2001db885a3");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityTests {

        @Test
        @DisplayName("should handle null header values safely")
        void shouldHandleNullHeaderValuesSafely() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should handle whitespace-only header values")
        void shouldHandleWhitespaceOnlyHeaderValues() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should handle mixed case unknown values")
        void shouldHandleMixedCaseUnknownValues() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("UNKNOWN");
            when(request.getHeader("X-Forwarded-For")).thenReturn("Unknown");
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should reject 0.0.0.0 IP addresses")
        void shouldRejectZeroIpAddresses() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("0.0.0.0");
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should handle very long header values")
        void shouldHandleVeryLongHeaderValues() {
            String longHeader = "a".repeat(1000) + ", 192.168.1.1";
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(longHeader);

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should handle empty string in comma-separated list")
        void shouldHandleEmptyStringInCommaSeparatedList() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(", , 192.168.1.1");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Testing")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle single digit IPv4 octets")
        void shouldHandleSingleDigitIPv4Octets() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("1.2.3.4");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("should handle boundary IPv4 values")
        void shouldHandleBoundaryIPv4Values() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("0.0.0.0");
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Forwarded")).thenReturn(null);
            when(request.getHeader("Forwarded-For")).thenReturn(null);
            when(request.getHeader("Forwarded")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            String result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("192.168.1.1"); // 0.0.0.0 should be rejected

            when(request.getHeader("X-Real-IP")).thenReturn("255.255.255.255");
            result = IPAddressUtil.getClientIpAddress(request);
            assertThat(result).isEqualTo("255.255.255.255");
        }

        @Test
        @DisplayName("should trim whitespace from IP addresses")
        void shouldTrimWhitespaceFromIpAddresses() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("  192.168.1.1  ");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }

        @Test
        @DisplayName("should handle comma-separated list with whitespace")
        void shouldHandleCommaSeparatedListWithWhitespace() {
            when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1 , 10.0.0.1 ,  203.0.113.1  ");

            String result = IPAddressUtil.getClientIpAddress(request);

            assertThat(result).isEqualTo("192.168.1.1");
        }
    }
}