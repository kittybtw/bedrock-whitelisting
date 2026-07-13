package kittybtw.bedrockwhitelisting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import command.BedrockWhitelistCommand;
import net.fabricmc.api.ModInitializer;

public class BedrockWhitelisting implements ModInitializer {
	public static final String MOD_ID = "bedrock-whitelisting";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("hello from bedrock whitelisting!");
		BedrockWhitelistCommand.register();
	};
};
