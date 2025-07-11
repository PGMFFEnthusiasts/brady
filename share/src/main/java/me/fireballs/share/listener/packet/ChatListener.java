package me.fireballs.share.listener.packet;

import me.fireballs.brady.core.ConsoleAudienceInterceptorKt;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.util.FootballStatistic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    private final StatManager statManager;

    private MatchPlayer lastThrower;

    public ChatListener(StatManager statManager) {
        this.statManager = statManager;
        ConsoleAudienceInterceptorKt.addConsoleForwarding(Audience.PROVIDER, this::onChatMessage);
    }

    private void onChatMessage(String messageIn) {
        String message = ChatColor.stripColor(messageIn);
        if (message.isEmpty() || (message.charAt(0) != '⚠' && message.charAt(0) != ' ')) return;

        for (FootballStatistic footballStatistic : FootballStatistic.values()) {
            for (Pattern pattern : footballStatistic.getChatPatterns()) {
                if (pattern.pattern().isEmpty()) continue;

                Matcher matcher = pattern.matcher(message);
                if (matcher.find()) {
                    Player player = Bukkit.getPlayer(matcher.group(1));
                    statManager.incrementStat(player.getUniqueId(), footballStatistic);

                    switch (footballStatistic) {
                        case THROWS -> lastThrower = PGM.get().getMatchManager().getPlayer(player);
                        case CATCHES -> {
                            MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                            if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                statManager.incrementStat(lastThrower.getId(), FootballStatistic.PASSES);
                            }
                        }
                        case TOUCHDOWNS -> {
                            MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                            if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                statManager.incrementStat(lastThrower.getId(), FootballStatistic.TOUCHDOWN_PASSES);
                            }
                        }
                    }

                    return;
                }
            }
        }
    }
}
