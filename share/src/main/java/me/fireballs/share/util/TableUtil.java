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
    private static final String HEADER_FORMAT = "%-10s %-16s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n";
    private static final String ROW_FORMAT = "%-10s %-16s %-10d %-10d %-10d %-10d %-10.1f %-10.1f %-10d %-10d %-10d %-10d %-10d %-10d %-10d\n";

    private static final String[] HEADERS = {"TEAM", "USERNAME", "KILLS", "DEATHS", "ASSISTS", "STREAK",
            "DMG_DEALT", "DMG_RCVD",
            "PICKUPS", "THROWS", "PASSES", "CATCHES", "STRIPS", "TOUCHDOWNS", "TD_PASSES"};

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
                    statManager.getStat(uuid, Action.PICKUPS),
                    statManager.getStat(uuid, Action.THROWS),
                    statManager.getStat(uuid, Action.PASSES),
                    statManager.getStat(uuid, Action.CATCHES),
                    statManager.getStat(uuid, Action.STRIPS),
                    statManager.getStat(uuid, Action.TOUCHDOWNS),
                    statManager.getStat(uuid, Action.TOUCHDOWN_PASSES)
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
                statline.touchdownPasses()
        )));

        return message.toString();
    }

    private record Statline(String team, String username, int kills, int deaths, int assists, int streak,
                           double damageDealt, double damageReceived,
                           int pickups, int throwz, int passes, int catches, int strips, int touchdowns, int touchdownPasses) implements Comparable<Statline> {

        @Override
        public int compareTo(Statline other) {
            return Integer.compare(other.kills, this.kills);
        }
    }
}
