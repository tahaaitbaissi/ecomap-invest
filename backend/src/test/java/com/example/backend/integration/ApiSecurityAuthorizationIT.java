package com.example.backend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.backend.BackendApplication;
import com.example.backend.controllers.dto.AuthResponse;
import com.example.backend.testsupport.AbstractPostgisRedisIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = BackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
class ApiSecurityAuthorizationIT extends AbstractPostgisRedisIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void healthLive_isPermitAll() {
        ResponseEntity<String> r = restTemplate.getForEntity(baseUrl() + "/api/v1/health/live", String.class);
        assertTrue(r.getStatusCode().is2xxSuccessful());
        assertTrue(r.getBody() != null && r.getBody().contains("UP"));
    }

    @Test
    void poi_withoutJwt_returnsUnauthorized() {
        ResponseEntity<String> r = restTemplate.getForEntity(
                baseUrl() + "/api/v1/poi?minX=-7.7&minY=33.5&maxX=-7.5&maxY=33.6",
                String.class);
        assertTrue(r.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void investorJwt_canReadPois() {
        String token = login("user@example.com", "user123");
        HttpHeaders h = bearerHeaders(token);
        ResponseEntity<String> r = restTemplate.exchange(
                baseUrl() + "/api/v1/poi?minX=-7.7&minY=33.5&maxX=-7.5&maxY=33.6",
                HttpMethod.GET,
                new HttpEntity<>(h),
                String.class);
        assertTrue(r.getStatusCode().is2xxSuccessful(), r::getBody);
    }

    @Test
    void rmiDemo_requiresAdmin_role() {
        assertTrue(
                restTemplate.getForEntity(baseUrl() + "/api/v1/rmi/ping", String.class).getStatusCode().isSameCodeAs(
                        HttpStatus.UNAUTHORIZED));

        String investorToken = login("user@example.com", "user123");
        ResponseEntity<String> forbidden = restTemplate.exchange(
                baseUrl() + "/api/v1/rmi/ping",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(investorToken)),
                String.class);
        assertTrue(forbidden.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN));

        String adminToken = login("admin@example.com", "admin123");
        ResponseEntity<String> allowed = restTemplate.exchange(
                baseUrl() + "/api/v1/rmi/ping",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)),
                String.class);
        var adminStatus = allowed.getStatusCode();
        assertFalse(
                adminStatus.isSameCodeAs(HttpStatus.UNAUTHORIZED) || adminStatus.isSameCodeAs(HttpStatus.FORBIDDEN),
                () -> "expected admin to reach RMI demo; status=" + adminStatus + " body=" + allowed.getBody());
        assertTrue(
                adminStatus.isSameCodeAs(HttpStatus.OK) || adminStatus.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE),
                () -> "unexpected status " + adminStatus);
    }

    @Test
    void soapFtDemo_requiresAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> postEmpty = new HttpEntity<>("{}", headers);
        ResponseEntity<String> anon = restTemplate.postForEntity(
                baseUrl() + "/api/v1/soap-ft/simulate", postEmpty, String.class);
        assertTrue(anon.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED));

        HttpHeaders investorHeaders = new HttpHeaders();
        investorHeaders.setContentType(MediaType.APPLICATION_JSON);
        investorHeaders.setBearerAuth(login("user@example.com", "user123"));
        ResponseEntity<String> investorResp = restTemplate.exchange(
                baseUrl() + "/api/v1/soap-ft/simulate",
                HttpMethod.POST,
                new HttpEntity<>("{}", investorHeaders),
                String.class);
        assertTrue(investorResp.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN));
    }

    @Test
    void adminDetailedHealth_requiresAdmin() {
        assertTrue(
                restTemplate
                        .getForEntity(baseUrl() + "/api/v1/admin/health/detailed", String.class)
                        .getStatusCode()
                        .isSameCodeAs(HttpStatus.UNAUTHORIZED));

        ResponseEntity<String> investorResp = restTemplate.exchange(
                baseUrl() + "/api/v1/admin/health/detailed",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(login("user@example.com", "user123"))),
                String.class);
        assertTrue(investorResp.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN));

        ResponseEntity<String> adminResp = restTemplate.exchange(
                baseUrl() + "/api/v1/admin/health/detailed",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(login("admin@example.com", "admin123"))),
                String.class);
        var adminCode = adminResp.getStatusCode();
        assertTrue(
                adminCode.isSameCodeAs(HttpStatus.OK)
                        || adminCode.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE),
                () -> String.valueOf(adminCode));
    }

    private static HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private String login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String raw = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                new HttpEntity<>(raw, headers),
                AuthResponse.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful(), () -> String.valueOf(resp.getBody()));
        assertTrue(resp.getBody() != null && resp.getBody().token() != null && !resp.getBody().token().isBlank());
        return resp.getBody().token();
    }
}
