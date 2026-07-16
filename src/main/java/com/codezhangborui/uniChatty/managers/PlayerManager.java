package com.codezhangborui.uniChatty.managers;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class PlayerManager {
    public static String getPlayerPrefix(Player player) {
        String playerPrefix = null;
        if(ConfigManager.getBoolean("connectors.luckperms")) {
            playerPrefix = com.codezhangborui.uniChatty.connectors.LuckPermsConnector.getPlayerPrefix(player);
        }
        if(playerPrefix == null) return null;
        else return MiniMessage.miniMessage().serialize(
                LegacyComponentSerializer.legacySection().deserialize(playerPrefix)
        ) + "<reset>";
    }
}
