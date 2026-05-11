package com.pmei.auth.controller;

import com.pmei.auth.dto.LoginRequest;
import com.pmei.auth.dto.LoginResponse;
import com.pmei.auth.dto.RegisterRequest;
import com.pmei.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for authentication operations.
 *
 * Provides endpoints for:
 * - User and company registration
 * - User login and JWT token generation
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new company and its admin user.
     *
     * @param request Registration data
     * @return success message
     */
    @Operation(summary = "Register a new company and admin user")
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);
        return ResponseEntity.ok("Successfully registered company.");
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request Login credentials
     * @return JWT token response
     */
    @Operation(summary = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }
}