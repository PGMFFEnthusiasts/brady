package me.fireballs.share.util;

import me.fireballs.share.manager.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.util.named.Named;

import java.util.*;

public class TableUtil {
    private static final String HEADER_FORMAT = "%-10s %-16s %-4s %-4s %-4s %-5s %-8s %-9s %-8s %-5s %-5s %-6s %-6s %-3s %-8s %-11s %-11s %-8s %-9s %-12s\n";
    private static final String ROW_FORMAT = "%-10s %-16s %-4d %-4d %-4d %-5d %-8.1f %-9.1f %-8d %-5d %-5d %-6d %-6d %-3d %-8d %-11d %-11d %-8d %-9d %-12.1f\n";

    private static final String[] HEADERS = {"TEAM", "USERNAME", "K", "D", "AST", "STRK",
            "DMG_DLT", "DMG_RCVD",
            "PICKUPS", "THRW", "PASS", "CATCH", "STRIP", "TD", "TD_PASS",
            "PASS_BLOX", "RCV_BLOX", "DEF_INT", "PASS_INT", "DMG_CARRIER"};

    public static String assembleTable(Map<UUID, PlayerStats> statsMap, StatManager statManager) {
        List<Statline> statlines = new ArrayList<>();

        statsMap.forEach((uuid, stats) -> {
            String username = Optional.ofNullable(Bukkit.getPlayer(uuid))
                    .map(Player::getName)
                    .orElseGet(() ->
                            Optional.ofNullable(PGM.get().getDatastore().getUsername(uuid))
                                    .map(Username::getNameLegacy)
                                    .orElse(uuid.toString())
                    );

            String team = Optional.ofNullable(PGM.get().getMatchManager().getPlayer(uuid))
                    .flatMap(player -> Optional.ofNullable(player.getCompetitor()))
                    .map(Named::getNameLegacy)
                    .orElse("UNKNOWN");

            statlines.add(new Statline(
                    team.substring(0, Math.min(team.length(), 10)),
                    username,
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
            ));
        });

        Collections.sort(statlines);

        StringBuilder message = new StringBuilder();
        message.append(String.format(HEADER_FORMAT, (Object[]) HEADERS));

        statlines.forEach(statline -> message.append(String.format(ROW_FORMAT,
                statline.team(),
                statline.username(),
                statline.kills(),
                statline.deaths(),
                statline.assists(),
                statline.streak(),
                statline.damageDealt(),
                statline.damageReceived(),
                statline.pickups(),
                statline.throwz(),
                statline.passes(),
                statline.catches(),
                statline.strips(),
                statline.touchdowns(),
                statline.touchdownPasses(),
                statline.maxPassingBlocks(),
                statline.maxReceivingBlocks(),
                statline.defInt(),
                statline.passInt(),
                statline.dmgCarrier()
        )));

        return message.toString();
    }

    private record Statline(
        String team, String username, int kills, int deaths, int assists, int streak,
        double damageDealt, double damageReceived, int pickups, int throwz, int passes,
        int catches, int strips, int touchdowns, int touchdownPasses,
        int maxPassingBlocks, int maxReceivingBlocks, int defInt, int passInt, double dmgCarrier
    ) implements Comparable<Statline> {

        @Override
        public int compareTo(Statline other) {
            return Integer.compare(other.kills, this.kills);
        }
    }
}
