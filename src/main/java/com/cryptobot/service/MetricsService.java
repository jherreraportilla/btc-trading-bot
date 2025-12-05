package com.cryptobot.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final BitcoinPriceService priceService;

    public MetricsService(MeterRegistry registry, BitcoinPriceService priceService) {
        this.priceService = priceService;

        // ✅ RSI actual
        Gauge.builder("btc_rsi_value", () -> safeDouble(priceService.getLastKnownRsi()))
                .description("RSI actual calculado por el bot")
                .register(registry);

        // ✅ Precio actual
        Gauge.builder("btc_price_usd", () -> safeDouble(priceService.getLastKnownPrice()))
                .description("Precio actual de BTC en USD")
                .register(registry);

        // ✅ Tipo de señal actual (0=HOLD, 1=BUY, 2=SELL)
        Gauge.builder("btc_signal_type", () -> {
            return switch (priceService.getLastSignalType()) {
                case BUY -> 1;
                case SELL -> 2;
                default -> 0;
            };
        })
        .description("Señal actual del bot: 0=HOLD, 1=BUY, 2=SELL")
        .register(registry);

        // ✅ Timestamp de última ejecución (epoch seconds)
        Gauge.builder("bot_last_execution_timestamp", () -> {
            if (priceService.getLastExecutionTime() == null) return 0;
            return priceService.getLastExecutionTime().getEpochSecond();
        })
        .description("Última ejecución del scheduler")
        .register(registry);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}
