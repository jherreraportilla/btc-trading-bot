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
    private final WhatsAppNotifier notifier;

    // ✅ Para evitar duplicados
    private Double lastPrice = null;
    private Double lastRsi = null;
    private SignalEvaluatorService.Signal.Type lastSignalType =
            SignalEvaluatorService.Signal.Type.HOLD;

    // ✅ Configurable: % de cambio para alertar
    private static final double PRICE_CHANGE_THRESHOLD = 1.0;

    @Autowired
    public BitcoinPriceService(CoinGeckoClient coinGeckoClient,
                               SignalEvaluatorService signalEvaluator,
                               WhatsAppNotifier notifier) {
        this.coinGeckoClient = coinGeckoClient;
        this.signalEvaluator = signalEvaluator;
        this.notifier = notifier;
    }

    public void process() throws Exception {

        List<PricePoint> prices = coinGeckoClient.getLastHourlyPrices(48);

        if (prices == null || prices.isEmpty()) {
            System.out.println("No hay datos de precios disponibles.");
            return;
        }

        double price = prices.get(prices.size() - 1).price();
        double rsi = signalEvaluator.calculateRSI(prices);

        // ✅ 1. Actualización automática
        sendPeriodicUpdate(price, rsi);

        // ✅ 2. Alerta por cambio de precio
        checkPriceChange(price);

        // ✅ 3. Alerta por cruce RSI
        checkRSICross(rsi, price);

        // ✅ 4. Señales RSI clásicas
        checkClassicSignal(prices, price, rsi);

        lastPrice = price;
        lastRsi = rsi;
    }

    private void sendPeriodicUpdate(double price, double rsi) {
        String msg = """
                📊 *Actualización BTC (30 min)*

                *Precio:* $%,.0f USD
                *RSI(14):* %.2f

                https://www.tradingview.com/x/BTCUSD_1h.png
                """.formatted(price, rsi);

        notifier.sendMessage(msg);
    }

    private void checkPriceChange(double price) {
        if (lastPrice == null) return;

        double change = ((price - lastPrice) / lastPrice) * 100;

        if (Math.abs(change) >= PRICE_CHANGE_THRESHOLD) {
            String msg = """
                    🚨 *Movimiento fuerte en BTC*

                    Cambio: %.2f%%
                    Precio actual: $%,.0f USD

                    https://www.tradingview.com/x/BTCUSD_1h.png
                    """.formatted(change, price);

            notifier.sendMessage(msg);
        }
    }

    private void checkRSICross(double rsi, double price) {
        if (lastRsi == null) return;

        if (lastRsi > 70 && rsi <= 70) {
            notifier.sendMessage("""
                    🔻 *RSI cruza hacia abajo 70 (sobrecompra)*

                    Precio: $%,.0f
                    RSI: %.2f
                    """.formatted(price, rsi));
        }

        if (lastRsi < 30 && rsi >= 30) {
            notifier.sendMessage("""
                    🔼 *RSI cruza hacia arriba 30 (sobreventa)*

                    Precio: $%,.0f
                    RSI: %.2f
                    """.formatted(price, rsi));
        }
    }

    private void checkClassicSignal(List<PricePoint> prices, double price, double rsi) {

        SignalEvaluatorService.Signal signal = signalEvaluator.evaluateRsiSignal(prices);

        if (signal == null || !signal.isActive()) {
            lastSignalType = SignalEvaluatorService.Signal.Type.HOLD;
            return;
        }

        if (signal.getType() == lastSignalType) return;

        lastSignalType = signal.getType();

        String mensaje = """
                *SEÑAL BTC AUTOMÁTICA - RSI 14*

                %s

                *Precio actual:* $%,.0f USD
                *RSI(14):* %.2f

                https://www.tradingview.com/x/BTCUSD_1h.png
                """.formatted(
                signal.getType().getMessage(),
                price,
                rsi
        );

        notifier.sendMessage(mensaje);
    }
}
