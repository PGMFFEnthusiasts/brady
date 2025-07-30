package me.fireballs.cps.listener.pgm;

import me.fireballs.cps.CPSPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public class MatchJoinListener implements Listener {
    private final CPSPlugin plugin;

    public MatchJoinListener(CPSPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchJoin(PlayerPartyChangeEvent event) {
        Player player = event.getPlayer().getBukkit();
        if (player == null) return;

        plugin.refreshShadows(player);
    }
}
