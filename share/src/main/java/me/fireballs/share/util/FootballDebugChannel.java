package me.fireballs.share.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

// ad-hoc chat channel for debugging purposes
public class FootballDebugChannel {
    private static BukkitAudiences AUDIENCE;
    private static final Set<Player> PLAYERS = new HashSet<>();

    public static void init(final JavaPlugin plugin) {
        AUDIENCE = BukkitAudiences.create(plugin);
    }

    public static void sendMessage(Component component) {
        final Component withPrefix = Component.text("[TB] ").color(NamedTextColor.GREEN).append(component);
        PLAYERS.forEach((p) -> AUDIENCE.player(p).sendMessage(withPrefix));
    }

    public static boolean togglePlayer(final Player player) {
        if (PLAYERS.contains(player)) {
            PLAYERS.remove(player);
            return false;
        }
        PLAYERS.add(player);
        return true;
    }

    public static void unload() {
        PLAYERS.clear();
    }
}
