package com.example.backend.controllers;

import com.example.backend.controllers.dto.AuthResponse;
import com.example.backend.controllers.dto.LoginRequest;
import com.example.backend.controllers.dto.RegisterRequest;
import com.example.backend.services.AuthService;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JWTUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,
                          JWTUtil jwtUtil,
                          UserRepository userRepository,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse out = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @PostMapping("/login")
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
        String token = jwtUtil.generateToken(request.email());
        long expiresInSeconds = 86400L;
        return ResponseEntity.ok(new AuthResponse(token, expiresInSeconds, user.getEmail(), user.getRole()));
    }
}
