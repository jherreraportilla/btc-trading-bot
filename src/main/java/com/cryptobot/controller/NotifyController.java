package com.cryptobot.controller;

import com.cryptobot.notification.WhatsAppNotifier;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class NotifyController {

    private final WhatsAppNotifier notifier;

    @Autowired
    public NotifyController(WhatsAppNotifier notifier) {
        this.notifier = notifier;
    }

    @GetMapping
    public Map<String, Object> sendMessage(@RequestParam(required = false) String msg) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (msg == null || msg.isBlank()) {
            response.put("status", "ERROR");
            response.put("message", "El parámetro 'msg' es obligatorio");
            return response;
        }

        notifier.sendMessage(msg);

        response.put("status", "OK");
        response.put("sent", msg);
        return response;
    }
    
    @PostMapping
    public Map<String, Object> sendMessagePost(@RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (body == null || !body.containsKey("msg") || body.get("msg").isBlank()) {
            response.put("status", "ERROR");
            response.put("message", "El campo 'msg' es obligatorio");
            return response;
        }

        String msg = body.get("msg");
        notifier.sendMessage(msg);

        response.put("status", "OK");
        response.put("sent", msg);
        return response;
    }

}
