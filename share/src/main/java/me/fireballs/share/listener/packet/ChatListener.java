package me.fireballs.share.listener.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.util.Action;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

public class ChatListener implements PacketListener, Listener {
    private final AtomicReference<Player> canary = new AtomicReference<>(null);

    private final StatManager statManager;

    private MatchPlayer lastThrower;

    public ChatListener(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CHAT_MESSAGE) return;

        if (canary.get() == null || event.getPlayer() != canary.get()) return;

        var packet = new WrapperPlayServerChatMessage(event);
        Component component = packet.getMessage().getChatContent();
        String message = PlainTextComponentSerializer.plainText().serialize(component);

        if (message.isEmpty() || (message.charAt(0) != '⚠' && message.charAt(0) != ' ')) return;

        Arrays.stream(Action.values())
                .filter(action -> !action.getPattern().pattern().isEmpty())
                .forEach(action -> {
                    Matcher matcher = action.getPattern().matcher(message);
                    if (matcher.find()) {
                        Player player = Bukkit.getPlayer(matcher.group(1));
                        statManager.incrementStat(player.getUniqueId(), action);

                        if (action == Action.THROWS) {
                            lastThrower = PGM.get().getMatchManager().getPlayer(player);
                        }

                        else if (action == Action.CATCHES) {
                            MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                            if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                statManager.incrementStat(lastThrower.getId(), Action.PASSES);
                            }
                        }

                        else if (action == Action.TOUCHDOWNS) {
                            MatchPlayer mp = PGM.get().getMatchManager().getPlayer(player);
                            if (mp != null && lastThrower != null && mp.getCompetitor() == lastThrower.getCompetitor()) {
                                statManager.incrementStat(lastThrower.getId(), Action.TOUCHDOWN_PASSES);
                            }
                        }
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (canary.get() == null) {
            canary.set(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == canary.get()) {
            canary.set(Bukkit.getServer().getOnlinePlayers().stream()
                    .filter(p -> p != event.getPlayer())
                    .findAny()
                    .orElse(null));
        }
    }
}
