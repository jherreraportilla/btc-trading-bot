package com.cryptobot.client;

import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoinGeckoClient {

    private static final String URL =
            "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=2";

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ Caché interna para evitar llamadas repetidas
    private List<PricePoint> cachedPrices = new ArrayList<>();
    private Instant lastCacheUpdate = Instant.EPOCH;

    // ✅ Cooldown global si CoinGecko devuelve 429
    private Instant lastRateLimitHit = null;

    public List<PricePoint> getLastHourlyPrices(int hours) {

        // ✅ 1. Cooldown si hubo rate limit reciente
        if (lastRateLimitHit != null &&
                Instant.now().minusSeconds(120).isBefore(lastRateLimitHit)) {
            System.out.println("⏳ Cooldown activo, devolviendo caché");
            return cachedPrices;
        }

        // ✅ 2. Si la caché tiene menos de 10 minutos → usarla
        if (Instant.now().minusSeconds(600).isBefore(lastCacheUpdate)) {
            return cachedPrices;
        }

        // ✅ 3. Intentos con backoff exponencial
        int[] delays = {2000, 5000, 10000}; // 2s, 5s, 10s

        for (int i = 0; i < delays.length; i++) {
            try {
                var response = restTemplate.getForObject(URL, CoinGeckoResponse.class);

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

                return prices;

            } catch (Exception e) {
                System.out.println("⚠️ Error CoinGecko (intento " + (i + 1) + "): " + e.getMessage());

                // ✅ Detectar rate limit
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    lastRateLimitHit = Instant.now();
                }

                try {
                    Thread.sleep(delays[i]);
                } catch (InterruptedException ignored) {}
            }
        }

        // ✅ 4. Si todos los intentos fallan → devolver caché
        System.out.println("✅ Usando caché como fallback final");
        return cachedPrices;
    }

    // ✅ Clase interna para mapear respuesta JSON
    public record CoinGeckoResponse(List<List<Object>> prices) {}
}
