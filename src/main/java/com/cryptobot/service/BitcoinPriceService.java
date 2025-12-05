package com.cryptobot.service;

import com.cryptobot.client.CoinGeckoClient;
import com.cryptobot.model.PricePoint;
import com.cryptobot.notification.WhatsAppNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BitcoinPriceService {

    private final CoinGeckoClient coinGeckoClient;
    private final SignalEvaluatorService signalEvaluator;
    private final WhatsAppNotifier notifier;

    // ✅ Valores iniciales seguros
    private Double lastPrice = 0.0;
    private Double lastRsi = 50.0;
    private SignalEvaluatorService.Signal.Type lastSignalType =
            SignalEvaluatorService.Signal.Type.HOLD;

    // ✅ Datos expuestos para /status
    private Double lastKnownPrice = 0.0;
    private Double lastKnownRsi = 50.0;
    private SignalEvaluatorService.Signal.Type lastKnownSignal =
            SignalEvaluatorService.Signal.Type.HOLD;
    private Instant lastExecutionTime = Instant.now();

    public Double getLastKnownPrice() { return lastKnownPrice; }
    public Double getLastKnownRsi() { return lastKnownRsi; }
    public SignalEvaluatorService.Signal.Type getLastSignalType() { return lastKnownSignal; }
    public Instant getLastExecutionTime() { return lastExecutionTime; }

    private static final double PRICE_CHANGE_THRESHOLD = 1.0;

    // ✅ RSI incremental
    private Double avgGain = null;
    private Double avgLoss = null;
    private Double lastRsiValue = null;

    // ✅ Últimos precios (solo 20 necesarios)
    private final List<Double> recentPrices = new ArrayList<>();

    @Autowired
    public BitcoinPriceService(CoinGeckoClient coinGeckoClient,
                               SignalEvaluatorService signalEvaluator,
                               WhatsAppNotifier notifier) {
        this.coinGeckoClient = coinGeckoClient;
        this.signalEvaluator = signalEvaluator;
        this.notifier = notifier;
    }

    public void process() {

        List<PricePoint> prices = safeList(coinGeckoClient.getLastHourlyPrices(48));

        if (prices.isEmpty()) {
            System.out.println("⚠️ No hay datos de precios disponibles (CoinGecko falló y no hay caché).");
            return;
        }

        double price = prices.get(prices.size() - 1).price();

        // ✅ RSI incremental
        double rsi = updateRsiIncremental(price);

        // ✅ 1. Actualización automática
        sendPeriodicUpdate(price, rsi);

        // ✅ 2. Alerta por cambio de precio
        checkPriceChange(price);

        // ✅ 3. Alerta por cruce RSI
        checkRSICross(rsi, price);

        // ✅ 4. Señales RSI clásicas
        SignalEvaluatorService.Signal signal = checkClassicSignal(prices, price, rsi);

        // ✅ Guardar últimos valores
        lastPrice = price;
        lastRsi = rsi;

        lastKnownPrice = price;
        lastKnownRsi = rsi;
        lastKnownSignal = signal != null ? signal.getType() : SignalEvaluatorService.Signal.Type.HOLD;
        lastExecutionTime = Instant.now();
    }

    // ✅ Evita nulls en listas
    private List<PricePoint> safeList(List<PricePoint> list) {
        return list != null ? list : Collections.emptyList();
    }

    // ✅ RSI incremental
    private double updateRsiIncremental(double newPrice) {

        // Guardar precio
        recentPrices.add(newPrice);
        if (recentPrices.size() > 20) {
            recentPrices.remove(0);
        }

        // ✅ Si no hay suficientes datos → RSI inicial
        if (recentPrices.size() < 15) {
            return calculateInitialRsi();
        }

        double prevPrice = recentPrices.get(recentPrices.size() - 2);
        double change = newPrice - prevPrice;

        double gain = Math.max(change, 0);
        double loss = Math.max(-change, 0);

        // ✅ Si aún no tenemos medias → RSI inicial
        if (avgGain == null || avgLoss == null) {
            return calculateInitialRsi();
        }

        // ✅ RSI incremental
        avgGain = ((avgGain * 13) + gain) / 14;
        avgLoss = ((avgLoss * 13) + loss) / 14;

        if (avgLoss == 0) {
            lastRsiValue = 100.0;
            return lastRsiValue;
        }

        double rs = avgGain / avgLoss;
        lastRsiValue = 100 - (100 / (1 + rs));

        return lastRsiValue;
    }

    // ✅ RSI inicial (solo se ejecuta una vez)
    private double calculateInitialRsi() {

        if (recentPrices.size() < 15) return 50.0;

        double gains = 0;
        double losses = 0;

        for (int i = 1; i < 15; i++) {
            double diff = recentPrices.get(i) - recentPrices.get(i - 1);
            if (diff > 0) gains += diff;
            else losses -= diff;
        }

        avgGain = gains / 14;
        avgLoss = losses / 14;

        if (avgLoss == 0) return 100.0;

        double rs = avgGain / avgLoss;
        lastRsiValue = 100 - (100 / (1 + rs));

        return lastRsiValue;
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
        if (lastPrice == null || lastPrice == 0.0) return;

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
        if (lastRsi == null || lastRsi == 0.0) return;

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

    private SignalEvaluatorService.Signal checkClassicSignal(List<PricePoint> prices, double price, double rsi) {

        SignalEvaluatorService.Signal signal = signalEvaluator.evaluateRsiSignal(prices);

        if (signal == null || !signal.isActive()) {
            lastSignalType = SignalEvaluatorService.Signal.Type.HOLD;
            return signal;
        }

        if (signal.getType() == lastSignalType) return signal;

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

        return signal;
    }
}
