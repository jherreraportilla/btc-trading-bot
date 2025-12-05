package com.cryptobot.controller;

import com.cryptobot.service.BitcoinPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class StatusController {

    private final BitcoinPriceService service;

    public StatusController(BitcoinPriceService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {

        Map<String, Object> status = new LinkedHashMap<>();

        Double price = service.getLastKnownPrice();
        Double rsi = service.getLastKnownRsi();
        var signal = service.getLastSignalType();
        var lastExec = service.getLastExecutionTime();

        status.put("price", price != null ? price : "Aún no disponible");
        status.put("rsi", rsi != null ? rsi : "Pendiente de primera ejecución");
        status.put("signal", signal != null ? signal.name() : "Sin señal todavía");
        status.put("lastExecution", lastExec != null ? lastExec.toString() : "Scheduler aún no ha corrido");
        status.put("status", "OK");

        return status;
    }
}
