package com.cryptobot.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class WhatsAppNotifier {

    private static final String URL_BASE = "https://btc-whatsapp.onrender.com";
    private static final String API_KEY   = "nsYJm0M$k1C0F5G#HRsj";
    private static final String INSTANCE  = "default";

    // ✅ JID REAL DEL GRUPO BOT-TRADIN
    private static final String GROUP_ID = "120363404627482611@g.us";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ✅ Cooldown si WhatsApp devuelve 429
    private Instant lastRateLimit = null;

    // ✅ Cola de mensajes pendientes
    private final Queue<String> messageQueue = new LinkedList<>();

    // ✅ Tiempo de cooldown tras 429 (10 segundos)
    private static final int COOLDOWN_SECONDS = 10;

    public void sendMessage(String mensaje) {
        // ✅ Guardar mensaje en cola
        messageQueue.add(mensaje);

        // ✅ Intentar enviar ahora
        processQueue();
    }

    private void processQueue() {
        try {
            // ✅ Si estamos en cooldown → no enviar
            if (lastRateLimit != null &&
                Instant.now().minusSeconds(COOLDOWN_SECONDS).isBefore(lastRateLimit)) {

                System.out.println("⏳ WhatsApp cooldown activo, mensajes en cola: " + messageQueue.size());
                return;
            }

            // ✅ Si no hay mensajes → salir
            if (messageQueue.isEmpty()) return;

            // ✅ Tomar el siguiente mensaje
            String mensaje = messageQueue.peek();

            // ✅ Escapar caracteres peligrosos
            String safeText = mensaje
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String json = "{"
                    + "\"number\":\"" + GROUP_ID + "\","
                    + "\"text\":\"" + safeText + "\","
                    + "\"options\":{\"delay\":1200}"
                    + "}";

            String url = URL_BASE + "/message/sendText/" + INSTANCE;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            // ✅ Éxito (200 o 201)
            if (status == 200 || status == 201) {
                System.out.println("✅ Mensaje enviado al grupo BOT-TRADIN");
                messageQueue.poll(); // ✅ eliminar mensaje enviado
                return;
            }

            // ✅ Rate limit → activar cooldown
            if (status == 429) {
                System.err.println("⚠️ WhatsApp rate limit (429). Activando cooldown.");
                lastRateLimit = Instant.now();
                return;
            }

            // ✅ Otros errores → log
            System.err.println("WhatsApp API error: " + status);
            System.err.println("Respuesta: " + response.body());

        } catch (Exception e) {
            System.err.println("❌ Fallo WhatsApp: " + e.getMessage());
        }
    }
}
