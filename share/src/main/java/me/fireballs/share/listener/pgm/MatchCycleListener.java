package me.fireballs.share.listener.pgm;

import me.fireballs.share.manager.StatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;

public class MatchCycleListener implements Listener {
    private final StatManager statManager;

    public MatchCycleListener(StatManager statManager) {
        this.statManager = statManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchCycle(MatchAfterLoadEvent event) {
        statManager.clearStats();
    }
}
