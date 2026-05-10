package com.ecomap.soap.ft.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;

/**
 * SOAP 1.1 WSDL for {@link com.ecomap.soap.ft.ws.FootTrafficSimulationEndpoint}.
 */
@EnableWs
@Configuration
public class WebServiceConfig implements WsConfigurer {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "footTraffic")
    public DefaultWsdl11Definition defaultWsdl11Definition(SimpleXsdSchema footTrafficSimulationXsd) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("FootTrafficSimulationPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://ecomap.example.com/foottraffic/ws");
        wsdl.setSchema(footTrafficSimulationXsd);
        return wsdl;
    }

    @Bean
    public SimpleXsdSchema footTrafficSimulationXsd() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/foot-traffic-simulation.xsd"));
    }
}
