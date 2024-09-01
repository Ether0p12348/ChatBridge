package com.ethan.chatbridge;

import com.ethan.chatbridge.events.MessageInteraction;
import com.ethan.chatbridge.exceptions.HttpErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.ini4j.Ini;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ChatBridge {
    public static JDA jda;
    public static Ini secret;

    public static void main (String[] args) {
        URL resource = ChatBridge.class.getClassLoader().getResource("secret.ini");
        String token;
        try {
            if (resource == null) {
                throw new RuntimeException("Secret file not found!");
            }

            /*File configFile = new File(resource.getFile());
            secret = new Ini(configFile);
            token = secret.get("discord", "token");*/
            try(InputStream inputStream = resource.openStream()){
                secret = new Ini(inputStream);
                token = secret.get("discord", "token");
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        }

        JDABuilder jdaBuilder = JDABuilder.createDefault(token);
        jda = jdaBuilder
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(
                        new MessageInteraction()
                )
                .build();



        jda.updateCommands().addCommands(
                Commands.slash("translate", "translate a message or input to another language"),
                localeContextCommands(jda, Command.Type.MESSAGE, "Translate")

        ).queue();

        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus("https://chatbridge.ethanrobins.com"));
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

    /*private static CommandData localeContextCommands (@NotNull JDA jda, @NotNull Command.Type type, @NotNull String name) {
        CommandData cmd = Commands.context(type, name);
        List<DiscordLocale> locales = Arrays.stream(DiscordLocale.values()).toList();
        BatchPayload batchPayload = new BatchPayload();

        for (int i = 0; i < locales.size(); i++) {
            batchPayload.addRequest("request-" + i, null, TranslateType.PLAIN.getSystemPrompt(), "(" + locales.get(i).getLocale() + ")" + name, 1000);
        }

        List<String> translated = translate(batchPayload, null);

        if (translated == null || translated.size() != locales.size()) {
            throw new RuntimeException("Could not find the expected number of translated locales.");
        }

        Map<DiscordLocale, String> translatedWithLocale = new HashMap<>();
        for (int i = 0; i < locales.size(); i++) {
            translatedWithLocale.put(locales.get(i), translated.get(i));
        }

        cmd.setNameLocalizations(translatedWithLocale);
        System.out.println("\"Translate\" MessageContextCommand successfully translated in all Discord locales.");
        return cmd;
    }

    public static List<String> translate(@NotNull BatchPayload payloads, @Nullable List<MessageContextInteractionEvent> events) {
        List<String> translated = new ArrayList<>();
        final String API_URL = ChatBridge.secret.get("chatgpt", "url");
        final String API_KEY = ChatBridge.secret.get("chatgpt", "key");

        HttpClient client = HttpClient.newHttpClient();

        try {
            ObjectMapper outMapper = new ObjectMapper();
            String jsonPayload = outMapper.writeValueAsString(payloads);

            System.out.println(jsonPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new HttpErrorCode(response.statusCode(), "Failed to translate the texts. HTTP Error Code: " + response.statusCode() + "\n" + response.body());
            }

            ObjectMapper inMapper = new ObjectMapper();
            ArrayNode jsonNodes = inMapper.readValue(response.body(), ArrayNode.class);

            for (JsonNode jsonNode : jsonNodes) {
                String translatedText = jsonNode.path("choices").get(0).path("message").path("content").asText().trim();
                translated.add(translatedText);
            }

            return translated;
        } catch (URISyntaxException | IOException | InterruptedException | HttpErrorCode e) {
            if (events != null) {
                for (MessageContextInteractionEvent event : events) {
                    event.getHook().editOriginal("**Failed to Translate the message. Please report this to <@269490769583276032>.**").queue(); // Make sure to translate this to the user's locale when single requests are possible
                }
            }
            e.printStackTrace();
            return null;
        }
    }*/
}
