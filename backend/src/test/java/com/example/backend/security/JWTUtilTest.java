package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = JWTUtil.class)
@TestPropertySource(
        properties = {
            "app.security.jwt.secret=TestJwtSecretKeyForUnitAndIntegrationTestsMustBe256BitLongString!!",
            "app.security.jwt.expiration=3600000"
        })
class JWTUtilTest {

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    void generateAndValidate_roundTrip() {
        String token = jwtUtil.generateToken("user@example.com");
        assertTrue(jwtUtil.validateToken(token, "user@example.com"));
        assertEquals("user@example.com", jwtUtil.getUsernameFromToken(token));
    }

    @Test
    void validate_failsOnWrongSubject() {
        String token = jwtUtil.generateToken("a@a.com");
        assertFalse(jwtUtil.validateToken(token, "b@b.com"));
    }

    @Test
    void validate_failsOnGarbage() {
        assertFalse(jwtUtil.validateToken("not.a.jwt", "user@example.com"));
    }
}
