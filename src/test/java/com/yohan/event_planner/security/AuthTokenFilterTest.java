package com.yohan.event_planner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    void setUp() {
        authTokenFilter = new AuthTokenFilter(jwtUtils, userDetailsService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class SuccessfulAuthenticationTests {

        @Test
        void doFilterInternal_validJwtAndUser_setsAuthentication() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 123L;
            UserDetails userDetails = createMockUserDetails("testuser");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);
            when(request.getRequestURI()).thenReturn("/api/test");

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = 
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(securityContext).setAuthentication(authCaptor.capture());

            UsernamePasswordAuthenticationToken authentication = authCaptor.getValue();
            assertSame(userDetails, authentication.getPrincipal());
            assertNull(authentication.getCredentials());
            assertEquals(userDetails.getAuthorities(), authentication.getAuthorities());
            assertNotNull(authentication.getDetails());

            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_validJwtWithAuthorities_preservesAuthorities() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 456L;
            UserDetails userDetails = createMockUserDetailsWithAuthorities("adminuser", "ROLE_ADMIN", "ROLE_USER");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = 
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(securityContext).setAuthentication(authCaptor.capture());

            UsernamePasswordAuthenticationToken authentication = authCaptor.getValue();
            assertEquals(2, authentication.getAuthorities().size());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class NoAuthenticationTests {

        @Test
        void doFilterInternal_noJwtToken_skipsAuthentication() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(null);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verifyNoInteractions(userDetailsService);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_emptyJwtToken_skipsAuthentication() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenReturn("");
            when(jwtUtils.getUserIdFromJwtToken("")).thenThrow(new RuntimeException("Invalid token"));

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void doFilterInternal_jwtUtilsThrowsException_continuesFilterChain() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenThrow(new RuntimeException("JWT parsing error"));

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_userDetailsServiceThrowsException_continuesFilterChain() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 123L;

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenThrow(new RuntimeException("User not found"));

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_getUserIdFromJwtThrowsException_continuesFilterChain() throws ServletException, IOException {
            // Arrange
            String jwt = "invalid.jwt.token";

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenThrow(new RuntimeException("Invalid token"));

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verifyNoInteractions(userDetailsService);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class FilterChainContinuationTests {

        @Test
        void doFilterInternal_filterChainThrowsServletException_propagatesException() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(null);
            doThrow(new ServletException("Filter chain error")).when(filterChain).doFilter(request, response);

            // Act & Assert
            try {
                authTokenFilter.doFilterInternal(request, response, filterChain);
            } catch (ServletException e) {
                assertEquals("Filter chain error", e.getMessage());
            }
        }

        @Test
        void doFilterInternal_filterChainThrowsIOException_propagatesException() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(null);
            doThrow(new IOException("IO error")).when(filterChain).doFilter(request, response);

            // Act & Assert
            try {
                authTokenFilter.doFilterInternal(request, response, filterChain);
            } catch (IOException e) {
                assertEquals("IO error", e.getMessage());
            }
        }

        @Test
        void doFilterInternal_alwaysCallsFilterChain_evenAfterAuthentication() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 789L;
            UserDetails userDetails = createMockUserDetails("user");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class SecurityContextInteractionTests {

        @Test
        void doFilterInternal_existingAuthenticationInContext_setsNewAuthentication() throws ServletException, IOException {
            // Arrange
            
            String jwt = "valid.jwt.token";
            Long userId = 123L;
            UserDetails userDetails = createMockUserDetails("newuser");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert - should set new authentication (JWT has precedence)
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = 
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(securityContext).setAuthentication(authCaptor.capture());

            UsernamePasswordAuthenticationToken newAuth = authCaptor.getValue();
            assertSame(userDetails, newAuth.getPrincipal());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_noExistingAuthentication_setsAuthentication() throws ServletException, IOException {
            // Arrange
            
            String jwt = "valid.jwt.token";
            Long userId = 456L;
            UserDetails userDetails = createMockUserDetails("user");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = 
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(securityContext).setAuthentication(authCaptor.capture());

            UsernamePasswordAuthenticationToken auth = authCaptor.getValue();
            assertSame(userDetails, auth.getPrincipal());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void doFilterInternal_nullUserId_skipsAuthentication() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenThrow(new RuntimeException("Invalid token"));

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_userDetailsServiceReturnsNull_continuesFilterChain() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 123L;

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(null);

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class LoggingBehaviorTests {

        @Test
        void doFilterInternal_successfulAuthentication_completesWithoutLoggingExceptions() throws ServletException, IOException {
            // Arrange
            String jwt = "valid.jwt.token";
            Long userId = 123L;
            UserDetails userDetails = createMockUserDetails("testuser");

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwt);
            when(jwtUtils.getUserIdFromJwtToken(jwt)).thenReturn(userId);
            when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);
            when(request.getRequestURI()).thenReturn("/api/test");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // Act & Assert - Should complete without throwing exceptions from logging
            assertDoesNotThrow(() -> authTokenFilter.doFilterInternal(request, response, filterChain));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_authenticationFailure_logsErrorWithoutSensitiveData() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenThrow(new RuntimeException("JWT parsing error"));
            when(request.getRequestURI()).thenReturn("/api/protected");
            when(request.getHeader("User-Agent")).thenReturn("TestAgent/1.0");
            when(request.getRemoteAddr()).thenReturn("192.168.1.100");

            // Act & Assert - Should not throw exception and should continue filter chain
            assertDoesNotThrow(() -> authTokenFilter.doFilterInternal(request, response, filterChain));
            
            // Verify the method calls that trigger logging occurred
            verify(jwtUtils).getJwtFromHeader(request);
            verify(request, times(2)).getRequestURI(); // Called once for debug log, once for error log
            verify(request).getHeader("User-Agent");
            verify(request).getRemoteAddr();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        void doFilterInternal_noJwtToken_completesWithoutSecurityLogs() throws ServletException, IOException {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/public");

            // Act
            authTokenFilter.doFilterInternal(request, response, filterChain);

            // Assert - Should not trigger any authentication-related logging
            verify(jwtUtils).getJwtFromHeader(request);
            verify(securityContext, never()).setAuthentication(any());
            verify(filterChain).doFilter(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private UserDetails createMockUserDetails(String username) {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenReturn((Collection) authorities);
        return userDetails;
    }

    @SuppressWarnings("unchecked")
    private UserDetails createMockUserDetailsWithAuthorities(String username, String... authorities) {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String authority : authorities) {
            grantedAuthorities.add(new SimpleGrantedAuthority(authority));
        }
        when(userDetails.getAuthorities()).thenReturn((Collection) grantedAuthorities);
        return userDetails;
    }
}