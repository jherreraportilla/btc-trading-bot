package com.cryptobot.service;

import com.cryptobot.client.CoinGeckoClient;
import com.cryptobot.model.PricePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BitcoinPriceService {

    private final CoinGeckoClient coinGeckoClient;
    private final SignalEvaluatorService signalEvaluator;

    @Autowired
    public BitcoinPriceService(CoinGeckoClient coinGeckoClient, SignalEvaluatorService signalEvaluator) {
        this.coinGeckoClient = coinGeckoClient;
        this.signalEvaluator = signalEvaluator;
    }

    public SignalEvaluatorService.Signal checkAndGetSignal() throws Exception {
        // Últimas 48 horas hourly → más que suficiente para RSI 14
        List<PricePoint> prices = coinGeckoClient.getLastHourlyPrices(48);
        return signalEvaluator.evaluateRsiSignal(prices);
    }
}