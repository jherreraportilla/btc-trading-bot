package com.cryptobot.scheduler;

import com.cryptobot.notification.WhatsAppNotifier;
import com.cryptobot.service.BitcoinPriceService;
import com.cryptobot.service.SignalEvaluatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BitcoinScheduler {

    private final BitcoinPriceService priceService;
    private final WhatsAppNotifier whatsAppNotifier;

    // ✅ Para evitar enviar señales duplicadas
    private SignalEvaluatorService.Signal.Type lastSignalType = SignalEvaluatorService.Signal.Type.HOLD;

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService, WhatsAppNotifier whatsAppNotifier) {
        this.priceService = priceService;
        this.whatsAppNotifier = whatsAppNotifier;
    }

    @Scheduled(fixedRate = 3600000) // cada 1 hora
    public void run() {
        try {
            SignalEvaluatorService.Signal signal = priceService.checkAndGetSignal();

            // ✅ Si no hay señal o es HOLD → log y salir
            if (signal == null || !signal.isActive()) {
                System.out.println("Sin señal fuerte → HOLD (RSI: " +
                        (signal != null ? String.format("%.2f", signal.getRsi()) : "N/A") + ")");
                lastSignalType = SignalEvaluatorService.Signal.Type.HOLD;
                return;
            }

            // ✅ Evitar enviar la misma señal repetida
            if (signal.getType() == lastSignalType) {
                System.out.println("Señal repetida (" + signal.getType() + ") → no se envía.");
                return;
            }

            lastSignalType = signal.getType();

            // ✅ Construir mensaje
            String mensaje = """
                    *SEÑAL BTC AUTOMÁTICA - RSI 14*
                    
                    %s
                    
                    *Precio actual:* $%,.0f USD
                    *RSI(14):* %.2f
                    
                    https://www.tradingview.com/x/BTCUSD_1h.png
                    """.formatted(
                    signal.getType().getMessage(),
                    signal.getPrice(),
                    signal.getRsi()
            );

            // ✅ Enviar notificación
            whatsAppNotifier.sendMessage(mensaje);
            System.out.println("✅ SEÑAL ENVIADA: " + signal.getType().getMessage());

        } catch (Exception e) {

            // ✅ Mensaje de error más claro
            String errorMsg = "⚠️ ERROR en el bot BTC: " + e.getMessage();
            System.err.println(errorMsg);

            // ✅ Intento de enviar alerta por WhatsApp
            try {
                whatsAppNotifier.sendMessage(errorMsg);
            } catch (Exception ignored) {
                System.err.println("No se pudo enviar el error por WhatsApp.");
            }

            e.printStackTrace();
        }
    }
}
