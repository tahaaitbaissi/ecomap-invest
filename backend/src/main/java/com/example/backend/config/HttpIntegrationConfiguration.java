package com.example.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound HTTP integration: shared {@link ObjectMapper}, Nominatim {@link RestClient}, Overpass
 * {@link RestTemplate} (with a trace {@link ClientHttpRequestInterceptor} only; resilience is
 * applied in {@code NominatimSearchClient} / {@code OverpassResilientHttpClient}).
 */
@Configuration
public class HttpIntegrationConfiguration {

    /** Qualifier for the Overpass {@link RestTemplate} bean. */
    public static final String OVERPASS_REST_TEMPLATE = "overpassRestTemplate";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return m;
    }

    @Bean
    public RestClient geocodingRestClient() {
        return RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org/search")
                .defaultHeader("User-Agent", "EcoMapInvest/1.0")
                .build();
    }

    @Bean(name = "nominatimExecutor")
    public Executor nominatimExecutor() {
        int pool = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(
                pool,
                pool,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(512),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean(name = OVERPASS_REST_TEMPLATE)
    public RestTemplate overpassRestTemplate(HttpRequestTraceInterceptor httpRequestTraceInterceptor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(List.of(httpRequestTraceInterceptor));
        return restTemplate;
    }

    @Bean
    public HttpRequestTraceInterceptor httpRequestTraceInterceptor() {
        return new HttpRequestTraceInterceptor();
    }

    @Bean(name = "overpassExecutor")
    public Executor overpassExecutor() {
        int pool = Math.max(4, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(
                pool,
                pool,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
