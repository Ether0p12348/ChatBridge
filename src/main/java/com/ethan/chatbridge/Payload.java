package com.ethan.chatbridge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Payload {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private final List<Payload.Message> messages;

    @JsonProperty("max_tokens")
    private int maxTokens = 1000;

    public Payload() {
        this.messages = new ArrayList<>();
    }

    public Payload(@Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, int maxTokens) {
        this.model = (model != null) ? model : ChatBridge.secret.get("chatgpt", "model");
        this.maxTokens = maxTokens;
        this.messages = new ArrayList<>();
        this.messages.add(new Payload.Message(Payload.MessageRole.SYSTEM, systemMessageContent));
        this.messages.add(new Payload.Message(Payload.MessageRole.USER, userMessageContent));
    }

    public Payload setModel(@NotNull String model) {
        this.model = model;
        return this;
    }

    public Payload setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public Payload addSystemMessage(@NotNull String content) {
        this.messages.add(new Payload.Message(Payload.MessageRole.SYSTEM, content));
        return this;
    }

    public Payload addUserMessage(@NotNull String content) {
        this.messages.add(new Payload.Message(Payload.MessageRole.USER, content));
        return this;
    }

    public Payload addMessage(@NotNull Payload.Message message) {
        this.messages.add(message);
        return this;
    }

    public String getModel() {
        return model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public List<Payload.Message> getMessages() {
        return messages;
    }

    public static class Message {
        @JsonIgnore
        private final Payload.MessageRole role;

        @JsonProperty("content")
        private final String content;

        public Message(Payload.MessageRole role, String content) {
            this.role = role;
            this.content = content;
        }

        @JsonProperty("role")
        public String getRole() {
            return role.getRole();
        }

        public String getContent() {
            return content;
        }
    }

    public enum MessageRole {
        SYSTEM("system"),
        USER("user");

        private final String role;

        MessageRole(String role) {
            this.role = role;
        }

        public String getRole() {
            return this.role;
        }

        public String toString() {
            return this.role;
        }
    }
}