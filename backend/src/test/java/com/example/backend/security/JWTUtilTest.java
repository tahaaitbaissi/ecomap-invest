package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = JwtService.class)
@TestPropertySource(
        properties = {
            "jwt.secret=TestJwtSecretKeyForUnitAndIntegrationTestsMustBe256BitLongString!!",
            "jwt.expiration=3600000"
        })
class JWTUtilTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateAndValidate_roundTrip() {
        var ud = org.springframework.security.core.userdetails.User.withUsername("user@example.com")
                .password("x")
                .authorities("ROLE_INVESTOR")
                .build();
        String token = jwtService.generateToken(ud);
        assertTrue(jwtService.validateToken(token, ud));
        assertEquals("user@example.com", jwtService.extractUsername(token));
    }

    @Test
    void validate_failsOnWrongSubject() {
        var udA = org.springframework.security.core.userdetails.User.withUsername("a@a.com")
                .password("x")
                .authorities("ROLE_INVESTOR")
                .build();
        var udB = org.springframework.security.core.userdetails.User.withUsername("b@b.com")
                .password("x")
                .authorities("ROLE_INVESTOR")
                .build();
        String token = jwtService.generateToken(udA);
        assertFalse(jwtService.validateToken(token, udB));
    }

    @Test
    void validate_failsOnGarbage() {
        var ud = org.springframework.security.core.userdetails.User.withUsername("user@example.com")
                .password("x")
                .authorities("ROLE_INVESTOR")
                .build();
        assertFalse(jwtService.validateToken("not.a.jwt", ud));
    }
}
