package com.cryptobot.controller;

import com.cryptobot.model.ChatInfo;
import com.cryptobot.service.WhatsAppService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final WhatsAppService whatsAppService;

    public ChatController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public List<ChatInfo> getChats() {
        return whatsAppService.getAllChats();
    }
}
