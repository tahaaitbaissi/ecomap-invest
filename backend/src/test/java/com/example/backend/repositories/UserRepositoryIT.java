package com.example.backend.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.entities.Role;
import com.example.backend.entities.User;
import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest
@Transactional
class UserRepositoryIT extends AbstractPostgisRedisIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_and_exists() {
        User u = new User();
        u.setEmail("it-repo@example.com");
        u.setUsername("ituser");
        u.setName("IT");
        u.setPassword("p");
        u.setRole(Role.USER);
        userRepository.save(u);
        userRepository.flush();
        assertTrue(userRepository.findByEmail("it-repo@example.com").isPresent());
        assertTrue(userRepository.existsByEmail("it-repo@example.com"));
    }
}
