package com.cryptobot.service;

import org.springframework.stereotype.Service;

import com.cryptobot.model.ChatInfo;

import java.util.List;
import java.util.ArrayList;

@Service
public class WhatsAppService {

    public List<ChatInfo> getAllChats() {
        // Aquí iría la lógica real para obtener chats desde tu API de WhatsApp
        List<ChatInfo> chats = new ArrayList<>();
        chats.add(new ChatInfo("34671072929-123456@g.us", "BOT-TRADIN"));
        chats.add(new ChatInfo("34671072929@s.whatsapp.net", "Mi número"));
        return chats;
    }
}
