package me.fireballs.share.listener.pgm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.MapMaker;
import com.google.gson.stream.JsonReader;
import me.fireballs.brady.core.ComponentKt;
import me.fireballs.share.SharePlugin;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.*;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.Pair;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.fireballs.brady.core.DebuggingKt.log;

public final class MatchStatsListener implements Listener {
    private static final String PASTES_DEV_URL = "https://pastes.dev/";
    private static final String WEBSITE_MATCH_FORMAT = "https://tombrady.fireballs.me/matches/%s";

    private final SharePlugin plugin;
    private final StatManager statManager;
    private final String serverName;
    private final Database database;

    public MatchStatsListener(SharePlugin plugin, StatManager statManager, String serverName, Database database) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.serverName = serverName;
        this.database = database;
    }

    // this is NECESSARY... probably
    // because I want to ensure that if the upload finishes first, the listener can send it at the right time
    // or if the listener happens first there can be a callback to send out the results without fuss.
    private final Map<String, StatsLink> alreadyUploaded = new MapMaker().makeMap();
    private final Map<String, Consumer<StatsLink>> pendingUpload = new MapMaker().makeMap();

    @EventHandler(priority = EventPriority.MONITOR)
    private void onMatchEnd(MatchFinishEvent event) {
        Match match = event.getMatch();
        final var matchKey = match.getId();
        final var gamemode = match.getMap().getGamemode();
        if (gamemode == null) return;
        final var gamemodeString = ComponentKt.plainText(gamemode);
        if (!gamemodeString.equals("Flag Football") && !gamemodeString.equals("Touchdown")) return;

        var statsModule = match.getModule(StatsMatchModule.class);
        if (statsModule == null) return;
        if (statsModule.getStats().isEmpty()) return;
        final var playerAndMatchData = getPlayerAndMatchData(match);

        if (plugin.uploadPaste) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String table = TableUtil.assembleTable(statsModule.getStats(), statManager, statsModule);

                try {
                    Stream<String> lines = HTTPUtil.post(table);
                    var first = lines.findFirst();
                    if (first.isPresent()) {
                        String response = first.get();
                        try (JsonReader reader = new JsonReader(new StringReader(response))) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                if (reader.nextName().equals("key")) {
                                    String key = reader.nextString();
                                    StatsLink statsLink = new StatsLink(PASTES_DEV_URL + key, "Match Stats");
                                    plugin.sendStats(statsLink);
                                    break;
                                }
                            }
                        } catch (IOException ex) {
                            plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                        }
                    }
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                }
            });
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FootballDebugChannel.sendMessage(Component.text("Attempting to write match data to database"));
            if (database != null && playerAndMatchData != null) {
                final int matchId = database.addMatchData(playerAndMatchData.matchData);
                FootballDebugChannel.sendMessage(Component.text(
                        "Successfully wrote match data, match #" + matchId
                ));
                database.batchAddPlayerMatchData(matchId, playerAndMatchData.stats);
                FootballDebugChannel.sendMessage(Component.text(
                        "Wrote player match data"
                ));

                final var statsLink = new StatsLink(String.format(WEBSITE_MATCH_FORMAT, matchId), "Website");
                plugin.sendStats(statsLink);
                alreadyUploaded.put(matchKey, statsLink);
                pendingUpload.getOrDefault(matchKey, (v) -> {
                }).accept(statsLink);
            }
        });
    }

    // out with the old, in with the new
    @EventHandler(priority = EventPriority.MONITOR)
    private void onCycleFinish(MatchAfterLoadEvent event) {
        log("match-stats", "cleaning garbage");
        alreadyUploaded.clear();
        pendingUpload.clear();
    }

    private void broadcast(String matchKey) {
        this.pendingUpload.put(matchKey, plugin::broadcastStats);
        // - there is a chance the stats link is sent twice
        //   but this is better than it having not been sent at all
        final var alreadyUploaded = this.alreadyUploaded.remove(matchKey);
        if (alreadyUploaded != null) plugin.broadcastStats(alreadyUploaded);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCycle(MatchUnloadEvent event) {
        log("match-stats", "matchUnload " + event.getMatch().getId());
        broadcast(event.getMatch().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchStats(MatchStatsEvent event) {
        log("match-stats", "matchStats " + event.getMatch().getId());
        broadcast(event.getMatch().getId());
    }

    private PlayerAndMatchData getPlayerAndMatchData(final Match match) {
        final Duration matchDuration = match.getDuration();
        final long startTime = System.currentTimeMillis() - matchDuration.toMillis();
        final int duration = (int) matchDuration.toSeconds();
        final List<Competitor> competitors = match.getCompetitors().stream().toList();
        final BiMap<Competitor, Integer> competitorIdentities = HashBiMap.create(2);
        if (competitors.size() != 2) {
            return null;
        }
        competitorIdentities.put(competitors.get(0), 1);
        competitorIdentities.put(competitors.get(1), 2);
        final int winner = (match.getWinners().size() == 1)
                ? competitorIdentities.get(match.getWinners().iterator().next())
                : -1;
        final ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);
        final StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
        if (scoreModule == null || statsModule == null) {
            return null;
        }
        final int teamOneScore = (int) scoreModule.getScore(competitorIdentities.inverse().get(1));
        final int teamTwoScore = (int) scoreModule.getScore(competitorIdentities.inverse().get(2));
        final int teamOneColor = competitors.get(0).getColor().ordinal();
        final int teamTwoColor = competitors.get(1).getColor().ordinal();
        final String map = match.getMap().getNormalizedName();
        final boolean isTourney = false;
        final MatchData matchData = new MatchData(
                serverName, startTime, duration, winner, teamOneScore, teamTwoScore, map, isTourney,
                competitors.get(0).getNameLegacy(), competitors.get(1).getNameLegacy(),
                teamOneColor, teamTwoColor
        );

        final Map<UUID, PlayerStats> statsMap = statsModule.getStats();
        final Map<UUID, PlayerFootballStats> footballStatsMap = statsMap.entrySet().stream().map(entry -> {
            final UUID uuid = entry.getKey();
            final PlayerStats stats = entry.getValue();

            final Competitor team = statsModule.getPrimaryTeam(uuid, false);
            if (team == null) {
                return null;
            }
            final int teamId = competitorIdentities.get(team);
            final PlayerFootballStats footballStats = new PlayerFootballStats(
                    teamId,
                    stats.getKills(),
                    stats.getDeaths(),
                    stats.getAssists(),
                    stats.getMaxKillstreak(),
                    stats.getDamageDone() / 2,
                    stats.getDamageTaken() / 2,
                    (int) statManager.getStat(uuid, FootballStatistic.PICKUPS),
                    (int) statManager.getStat(uuid, FootballStatistic.THROWS),
                    (int) statManager.getStat(uuid, FootballStatistic.PASSES),
                    (int) statManager.getStat(uuid, FootballStatistic.CATCHES),
                    (int) statManager.getStat(uuid, FootballStatistic.STRIPS),
                    (int) statManager.getStat(uuid, FootballStatistic.TOUCHDOWNS),
                    (int) statManager.getStat(uuid, FootballStatistic.TOUCHDOWN_PASSES),
                    (int) statManager.getStat(uuid, FootballStatistic.TOTAL_PASSING_BLOCKS),
                    (int) statManager.getStat(uuid, FootballStatistic.TOTAL_RECEIVING_BLOCKS),
                    (int) statManager.getStat(uuid, FootballStatistic.DEFENSE_INTERCEPTIONS),
                    (int) statManager.getStat(uuid, FootballStatistic.PASS_INTERCEPTIONS),
                    statManager.getStat(uuid, FootballStatistic.DMG_CARRIER) / 2.0
            );
            return new Pair<>(uuid, footballStats);
        }).filter(Objects::nonNull).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return new PlayerAndMatchData(matchData, footballStatsMap);
    }

    private record PlayerAndMatchData(
            MatchData matchData,
            Map<UUID, PlayerFootballStats> stats
    ) {
    }
}
