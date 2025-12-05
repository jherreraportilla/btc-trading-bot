package com.cryptobot.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;

@Component
public class WhatsAppNotifier {

    private static final String URL_BASE = "https://btc-whatsapp.onrender.com";
    private static final String API_KEY   = "nsYJm0M$k1C0F5G#HRsj";
    private static final String INSTANCE  = "default";

    // ✅ JID REAL DEL GRUPO BOT-TRADIN
    private static final String GROUP_ID = "120363404627482611@g.us";

    // ✅ HttpClient reutilizable
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendMessage(String mensaje) {
        try {
            String url = URL_BASE + "/message/sendText/" + INSTANCE;

            String json = """
                {
                    "number": "%s",
                    "text": "%s",
                    "options": {"delay": 1200}
                }
                """.formatted(GROUP_ID, mensaje.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("apikey", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("WhatsApp API error: " + response.statusCode());
                System.err.println("Respuesta: " + response.body());
            } else {
                System.out.println("✅ Mensaje enviado al grupo BOT-TRADIN");
            }

        } catch (Exception e) {
            System.err.println("❌ Fallo WhatsApp: " + e.getMessage());
        }
    }
}
