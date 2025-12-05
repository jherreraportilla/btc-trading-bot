package com.cryptobot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;

@Configuration
public class ActuatorConfig {

    @Bean
    public WebEndpointProperties webEndpointProperties() {
        WebEndpointProperties props = new WebEndpointProperties();
        props.getExposure().getInclude().add("health");
        props.getExposure().getInclude().add("info");
        props.getExposure().getInclude().add("prometheus");
        return props;
    }
}
