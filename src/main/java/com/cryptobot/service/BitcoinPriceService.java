package com.cryptobot.service;

import com.cryptobot.client.CoinGeckoClient;
import com.cryptobot.config.BotProperties;
import com.cryptobot.model.PricePoint;
import com.cryptobot.notification.WhatsAppNotifier;
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
    private final BotProperties config;

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

    // ✅ RSI incremental
    private Double avgGain = null;
    private Double avgLoss = null;
    private Double lastRsiValue = null;

    // ✅ Últimos precios
    private final List<Double> recentPrices = new ArrayList<>();

    public BitcoinPriceService(CoinGeckoClient coinGeckoClient,
                               SignalEvaluatorService signalEvaluator,
                               WhatsAppNotifier notifier,
                               BotProperties config) {
        this.coinGeckoClient = coinGeckoClient;
        this.signalEvaluator = signalEvaluator;
        this.notifier = notifier;
        this.config = config;
    }

    // ✅ Getters para /status
    public Double getLastKnownPrice() { return lastKnownPrice; }
    public Double getLastKnownRsi() { return lastKnownRsi; }
    public SignalEvaluatorService.Signal.Type getLastSignalType() { return lastKnownSignal; }
    public Instant getLastExecutionTime() { return lastExecutionTime; }

    public void process() {
        List<PricePoint> prices = safeList(coinGeckoClient.getLastHourlyPrices(48));

        if (prices.isEmpty()) {
            System.out.println("⚠️ No hay datos de precios disponibles");
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

    // ✅ RSI incremental con configuración desde properties
    private double updateRsiIncremental(double newPrice) {
        int recentLimit = config.getBitcoin().getPrice().getRecentPricesLimit();
        int minDataPoints = config.getBitcoin().getRsi().getMinDataPoints();
        int period = config.getBitcoin().getRsi().getPeriod();

        // Guardar precio
        recentPrices.add(newPrice);
        if (recentPrices.size() > recentLimit) {
            recentPrices.remove(0);
        }

        // ✅ Si no hay suficientes datos → RSI inicial
        if (recentPrices.size() < minDataPoints) {
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

        // ✅ RSI incremental con período configurable
        avgGain = ((avgGain * (period - 1)) + gain) / period;
        avgLoss = ((avgLoss * (period - 1)) + loss) / period;

        if (avgLoss == 0) {
            lastRsiValue = 100.0;
            return lastRsiValue;
        }

        double rs = avgGain / avgLoss;
        lastRsiValue = 100 - (100 / (1 + rs));

        return lastRsiValue;
    }

    // ✅ RSI inicial con configuración desde properties
    private double calculateInitialRsi() {
        int minDataPoints = config.getBitcoin().getRsi().getMinDataPoints();
        int period = config.getBitcoin().getRsi().getPeriod();

        if (recentPrices.size() < minDataPoints) {
            return 50.0;
        }

        double gains = 0;
        double losses = 0;

        for (int i = 1; i < minDataPoints; i++) {
            double diff = recentPrices.get(i) - recentPrices.get(i - 1);
            if (diff > 0) gains += diff;
            else losses -= diff;
        }

        avgGain = gains / period;
        avgLoss = losses / period;

        if (avgLoss == 0) return 100.0;

        double rs = avgGain / avgLoss;
        lastRsiValue = 100 - (100 / (1 + rs));

        return lastRsiValue;
    }

    // ✅ Actualización periódica con template desde properties
    private void sendPeriodicUpdate(double price, double rsi) {
        String template = config.getNotification().getTemplate().getPeriodic();
        String msg = String.format(template, price, rsi);
        notifier.sendMessage(msg);
    }

    // ✅ Alerta de cambio de precio con threshold configurable
    private void checkPriceChange(double price) {
        if (lastPrice == null || lastPrice == 0.0) return;

        double change = ((price - lastPrice) / lastPrice) * 100;
        double threshold = config.getBitcoin().getPrice().getChangeThreshold();

        if (Math.abs(change) >= threshold) {
            String template = config.getNotification().getTemplate().getPriceChange();
            String msg = String.format(template, change, price);
            notifier.sendMessage(msg);
        }
    }

    // ✅ Cruces de RSI con niveles configurables
    private void checkRSICross(double rsi, double price) {
        if (lastRsi == null || lastRsi == 0.0) return;

        int overboughtLevel = config.getBitcoin().getRsi().getOverboughtLevel();
        int oversoldLevel = config.getBitcoin().getRsi().getOversoldLevel();

        // Cruce hacia abajo del nivel de sobrecompra
        if (lastRsi > overboughtLevel && rsi <= overboughtLevel) {
            String template = config.getNotification().getTemplate().getRsiDown70();
            String msg = String.format(template, price, rsi);
            notifier.sendMessage(msg);
        }

        // Cruce hacia arriba del nivel de sobreventa
        if (lastRsi < oversoldLevel && rsi >= oversoldLevel) {
            String template = config.getNotification().getTemplate().getRsiUp30();
            String msg = String.format(template, price, rsi);
            notifier.sendMessage(msg);
        }
    }

    // ✅ Señales clásicas RSI con template configurable
    private SignalEvaluatorService.Signal checkClassicSignal(List<PricePoint> prices, double price, double rsi) {
        SignalEvaluatorService.Signal signal = signalEvaluator.evaluateRsiSignal(prices);

        if (signal == null || !signal.isActive()) {
            lastSignalType = SignalEvaluatorService.Signal.Type.HOLD;
            return signal;
        }

        if (signal.getType() == lastSignalType) {
            return signal;
        }

        lastSignalType = signal.getType();

        String template = config.getNotification().getTemplate().getClassicSignal();
        String msg = String.format(template,
                signal.getType().getMessage(),
                price,
                rsi
        );

        notifier.sendMessage(msg);

        return signal;
    }
}