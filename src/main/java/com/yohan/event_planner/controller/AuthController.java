package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (RuntimeException ex) {
            // Customize this error handling, maybe map exceptions to proper responses
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
