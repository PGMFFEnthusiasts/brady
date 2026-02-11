package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import me.fireballs.brady.core.FeatureFlagBool;
import me.fireballs.brady.core.PlayerExtensionsKt;
import me.fireballs.brady.core.PluginExtensionsKt;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.entity.Player;
import org.jctools.maps.NonBlockingHashMap;
import org.jetbrains.annotations.NotNull;
import org.koin.java.KoinJavaComponent;

import java.util.Map;

public class JumpResetParticles extends PacketListenerAbstract {

    private static final short TRANSACTION_ID = -333;

    private final Tools plugin;
    private final ToolsSettings settings;
    private final Map<User, PlayerState> states = new NonBlockingHashMap<>();
    private final FeatureFlagBool enabled = new FeatureFlagBool("jumpResetParticles", true);

    public JumpResetParticles() {
        this.plugin = KoinJavaComponent.get(Tools.class);
        this.settings = KoinJavaComponent.get(ToolsSettings.class);
        PluginExtensionsKt.registerPacketEvents(plugin, this);
    }

    private static class PlayerState {
        boolean receivedVelocity;
        double y;
        double lastY;
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        states.put(event.getUser(), new PlayerState());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        states.remove(event.getUser());
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            PlayerState state = states.get(event.getUser());
            if (state == null) return;

            handleFlying(event, state);
        }
        else if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            PlayerState state = states.get(event.getUser());
            if (state == null) return;

            handleTransaction(event, state);
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            handleVelocity(event);
        }
    }

    private void handleFlying(PacketReceiveEvent event, PlayerState state) {
        var wrapper = new WrapperPlayClientPlayerFlying(event);

        if (wrapper.hasPositionChanged()) {
            state.lastY = state.y;
            state.y = wrapper.getLocation().getY();

            double dy = state.y - state.lastY;
            boolean jumped = Math.abs(dy - 0.42) < 1e-5;

            if (jumped && state.receivedVelocity) {
                playEffect(event.getPlayer(), wrapper.getLocation());
            }
        }

        state.receivedVelocity = false;
    }

    private void handleTransaction(PacketReceiveEvent event, PlayerState state) {
        var wrapper = new WrapperPlayClientWindowConfirmation(event);
        if (wrapper.getActionId() != TRANSACTION_ID) return;

        state.receivedVelocity = true;
    }

    private void handleVelocity(PacketSendEvent event) {
        var wrapper = new WrapperPlayServerEntityVelocity(event);
        if (wrapper.getEntityId() != event.getUser().getEntityId()) return;

        var ping = new WrapperPlayServerWindowConfirmation(0, TRANSACTION_ID, false);
        event.getTasksAfterSend().add(() -> event.getUser().sendPacket(ping));
    }

    private void playEffect(Player player, Location loc) {
        if (!enabled.getState()) return;
        var packet = new PacketPlayOutWorldParticles(
                EnumParticle.SPELL_WITCH, false,
                (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                0.15f, 0f, 0.15f, // offset
                0f, 30 // speed, count
        );

        for (Player p : player.getWorld().getPlayers()) {
            if (!settings.getJumpResetParticles().retrieveValue(p)) continue;
            PlayerExtensionsKt.sendPacket(p, packet);
        }
    }
}
