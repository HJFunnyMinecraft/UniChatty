package com.codezhangborui.uniChatty.commands;

import com.codezhangborui.uniChatty.managers.ConfigManager;
import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.List;

public class UniChattyCommand implements SimpleCommand {
    private ProxyServer server;
    private ComponentLogger logger;

    public UniChattyCommand(ProxyServer server, ComponentLogger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if(args.length != 1) {
            invocation.source().sendMessage(MessagesManager.get("unichatty-cmd-usage"));
            return;
        }
        if(args[0].equals("reload")) {
            if(!invocation.source().hasPermission("unichatty.reload")) {
                invocation.source().sendMessage(MessagesManager.get("no-permission"));
                return;
            }
            ConfigManager.reload();
            MessagesManager.setLanguage(ConfigManager.getString("language"));
            MessagesManager.reload();
            invocation.source().sendMessage(MessagesManager.get("reload-success"));
        } else if (args[0].equals("help")) {
            invocation.source().sendMessage(MessagesManager.get("plugin-usage"));
        } else {
            invocation.source().sendMessage(MessagesManager.get("unichatty-cmd-usage"));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) { return true; }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if(args.length <= 1) {
            if(invocation.source().hasPermission("unichatty.reload")) {
                return List.of("reload", "help");
            } else {
                return List.of("help");
            }
        }
        return List.of();
    }
}
