package com.ethan.chatbridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BatchTranslator {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "";

    public static void main(String[] args) {
        // Example data for batch processing
        List<Map<String, Object>> requests = List.of(
                Map.of("model", "ft:gpt-4o-mini-2024-07-18:personal:chatbridge-6-1:A2G2goAB",
                        "messages", List.of(Map.of("role", "system", "content", "Plain Translation."),
                                Map.of("role", "user", "content", "(bg)Translate")),
                        "max_tokens", 1000),
                Map.of("model", "ft:gpt-4o-mini-2024-07-18:personal:chatbridge-6-1:A2G2goAB",
                        "messages", List.of(Map.of("role", "system", "content", "Plain Translation."),
                                Map.of("role", "user", "content", "(zh-CN)Translate")),
                        "max_tokens", 1000)
                // Add more requests as needed
        );

        BatchTranslator translator = new BatchTranslator();
        translator.processBatch(requests);
    }

    public void processBatch(List<Map<String, Object>> requests) {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        // Create a list to hold all the futures
        List<CompletableFuture<String>> futures = requests.stream()
                .map(request -> {
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(request);

                        System.out.println(jsonPayload);

                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(new URI(API_URL))
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                                .build();

                        // Send the request asynchronously
                        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                                .thenApply(response -> {
                                    try {
                                        if (response.statusCode() != 200) {
                                            throw new RuntimeException("Failed with HTTP code: " + response.statusCode());
                                        }
                                        JsonNode jsonNode = objectMapper.readTree(response.body());
                                        return jsonNode.path("choices").get(0).path("message").path("content").asText().trim();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    } catch (URISyntaxException | IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        // Wait for all futures to complete
        try {
            for (CompletableFuture<String> future : futures) {
                System.out.println("Translation: " + future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
