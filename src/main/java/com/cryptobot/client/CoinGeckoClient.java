package com.cryptobot.client;

import com.cryptobot.config.BotProperties;
import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoinGeckoClient {

    private final BotProperties config;
    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ Caché interna
    private List<PricePoint> cachedPrices = new ArrayList<>();
    private Instant lastCacheUpdate = Instant.EPOCH;
    private Instant lastRateLimitHit = null;

    public CoinGeckoClient(BotProperties config) {
        this.config = config;
    }

    public List<PricePoint> getLastHourlyPrices(int hours) {
        // ✅ 1. Cooldown si hubo rate limit reciente
        int cooldownSeconds = config.getCoingecko().getRateLimit().getCooldownSeconds();
        if (lastRateLimitHit != null &&
                Instant.now().minusSeconds(cooldownSeconds).isBefore(lastRateLimitHit)) {
            System.out.println("⏳ Cooldown activo (" + cooldownSeconds + "s), devolviendo caché");
            return cachedPrices;
        }

        // ✅ 2. Si la caché es reciente → usarla
        int cacheTtl = config.getCoingecko().getCache().getTtlSeconds();
        if (Instant.now().minusSeconds(cacheTtl).isBefore(lastCacheUpdate)) {
            System.out.println("✅ Caché válida (TTL: " + cacheTtl + "s)");
            return cachedPrices;
        }

        // ✅ 3. Intentos con backoff configurado
        List<Integer> delays = config.getCoingecko().getRetry().getDelays();
        int maxAttempts = Math.min(config.getCoingecko().getRetry().getMaxAttempts(), delays.size());
        
        String apiUrl = config.getCoingecko().getApi().getUrl();

        for (int i = 0; i < maxAttempts; i++) {
            try {
                System.out.println("🌐 Llamando a CoinGecko (intento " + (i + 1) + "/" + maxAttempts + ")");
                
                var response = restTemplate.getForObject(apiUrl, CoinGeckoResponse.class);
                
                if (response == null || response.prices() == null) {
                    throw new RuntimeException("Respuesta inválida de CoinGecko");
                }

                List<PricePoint> prices = new ArrayList<>();
                for (List<Object> entry : response.prices()) {
                    long timestamp = ((Number) entry.get(0)).longValue();
                    Instant instant = Instant.ofEpochMilli(timestamp);
                    double price = ((Number) entry.get(1)).doubleValue();
                    prices.add(new PricePoint(instant, price));
                }

                // ✅ Actualizar caché
                cachedPrices = prices;
                lastCacheUpdate = Instant.now();
                System.out.println("✅ Datos obtenidos de CoinGecko: " + prices.size() + " puntos");
                
                return prices;

            } catch (Exception e) {
                System.err.println("⚠️ Error CoinGecko (intento " + (i + 1) + "): " + e.getMessage());

                // ✅ Detectar rate limit
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    lastRateLimitHit = Instant.now();
                    System.err.println("❌ Rate Limit detectado. Activando cooldown de " + cooldownSeconds + "s");
                    break; // ✅ NO reintentar si es 429
                }

                // ✅ Esperar antes del siguiente intento
                if (i < maxAttempts - 1) {
                    try {
                        int delay = delays.get(i);
                        System.out.println("⏳ Esperando " + delay + "ms antes del siguiente intento...");
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // ✅ 4. Si todos los intentos fallan → devolver caché
        System.out.println("✅ Usando caché como fallback (" + cachedPrices.size() + " puntos)");
        return cachedPrices;
    }

    public record CoinGeckoResponse(List<List<Object>> prices) {}
}