package com.cryptobot.controller;

import com.cryptobot.model.ChatInfo;
import com.cryptobot.service.WhatsAppService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final WhatsAppService whatsAppService;

    public ChatController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public Map<String, Object> getChats() {

        Map<String, Object> response = new LinkedHashMap<>();

        try {
            List<ChatInfo> chats = whatsAppService.getAllChats();

            if (chats == null || chats.isEmpty()) {
                response.put("status", "OK");
                response.put("message", "No se encontraron chats");
                response.put("chats", List.of());
                return response;
            }

            response.put("status", "OK");
            response.put("count", chats.size());
            response.put("chats", chats);
            return response;

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "No se pudieron obtener los chats");
            response.put("details", e.getMessage());
            return response;
        }
    }
}
