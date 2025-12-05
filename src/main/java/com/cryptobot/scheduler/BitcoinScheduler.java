package com.cryptobot.scheduler;

import com.cryptobot.service.BitcoinPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BitcoinScheduler {

    private final BitcoinPriceService priceService;

    // ✅ Guardamos el último fallo para activar cooldown
    private Instant lastFailure = null;

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService) {
        this.priceService = priceService;
    }

    // ✅ Jitter aleatorio para evitar sincronización global
    private long randomJitter() {
        return (long) (Math.random() * 10000); // 0–10 segundos
    }

    @Scheduled(cron = "0 0/30 * * * *", initialDelay = 30000)
    public void run() {
        try {
            // ✅ Evitar que miles de bots llamen a CoinGecko al mismo tiempo
            Thread.sleep(randomJitter());

            // ✅ Cooldown si CoinGecko falló recientemente (429)
            if (lastFailure != null && Instant.now().minusSeconds(120).isBefore(lastFailure)) {
                System.out.println("⏳ Cooldown activo, evitando llamada a CoinGecko");
                return;
            }

            // ✅ Ejecutar proceso principal
            priceService.process();

        } catch (Exception e) {
            // ✅ Guardamos el fallo para activar cooldown
            lastFailure = Instant.now();

            System.err.println("⚠️ ERROR en el scheduler BTC: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
