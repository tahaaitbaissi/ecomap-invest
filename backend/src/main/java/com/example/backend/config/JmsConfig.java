package com.example.backend.config;

import com.example.backend.events.ProfileGeneratedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.util.ErrorHandler;

/**
 * JMS (Apache ActiveMQ Artemis): JSON mapping for {@link ProfileGeneratedMessage} and listener
 * container factory.
 */
@Configuration
@EnableJms
public class JmsConfig {

    public static final String PROFILE_GENERATED_TYPE_ID = "ProfileGeneratedMessage";

    @Bean
    MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put(PROFILE_GENERATED_TYPE_ID, ProfileGeneratedMessage.class);
        converter.setTypeIdMappings(typeIdMappings);
        return converter;
    }

    @Bean
    DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("jacksonJmsMessageConverter") MessageConverter messageConverter,
            ErrorHandler jmsErrorHandler) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setErrorHandler(jmsErrorHandler);
        factory.setSessionTransacted(false);
        factory.setAutoStartup(true);
        return factory;
    }

    /** Logs listener failures without crashing the JVM; broker redelivery policies apply separately. */
    @Bean
    ErrorHandler jmsErrorHandler() {
        return t -> org.slf4j.LoggerFactory.getLogger(JmsConfig.class)
                .error("JMS listener error: {}", t.getMessage(), t);
    }
}
