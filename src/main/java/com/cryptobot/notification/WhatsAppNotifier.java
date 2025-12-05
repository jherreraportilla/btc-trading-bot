package com.cryptobot.notification;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class WhatsAppNotifier {
    private static final String URL_BASE = "https://btc-whatsapp.onrender.com";
    private static final String API_KEY   = "nsYJm0M$k1C0F5G#HRsj";   // la que pusiste en Render
    private static final String INSTANCE  = "default";
    private static final String TU_NUMERO = "34671072929";         // tu número en internacional (con 34)

    public void sendMessage(String mensaje) {
        try {
            String url = URL_BASE + "/message/sendText/" + INSTANCE;
            HttpClient client = HttpClient.newHttpClient();
            String json = """
                {
                    "number": "%s",
                    "text": "%s",
                    "options": {"delay": 1200}
                }
                """.formatted(TU_NUMERO, mensaje.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("apikey", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ Alerta enviada a WhatsApp: " + mensaje);

        } catch (Exception e) {
            System.err.println("❌ Fallo al enviar WhatsApp: " + e.getMessage());
        }
    }

    // Ejemplo de uso:
    public void main(String[] args) {
    	sendMessage("🚨 BTC rompió los 108.000$ ¡Larga activada!");
    }
}