package me.fireballs.share.listener.pgm;

import me.fireballs.share.SharePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public class MatchJoinListener implements Listener {
    private final SharePlugin plugin;

    public MatchJoinListener(SharePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchJoin(PlayerPartyChangeEvent event) {
        Player player = event.getPlayer().getBukkit();
        if (player == null) return;

        plugin.refreshCPS(player);
    }
}
