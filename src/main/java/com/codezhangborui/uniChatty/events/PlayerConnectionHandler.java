package com.codezhangborui.uniChatty.events;

import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PlayerConnectionHandler {
    private ComponentLogger logger;
    private final ProxyServer server;

    public PlayerConnectionHandler(ProxyServer server, ComponentLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe(priority = 10)
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player source = event.getPlayer();
        if(event.getPreviousServer() == null) {
            // Player has just connected to the proxy
            if(!ConfigManager.getBoolean("join-leave-proxy-broadcast.enable")) return;
            Component finalMessage = MiniMessage.miniMessage().deserialize(
                    ConfigManager.getString("join-leave-proxy-broadcast.join-message")
                            .replace("%player%", source.getUsername())
            );
            server.getAllPlayers().forEach(player -> player.sendMessage(finalMessage));
            logger.info(finalMessage);
        } else {
            // Player has switched to another sub-server
            if(!ConfigManager.getBoolean("join-leave-server-broadcast.enable")) return;
            RegisteredServer sourceServer = event.getPreviousServer();
            RegisteredServer currentServer = event.getPlayer().getCurrentServer().get().getServer();
            Component leaveMessage = MiniMessage.miniMessage().deserialize(
                    ConfigManager.getString("join-leave-server-broadcast.leave-message")
                            .replace("%player%", source.getUsername())
                            .replace("%server%", sourceServer.getServerInfo().getName())
            );
            Component joinMessage = MiniMessage.miniMessage().deserialize(
                    ConfigManager.getString("join-leave-server-broadcast.join-message")
                            .replace("%player%", source.getUsername())
                            .replace("%server%", currentServer.getServerInfo().getName())
            );
            server.getAllPlayers().forEach(player -> {
                if(player.getCurrentServer().get().getServer() == sourceServer) {
                    player.sendMessage(leaveMessage);
                } else if(player.getCurrentServer().get().getServer() == currentServer) {
                    player.sendMessage(joinMessage);
                }
            });
        }
    }

    @Subscribe(priority = 10)
    public void onDisconnect(DisconnectEvent event) {
        if(!ConfigManager.getBoolean("join-leave-proxy-broadcast.enable")) return;
        Player source = event.getPlayer();
        Component finalMessage = MiniMessage.miniMessage().deserialize(
                ConfigManager.getString("join-leave-proxy-broadcast.leave-message")
                        .replace("%player%", source.getUsername())
        );
        server.getAllPlayers().forEach(player -> player.sendMessage(finalMessage));
        logger.info(finalMessage);
    }
}
