package com.cryptobot.service;

import com.cryptobot.model.PricePoint;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.List;

@Service
public class SignalEvaluatorService {

    public enum Signal { BUY, SELL, HOLD }

    public Signal evaluateRsiSignal(List<PricePoint> prices) {
        if (prices.size() < 14) return Signal.HOLD;

        BarSeries series = new BaseBarSeriesBuilder().withName("BTC-USD").build();

        for (PricePoint p : prices) {
            series.addBar(p.dateTime(), p.price(), p.price(), p.price(), p.price(), 0);
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        double rsiValue = rsi.getValue(series.getEndIndex()).doubleValue();

        System.out.println("RSI(14) actual: " + String.format("%.2f", rsiValue));

        if (rsiValue < 30) return Signal.BUY;
        if (rsiValue > 70) return Signal.SELL;
        return Signal.HOLD;
    }
}