package com.cryptobot.scheduler;

import com.cryptobot.notification.WhatsAppNotifier;
import com.cryptobot.service.BitcoinPriceService;
import com.cryptobot.service.SignalEvaluatorService.Signal;
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

    @Scheduled(fixedRate = 300000) // cada 5 minutos
    public void run() {
        try {
            Signal signal = priceService.checkAndGetSignal();

            String mensaje = switch (signal) {
                case BUY -> "COMPRA BTC YA!!\nRSI muy bajo, posible rebote fuerte";
                case SELL -> "VENDE BTC!!\nRSI muy alto, posible corrección";
                case HOLD -> "Nada que hacer... HOLD";
            };

            whatsAppNotifier.sendMessage(mensaje);
            System.out.println(mensaje);

        } catch (Exception e) {
        	whatsAppNotifier.sendMessage("Error en el bot: " + e.getMessage());
        }
    }
}