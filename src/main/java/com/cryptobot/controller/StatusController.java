package com.cryptobot.controller;

import com.cryptobot.service.BitcoinPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/status")
public class StatusController {

    private final BitcoinPriceService priceService;

    @Autowired
    public StatusController(BitcoinPriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public Map<String, Object> getStatus() {
        return Map.of(
                "price", priceService.getLastKnownPrice(),
                "rsi", priceService.getLastKnownRsi(),
                "lastSignal", priceService.getLastSignalType().name(),
                "lastExecution", priceService.getLastExecutionTime().toString(),
                "status", "OK"
        );
    }
}
