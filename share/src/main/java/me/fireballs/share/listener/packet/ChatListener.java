package me.fireballs.share.listener.packet;

import me.fireballs.brady.core.ConsoleAudienceInterceptorKt;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.util.Action;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;

import java.util.Arrays;
import java.util.regex.Matcher;

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

        Arrays.stream(Action.values())
                .filter(action -> !action.getPattern().pattern().isEmpty())
                .forEach(action -> {
                    Matcher matcher = action.getPattern().matcher(message);
                    if (matcher.find()) {
                        Player player = Bukkit.getPlayer(matcher.group(1));
                        statManager.incrementStat(player.getUniqueId(), action);

                        switch (action) {
                            case THROWS -> lastThrower = PGM.get().getMatchManager().getPlayer(player);
                            case CATCHES -> {
                                MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                                if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                    statManager.incrementStat(lastThrower.getId(), Action.PASSES);
                                }
                            }
                            case TOUCHDOWNS -> {
                                MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                                if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                    statManager.incrementStat(lastThrower.getId(), Action.TOUCHDOWN_PASSES);
                                }
                            }
                        }
                    }
                });
    }
}
