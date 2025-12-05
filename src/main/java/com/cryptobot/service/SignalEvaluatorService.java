package com.cryptobot.service;

import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    // ✅ Calcular RSI actual
    public double calculateRSI(List<PricePoint> prices) {

        if (prices == null || prices.size() < 15) return -1;

        prices.sort((a, b) -> a.dateTime().compareTo(b.dateTime()));

        BarSeries series = new BaseBarSeriesBuilder().withName("BTC-USD").build();

        for (PricePoint p : prices) {

            ZonedDateTime zdt = p.dateTime().atZone(ZoneId.of("UTC"));

            series.addBar(
                    Duration.ofHours(1),
                    zdt,
                    p.price(),
                    p.price(),
                    p.price(),
                    p.price(),
                    0
            );
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        double rsi = rsiIndicator.getValue(series.getEndIndex()).doubleValue();
        return Double.isNaN(rsi) ? -1 : rsi;
    }

    // ✅ Señal clásica BUY / SELL / HOLD
    public Signal evaluateRsiSignal(List<PricePoint> prices) {

        if (prices == null || prices.size() < 15) {
            double lastPrice = (prices != null && !prices.isEmpty())
                    ? prices.get(prices.size() - 1).price()
                    : 0;

            return new Signal(Signal.Type.HOLD, -1, lastPrice);
        }

        prices.sort((a, b) -> a.dateTime().compareTo(b.dateTime()));

        BarSeries series = new BaseBarSeriesBuilder().withName("BTC-USD").build();

        for (PricePoint p : prices) {

            ZonedDateTime zdt = p.dateTime().atZone(ZoneId.of("UTC"));

            series.addBar(
                    Duration.ofHours(1),
                    zdt,
                    p.price(),
                    p.price(),
                    p.price(),
                    p.price(),
                    0
            );
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 14);

        double rsiValue = rsiIndicator.getValue(series.getEndIndex()).doubleValue();
        double currentPrice = prices.get(prices.size() - 1).price();

        if (Double.isNaN(rsiValue)) {
            return new Signal(Signal.Type.HOLD, -1, currentPrice);
        }

        if (rsiValue < 30) return new Signal(Signal.Type.BUY, rsiValue, currentPrice);
        if (rsiValue > 70) return new Signal(Signal.Type.SELL, rsiValue, currentPrice);

        return new Signal(Signal.Type.HOLD, rsiValue, currentPrice);
    }
}
