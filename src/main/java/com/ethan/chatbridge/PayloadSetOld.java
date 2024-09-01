package com.ethan.chatbridge;

import com.ethan.chatbridge.exceptions.IllegalObjectClassTypeException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonSerialize(using = PayloadSetSerializerOld.class)
public class PayloadSetOld {
    private final List<Payload> payloads = new ArrayList<>();

    public PayloadSetOld addPayload (@Nullable String model, @NotNull Object systemMessage, @NotNull Object userMessage, @NotNull int maxTokens) throws IllegalObjectClassTypeException {
        Payload payload = new Payload().setMaxTokens(maxTokens);

        if (systemMessage instanceof Payload.System) {
            payload.setSystemMessage((Payload.System) systemMessage);
        } else if (systemMessage instanceof String) {
            payload.setSystemMessageContent((String) systemMessage);
        } else {
            throw new IllegalObjectClassTypeException("systemMessage can only be and instance of com.ethanrobins.chatbridge.PayloadSet.Payload.System OR String");
        }

        if (userMessage instanceof Payload.User) {
            payload.setUserMessage((Payload.User) userMessage);
        } else if (userMessage instanceof String) {
            payload.setUserMessageContent((String) userMessage);
        } else {
            throw new IllegalObjectClassTypeException("userMessage can only be and instance of com.ethanrobins.chatbridge.PayloadSet.Payload.User OR String");
        }

        if (model != null) {
            payload.setModel(model);
        }

        payloads.add(payload);

        return this;
    }

    public PayloadSetOld addPayload (@Nullable String model, @NotNull Object systemMessage, @NotNull Object userMessage) throws IllegalObjectClassTypeException {
        Payload payload = new Payload();

        if (systemMessage instanceof Payload.System) {
            payload.setSystemMessage((Payload.System) systemMessage);
        } else if (systemMessage instanceof String) {
            payload.setSystemMessageContent((String) systemMessage);
        } else {
            throw new IllegalObjectClassTypeException("systemMessage can only be and instance of com.ethanrobins.chatbridge.PayloadSet.Payload.System OR String");
        }

        if (userMessage instanceof Payload.User) {
            payload.setUserMessage((Payload.User) userMessage);
        } else if (userMessage instanceof String) {
            payload.setUserMessageContent((String) userMessage);
        } else {
            throw new IllegalObjectClassTypeException("userMessage can only be and instance of com.ethanrobins.chatbridge.PayloadSet.Payload.User OR String");
        }

        if (model != null) {
            payload.setModel(model);
        }

        payloads.add(payload);

        return this;
    }

    public PayloadSetOld addPayload (@NotNull Payload payload) {
        this.payloads.add(payload);
        return this;
    }

    @JsonIgnore
    public List<Payload> getPayloads() {
        return this.payloads;
    }

    public static class Payload {
        @JsonProperty("model")
        private String model;
        //@JsonProperty("messages")
        @JsonIgnore
        private final Object[] messages = new Object[] {new System(), new User()};
        @JsonProperty("max_tokens")
        private int maxTokens = 1000;

        public Payload (@NotNull String model) {
            this.model = model;
        }

        public Payload () {
            this.model = ChatBridge.secret.get("chatgpt", "model");
        }

        public Payload setModel (@NotNull String model) {
            this.model = model;
            return this;
        }

        public Payload setSystemMessage (@NotNull System systemMessage) {
            this.messages[0] = systemMessage;
            return this;
        }

        public Payload setSystemMessageContent (@NotNull String systemMessageContent) {
            System systemMsg = (System) this.messages[0];
            systemMsg.setContent(systemMessageContent);
            this.messages[0] = systemMsg;
            return this;
        }

        public Payload setUserMessage (@NotNull User userMessage) {
            this.messages[1] = userMessage;
            return this;
        }

        public Payload setUserMessageContent (@NotNull String userMessageContent) {
            User userMsg = (User) this.messages[1];
            userMsg.setContent(userMessageContent);
            this.messages[1] = userMsg;
            return this;
        }

        @JsonProperty("messages")
        public List<Object> getMessages() {
            return Arrays.asList(this.messages[0], this.messages[1]);
        }

        public Payload setMaxTokens (int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public String getModel() {
            return this.model;
        }

        @JsonIgnore
        public System getSystemMessage() {
            return (System) this.messages[0];
        }

        @JsonIgnore
        public User getUserMessage() {
            return (User) this.messages[1];
        }

        public int getMaxTokens() {
            return this.maxTokens;
        }

        public static class System {
            private String role = "system";
            private String content;

            public System (@NotNull String content) {
                this.content = content;
            }

            public System () {
                this.content = "Translate with ChatBridge";
            }

            public System setContent (@NotNull String content) {
                this.content = content;
                return this;
            }

            public String getRole() {
                return this.role;
            }

            public String getContent() {
                return this.content;
            }
        }

        public static class User {
            private String role = "user";
            private String content;

            public User (@NotNull String content) {
                this.content = content;
            }

            public User () {
                this.content = null;
            }

            public User setContent (@NotNull String content) {
                this.content = content;
                return this;
            }

            public String getRole() {
                return this.role;
            }

            public String getContent() {
                return this.content;
            }
        }
    }
}
