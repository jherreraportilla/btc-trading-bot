package com.cryptobot.scheduler;

import com.cryptobot.config.BotProperties;
import com.cryptobot.service.BitcoinPriceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BitcoinScheduler {

    private final BitcoinPriceService priceService;
    private final BotProperties config;

    // ✅ Control de estado
    private Instant lastFailure = null;
    private Instant lastSuccess = null;
    private boolean firstRun = true;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public BitcoinScheduler(BitcoinPriceService priceService, BotProperties config) {
        this.priceService = priceService;
        this.config = config;
    }

    private long randomJitter() {
        int maxJitter = config.getBitcoin().getScheduler().getMaxJitterSeconds();
        return (long) (Math.random() * maxJitter * 1000);
    }

    private boolean isInCooldown() {
        if (lastFailure == null) return false;

        Duration timeSinceFailure = Duration.between(lastFailure, Instant.now());
        long cooldownSeconds = config.getBitcoin().getScheduler().getCooldownMinutes() * 60L;

        // ✅ Cooldown exponencial si hay fallos consecutivos
        if (consecutiveFailures.get() > 0) {
            cooldownSeconds *= Math.min(consecutiveFailures.get(), 4);
        }

        return timeSinceFailure.getSeconds() < cooldownSeconds;
    }

    @Scheduled(cron = "${bitcoin.scheduler.cron}")
    public void run() {
        try {
            // ✅ Saltar primera ejecución
            if (firstRun && config.getBitcoin().getScheduler().isSkipFirstRun()) {
                firstRun = false;
                System.out.println("⏭️ Saltando primera ejecución tras el arranque");
                return;
            }
            firstRun = false;

            // ✅ Verificar cooldown
            if (isInCooldown()) {
                int minutesRemaining = (int) Duration.between(Instant.now(),
                        lastFailure.plusSeconds(config.getBitcoin().getScheduler().getCooldownMinutes() * 60L)).toMinutes();
                System.out.println("⏳ Cooldown activo. Tiempo restante: ~" + minutesRemaining + " minutos");
                return;
            }

            // ✅ Aplicar jitter
            long jitter = randomJitter();
            System.out.println("⏱️ Aplicando jitter de " + (jitter / 1000) + " segundos");
            Thread.sleep(jitter);

            // ✅ Ejecutar
            System.out.println("🚀 Ejecutando consulta de precio Bitcoin...");
            priceService.process();

            // ✅ Registrar éxito
            lastSuccess = Instant.now();
            consecutiveFailures.set(0);
            System.out.println("✅ Consulta exitosa a las " + lastSuccess);

        } catch (Exception e) {
            lastFailure = Instant.now();
            int failures = consecutiveFailures.incrementAndGet();

            System.err.println("❌ ERROR en scheduler BTC (fallo #" + failures + "): " + e.getMessage());

            if (failures <= 3) {
                e.printStackTrace();
            } else {
                System.err.println("⚠️ Múltiples fallos consecutivos. Aumentando cooldown.");
            }
        }
    }

    @Scheduled(cron = "${bitcoin.scheduler.health-check-cron}")
    public void healthCheck() {
        if (lastSuccess != null) {
            Duration timeSinceSuccess = Duration.between(lastSuccess, Instant.now());
            System.out.println("🏥 Health Check - Última ejecución exitosa hace "
                    + timeSinceSuccess.toMinutes() + " minutos");
        }

        // ✅ Resetear fallos después de X horas
        if (lastFailure != null) {
            Duration timeSinceFailure = Duration.between(lastFailure, Instant.now());
            int resetHours = config.getBitcoin().getScheduler().getResetFailuresAfterHours();
            
            if (timeSinceFailure.toHours() >= resetHours) {
                System.out.println("🔄 Reseteando contador de fallos consecutivos");
                consecutiveFailures.set(0);
            }
        }
    }
}