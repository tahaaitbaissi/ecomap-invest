package com.example.backend.bootstrap;

import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if not exists
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setUsername("admin");
            admin.setName("Administrator");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("=> Admin user created: admin@example.com / admin123");
        }

        // Create test user if not exists
        if (!userRepository.existsByEmail("user@example.com")) {
            User testUser = new User();
            testUser.setEmail("user@example.com");
            testUser.setUsername("testuser");
            testUser.setName("Test User");
            testUser.setPassword(passwordEncoder.encode("user123"));
            testUser.setRole(Role.USER);
            userRepository.save(testUser);
            System.out.println("=> Test user created: user@example.com / user123");
        }
    }
}
