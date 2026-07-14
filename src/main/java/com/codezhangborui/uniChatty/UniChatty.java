package com.codezhangborui.uniChatty;

import com.codezhangborui.uniChatty.connectors.LuckPermsConnector;
import com.codezhangborui.uniChatty.events.PlayerChatHandler;
import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "unichatty", name = "UniChatty", version = "1.0", authors = {"CodeZhangBorui"})
public class UniChatty {
    private ComponentLogger logger;
    private final ProxyServer server;

    @Inject
    @DataDirectory
    private Path dataDictionary;
    public ConfigManager config;

    @Inject
    public UniChatty(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = ComponentLogger.logger("UniChatty");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ConfigManager.initialize(dataDictionary, "config.yml", logger);
        ConfigManager.load();
        MessagesManager.initialize(dataDictionary, logger);
        MessagesManager.loadLanguage(ConfigManager.getString("language"));
        MessagesManager.setLanguage(ConfigManager.getString("language"));
        server.getEventManager().register(this, new PlayerChatHandler(server, logger));
        if(ConfigManager.getBoolean("connectors.luckperms")) {
            LuckPermsConnector.load(logger);
        }
        logger.info(MessagesManager.get("started-successfully"));
    }
}
