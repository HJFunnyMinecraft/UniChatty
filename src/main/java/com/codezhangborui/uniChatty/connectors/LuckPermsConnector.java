package com.codezhangborui.uniChatty.connectors;

import com.codezhangborui.uniChatty.managers.MessagesManager;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class LuckPermsConnector {
    public static LuckPerms luckPerms;

    public static void load(ComponentLogger logger) {
        luckPerms = LuckPermsProvider.get();
        logger.info(MessagesManager.get("luckperms-connected"));
    }

    public static String getPlayerPrefix(Player player) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if(user == null) {
            return null;
        }
        return user.getCachedData().getMetaData().getPrefix();
    }
}
