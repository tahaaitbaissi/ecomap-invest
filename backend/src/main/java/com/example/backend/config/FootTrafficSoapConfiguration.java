package com.example.backend.config;

import com.example.backend.foottraffic.config.FootTrafficProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class FootTrafficSoapConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.foot-traffic.soap.enabled", havingValue = "true", matchIfMissing = true)
    public WebServiceTemplate footTrafficWebServiceTemplate(
            WebServiceTemplateBuilder builder, FootTrafficProperties footTrafficProperties) {
        return builder.setDefaultUri(footTrafficProperties.getSoap().getUrl()).build();
    }
}
