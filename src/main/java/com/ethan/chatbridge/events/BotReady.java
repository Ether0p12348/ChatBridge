package com.ethan.chatbridge.events;

import com.ethan.chatbridge.ChatBridge;
import com.ethan.chatbridge.Payload;
import com.ethan.chatbridge.TranslateType;
import com.ethan.chatbridge.exceptions.HttpErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BotReady extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent e) {
        super.onReady(e);

        System.out.println("Bot is ready! Proceeding with command registration...");
        JDA jda = e.getJDA();
        registerLocaleContextCommands(jda);
    }

    private static CommandData localeContextCommands(@NotNull JDA jda, @NotNull Command.Type type, @NotNull String name) {
        CommandData cmd = Commands.context(type, name);
        List<DiscordLocale> locales = Arrays.stream(DiscordLocale.values()).toList();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < locales.size(); i++) {
            final String systemMessageContent = TranslateType.PLAIN.getSystemPrompt();
            final String userMessageContent = "(" + locales.get(i).getLocale() + ")" + name;

            CompletableFuture<String> future = new CompletableFuture<>();
            scheduler.schedule(() -> {
                translateAsync(systemMessageContent, userMessageContent, 1000).whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(result);
                    }
                });
            }, i * 22, TimeUnit.SECONDS); // Schedule each request with a 22-second interval

            futures.add(future);
        }

        // Wait for all translations to complete
        List<String> translated;
        try {
            translated = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to complete translations", e);
        }

        if (translated == null || translated.size() != locales.size()) {
            throw new RuntimeException("Could not find the expected number of translated locales.");
        }

        Map<DiscordLocale, String> translatedWithLocale = new HashMap<>();
        for (int i = 0; i < locales.size(); i++) {
            if (translated.get(i) != null) {
                translatedWithLocale.put(locales.get(i), translated.get(i));
            }
        }

        cmd.setNameLocalizations(translatedWithLocale);
        System.out.println("\"Translate\" MessageContextCommand successfully translated in all Discord locales.");
        scheduler.shutdown(); // Shutdown the scheduler
        return cmd;
    }

    public static CompletableFuture<String> translateAsync(@NotNull String systemMessageContent,
                                                           @NotNull String userMessageContent, int maxTokens) {
        final String API_URL = ChatBridge.secret.get("chatgpt", "url");
        final String API_KEY = ChatBridge.secret.get("chatgpt", "key");

        return CompletableFuture.supplyAsync(() -> {
            try {
                Payload payload = new Payload(null, systemMessageContent, userMessageContent, maxTokens);

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonPayload = objectMapper.writeValueAsString(payload);

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
                System.out.println(userMessageContent + " has completed.");
                return jsonNode.path("choices").get(0).path("message").path("content").asText().trim();

            } catch (URISyntaxException | IOException | InterruptedException | HttpErrorCode e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
