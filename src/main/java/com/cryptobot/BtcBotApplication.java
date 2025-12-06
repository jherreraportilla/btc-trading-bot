package com.cryptobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// No necesitas @EnableConfigurationProperties si usas @Configuration en BotProperties
public class BtcBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BtcBotApplication.class, args);
    }
}