package com.example.backend.services;

import com.example.backend.controllers.dto.AuthResponse;
import com.example.backend.controllers.dto.RegisterRequest;
import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.exceptions.EmailAlreadyExistsException;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JWTUtil;
import java.sql.Timestamp;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public AuthService(UserRepository userRepository, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(encoder.encode(request.password()));
        user.setRole(Role.ROLE_INVESTOR.name());
        user.setCompanyName(request.companyName());
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        long expiresInSeconds = 86400L;
        return new AuthResponse(token, expiresInSeconds, user.getEmail(), user.getRole());
    }
}

