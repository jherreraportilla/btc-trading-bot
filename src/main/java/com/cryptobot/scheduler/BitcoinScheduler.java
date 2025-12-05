package com.cryptobot.scheduler;

import com.cryptobot.service.BitcoinPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BitcoinScheduler {

    private final BitcoinPriceService priceService;

    @Autowired
    public BitcoinScheduler(BitcoinPriceService priceService) {
        this.priceService = priceService;
    }

    @Scheduled(cron = "0 0/30 * * * *") // ✅ cada 30 minutos
    public void run() {
        try {
            priceService.process();
        } catch (Exception e) {
            System.err.println("⚠️ ERROR en el scheduler BTC: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
