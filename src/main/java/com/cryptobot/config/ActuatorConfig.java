package com.cryptobot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Configuration
public class ActuatorConfig {

    @Bean
    @Primary
    public WebEndpointProperties actuatorWebEndpointProperties() {
        WebEndpointProperties props = new WebEndpointProperties();
        props.getExposure().getInclude().add("*");
        return props;
    }
}
