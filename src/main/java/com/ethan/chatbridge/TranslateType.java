package com.ethan.chatbridge;

public enum TranslateType {
    PLAIN("Plain Translation."),
    DECORATED("Decorated Translation.");

    private final String systemPrompt;

    TranslateType(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String toString() {
        return systemPrompt;
    }
}
