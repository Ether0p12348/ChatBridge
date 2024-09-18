package com.ethan.chatbridge;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.HashMap;
import java.util.Map;

public class EndUserError {
    private final Exception exception;
    private final Map<DiscordLocale, String> localeMessages = new HashMap<>();

    public EndUserError(Exception exception, Map<DiscordLocale, String> localeMessages) {
        this.exception = exception;
        this.localeMessages.putAll(localeMessages);
    }

    public Exception getException() {
        return this.exception;
    }

    public Map<DiscordLocale, String> getLocaleMessages() {
        return this.localeMessages;
    }
}
