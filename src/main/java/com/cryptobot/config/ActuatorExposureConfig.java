package com.cryptobot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;

@Configuration
public class ActuatorExposureConfig {

    @Bean
    public WebEndpointProperties exposeAllEndpoints() {
        WebEndpointProperties props = new WebEndpointProperties();
        props.getExposure().getInclude().add("*");
        return props;
    }
}
