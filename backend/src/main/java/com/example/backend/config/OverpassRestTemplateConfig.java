package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OverpassRestTemplateConfig {

    public static final String OVERPASS_REST_TEMPLATE = "overpassRestTemplate";

    /**
     * RestTemplate for Overpass: public instances often return 504 when busy; allow long reads + retries in the client.
     */
    @Bean(name = OVERPASS_REST_TEMPLATE)
    public RestTemplate overpassRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        return new RestTemplate(factory);
    }
}
