package com.example.backend.controllers;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.backend.controllers.dto.LoginRequest;
import com.example.backend.controllers.dto.RegisterRequest;
import com.example.backend.services.AuthService;
import com.example.backend.services.LoginService;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Register and login (JWT issuance)")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginService loginService;
    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginService loginService,
            UserRepository userRepository,
            AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginService = loginService;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Operation(summary = "Register a new investor account")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse out = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @Operation(summary = "Login and obtain JWT")
    @PostMapping("/login")
    @Audited(action = "LOGIN", maskParams = {"request"})
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Invalid credentials"));
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        UserDetails userDetails = loginService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        long expiresInSeconds = 86400L;
        return ResponseEntity.ok(new AuthResponse(token, expiresInSeconds, user.getEmail(), user.getRole()));
    }
}
