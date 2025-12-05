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

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService) {
        this.priceService = priceService;
    }

    // ✅ Jitter aleatorio para evitar sincronización global
    private long randomJitter() {
        return (long) (Math.random() * 10000); // 0–10 segundos
    }

    // ✅ Cada 30 minutos, con delay inicial de 30s (compatible con Spring 7)
    @Scheduled(fixedRate = 1800000, initialDelay = 30000)
    public void run() {
        try {
            // ✅ Evitar que miles de bots llamen a CoinGecko al mismo tiempo
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
