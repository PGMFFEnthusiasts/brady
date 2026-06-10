package me.fireballs.brady.tools;

import me.fireballs.brady.core.BooleanSettingValue;
import me.fireballs.brady.core.tag.TagAPI;
import me.fireballs.brady.core.tag.TagLayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.koin.java.KoinJavaComponent;

public class PingTags {

    private final BooleanSettingValue setting;

    public PingTags() {
        ToolsSettings settings = KoinJavaComponent.get(ToolsSettings.class);
        this.setting = settings.getPingTags();

        TagAPI tagAPI = KoinJavaComponent.get(TagAPI.class);
        tagAPI.register((new TagLayer("ping", 1, this::getPing, (_, viewer) -> setting.retrieveValue(viewer))));
    }

    private String getPing(Player target) {
        int ping = target.spigot().getPing();
        return pingColor(ping).toString() + ping + " §7ms";
    }

    private static ChatColor pingColor(int ping) {
        if (ping < 50) return ChatColor.GREEN;
        if (ping < 100) return ChatColor.DARK_GREEN;
        if (ping < 150) return ChatColor.YELLOW;
        if (ping < 200) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }
}
