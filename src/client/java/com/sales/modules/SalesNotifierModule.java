package com.sales.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;


public class SalesNotifierModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("SalesNotifier");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static HttpServer httpServer = null;
    private static final int HTTP_PORT = 8080;

    private String webhookUrl = "";
    private boolean enabled = true;
    private boolean notifyBlackMarket = true;
    private boolean notifyMerchant = true;
    private boolean notifyBoss = true;
    private boolean autoServerJoin = true;

    private boolean merchantRestockDetected = false;
    private long lastMerchantMessageTime = 0;
    private static final long MERCHANT_MESSAGE_TIMEOUT = 3000;
    private List<String> replenishedItems = new ArrayList<>();
    private boolean bossSpawnDetected = false;
    private String bossName = "";
    private String bossRating = "";
    private long lastServerCommandTime = 0;
    private List<String> availableNpcs = new ArrayList<>();
    private boolean merchantBuyingActive = false;
    private String targetNpcToBuy = null;
    private int merchantBuyState = 0; // 0 = idle, 1 = sent /merchant, 2 = waiting for GUI, 3 = waiting 1 second, 4 = found and clicked NPC, 5 = waiting for NPC Store GUI, 6 = clicking buy, 7 = done
    private long lastMerchantActionTime = 0;
    private int buyClickCount = 0;


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


    public void setAutoServerJoin(boolean autoServerJoin) {
        this.autoServerJoin = autoServerJoin;
    }

    public void onActivate() {
        merchantRestockDetected = false;
        lastMerchantMessageTime = 0;
        replenishedItems.clear();
        bossSpawnDetected = false;
        bossName = "";
        bossRating = "";
        lastServerCommandTime = 0;
        availableNpcs.clear();
        merchantBuyingActive = false;
        targetNpcToBuy = null;
        merchantBuyState = 0;
        buyClickCount = 0;
        startHttpServer();
    }

    public void onDeactivate() {
        merchantRestockDetected = false;
        lastMerchantMessageTime = 0;
        replenishedItems.clear();
        bossSpawnDetected = false;
        bossName = "";
        bossRating = "";
        lastServerCommandTime = 0;
        availableNpcs.clear();
        merchantBuyingActive = false;
        targetNpcToBuy = null;
        merchantBuyState = 0;
        buyClickCount = 0;
        stopHttpServer();
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
            availableNpcs.clear();
        }

        if (notifyMerchant && merchantRestockDetected) {
            if (plainMessage.trim().startsWith("+ [") && plainMessage.contains("]")) {
                replenishedItems.add(plainMessage.trim());
                // Extract NPC name from the message
                String npcName = extractNpcName(plainMessage.trim());
                if (!npcName.isEmpty()) {
                    availableNpcs.add(npcName);
                }
                return;
            }
        }

        // Handle buy commands from chat
        if (plainMessage.startsWith("!buy ") || plainMessage.startsWith("/buy ")) {
            String npcToBuy = plainMessage.substring(plainMessage.indexOf(" ") + 1).trim();
            if (!npcToBuy.isEmpty()) {
                buyNpc(npcToBuy);
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
                    sendDiscordNotification("🐰 **Boss Spawn**", bossName + " Has Spawned!\n" + bossRating);
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
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    LOGGER.info("Discord notification sent successfully");
                } else {
                    LOGGER.error("Failed to send Discord notification. Status: " + response.statusCode() + ", body: " + response.body());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Failed to send Discord notification", e);
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

        if (!availableNpcs.isEmpty()) {
            // Add structured data for Discord bot to create dropdown
            description.append("\n**NPC_BUY_DATA:** `");
            for (int i = 0; i < availableNpcs.size(); i++) {
                if (i > 0) description.append(",");
                description.append(availableNpcs.get(i).replace(" ", "_"));
            }
            description.append("`");
        }

        sendDiscordNotification("Merchant Update", description.toString().trim());
    }

    private String extractNpcName(String message) {
        // Extract NPC name from messages like "+ [Mythic] Avatar 3x (0.25%)"
        // Returns "Mythic Avatar"
        int startBracket = message.indexOf("[");
        int endBracket = message.indexOf("]");
        if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
            String tier = message.substring(startBracket + 1, endBracket);
            int nameStart = endBracket + 2; // Skip "] "
            int nameEnd = message.indexOf(" ", nameStart);
            if (nameEnd == -1) {
                nameEnd = message.indexOf(" x", nameStart);
                if (nameEnd == -1) {
                    nameEnd = message.length();
                }
            }
            if (nameStart < nameEnd) {
                String name = message.substring(nameStart, nameEnd);
                return tier + " " + name;
            }
        }
        return "";
    }

    private void buyNpc(String npcInput) {
        if (npcInput.isEmpty() || availableNpcs.isEmpty()) return;

        String npcToBuy = null;

        // Check if input is a number (index)
        try {
            int index = Integer.parseInt(npcInput) - 1; // Convert to 0-based
            if (index >= 0 && index < availableNpcs.size()) {
                npcToBuy = availableNpcs.get(index);
            }
        } catch (NumberFormatException e) {
            // Input is not a number, treat as NPC name
            for (String npc : availableNpcs) {
                if (npc.toLowerCase().contains(npcInput.toLowerCase())) {
                    npcToBuy = npc;
                    break;
                }
            }
        }

        if (npcToBuy != null) {
            LOGGER.info("Attempting to buy NPC: " + npcToBuy);
            targetNpcToBuy = npcToBuy;
            merchantBuyingActive = true;
            merchantBuyState = 0; // Will send /merchant command in handleMerchantBuying
        } else {
            LOGGER.warn("NPC not found: " + npcInput);
        }
    }

    private void handleMerchantBuying() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long currentTime = System.currentTimeMillis();

        switch (merchantBuyState) {
            case 0: // Send /merchant command
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendChatCommand("merchant");
                    lastMerchantActionTime = currentTime;
                    merchantBuyState = 1;
                    LOGGER.info("Sent /merchant command for NPC: " + targetNpcToBuy);
                }
                break;

            case 1: // Wait for NPC Merchant GUI to open
                if (mc.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
                    String guiTitle = screen.getTitle().getString();
                    if (guiTitle.equals("NPC Merchant")) {
                        lastMerchantActionTime = currentTime;
                        merchantBuyState = 2;
                        LOGGER.info("NPC Merchant GUI opened");
                    }
                }
                break;

            case 2: // Wait 1 second after GUI opens
                if (currentTime - lastMerchantActionTime >= 1000) {
                    merchantBuyState = 3;
                    LOGGER.info("Waited 1 second, now looking for NPC: " + targetNpcToBuy);
                }
                break;

            case 3: // Find and click NPC head
                if (mc.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
                    String guiTitle = screen.getTitle().getString();

                    if (!guiTitle.equals("NPC Merchant")) {
                        LOGGER.warn("Wrong GUI title: " + guiTitle + ", expected NPC Merchant");
                        resetMerchantBuying();
                        return;
                    }

                    // Look for the NPC to buy in slots 9-44
                    for (int slot = 9; slot <= 44; slot++) {
                        ItemStack stack = screen.getScreenHandler().getSlot(slot).getStack();
                        if (!stack.isEmpty() && stack.getItem() == Items.PLAYER_HEAD) {
                            String npcName = stack.getName().getString();
                            String[] targetParts = targetNpcToBuy.split(" ");
                            if (targetParts.length >= 2) {
                                String targetTier = targetParts[0];
                                String targetName = targetParts[1];
                                if (npcName.contains(targetTier) && npcName.contains(targetName)) {
                                    LOGGER.info("Found NPC " + npcName + " in slot " + slot + ", clicking...");
                                    clickSlot(screen, slot);
                                    lastMerchantActionTime = currentTime;
                                    merchantBuyState = 4;
                                    return;
                                }
                            }
                        }
                    }
                    LOGGER.warn("Target NPC not found: " + targetNpcToBuy);
                    resetMerchantBuying();
                }
                break;

            case 4: // Wait for NPC Store GUI with head in slot 13
                if (mc.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
                    String guiTitle = screen.getTitle().getString();

                    if (guiTitle.equals("NPC Store")) {
                        ItemStack slot13 = screen.getScreenHandler().getSlot(13).getStack();
                        if (slot13.getItem() == Items.PLAYER_HEAD) {
                            lastMerchantActionTime = currentTime;
                            merchantBuyState = 5;
                            LOGGER.info("NPC Store GUI opened with NPC in slot 13");
                        }
                    }
                }
                break;

            case 5: // Click buy button (slot 13) one at a time
                if (mc.currentScreen instanceof HandledScreen) {
                    HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
                    String guiTitle = screen.getTitle().getString();

                    if (guiTitle.equals("NPC Store")) {
                        ItemStack slot13 = screen.getScreenHandler().getSlot(13).getStack();
                        // Check if slot 13 has either a player head (buy button) or is empty/air (after purchase)
                        boolean canClick = slot13.getItem() == Items.PLAYER_HEAD || slot13.isEmpty();

                        if (canClick) {
                            if (buyClickCount == 0) {
                                LOGGER.info("Starting purchase process for " + targetNpcToBuy + " - will click buy button 6 times");
                            }

                            if (currentTime - lastMerchantActionTime >= 500) { // Increased to 500ms between clicks
                                clickSlot(screen, 13);
                                buyClickCount++;
                                lastMerchantActionTime = currentTime;
                                LOGGER.info("Clicked buy button (attempt " + buyClickCount + "/6) for " + targetNpcToBuy + " - slot contains: " + slot13.getItem().getName().getString());

                                if (buyClickCount >= 6) {
                                    LOGGER.info("Completed all buy attempts for " + targetNpcToBuy + ", waiting for confirmation");
                                    buyClickCount = 0; // Reset for next use
                                    lastMerchantActionTime = currentTime;
                                    merchantBuyState = 6;
                                }
                            }
                        } else {
                            LOGGER.warn("Unexpected item in slot 13: " + slot13.getItem().getName().getString() + " - stopping purchase attempts");
                            // Don't reset immediately, give it one more try
                            if (buyClickCount > 0) {
                                LOGGER.info("Already attempted " + buyClickCount + " clicks, completing purchase process");
                                buyClickCount = 0;
                                lastMerchantActionTime = currentTime;
                                merchantBuyState = 6;
                            } else {
                                resetMerchantBuying();
                            }
                        }
                    } else {
                        LOGGER.warn("Lost NPC Store GUI during purchase process");
                        resetMerchantBuying();
                    }
                } else {
                    LOGGER.warn("Lost GUI during purchase process");
                    resetMerchantBuying();
                }
                break;

            case 6: // Close GUI and finish
                // Wait a bit longer to ensure all purchases are processed
                if (currentTime - lastMerchantActionTime >= 2000) { // Wait 2 seconds after last click
                    if (mc.currentScreen instanceof HandledScreen) {
                        mc.player.closeHandledScreen();
                    }
                    LOGGER.info("NPC purchase completed for: " + targetNpcToBuy);
                    // Send success message to player
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§a✓ Purchased " + targetNpcToBuy), false);
                    }
                    resetMerchantBuying();
                }
                break;
        }
    }

    private void resetMerchantBuying() {
        merchantBuyingActive = false;
        targetNpcToBuy = null;
        merchantBuyState = 0;
        buyClickCount = 0;
    }

    private void clickSlot(HandledScreen<?> screen, int slot) {
        if (MinecraftClient.getInstance().interactionManager == null || screen.getScreenHandler() == null) return;
        MinecraftClient.getInstance().interactionManager.clickSlot(
            screen.getScreenHandler().syncId,
            slot,
            0,
            SlotActionType.PICKUP,
            MinecraftClient.getInstance().player
        );
    }



    private void checkForGamemodeSelector() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastServerCommandTime < 2500) return;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.COMPASS && !stack.isEmpty()) {
                String displayName = stack.getName().getString();
                if ("GAMEMODE SELECTOR".equals(displayName)) {
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendChatCommand("server tycoon");
                        lastServerCommandTime = currentTime;
                    }
                    break;
                } else if ("Find a Server".equals(displayName)) {
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendChatCommand("join sales");
                        lastServerCommandTime = currentTime;
                    }
                    break;
                }
            }
        }
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


        if (autoServerJoin && MinecraftClient.getInstance().player != null) {
            checkForGamemodeSelector();
        }

        if (merchantBuyingActive && MinecraftClient.getInstance().player != null) {
            handleMerchantBuying();
        }
    }

    private void startHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
            httpServer.createContext("/buy", new BuyHandler());
            httpServer.setExecutor(null);
            httpServer.start();
            LOGGER.info("HTTP server started on port " + HTTP_PORT + " for Discord bot integration");
        } catch (IOException e) {
            LOGGER.error("Failed to start HTTP server", e);
        }
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            LOGGER.info("HTTP server stopped");
        }
    }

    private class BuyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read the request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                LOGGER.info("Received NPC purchase request from Discord: " + requestBody);

                // Parse the NPC name from the request
                String npcToBuy = parseNpcFromRequest(requestBody);
                if (npcToBuy != null && !npcToBuy.isEmpty()) {
                    // Process the buy request on the main thread
                    MinecraftClient.getInstance().execute(() -> {
                        LOGGER.info("Processing Discord purchase request for NPC: " + npcToBuy);
                        buyNpc(npcToBuy);
                    });
                }

                // Send response
                String response = "{\"status\":\"received\",\"npc\":\"" + (npcToBuy != null ? npcToBuy : "unknown") + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }

        private String parseNpcFromRequest(String requestBody) {
            // Parse JSON: {"npc":"Mythic_Avatar","user":"username","timestamp":"..."}
            try {
                int npcIndex = requestBody.indexOf("\"npc\":\"");
                if (npcIndex != -1) {
                    int start = npcIndex + 7; // Length of "\"npc\":\""
                    int end = requestBody.indexOf("\"", start);
                    if (end != -1) {
                        return requestBody.substring(start, end).replace("_", " ");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse NPC from Discord request", e);
            }
            return null;
        }
    }

}
