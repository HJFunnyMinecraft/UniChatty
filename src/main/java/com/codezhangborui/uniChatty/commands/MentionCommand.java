package com.codezhangborui.uniChatty.commands;

import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.codezhangborui.uniChatty.managers.PlayerManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MentionCommand implements SimpleCommand {
    private ProxyServer server;
    private ComponentLogger logger;

    public MentionCommand(ProxyServer server, ComponentLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(MessagesManager.get("only-player"));
            return;
        }
        Player sender = (Player) invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(MessagesManager.get("mention-usage"));
            return;
        }
        String targetPlayerName = args[0];
        Player targetPlayer = server.getPlayer(targetPlayerName).orElse(null);
        if (targetPlayer == null) {
            sender.sendMessage(MessagesManager.get("mention-player-not-found", Map.of(
                    "%player%", targetPlayerName
            )));
            return;
        }
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("<light_purple><bold>@");
        messageBuilder.append(targetPlayerName);
        messageBuilder.append("</bold></light_purple>");
        if(args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(" ");
                messageBuilder.append(args[i]);
            }
        }
        String playerPrefix = PlayerManager.getPlayerPrefix(sender);
        String message = messageBuilder.toString();
        Component finalMessage = MiniMessage.miniMessage().deserialize(ConfigManager.getString("chat.format")
                .replace("%player%", sender.getUsername())
                .replace("%server%", sender.getCurrentServer().get().getServerInfo().getName())
                .replace("%message%", message)
                .replace("%prefix%", playerPrefix == null ? "" : playerPrefix));
        server.getAllPlayers().forEach(player -> player.sendMessage(finalMessage));
        logger.info(finalMessage);
        targetPlayer.sendMessage(MessagesManager.get("mention-receive", Map.of(
                "%sender%", sender.getUsername()
        )));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return ConfigManager.getBoolean("mention.enable");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            for (Player player : server.getAllPlayers()) {
                if (player.getUsername().toLowerCase().startsWith(prefix)) {
                    suggestions.add(player.getUsername());
                }
            }
        }

        return suggestions;
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }
}
