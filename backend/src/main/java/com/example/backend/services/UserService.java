package com.example.backend.services;

import com.example.backend.controllers.dto.UpdateProfileRequest;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserRepository userRepository;

    @Autowired
    public UserService(UserRepository ur) {
        if (ur == null)
            throw new IllegalArgumentException("User Repository cannot be null");
        userRepository = ur;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public User updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = getUserByEmail(currentEmail);

        boolean hasChanges = false;

        if (request.username() != null) {
            String newUsername = request.username().trim();
            if (newUsername.isEmpty()) {
                throw new IllegalArgumentException("Username cannot be blank");
            }
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new IllegalStateException("Username already used");
                }
                user.setUsername(newUsername);
                hasChanges = true;
            }
        }

        if (request.email() != null) {
            String newEmail = request.email().trim();
            if (newEmail.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be blank");
            }
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new IllegalStateException("Email already used");
                }
                user.setEmail(newEmail);
                hasChanges = true;
            }
        }

        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
            if (!newName.equals(user.getName())) {
                user.setName(newName);
                hasChanges = true;
            }
        }

        if (!hasChanges) {
            throw new IllegalArgumentException("No profile changes provided");
        }

        return userRepository.save(user);
    }
}
