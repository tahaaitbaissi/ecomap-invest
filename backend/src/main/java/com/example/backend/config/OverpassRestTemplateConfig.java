package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OverpassRestTemplateConfig {

    public static final String OVERPASS_REST_TEMPLATE = "overpassRestTemplate";

    /**
     * RestTemplate for Overpass: 5s connect, 30s read (large bbox queries can be slow).
     */
    @Bean(name = OVERPASS_REST_TEMPLATE)
    public RestTemplate overpassRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }
}
