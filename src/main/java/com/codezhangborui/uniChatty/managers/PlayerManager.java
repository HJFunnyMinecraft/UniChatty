package com.codezhangborui.uniChatty.managers;

import com.velocitypowered.api.proxy.Player;

public class PlayerManager {
    public static String getPlayerPrefix(Player player) {
        if(ConfigManager.getBoolean("connectors.luckperms")) {
            return com.codezhangborui.uniChatty.connectors.LuckPermsConnector.getPlayerPrefix(player);
        } else {
            return null;
        }
    }
}
