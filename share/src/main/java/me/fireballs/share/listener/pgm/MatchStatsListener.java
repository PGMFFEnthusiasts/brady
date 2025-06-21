package me.fireballs.share.listener.pgm;

import me.fireballs.brady.core.ComponentKt;
import me.fireballs.share.SharePlugin;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.util.HTTPUtil;
import me.fireballs.share.util.TableUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.text.TemporalComponent;

import java.io.IOException;
import java.util.StringJoiner;
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
        if (gamemode == null || !ComponentKt.plainText(gamemode).equals("Flag Football")) return;

        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
        if (statsModule == null) return;
        if (statsModule.getStats().isEmpty()) return;

        ScoreMatchModule scoreMatchModule = match.getModule(ScoreMatchModule.class);
        if (scoreMatchModule == null) return;

        String mapName = "Map: " + match.getMap().getName();
        String time = "Time: " + ComponentKt.plainText(
                ComponentKt.forWhom(TemporalComponent.clock(match.getDuration()), Bukkit.getConsoleSender())
        );
        String timestamp = "Timestamp: " + System.currentTimeMillis();

        StringJoiner teamList = new StringJoiner("\n");
        scoreMatchModule.getScores().forEach((key, value) -> {
            String teamName = key.getNameLegacy();
            String colorName = key.getColor().name();
            int score = value.intValue();
            // Pats (BLUE) - 5

            String line = teamName + " (" + colorName + ") - " + score;
            teamList.add(line);
        });

        // scoreLimit > 0, valid :D
        // int scoreLimit = scoreMatchModule.getScoreLimit();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String table = TableUtil.assembleTable(statsModule.getStats(), statManager);

            try {
                Stream<String> lines = HTTPUtil.post(mapName + "\n" + time + "\n" + timestamp + "\n" + "Teams:\n" + teamList + "\n\n" + table);
                lines.findFirst().ifPresent(response ->
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.sendStats(response)));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
            }
        });
    }
}
