package com.example.backend.services;

import com.example.backend.audit.Audited;
import com.example.backend.controllers.dto.UpdateProfileRequest;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository ur, PasswordEncoder passwordEncoder) {
        if (ur == null) {
            throw new IllegalArgumentException("User Repository cannot be null");
        }
        userRepository = ur;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Audited(action = "UPDATE_PROFILE", maskParams = {"request"})
    public User updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = getUserByEmail(currentEmail);

        boolean hasChanges = false;

        if (request.companyName() != null) {
            String newCompany = request.companyName().trim();
            if (newCompany.isEmpty()) {
                throw new IllegalArgumentException("Company name cannot be blank");
            }
            if (!newCompany.equals(user.getCompanyName())) {
                user.setCompanyName(newCompany);
                hasChanges = true;
            }
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new IllegalArgumentException("Current password is required to set a new password");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
            hasChanges = true;
        }

        if (!hasChanges) {
            throw new IllegalArgumentException("No profile changes provided");
        }

        return userRepository.save(user);
    }
}
