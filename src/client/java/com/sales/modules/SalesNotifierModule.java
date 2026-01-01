package com.sales.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class SalesNotifierModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("SalesNotifier");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private String webhookUrl = "";
    private boolean enabled = true;
    private boolean notifyBlackMarket = true;
    private boolean notifyMerchant = true;
    private boolean notifyBoss = true;

    private boolean merchantRestockDetected = false;
    private long lastMerchantMessageTime = 0;
    private static final long MERCHANT_MESSAGE_TIMEOUT = 3000;
    private List<String> replenishedItems = new ArrayList<>();
    private boolean bossSpawnDetected = false;
    private String bossName = "";
    private String bossRating = "";

    public SalesNotifierModule() {
        this.webhookUrl = "";
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setNotifyBlackMarket(boolean notifyBlackMarket) {
        this.notifyBlackMarket = notifyBlackMarket;
    }

    public void setNotifyMerchant(boolean notifyMerchant) {
        this.notifyMerchant = notifyMerchant;
    }

    public void setNotifyBoss(boolean notifyBoss) {
        this.notifyBoss = notifyBoss;
    }

    public void onActivate() {
        merchantRestockDetected = false;
        lastMerchantMessageTime = 0;
        replenishedItems.clear();
        bossSpawnDetected = false;
        bossName = "";
        bossRating = "";
    }

    public void onDeactivate() {
        merchantRestockDetected = false;
        lastMerchantMessageTime = 0;
        replenishedItems.clear();
        bossSpawnDetected = false;
        bossName = "";
        bossRating = "";
    }

    public void onChatMessage(Text message) {
        String plainMessage = message.getString().replaceAll("§[0-9a-fk-or]", "");

        if (!enabled || webhookUrl.isEmpty()) {
            return;
        }

        if (notifyBlackMarket && (plainMessage.contains("The Black Market has been restocked!") ||
                                  plainMessage.contains("Black Market has been restocked"))) {
            sendDiscordNotification("🛒 **Black Market Restocked!**", "The Black Market is now available for shopping!");
            return;
        }

        if (notifyMerchant && (plainMessage.contains("NPC's Back in Stock") ||
                               plainMessage.contains("NPCs Back in Stock") ||
                               plainMessage.contains("Back in Stock"))) {
            merchantRestockDetected = true;
            lastMerchantMessageTime = System.currentTimeMillis();
            replenishedItems.clear();
        }

        if (notifyMerchant && merchantRestockDetected) {
            if (plainMessage.trim().startsWith("+ [") && plainMessage.contains("]")) {
                replenishedItems.add(plainMessage.trim());
                return;
            }
        }

        if (notifyBoss) {
            if (plainMessage.contains("] Has Spawned!")) {
                int startBracket = plainMessage.indexOf('[');
                int endBracket = plainMessage.indexOf(']');
                if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
                    bossName = plainMessage.substring(startBracket + 1, endBracket);
                    bossSpawnDetected = true;
                    bossRating = "";
                }
            } else if (bossSpawnDetected && plainMessage.contains("(Rating)") && plainMessage.contains("→")) {
                int starStart = plainMessage.indexOf('→');
                int ratingStart = plainMessage.indexOf('(');
                if (starStart != -1 && ratingStart != -1 && ratingStart > starStart) {
                    bossRating = plainMessage.substring(starStart + 1, ratingStart).trim();
                    sendDiscordNotification(" **Boss Spawn**", bossName + " Has Spawned!\n" + bossRating);
                    bossSpawnDetected = false;
                    bossName = "";
                    bossRating = "";
                }
            }
        }
    }

    private String escapeJson(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
    }

    private void sendDiscordNotification(String title, String description) {
        if (webhookUrl.isEmpty()) {
            return;
        }

        String safeTitle = escapeJson(title);
        String safeDescription = escapeJson(description);

        String titleField = safeTitle.isEmpty() ? "" : String.format("\"title\": \"%s\", ", safeTitle);
        String jsonPayload = String.format(
            "{\"username\": \"Sales Notifier\", \"embeds\": [{%s\"description\": \"%s\", \"color\": 3066993}]}",
            titleField,
            safeDescription
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
            }
        });
    }

    private void sendConsolidatedMerchantNotification() {
        if (replenishedItems.isEmpty()) {
            return;
        }

        StringBuilder description = new StringBuilder();
        description.append("🛍️ **Merchant Restocked!**\n");
        description.append("Merchant stocks have been replenished!\n\n");
        description.append("**Replenished NPCs:**\n");

        for (String item : replenishedItems) {
            description.append(item.trim()).append("\n");
        }

        sendDiscordNotification("Merchant Update", description.toString().trim());
    }

    public void onClientTick() {
        if (merchantRestockDetected) {
            long timeSinceLastMessage = System.currentTimeMillis() - lastMerchantMessageTime;

            if (timeSinceLastMessage > MERCHANT_MESSAGE_TIMEOUT && !replenishedItems.isEmpty()) {
                sendConsolidatedMerchantNotification();
                merchantRestockDetected = false;
                replenishedItems.clear();
            } else if (timeSinceLastMessage > MERCHANT_MESSAGE_TIMEOUT) {
                merchantRestockDetected = false;
                replenishedItems.clear();
            }
        }
    }
}

