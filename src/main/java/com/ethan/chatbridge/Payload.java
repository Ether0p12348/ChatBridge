package com.ethan.chatbridge;

import com.ethan.chatbridge.exceptions.HttpErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Payload {
    @JsonIgnore
    private String id = "payload_" + ChatBridge.genId(8);

    @JsonProperty("model")
    private String model = ChatBridge.secret.get("chatgpt", "model");

    @JsonProperty("messages")
    private final List<Payload.Message> messages = new ArrayList<>();

    @JsonProperty("max_tokens")
    private int maxTokens = 1000;

    @JsonIgnore
    private String result = null;

    public Payload(){}

    public Payload(@Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, @Nullable Integer maxTokens) {
        this.model = (model != null) ? model : this.model;
        this.maxTokens = (maxTokens != null) ? maxTokens : this.maxTokens;
        this.messages.add(new Payload.Message(Payload.MessageRole.SYSTEM, systemMessageContent));
        this.messages.add(new Payload.Message(Payload.MessageRole.USER, userMessageContent));
    }

    public Payload(@NotNull String id, @Nullable String model, @NotNull String systemMessageContent, @NotNull String userMessageContent, @Nullable Integer maxTokens) {
        this.model = (model != null) ? model : this.model;
        this.maxTokens = (maxTokens != null) ? maxTokens : this.maxTokens;
        this.messages.add(new Payload.Message(Payload.MessageRole.SYSTEM, systemMessageContent));
        this.messages.add(new Payload.Message(Payload.MessageRole.USER, userMessageContent));
        this.id = id;
    }

    @JsonIgnore
    public Payload setId (@NotNull String id) {
        this.id = id;
        return this;
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

    @JsonIgnore
    public String getId() {
        return id;
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

    @JsonIgnore
    public Payload.Message getSystemMessage() {
        for (Payload.Message m : this.messages) {
            if (m.role.equals(Payload.MessageRole.SYSTEM)) {
                return m;
            }
        }
        return null;
    }

    @JsonIgnore
    public Payload.Message getUserMessage() {
        for (Payload.Message m : this.messages) {
            if (m.role.equals(Payload.MessageRole.USER)) {
                return m;
            }
        }
        return null;
    }

    @JsonIgnore
    public String getResult() {
        return this.result;
    }

    @JsonIgnore
    public String queue() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = new CompletableFuture<>();
        translateAsync().whenComplete((result, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                future.complete(result);
            }
        });

        //try {
            return future.get();
        /*} catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to complete translation", e);
        }*/
    }

    @JsonIgnore
    public CompletableFuture<String> translateAsync() {
        final String API_URL = ChatBridge.secret.get("chatgpt", "url");
        final String API_KEY = ChatBridge.secret.get("chatgpt", "key");

        return CompletableFuture.supplyAsync(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonPayload = objectMapper.writeValueAsString(this);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(API_URL))
                        .header("Authorization", "Bearer " + API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new HttpErrorCode(response.statusCode(), "Failed to translate the text. HTTP Error Code: " + response.statusCode() + "\n" + response.body());
                }

                JsonNode jsonNode = objectMapper.readTree(response.body());
                String result = jsonNode.path("choices").get(0).path("message").path("content").asText().trim();

                System.out.println(this.getId() + " has completed - " + result);

                this.result = result;
                return result;
            } catch (URISyntaxException | IOException | InterruptedException | HttpErrorCode e) {
                e.printStackTrace();
                return null;
            }
        });
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