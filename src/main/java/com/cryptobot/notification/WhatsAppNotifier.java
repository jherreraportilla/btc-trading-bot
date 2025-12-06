package com.cryptobot.notification;

import com.cryptobot.config.BotProperties;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WhatsAppNotifier {

    private final RestTemplate restTemplate;
    private final BotProperties config;

    // ✅ Control de rate limiting
    private Instant lastMessageSent = null;
    private final AtomicInteger messagesThisHour = new AtomicInteger(0);
    private Instant hourStartTime = Instant.now();

    public WhatsAppNotifier(BotProperties config) {
        this.config = config;
        
        // ✅ Configurar RestTemplate con timeout desde properties
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = config.getWhatsapp().getHttp().getTimeoutMs();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        
        this.restTemplate = new RestTemplate(factory);
    }

    public void sendMessage(String message) {
        try {
            // ✅ Verificar rate limiting
            if (!checkRateLimit()) {
                System.out.println("⚠️ Rate limit alcanzado, mensaje no enviado");
                return;
            }

            // ✅ Cooldown entre mensajes
            if (lastMessageSent != null) {
                long cooldownMs = config.getWhatsapp().getRateLimit().getCooldownMs();
                long timeSinceLastMs = Duration.between(lastMessageSent, Instant.now()).toMillis();
                
                if (timeSinceLastMs < cooldownMs) {
                    long waitMs = cooldownMs - timeSinceLastMs;
                    System.out.println("⏳ Cooldown activo, esperando " + (waitMs / 1000) + "s");
                    Thread.sleep(waitMs);
                }
            }

            // ✅ Intentar enviar con reintentos
            boolean sent = sendWithRetry(message);
            
            if (sent) {
                lastMessageSent = Instant.now();
                messagesThisHour.incrementAndGet();
                System.out.println("✅ Mensaje WhatsApp enviado correctamente");
            }

        } catch (Exception e) {
            System.err.println("❌ Error enviando mensaje WhatsApp: " + e.getMessage());
        }
    }

    private boolean checkRateLimit() {
        // ✅ Resetear contador cada hora
        Duration timeSinceHourStart = Duration.between(hourStartTime, Instant.now());
        if (timeSinceHourStart.toHours() >= 1) {
            messagesThisHour.set(0);
            hourStartTime = Instant.now();
        }

        int maxPerHour = config.getWhatsapp().getRateLimit().getMaxPerHour();
        return messagesThisHour.get() < maxPerHour;
    }

    private boolean sendWithRetry(String message) {
        int maxAttempts = config.getWhatsapp().getRetry().getMaxAttempts();
        int initialDelay = config.getWhatsapp().getRetry().getInitialDelayMs();
        int maxDelay = config.getWhatsapp().getRetry().getMaxDelayMs();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // ✅ Construir URL desde config
                String url = String.format("%s/%s/messages/chat",
                        config.getWhatsapp().getApi().getUrl(),
                        config.getWhatsapp().getInstance().getId());

                // ✅ Preparar headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // ✅ Preparar body
                Map<String, String> body = new HashMap<>();
                body.put("token", config.getWhatsapp().getApi().getToken());
                body.put("to", config.getWhatsapp().getRecipient().getPhone());
                body.put("body", message);

                HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

                // ✅ Enviar request
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }

                System.err.println("⚠️ WhatsApp API respuesta no exitosa: " + response.getStatusCode());

            } catch (Exception e) {
                System.err.println("⚠️ Intento " + (attempt + 1) + "/" + maxAttempts + " falló: " + e.getMessage());

                // ✅ Detectar errores específicos
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("429")) {
                        System.err.println("⚠️ WhatsApp rate limit detectado");
                    } else if (e.getMessage().contains("502") || e.getMessage().contains("Bad Gateway")) {
                        System.err.println("⚠️ WhatsApp API 502 (Bad Gateway)");
                    }
                }

                // ✅ Backoff exponencial
                if (attempt < maxAttempts - 1) {
                    try {
                        long delay = Math.min(initialDelay * (long) Math.pow(2, attempt), maxDelay);
                        System.out.println("⏳ Reintentando en " + (delay / 1000) + " segundos...");
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        System.err.println("❌ Todos los intentos de envío WhatsApp fallaron");
        return false;
    }
}