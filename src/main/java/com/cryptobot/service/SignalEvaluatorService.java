package com.cryptobot.service;

import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.Duration;
import java.util.List;

@Service
public class SignalEvaluatorService {

    public static class Signal {
        private final Type type;
        private final double rsi;
        private final double price;

        public Signal(Type type, double rsi, double price) {
            this.type = type;
            this.rsi = rsi;
            this.price = price;
        }

        public Type getType() { return type; }
        public double getRsi() { return rsi; }
        public double getPrice() { return price; }
        public boolean isActive() { return type != Type.HOLD; }

        public enum Type {
            BUY("COMPRA FUERTE - RSI < 30"),
            SELL("VENTA FUERTE - RSI > 70"),
            HOLD("MANTENER - Sin señal");

            private final String message;
            Type(String message) { this.message = message; }
            public String getMessage() { return message; }
        }
    }

    public Signal evaluateRsiSignal(List<PricePoint> prices) {

        // ✅ Validación inicial
        if (prices == null || prices.size() < 15) {
            double lastPrice = (prices != null && !prices.isEmpty())
                    ? prices.get(prices.size() - 1).price()
                    : 0;

            return new Signal(Signal.Type.HOLD, -1, lastPrice);
        }

        // ✅ Ordenar por fecha (muy importante)
        prices.sort((a, b) -> a.dateTime().compareTo(b.dateTime()));

        BarSeries series = new BaseBarSeriesBuilder().withName("BTC-USD").build();

        // ✅ Construcción segura de la serie
        for (PricePoint p : prices) {
            series.addBar(
                    Duration.ofHours(1),
                    p.dateTime(),
                    p.price(),
                    p.price(),
                    p.price(),
                    p.price(),
                    0
            );
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        double rsi_value = rsiIndicator.getValue(series.getEndIndex()).doubleValue();
        double currentPrice = prices.get(prices.size() - 1).price();

        // ✅ Manejo de NaN
        if (Double.isNaN(rsi_value)) {
            return new Signal(Signal.Type.HOLD, -1, currentPrice);
        }

        System.out.println("RSI(14): " + String.format("%.2f", rsi_value) +
                " | Precio: $" + String.format("%,.0f", currentPrice));

        // ✅ Señales
        if (rsi_value < 30) {
            return new Signal(Signal.Type.BUY, rsi_value, currentPrice);
        }
        if (rsi_value > 70) {
            return new Signal(Signal.Type.SELL, rsi_value, currentPrice);
        }

        return new Signal(Signal.Type.HOLD, rsi_value, currentPrice);
    }
}
