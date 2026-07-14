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
            invocation.source().sendMessage(MessagesManager.get("unichatty-usage"));
            return;
        }
        if(args[0].equals("reload")) {
            ConfigManager.reload();
            MessagesManager.setLanguage(ConfigManager.getString("language"));
            MessagesManager.reload();
            invocation.source().sendMessage(MessagesManager.get("reload-success"));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        if(!(invocation.source() instanceof Player)) return true;
        return invocation.source().hasPermission("unichatty.admin");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if(args.length <= 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
