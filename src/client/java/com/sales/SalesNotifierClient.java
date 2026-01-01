package com.sales;

import com.sales.commands.SalesNotifierCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class SalesNotifierClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			SalesNotifierCommand.register(dispatcher);
		});
	}
}