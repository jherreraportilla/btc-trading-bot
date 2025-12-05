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

    // ✅ Variables de caché
    private List<PricePoint> cachedPrices;
    private Instant lastFetchTime;

    public List<PricePoint> getLastHourlyPrices(int hours) throws Exception {

        // ✅ Si tenemos datos recientes (< 55 minutos), devolvemos la caché
        if (cachedPrices != null && lastFetchTime != null &&
                Duration.between(lastFetchTime, Instant.now()).toMinutes() < 55) {
            return cachedPrices;
        }

        // ✅ Llamada real a CoinGecko
        String url = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart" +
                "?vs_currency=usd&days=" + (hours / 24.0) + "&interval=hourly";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ✅ Manejo del rate limit
        if (response.statusCode() == 429) {
            System.out.println("Rate limit alcanzado. Usando datos en caché si existen.");

            if (cachedPrices != null) {
                return cachedPrices;
            } else {
                throw new RuntimeException("CoinGecko rate limit y no hay caché disponible.");
            }
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("CoinGecko error: " + response.statusCode() + " " + response.body());
        }

        JsonNode prices = objectMapper.readTree(response.body()).get("prices");
        List<PricePoint> result = new ArrayList<>();

        for (JsonNode node : prices) {
            long timestamp = node.get(0).asLong();
            double price = node.get(1).asDouble();
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
            result.add(new PricePoint(dateTime, price));
        }

        // ✅ Guardamos en caché
        cachedPrices = result;
        lastFetchTime = Instant.now();

        return result;
    }
}
