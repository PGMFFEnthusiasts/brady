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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jctools.maps.NonBlockingHashMap;
import org.jetbrains.annotations.NotNull;
import org.koin.java.KoinJavaComponent;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import static me.fireballs.brady.core.CommandKt.command;
import static me.fireballs.brady.core.PluginExtensionsKt.registerPacketEvents;

public class Ping extends PacketListenerAbstract {

    private final Map<User, PingState> pings = new NonBlockingHashMap<>();
    private final Map<Long, PendingPing> callbacks = new NonBlockingHashMap<>();

    public Ping() {
        Tools plugin = KoinJavaComponent.get(Tools.class);
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
        pings.put(event.getUser(), new PingState());
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

        PingState state = pings.get(event.getUser());
        if (state != null) {
            state.outgoing.add(System.currentTimeMillis());
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) return;

        User user = event.getUser();
        var wrapper = new WrapperPlayClientKeepAlive(event);
        long id = wrapper.getId();

        PingState state = pings.get(user);
        if (state == null) return;

        if (callbacks.containsKey(id)) {
            PendingPing pending = callbacks.remove(id);

            int ping = (int) (System.currentTimeMillis() - pending.sentTime);
            int average = state.getAverage();
            int jitter = state.getJitter();

            String targetName = pending.ctx.player() == pending.target
                    ? ""
                    : pending.target.getDisplayName() + "§b's ";

            pending.ctx.player().sendMessage(
                    String.format("§6Pong! §7(%s§l%dms§7) %s§b60s Average§f: %s§l%dms§b ± %s%d",
                            getPingColor(ping), ping,
                            targetName,
                            getPingColor(average), average,
                            getJitterColor(jitter), jitter
                    )
            );

            state.trackPing(ping);

            event.setCancelled(true);
            return; // don't track our manual keepalives to not skew the 60-second average
        }

        Long sentTime = state.outgoing.poll();
        if (sentTime == null) return;

        int ping = (int) (System.currentTimeMillis() - sentTime);
        state.trackPing(ping);
    }

    private static class PingState {
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

    private static ChatColor getPingColor(int ping) {
        if (ping < 50) return ChatColor.GREEN;
        if (ping < 100) return ChatColor.DARK_GREEN;
        if (ping < 150) return ChatColor.YELLOW;
        if (ping < 200) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }

    private static ChatColor getJitterColor(int jitter) {
        if (jitter < 5) return ChatColor.GREEN;
        if (jitter < 10) return ChatColor.DARK_GREEN;
        if (jitter < 15) return ChatColor.YELLOW;
        if (jitter < 20) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
