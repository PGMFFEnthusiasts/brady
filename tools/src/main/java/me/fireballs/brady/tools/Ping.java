package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import kotlin.Unit;
import me.fireballs.brady.core.CommandBuilder;
import me.fireballs.brady.core.CommandExecution;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jctools.maps.NonBlockingHashMap;
import org.jetbrains.annotations.NotNull;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.named.NameStyle;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import static me.fireballs.brady.core.CommandKt.command;
import static me.fireballs.brady.core.PluginExtensionsKt.registerPacketEvents;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public class Ping extends PacketListenerAbstract {

    private final Tools plugin;
    private final BukkitAudiences bukkitAudiences;

    private final Map<User, PingStats> pings = new NonBlockingHashMap<>();
    private final Map<Long, PendingPing> callbacks = new NonBlockingHashMap<>();

    public Ping() {
        this.plugin = KoinJavaComponent.get(Tools.class);
        this.bukkitAudiences = KoinJavaComponent.get(BukkitAudiences.class);

        registerPacketEvents(plugin, this);

        command(
                "ping",
                "shows ping",
                null,
                "/ping <user>",
                new String[0],
                this::buildCommand
        );
    }

    private Unit buildCommand(CommandBuilder builder) {
        builder.tabCompleter(builder.getPlayerCompleter());

        builder.executor((ctx, _) -> {
            executeCommand(ctx);
            return Unit.INSTANCE;
        });

        return Unit.INSTANCE;
    }

    private void executeCommand(CommandExecution ctx) {
        try {
            Player target = switch (ctx.getArgs().length) {
                case 0 -> ctx.player();
                case 1 -> Bukkit.getPlayerExact(ctx.getArgs()[0]);
                default -> {
                    ctx.err("Too many arguments!");
                    yield null;
                }
            };

            if (target == null) {
                ctx.err("Couldn't find that player!");
                return;
            }

            long id = ThreadLocalRandom.current().nextInt();
            callbacks.put(id, new PendingPing(ctx, target));
            PacketEvents.getAPI().getPlayerManager().sendPacket(target, new WrapperPlayServerKeepAlive(id));
        } catch (Exception e) {
            sneakyThrow(e);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        pings.put(event.getUser(), new PingStats());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        pings.remove(user);
        callbacks.values().removeIf(pending -> pending.target.getUniqueId().equals(user.getUUID()));
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.KEEP_ALIVE) return;

        var wrapper = new WrapperPlayServerKeepAlive(event);
        PendingPing pending = callbacks.get(wrapper.getId());
        if (pending != null) {
            pending.sentTime = System.currentTimeMillis();
            return;
        }

        PingStats stats = pings.get(event.getUser());
        if (stats != null) {
            stats.outgoing.add(System.currentTimeMillis());
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) return;

        User user = event.getUser();
        var wrapper = new WrapperPlayClientKeepAlive(event);
        long id = wrapper.getId();

        PingStats stats = pings.get(user);
        if (stats == null) return;

        if (callbacks.containsKey(id)) {
            PendingPing pending = callbacks.remove(id);

            Player viewer = pending.ctx.player();
            Player target = pending.target;

            int ping = (int) (System.currentTimeMillis() - pending.sentTime);
            int average = stats.getAverage();
            int jitter = stats.getJitter();
            var line = stats.constructHistoryLine();

            Bukkit.getScheduler().runTask(plugin, () -> {
                var targetMP = PGM.get().getMatchManager().getPlayer(target);
                var senderMP = PGM.get().getMatchManager().getPlayer(viewer);
                if (senderMP == null || targetMP == null) return;

//                senderMP.sendMessage(empty());
//                senderMP.sendMessage(text(" ⚡ Ping for ", NamedTextColor.YELLOW)
//                        .append(targetMP.getName(NameStyle.FANCY)));
//                senderMP.sendMessage(empty());
//                senderMP.sendMessage(text(" [", NamedTextColor.GRAY)
//                        .append(line)
//                        .append(text("] ", NamedTextColor.GRAY))
//                        .append(text(average + "ms", getPingColor(average)))
//                        .append(text(" ± ", NamedTextColor.GRAY))
//                        .append(text(jitter, getJitterColor(jitter)))
//                        .append(text(" (60s avg)", NamedTextColor.GRAY, TextDecoration.ITALIC)));
//                senderMP.sendMessage(empty());
//                senderMP.sendMessage(text(" ⚡ Latest: ", NamedTextColor.YELLOW)
//                        .append(text(ping + "ms", getPingColor(ping))));
//                senderMP.sendMessage(text(" ")
//                        .append(targetMP.getName(NameStyle.FANCY))
//                        .append(text(" [", NamedTextColor.GRAY))
//                        .append(line)
//                        .append(text("] ", NamedTextColor.GRAY)));
//                senderMP.sendMessage(text(" ⤷ ", NamedTextColor.GRAY)
//                        .append(text(average + "ms", getPingColor(average)))
//                        .append(text(" ± ", NamedTextColor.GRAY))
//                        .append(text(jitter, getJitterColor(jitter)))
//                        .append(text(" (60s avg)", NamedTextColor.GRAY, TextDecoration.ITALIC)));
//                senderMP.sendMessage(text(" ⤷ ", NamedTextColor.GRAY)
//                        .append(text(ping + "ms", getPingColor(ping)))
//                        .append(text(" (latest)", NamedTextColor.GRAY, TextDecoration.ITALIC)));
//                senderMP.sendMessage(empty());

                senderMP.sendMessage(text(" ")
                        .append(targetMP.getName(NameStyle.FANCY))
                        .append(text(" "))
                        .append(text(ping + "ms", getPingColor(ping)))
                        .append(text(" (latest)", NamedTextColor.GRAY, TextDecoration.ITALIC)));
                senderMP.sendMessage(text(" [", NamedTextColor.GRAY)
                        .append(line)
                        .append(text("] ", NamedTextColor.GRAY))
                        .append(text(average + "ms", getPingColor(average)))
                        .append(text(" ± ", NamedTextColor.GRAY))
                        .append(text(jitter, getJitterColor(jitter)))
                        .append(text(" (60s avg)", NamedTextColor.GRAY, TextDecoration.ITALIC)));

//                TextComponent.Builder message = text()
//                        .append(text("Pong! ", NamedTextColor.GOLD))
//                        .append(text("(", NamedTextColor.GRAY))
//                        .append(text(ping + "ms", getPingColor(ping), TextDecoration.BOLD))
//                        .append(text(") ", NamedTextColor.GRAY));
//
//                if (viewer != target) {
//                    MatchPlayer pgmTarget = PGM.get().getMatchManager().getPlayer(target);
//                    if (pgmTarget == null) return;
//
//                    message.append(pgmTarget.getName(NameStyle.FANCY))
//                            .append(text("'s "));
//                }
//
//                message.append(text("60s Average", NamedTextColor.AQUA))
//                        .append(text(": ", NamedTextColor.WHITE))
//                        .append(text(average + "ms", getPingColor(average), TextDecoration.BOLD))
//                        .append(text(" ± ", NamedTextColor.AQUA))
//                        .append(text(jitter, getJitterColor(jitter)));
//
//                bukkitAudiences.player(viewer).sendMessage(message.build());
            });

            event.setCancelled(true);
            return; // don't track our manual keepalives to not skew the 60-second average
        }

        Long sentTime = stats.outgoing.poll();
        if (sentTime == null) return;

        int ping = (int) (System.currentTimeMillis() - sentTime);
        stats.trackPing(ping);
    }

    private static class PingStats {
        final Queue<Long> outgoing = new ArrayDeque<>();
        final int[] responses = new int[30]; // server sends a keepalive every 2 seconds
        int index;
        int count;

        void trackPing(int ping) {
            responses[index] = ping;
            index = (index + 1) % responses.length;
            if (count < responses.length) count++;
        }

        int getAverage() {
            if (count == 0) return -1;
            int sum = 0;
            for (int i = 0; i < count; i++) {
                sum += responses[i];
            }
            return sum / count;
        }

        int getJitter() {
            if (count == 0) return -1;
            int sum = 0;
            int average = getAverage();
            for (int i = 0; i < count; i++)
                sum = sum + Math.abs(responses[i] - average);
            return sum / count;
        }

        private static final Component EMPTY_BAR = text("┃", NamedTextColor.GRAY);
        Component constructHistoryLine() {
            var bars = new Component[responses.length];
            Arrays.fill(bars, EMPTY_BAR);
            if (count != 0) {
                for (var i = 0; i < responses.length; i++) {
                    var ping = responses[Math.floorMod(index - count + i, responses.length)];
                    var color = getPingColor(ping);
                    bars[i] = text("┃", color).hoverEvent(text(ping + "ms", color));
                }
            }

            return Component.join(JoinConfiguration.noSeparators(), bars);
        }
    }

    private static class PendingPing {
        final CommandExecution ctx;
        final Player target;
        long sentTime;

        PendingPing(CommandExecution ctx, Player target) {
            this.ctx = ctx;
            this.target = target;
        }
    }

    private static NamedTextColor getPingColor(int ping) {
        if (ping < 50) return NamedTextColor.GREEN;
        if (ping < 100) return NamedTextColor.DARK_GREEN;
        if (ping < 150) return NamedTextColor.YELLOW;
        if (ping < 200) return NamedTextColor.RED;
        return NamedTextColor.DARK_RED;
    }

    private static NamedTextColor getJitterColor(int jitter) {
        if (jitter < 5) return NamedTextColor.GREEN;
        if (jitter < 10) return NamedTextColor.DARK_GREEN;
        if (jitter < 15) return NamedTextColor.YELLOW;
        if (jitter < 20) return NamedTextColor.RED;
        return NamedTextColor.DARK_RED;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
