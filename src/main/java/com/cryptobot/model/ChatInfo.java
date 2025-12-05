package com.cryptobot.model;

public class ChatInfo {
    private String jid;
    private String name;

    public ChatInfo(String jid, String name) {
        this.jid = jid;
        this.name = name;
    }

    public String getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }
}
