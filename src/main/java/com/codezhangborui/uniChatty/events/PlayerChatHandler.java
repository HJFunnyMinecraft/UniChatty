package com.codezhangborui.uniChatty.events;

import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.codezhangborui.uniChatty.managers.PlayerManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.UUID;

public class PlayerChatHandler {
    private ComponentLogger logger;
    private final ProxyServer server;

    public PlayerChatHandler(ProxyServer server, ComponentLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    private boolean hasBannedWords(String message) {
        // load banned words list from ConfigManager
        String[] bannedWords = ConfigManager.getString("ban-words.words").split(",");
        for (String bannedWord : bannedWords) {
            if (message.toLowerCase().contains(bannedWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Subscribe(priority = 10)
    public void onPlayerChat(PlayerChatEvent event) {
        String message = event.getMessage();
        Player source = event.getPlayer();
        UUID sourceUUID = source.getUniqueId();
        if(ConfigManager.getBoolean("ban-words.enable")) {
            if (hasBannedWords(message)) {
                // event.setResult(PlayerChatEvent.ChatResult.denied());
                source.sendMessage(MessagesManager.get("ban-words-message"));
                logger.info(MessagesManager.get("ban-words-console", Map.of(
                        "%player%", source.getUsername(),
                        "%message%", message
                )));
                return;
            }
        }
        String sourceServer = source.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("Unknown");
        String playerPrefix = PlayerManager.getPlayerPrefix(source);
        Component finalMessage = MiniMessage.miniMessage().deserialize(ConfigManager.getString("chat.format")
                .replace("%player%", source.getUsername())
                .replace("%server%", sourceServer)
                .replace("%message%", message)
                .replace("%prefix%", playerPrefix == null ? "" : playerPrefix));
        logger.info(finalMessage);
        if(ConfigManager.getList("chat.ignore").contains(sourceServer)) {
            return;
        }
        server.getAllPlayers().forEach(player -> {
            String targetServer = player.getCurrentServer()
                    .map(serverConnection -> serverConnection.getServerInfo().getName())
                    .orElse("Unknown");
            if(!sourceServer.equals(targetServer)) {
                player.sendMessage(finalMessage);
            }
        });
    }
}
