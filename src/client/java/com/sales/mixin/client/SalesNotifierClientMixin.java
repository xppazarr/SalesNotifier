package com.sales.mixin.client;

import com.sales.SalesNotifierAPI;
import com.sales.config.SalesNotifierConfig;
import com.sales.modules.SalesNotifierModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class SalesNotifierClientMixin {
    private static SalesNotifierModule notifierModule;
    private static SalesNotifierConfig config;

    @Inject(at = @At("HEAD"), method = "run")
    private void init(CallbackInfo info) {
        // Load configuration
        config = SalesNotifierConfig.load();

        // Initialize the notifier module
        notifierModule = new SalesNotifierModule();
        notifierModule.setWebhookUrl(config.webhookUrl);
        notifierModule.setEnabled(config.enabled);
        notifierModule.setNotifyBlackMarket(config.notifyBlackMarket);
        notifierModule.setNotifyMerchant(config.notifyMerchant);
        notifierModule.setNotifyBoss(config.notifyBoss);

        // Set the API references
        SalesNotifierAPI.setNotifierModule(notifierModule);
        SalesNotifierAPI.setConfig(config);

        // Activate the module
        if (notifierModule != null) {
            notifierModule.onActivate();
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onTick(CallbackInfo info) {
        if (notifierModule != null) {
            notifierModule.onClientTick();
        }
    }
}
