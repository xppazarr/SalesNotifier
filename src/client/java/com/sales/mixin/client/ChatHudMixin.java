package com.sales.mixin.client;

import com.sales.SalesNotifierAPI;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        // Get the module instance from the API
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            var module = SalesNotifierAPI.getNotifierModule();
            if (module != null) {
                module.onChatMessage(message);
            }
        }
    }
}
