package com.cryptobot.client;

import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoinGeckoClient {

    private static final String URL =
            "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=2";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ✅ Caché persistente
    private List<PricePoint> cachedPrices = null;
    private Instant lastFetchTime = Instant.EPOCH;

    // ✅ TTL de caché (minutos)
    private static final long CACHE_TTL_MINUTES = 25;

    // ✅ Reintentos
    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_SECONDS = {1, 3, 5};

    public List<PricePoint> getLastHourlyPrices(int hours) {

        // ✅ 1. Usar caché si está fresca
        if (cachedPrices != null &&
                Instant.now().minusSeconds(CACHE_TTL_MINUTES * 60)
                        .isBefore(lastFetchTime)) {

            System.out.println("✅ Usando caché CoinGecko (válida)");
            return cachedPrices;
        }

        // ✅ 2. Intentar obtener datos con reintentos
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                // ✅ Manejo de rate limit
                if (response.statusCode() == 429) {
                    System.out.println("⚠️ CoinGecko rate limit (429). Intento " + attempt);
                    waitBackoff(attempt);
                    continue;
                }

                // ✅ Manejo de errores HTTP
                if (response.statusCode() != 200) {
                    System.out.println("⚠️ Error HTTP CoinGecko: " + response.statusCode());
                    waitBackoff(attempt);
                    continue;
                }

                // ✅ Parsear respuesta
                List<PricePoint> parsed = parsePrices(response.body());

                // ✅ Actualizar caché
                cachedPrices = parsed;
                lastFetchTime = Instant.now();

                System.out.println("✅ Datos CoinGecko actualizados correctamente");
                return parsed;

            } catch (Exception e) {
                System.out.println("⚠️ Error CoinGecko (intento " + attempt + "): " + e.getMessage());
                waitBackoff(attempt);
            }
        }

        // ✅ 3. Si todos los intentos fallan → usar caché
        System.out.println("✅ Usando caché como fallback final");
        return cachedPrices != null ? cachedPrices : List.of();
    }

    private void waitBackoff(int attempt) {
        try {
            int seconds = BACKOFF_SECONDS[Math.min(attempt - 1, BACKOFF_SECONDS.length - 1)];
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {}
    }

    private List<PricePoint> parsePrices(String json) {
        List<PricePoint> list = new ArrayList<>();

        try {
            String pricesArray = json.split("\"prices\":")[1];
            pricesArray = pricesArray.substring(1, pricesArray.indexOf("]"));

            String[] entries = pricesArray.split("],");

            for (String entry : entries) {
                entry = entry.replace("[", "").replace("]", "");
                String[] parts = entry.split(",");

                long timestamp = Long.parseLong(parts[0].trim());
                double price = Double.parseDouble(parts[1].trim());

                list.add(new PricePoint(Instant.ofEpochMilli(timestamp), price));
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error parseando CoinGecko: " + e.getMessage());
        }

        return list;
    }
}
