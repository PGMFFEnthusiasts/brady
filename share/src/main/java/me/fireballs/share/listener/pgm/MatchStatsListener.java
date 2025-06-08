package me.fireballs.share.listener.pgm;

import me.fireballs.share.SharePlugin;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.util.HTTPUtil;
import me.fireballs.share.util.TableUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.stats.StatsMatchModule;

import java.io.IOException;
import java.util.logging.Level;
import java.util.stream.Stream;

public class MatchStatsListener implements Listener {
    private final SharePlugin plugin;
    private final StatManager statManager;

    public MatchStatsListener(SharePlugin plugin, StatManager statManager) {
        this.plugin = plugin;
        this.statManager = statManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchStats(MatchStatsEvent event) {
        Match match = event.getMatch();
        Component gamemode = match.getMap().getGamemode();
        if (gamemode == null || !PlainTextComponentSerializer.plainText().serialize(gamemode).equals("Flag Football")) return;

        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
        if (statsModule == null) return;
        if (statsModule.getStats().isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String table = TableUtil.assembleTable(statsModule.getStats(), statManager);

            try {
                Stream<String> lines = HTTPUtil.post(table);
                lines.findFirst().ifPresent(response ->
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.sendStats(response)));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
            }
        });
    }
}
