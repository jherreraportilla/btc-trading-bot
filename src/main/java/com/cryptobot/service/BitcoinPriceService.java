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
        List<PricePoint> prices = coinGeckoClient.getLastHourlyPrices(48);
        SignalEvaluatorService.Signal signal = signalEvaluator.evaluateRsiSignal(prices);

        if (signal != null && signal.isActive()) {
            double precioActual = prices.get(prices.size() - 1).price();

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

            whatsAppNotifier.sendMessage(mensaje);
        }

        return signal;
    }
}