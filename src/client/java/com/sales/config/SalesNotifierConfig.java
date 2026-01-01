package com.sales.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SalesNotifierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("salesnotifier.json");

    public String webhookUrl = "";
    public boolean enabled = true;
    public boolean notifyBlackMarket = true;
    public boolean notifyMerchant = true;

    public static SalesNotifierConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                return GSON.fromJson(reader, SalesNotifierConfig.class);
            } catch (IOException e) {
                System.err.println("Failed to load SalesNotifier config: " + e.getMessage());
            }
        }

        // Return default config
        return new SalesNotifierConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save SalesNotifier config: " + e.getMessage());
        }
    }
}
