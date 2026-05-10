package com.example.backend.testsupport;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Brings up PostGIS + Redis + ActiveMQ Artemis for @SpringBootTest. Requires Docker.
 * <p>Tests that need this should also use {@code @Tag("integration")} so the default
 * {@code mvn test} skips them. Run locally with Docker: {@code ./mvnw -f ../pom.xml verify -Pwith-integration}
 * (or {@code -Dtest.excluded.groups=} on shells that allow an empty value). GitHub Actions CI uses
 * {@code -Pwith-integration}.
 */
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractPostgisRedisIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("ecomap")
            .withUsername("ecomap")
            .withPassword("secret");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Container
    static final ArtemisContainer ARTEMIS =
            new ArtemisContainer(DockerImageName.parse("apache/activemq-artemis:2.37.0"))
                    .withUser("ecomap")
                    .withPassword("artemis-test-secret");

    @DynamicPropertySource
    static void dataSourceAndRedis(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        r.add("spring.artemis.mode", () -> "native");
        r.add("spring.artemis.broker-url", ARTEMIS::getBrokerUrl);
        r.add("spring.artemis.user", ARTEMIS::getUser);
        r.add("spring.artemis.password", ARTEMIS::getPassword);
    }
}
