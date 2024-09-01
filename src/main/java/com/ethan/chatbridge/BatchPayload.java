package com.ethan.chatbridge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JsonSerialize(using = BatchPayloadSerializer.class)
public class BatchPayload {
    @JsonIgnore
    private final List<RequestPayload> requests = new ArrayList<>();

    public BatchPayload addRequest(@NotNull String customId, @Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, int maxTokens) {
        RequestPayload request = new RequestPayload(customId, model, systemMessageContent, userMessageContent, maxTokens);
        requests.add(request);
        return this;
    }

    public BatchPayload addRequest(RequestPayload request) {
        requests.add(request);
        return this;
    }

    public List<RequestPayload> getRequests() {
        return this.requests;
    }

    public static class RequestPayload {
        @JsonProperty("custom_id")
        private final String customId;

        @JsonProperty("method")
        private final String method = "POST";

        @JsonProperty("url")
        private final String url = "/v1/chat/completions";

        @JsonProperty("body")
        private final Body body;

        public RequestPayload(@NotNull String customId, @Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, int maxTokens) {
            this.customId = customId;
            this.body = new Body(model, systemMessageContent, userMessageContent, maxTokens);
        }

        public RequestPayload(@NotNull String customId, @NotNull Body body) {
            this.customId = customId;
            this.body = body;
        }
    }

    public static class Body {

        @JsonProperty("model")
        private String model;

        @JsonProperty("messages")
        private final List<Message> messages;

        @JsonProperty("max_tokens")
        private int maxTokens = 1000;

        public Body() {
            this.messages = new ArrayList<>();
        }

        public Body(@Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, int maxTokens) {
            this.model = (model != null) ? model : ChatBridge.secret.get("chatgpt", "model");
            this.maxTokens = maxTokens;
            this.messages = new ArrayList<>();
            this.messages.add(new Message(MessageRole.SYSTEM, systemMessageContent));
            this.messages.add(new Message(MessageRole.USER, userMessageContent));
        }

        public Body setModel(@NotNull String model) {
            this.model = model;
            return this;
        }

        public Body setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Body addSystemMessage(@NotNull String content) {
            this.messages.add(new Message(MessageRole.SYSTEM, content));
            return this;
        }

        public Body addUserMessage(@NotNull String content) {
            this.messages.add(new Message(MessageRole.USER, content));
            return this;
        }

        public Body addMessage(@NotNull Message message) {
            this.messages.add(message);
            return this;
        }

        public String getModel() {
            return model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public static class Message {
            @JsonIgnore
            private final MessageRole role;

            @JsonProperty("content")
            private final String content;

            public Message(MessageRole role, String content) {
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
}