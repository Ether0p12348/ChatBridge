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
                Commands.slash("translate", "translate a message or input to another language")//,
                //localeContextCommands(jda, Command.Type.MESSAGE, "Translate")

        ).queue();

        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus("https://chatbridge.ethanrobins.com"));
    }

    private static CommandData localeContextCommands (@NotNull JDA jda, @NotNull Command.Type type, @NotNull String name) {
        CommandData cmd = Commands.context(type, name);
        List<DiscordLocale> locales = Arrays.stream(DiscordLocale.values()).toList();
        PayloadSetOld payloadSet = new PayloadSetOld();

        for (DiscordLocale locale : locales) {
            payloadSet.addPayload(null, TranslateType.PLAIN.getSystemPrompt(), "(" + locale.getLocale() + ")" + name);
        }

        List<String> translated = translate(payloadSet, null);

        Map<DiscordLocale, String> translatedWithLocale = new HashMap<>();
        if (translated != null) {
            for (int i = 0; i < locales.size(); i++) {
                if (translated.get(i) != null) {
                    translatedWithLocale.put(locales.get(i), translated.get(i));
                }
            }
        } else {
            throw new RuntimeException("Could not find any translated locales.");
        }

        cmd.setNameLocalizations(translatedWithLocale);
        System.out.println("\"Translate\" MessageContextCommand successfully translated in all Discord locales.");
        return cmd;
    }

    public static List<String> translate (@NotNull PayloadSetOld payloads, @Nullable List<MessageContextInteractionEvent> events) {
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
                String translatedtext = jsonNode.path("choices").get(0).path("message").path("content").asText().trim();
                translated.add(translatedtext);
            }

            return translated;
        } catch (URISyntaxException | IOException | InterruptedException | HttpErrorCode e) {
            if (events != null) {
                for (MessageContextInteractionEvent event : events) {
                    event.getHook().editOriginal("**Failed to Translate the message. Please report this to <@269490769583276032>.**").queue();
                }
            }
            e.printStackTrace();
            return null;
        }
    }
}
