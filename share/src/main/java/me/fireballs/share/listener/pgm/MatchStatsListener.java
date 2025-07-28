package me.fireballs.share.listener.pgm;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.fireballs.share.SharePlugin;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.FootballDebugChannel;
import me.fireballs.share.util.FootballStatistic;
import me.fireballs.share.util.HTTPUtil;
import me.fireballs.share.util.MatchData;
import me.fireballs.share.util.PlayerFootballStats;
import me.fireballs.share.util.TableUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStatsEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.Pair;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchStatsListener implements Listener {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchStats(MatchStatsEvent event) {
        Match match = event.getMatch();
        Component gamemode = match.getMap().getGamemode();
        if (gamemode == null || !PlainTextComponentSerializer.plainText().serialize(gamemode).equals("Flag Football")) return;

        StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
        if (statsModule == null) return;
        if (statsModule.getStats().isEmpty()) return;
        final PlayerAndMatchData playerAndMatchData = getPlayerAndMatchData(match);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String table = TableUtil.assembleTable(statsModule.getStats(), statManager);

            try {
                Stream<String> lines = HTTPUtil.post(table);
                lines.findFirst().ifPresent(response ->
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.sendStatsPaste(response)));
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
            }
        });
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
                plugin.sendStats("Website", String.format(WEBSITE_MATCH_FORMAT, matchId));
            }
        });
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

            final Competitor team = Optional.ofNullable(PGM.get().getMatchManager().getPlayer(uuid))
                .flatMap(player -> Optional.ofNullable(player.getCompetitor())).orElse(null);
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
    ) {}
}
