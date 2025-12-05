package com.cryptobot.service;

import com.cryptobot.client.CoinGeckoClient;
import com.cryptobot.model.PricePoint;
import com.cryptobot.notification.WhatsAppNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BitcoinPriceService {

    private final CoinGeckoClient coinGeckoClient;
    private final SignalEvaluatorService signalEvaluator;
    private final WhatsAppNotifier whatsAppNotifier;

    @Autowired
    public BitcoinPriceService(CoinGeckoClient coinGeckoClient,
                               SignalEvaluatorService signalEvaluator,
                               WhatsAppNotifier whatsAppNotifier) {
        this.coinGeckoClient = coinGeckoClient;
        this.signalEvaluator = signalEvaluator;
        this.whatsAppNotifier = whatsAppNotifier;
    }

    public SignalEvaluatorService.Signal checkAndGetSignal() throws Exception {

        // ✅ 1. Obtener precios
        List<PricePoint> prices = coinGeckoClient.getLastHourlyPrices(48);

        // ✅ 2. Validar que haya datos
        if (prices == null || prices.isEmpty()) {
            System.out.println("No hay datos de precios disponibles. Usando HOLD.");
            return null;
        }

        // ✅ 3. Evaluar señal RSI
        SignalEvaluatorService.Signal signal = signalEvaluator.evaluateRsiSignal(prices);

        // ✅ 4. Si no hay señal activa → HOLD
        if (signal == null || !signal.isActive()) {
            System.out.println("Sin señal fuerte → HOLD (RSI: " +
                    (signal != null ? String.format("%.2f", signal.getRsi()) : "N/A") + ")");
            return signal;
        }

        // ✅ 5. Obtener precio actual de forma segura
        double precioActual = prices.get(prices.size() - 1).price();

        // ✅ 6. Construir mensaje
        String mensaje = """
                *SEÑAL BTC AUTOMÁTICA - RSI 14*
                
                %s
                
                *Precio actual:* $%,.0f USD
                *RSI(14):* %.2f
                
                https://www.tradingview.com/x/BTCUSD_1h.png
                """.formatted(
                signal.getType().getMessage(),
                precioActual,
                signal.getRsi()
        );

        // ✅ 7. Enviar notificación
        whatsAppNotifier.sendMessage(mensaje);
        System.out.println("SEÑAL ENVIADA: " + signal.getType().getMessage());

        return signal;
    }
}
