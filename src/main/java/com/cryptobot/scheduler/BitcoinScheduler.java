package com.cryptobot.scheduler;

import com.cryptobot.service.BitcoinPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BitcoinScheduler {

    private final BitcoinPriceService priceService;

    // ✅ Cooldown si CoinGecko devuelve 429
    private Instant lastFailure = null;

    // ✅ Evitar ejecutar justo al arrancar
    private boolean firstRun = true;

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService) {
        this.priceService = priceService;
    }

    // ✅ Jitter aleatorio para evitar sincronización global
    private long randomJitter() {
        return (long) (Math.random() * 10000); // 0–10 segundos
    }

    // ✅ Cron exacto: 00 y 15 de cada hora
    @Scheduled(cron = "0 0/15 * * * *")
    public void run() {
        try {
            // ✅ Saltar la primera ejecución tras el arranque
            if (firstRun) {
                firstRun = false;
                System.out.println("⏳ Saltando primera ejecución tras el arranque");
                return;
            }

            // ✅ Evitar sincronización global
            Thread.sleep(randomJitter());

            // ✅ Cooldown si CoinGecko falló recientemente
            if (lastFailure != null && Instant.now().minusSeconds(120).isBefore(lastFailure)) {
                System.out.println("⏳ Cooldown activo, evitando llamada a CoinGecko");
                return;
            }

            priceService.process();

        } catch (Exception e) {
            lastFailure = Instant.now();
            System.err.println("⚠️ ERROR en el scheduler BTC: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
