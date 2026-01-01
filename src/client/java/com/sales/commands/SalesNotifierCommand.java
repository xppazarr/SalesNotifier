package com.sales.commands;

import com.sales.SalesNotifierAPI;
import com.sales.config.SalesNotifierConfig;
import com.sales.modules.SalesNotifierModule;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class SalesNotifierCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("salesnotifier")
            .then(ClientCommandManager.literal("webhook")
                .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                    .executes(context -> {
                        String url = StringArgumentType.getString(context, "url");
                        SalesNotifierConfig config = SalesNotifierAPI.getConfig();
                        SalesNotifierModule module = SalesNotifierAPI.getNotifierModule();

                        if (config != null && module != null) {
                            config.webhookUrl = url;
                            module.setWebhookUrl(url);
                            SalesNotifierAPI.saveConfig();
                            context.getSource().sendFeedback(Text.literal("§aDiscord webhook URL updated!"));
                        }
                        return 1;
                    })
                )
            )
            .then(ClientCommandManager.literal("toggle")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        SalesNotifierConfig config = SalesNotifierAPI.getConfig();
                        SalesNotifierModule module = SalesNotifierAPI.getNotifierModule();

                        if (config != null && module != null) {
                            config.enabled = enabled;
                            module.setEnabled(enabled);
                            SalesNotifierAPI.saveConfig();
                            context.getSource().sendFeedback(Text.literal("§aSalesNotifier " + (enabled ? "enabled" : "disabled") + "!"));
                        }
                        return 1;
                    })
                )
            )
            .then(ClientCommandManager.literal("blackmarket")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        SalesNotifierConfig config = SalesNotifierAPI.getConfig();
                        SalesNotifierModule module = SalesNotifierAPI.getNotifierModule();

                        if (config != null && module != null) {
                            config.notifyBlackMarket = enabled;
                            module.setNotifyBlackMarket(enabled);
                            SalesNotifierAPI.saveConfig();
                            context.getSource().sendFeedback(Text.literal("§aBlack Market notifications " + (enabled ? "enabled" : "disabled") + "!"));
                        }
                        return 1;
                    })
                )
            )
            .then(ClientCommandManager.literal("merchant")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        SalesNotifierConfig config = SalesNotifierAPI.getConfig();
                        SalesNotifierModule module = SalesNotifierAPI.getNotifierModule();

                        if (config != null && module != null) {
                            config.notifyMerchant = enabled;
                            module.setNotifyMerchant(enabled);
                            SalesNotifierAPI.saveConfig();
                            context.getSource().sendFeedback(Text.literal("§aMerchant notifications " + (enabled ? "enabled" : "disabled") + "!"));
                        }
                        return 1;
                    })
                )
            )
            .then(ClientCommandManager.literal("status")
                .executes(context -> {
                    SalesNotifierConfig config = SalesNotifierAPI.getConfig();
                    if (config != null) {
                        context.getSource().sendFeedback(Text.literal("§6=== SalesNotifier Status ==="));
                        context.getSource().sendFeedback(Text.literal("§7Enabled: §f" + config.enabled));
                        context.getSource().sendFeedback(Text.literal("§7Webhook URL: §f" + (config.webhookUrl.isEmpty() ? "Not set" : "Configured")));
                        context.getSource().sendFeedback(Text.literal("§7Black Market: §f" + config.notifyBlackMarket));
                        context.getSource().sendFeedback(Text.literal("§7Merchant: §f" + config.notifyMerchant));
                    }
                    return 1;
                })
            )
            .executes(context -> {
                context.getSource().sendFeedback(Text.literal("§6SalesNotifier Commands:"));
                context.getSource().sendFeedback(Text.literal("§7/salesnotifier webhook <url> §f- Set Discord webhook URL"));
                context.getSource().sendFeedback(Text.literal("§7/salesnotifier toggle <true/false> §f- Enable/disable notifications"));
                context.getSource().sendFeedback(Text.literal("§7/salesnotifier blackmarket <true/false> §f- Toggle Black Market notifications"));
                context.getSource().sendFeedback(Text.literal("§7/salesnotifier merchant <true/false> §f- Toggle Merchant notifications"));
                context.getSource().sendFeedback(Text.literal("§7/salesnotifier status §f- Show current settings"));
                return 1;
            })
        );
    }
}
