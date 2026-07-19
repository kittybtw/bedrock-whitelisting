package command;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;


public class BedrockWhitelistCommand {

    public static void register() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                Commands.literal("whitelist-bedrock")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                    .then(
                        Commands.literal("add")
                            .then(
                                Commands.argument("gamertag", StringArgumentType.greedyString())
                                    .executes(context -> addPlayerToWhitelist(context))
                            )
                    )
                    .then(
                        Commands.literal("remove")
                            .then(
                                Commands.argument("gamertag", StringArgumentType.greedyString())
                                    .executes(context -> removePlayerFromWhitelist(context))
                                )
                    )
                    .then(
                        Commands.literal("list")
                            .executes(context -> listWhitelistedBedrockPlayers(context))
                    )
                );
            
            });
        }


    private static int addPlayerToWhitelist(CommandContext<CommandSourceStack> context) {
        String passedUsername = StringArgumentType.getString(
        context,
        "gamertag"
        ).replaceFirst("\\.", "");

    UserWhiteList whitelist = getWhitelistFromContext(context);

    CompletableFuture<UserWhiteListEntry> futureWhitelistEntry = CompletableFuture.supplyAsync(() -> {
        try {
            return getWhitelistEntry(passedUsername);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("An error occured while getting the whitelist entry: " + e.getMessage())
            );
            throw new RuntimeException(e);
        }
    });
    
    futureWhitelistEntry.thenAccept(whitelistEntry -> {
        UUID uuid = whitelistEntry.getUser().id();
        String username = whitelistEntry.getUser().name();

        if (whitelist.isWhiteListed(whitelistEntry.getUser())) {
            context.getSource().sendFailure(
                Component.literal("Bedrock player is already whitelisted")
            );

        } else {
            whitelist.add(whitelistEntry);
            saveWhitelist(context);

            context.getSource().sendSuccess(
                () -> Component.literal("Added Bedrock player " + username + " to the whitelist"),
                false
            );
        }
    });

        return Command.SINGLE_SUCCESS;
    };


    private static int removePlayerFromWhitelist(CommandContext<CommandSourceStack> context) {
        String passedUsername = StringArgumentType.getString(
        context,
        "gamertag"
        ).replaceFirst("\\.", "");

        UserWhiteList whitelist = getWhitelistFromContext(context);

        CompletableFuture<UserWhiteListEntry> futureWhitelistEntry = CompletableFuture.supplyAsync(() -> {
            try {
                return getWhitelistEntry(passedUsername);
            } catch (Exception e) {
                context.getSource().sendFailure(
                    Component.literal("An error occured while getting the whitelist entry: " + e.getMessage())
                );
                throw new RuntimeException(e);
            }
        });

        futureWhitelistEntry.thenAccept(whitelistEntry -> {
            UUID uuid = whitelistEntry.getUser().id();
            String username = whitelistEntry.getUser().name();

            if (!whitelist.isWhiteListed(whitelistEntry.getUser())) {
                context.getSource().sendFailure(
                Component.literal("Bedrock player is not whitelisted")
            );

            } else {
                whitelist.remove(whitelistEntry.getUser());
                saveWhitelist(context);

                context.getSource().sendSuccess(
                () -> Component.literal("Removed Bedrock player " + username + " from the whitelist"),
                false
                );
            }
        });

        return Command.SINGLE_SUCCESS;
    }
    

    private static int listWhitelistedBedrockPlayers(CommandContext<CommandSourceStack> context) {
        
        return Command.SINGLE_SUCCESS;
    }


    private static UserWhiteListEntry getWhitelistEntry(String passedUsername) throws Exception {
        String xboxProfileEndpoint = "https://api.rghlab.co.uk/xuid?gamertag=" + passedUsername.replaceAll(" ", "%20");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(xboxProfileEndpoint))
        .GET()
        .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        JsonObject responseData = JsonParser.parseString(response.body()).getAsJsonObject();

        String xuidHex = responseData
            .getAsJsonArray("profiles")
            .get(0).getAsJsonObject()
            .getAsJsonObject("summary")
            .get("xuid_hex")
            .getAsString();

        String xuid = "00000000-0000-0000-"
            + xuidHex.substring(0, 4)
            + "-"
            + xuidHex.substring(4);

        String username = responseData
            .getAsJsonArray("profiles")
            .get(0).getAsJsonObject()
            .getAsJsonObject("summary")
            .get("gamertag")
            .getAsString();

        JsonObject whitelistEntryJson = new JsonObject();
        whitelistEntryJson.addProperty("uuid", xuid);
        whitelistEntryJson.addProperty("name", "." + username);

        UserWhiteListEntry whitelistEntry = new UserWhiteListEntry(whitelistEntryJson);

        return whitelistEntry;
    }


    private static void saveWhitelist(CommandContext<CommandSourceStack> context) {
        UserWhiteList whitelist = getWhitelistFromContext(context);

        try {
            whitelist.save();
        } catch (IOException e) {
            context.getSource().sendFailure(
                Component.literal("Failed to save the whitelist: " + e.getMessage())
            );
        }
    }


    private static UserWhiteList getWhitelistFromContext(CommandContext<CommandSourceStack> context) {
        UserWhiteList whitelist = context.getSource().getServer().getPlayerList().getWhiteList();

        return whitelist;
    }
}
