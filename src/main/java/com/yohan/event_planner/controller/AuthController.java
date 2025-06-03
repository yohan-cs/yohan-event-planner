package com.yohan.event_planner.controller;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that handles user authentication endpoints.
 *
 * <p>
 * This controller supports login and registration via JSON payloads.
 * Authentication is delegated to Spring Security's {@link AuthenticationManager},
 * while user persistence and uniqueness checks are managed by {@link UserService}.
 * </p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    /**
     * Authenticates a user and returns a signed JWT along with basic user information.
     *
     * @param request the login credentials
     * @return a {@link LoginResponseDTO} containing the token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);
        User user = userDetails.getUser();

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTimezone()
        );

        return ResponseEntity.ok(response);
    }



    /**
     * Registers a new user account.
     *
     * @param request the registration details
     * @return HTTP 201 Created if successful; HTTP 409 Conflict if username or email already exists
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        if (userService.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (userService.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UserCreateDTO createDTO = new UserCreateDTO(
                request.username(),
                request.password(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.timezone()
        );

        userService.createUser(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
