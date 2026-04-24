package com.example.backend.config;

import com.uber.h3core.H3Core;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H3Configuration {

    @Bean
    H3Core h3Core() throws IOException {
        return H3Core.newInstance();
    }
}
