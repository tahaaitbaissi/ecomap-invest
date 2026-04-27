package com.example.backend.bootstrap;

import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.repositories.PoiRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.services.CsvIngestionService;
import java.sql.Timestamp;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoiRepository poiRepository;
    private final CsvIngestionService csvIngestionService;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           PoiRepository poiRepository,
                           CsvIngestionService csvIngestionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.poiRepository = poiRepository;
        this.csvIngestionService = csvIngestionService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create admin user if not exists
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ROLE_ADMIN.name());
            admin.setCompanyName("EcoMap Admin");
            admin.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(admin);
            System.out.println("=> Admin user created: admin@example.com / admin123");
        }

        // Create test user if not exists
        if (!userRepository.existsByEmail("user@example.com")) {
            User testUser = new User();
            testUser.setEmail("user@example.com");
            testUser.setPassword(passwordEncoder.encode("user123"));
            testUser.setRole(Role.ROLE_INVESTOR.name());
            testUser.setCompanyName("Test Company");
            testUser.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(testUser);
            System.out.println("=> Test user created: user@example.com / user123");
        }

        if (poiRepository.count() == 0) {
            int inserted = csvIngestionService.ingestFromCsv("data/mock_poi.csv");
            System.out.println("=> POIs seeded from CSV: " + inserted);
        }
    }
}
