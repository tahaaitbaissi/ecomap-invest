package com.example.backend;

import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full application smoke: Flyway, PostGIS, Redis, JPA, Batch metadata. Needs Docker
 * (see parent {@link AbstractPostgisRedisIntegrationTest}). Excluded from default
 * {@code mvn test}; included in CI via {@code -Pwith-integration}.
 */
@SpringBootTest
@Tag("integration")
class BackendApplicationTests extends AbstractPostgisRedisIntegrationTest {

	@Test
	void contextLoads() {
	}
}
