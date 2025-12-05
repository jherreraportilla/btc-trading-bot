package com.cryptobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.cryptobot.notification.WhatsAppNotifier; // IMPORTANTE

@SpringBootApplication
public class BtcBotApplication implements CommandLineRunner {

    @Autowired
    private WhatsAppNotifier whatsAppNotifier;

    public static void main(String[] args) {
        SpringApplication.run(BtcBotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Se ejecuta cuando Spring ya está completamente iniciado
        whatsAppNotifier.sendMessage("LA YAMPI CULISHEA MUCHO");
    }
}
