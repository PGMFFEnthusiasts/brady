package me.fireballs.brady.tools;

import me.fireballs.brady.corepgm.PGMExtensionsKt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public class BuzzerBeater implements Listener {

    private final Tools plugin;

    public BuzzerBeater() {
        this.plugin = KoinJavaComponent.get(Tools.class);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCountdownEnd(CountdownEndEvent event) {
        Match match = event.getMatch();
        if (!PGMExtensionsKt.isTouchdown(match)) return;

        TimeLimitMatchModule tlmm = match.getModule(TimeLimitMatchModule.class);
        if (tlmm == null) return;
        if (event.getCountdown() != tlmm.getCountdown()) return;

        TimeLimit limit = tlmm.getTimeLimit();
        if (limit == null) return;

        match.removeVictoryCondition(limit);

        new BukkitRunnable() {
            boolean firstRun = true;

            @Override
            public void run() {
                if (!match.isRunning()) {
                    cancel();
                    return;
                }

                if (firstRun) {
                    tlmm.cancel();
                    firstRun = false;
                }

                tlmm.setFinished(false);

                if (!hasSnowballs(match)) {
                    if (limit.currentWinner(match) != null) {
                        match.addVictoryCondition(limit);
                        tlmm.setFinished(true);
                        match.calculateVictory();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static boolean hasSnowballs(Match match) {
        for (Snowball snowball : match.getWorld().getEntitiesByClass(Snowball.class)) {
            if (snowball.isValid()) {
                return true;
            }
        }
        return false;
    }
}
