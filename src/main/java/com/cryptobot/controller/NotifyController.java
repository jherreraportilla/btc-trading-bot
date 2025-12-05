package com.cryptobot.controller;

import com.cryptobot.notification.WhatsAppNotifier;
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
    public String sendMessage(@RequestParam String msg) {
        notifier.sendMessage(msg);
        return "Mensaje enviado: " + msg;
    }
}
