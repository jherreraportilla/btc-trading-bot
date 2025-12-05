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

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService, WhatsAppNotifier whatsAppNotifier) {
        this.priceService = priceService;
        this.whatsAppNotifier = whatsAppNotifier;
    }

    // Cada 5 minutos (300000 ms) → puedes cambiar a 3600000 para cada hora
    @Scheduled(fixedRate = 300000)
    public void run() {
        try {
            SignalEvaluatorService.Signal signal = priceService.checkAndGetSignal();

            // Solo enviamos si hay señal activa (no HOLD)
            if (signal != null && signal.isActive()) {
                double precio = signal.getPrice();
                double rsi = signal.getRsi();

                String mensaje = """
                    *SEÑAL BTC AUTOMÁTICA - RSI 14*
                    
                    %s
                    
                    *Precio actual:* $%,.0f USD
                    *RSI(14):* %.2f
                    
                    https://www.tradingview.com/x/BTCUSD_1h.png
                    """.formatted(
                    signal.getType().getMessage(),
                    precio,
                    rsi
                );

                whatsAppNotifier.sendMessage(mensaje);
                System.out.println("SEÑAL ENVIADA: " + signal.getType().getMessage());

            } else {
                System.out.println("Sin señal fuerte → HOLD (RSI: " + 
                    (signal != null ? String.format("%.2f", signal.getRsi()) : "N/A") + ")");
            }

        } catch (Exception e) {
            String errorMsg = "ERROR CRÍTICO EN EL BOT: " + e.getMessage();
            whatsAppNotifier.sendMessage(errorMsg);
            e.printStackTrace();
        }
    }
}