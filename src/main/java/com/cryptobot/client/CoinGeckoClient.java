package com.cryptobot.client;

import com.cryptobot.model.PricePoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CoinGeckoClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Caché para evitar rate limits
    private List<PricePoint> cachedPrices;
    private Instant lastFetchTime;

    public List<PricePoint> getLastHourlyPrices(int hours) throws Exception {

        // ✅ Si tenemos datos recientes (< 55 min), devolvemos caché
        if (cachedPrices != null && lastFetchTime != null &&
                Duration.between(lastFetchTime, Instant.now()).toMinutes() < 55) {
            return cachedPrices;
        }

        // ✅ CoinGecko exige mínimo 2 días para datos horarios
        double days = Math.max(2, hours / 24.0);

        String url = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart" +
                "?vs_currency=usd&days=" + days;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ✅ Manejo de rate limit (429)
        if (response.statusCode() == 429) {
            System.out.println("Rate limit alcanzado. Usando caché si existe.");

            if (cachedPrices != null) {
                return cachedPrices;
            } else {
                throw new RuntimeException("CoinGecko rate limit y no hay caché disponible.");
            }
        }

        // ✅ Manejo de error 401 (interval=hourly prohibido)
        if (response.statusCode() == 401) {
            System.out.println("CoinGecko 401: revisa parámetros. Usando caché si existe.");

            if (cachedPrices != null) {
                return cachedPrices;
            } else {
                throw new RuntimeException("CoinGecko 401 y no hay caché disponible.");
            }
        }

        // ✅ Otros errores
        if (response.statusCode() != 200) {
            throw new RuntimeException("CoinGecko error: " +
                    response.statusCode() + " " + response.body());
        }

        // ✅ Parseo de JSON
        JsonNode prices = objectMapper.readTree(response.body()).get("prices");
        List<PricePoint> result = new ArrayList<>();

        for (JsonNode node : prices) {
            long timestamp = node.get(0).asLong();
            double price = node.get(1).asDouble();

            ZonedDateTime dateTime =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));

            result.add(new PricePoint(dateTime, price));
        }

        // ✅ Guardar en caché
        cachedPrices = result;
        lastFetchTime = Instant.now();

        return result;
    }
}
