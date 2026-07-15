package com.codezhangborui.uniChatty.commands;

import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UcTellCommand implements SimpleCommand {
    private ProxyServer server;
    private ComponentLogger logger;

    public UcTellCommand(ProxyServer server, ComponentLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(MessagesManager.get("only-player"));
            return;
        }
        Player sender = (Player) invocation.source();
        String[] args = invocation.arguments();
        if(args.length < 2) {
            sender.sendMessage(MessagesManager.get("uctell-usage"));
            return;
        }
        String targetPlayerName = args[0];
        Player targetPlayer = server.getPlayer(targetPlayerName).orElse(null);
        if(targetPlayer == null) {
            sender.sendMessage(MessagesManager.get("player-not-found", Map.of(
                    "%player%", targetPlayerName
            )));
            return;
        }
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(" ");
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();
        targetPlayer.sendMessage(MessagesManager.get("uctell-to", Map.of(
                "%player_from%", sender.getUsername(),
                "%player_to%", targetPlayer.getUsername(),
                "%message%", message
        )));
        targetPlayer.sendMessage(MessagesManager.get("uctell-to", Map.of(
                "%player_from%", sender.getUsername(),
                "%player_to%", targetPlayer.getUsername(),
                "%message%", message
        )));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return ConfigManager.getBoolean("privatemsg.enable");
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
