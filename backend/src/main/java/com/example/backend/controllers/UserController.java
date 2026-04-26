package com.example.backend.controllers;

import com.example.backend.controllers.dto.UserProfileDTO;
import com.example.backend.controllers.dto.UpdateProfileRequest;
import com.example.backend.entities.User;
import com.example.backend.services.UserService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        User user = userService.getUserByEmail(principal.getUsername());
        return ResponseEntity.ok(toUserProfileDto(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMyProfile(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        User updated = userService.updateProfile(principal.getUsername(), request);
        return ResponseEntity.ok(toUserProfileDto(updated));
    }

    private UserProfileDTO toUserProfileDto(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getCompanyName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
