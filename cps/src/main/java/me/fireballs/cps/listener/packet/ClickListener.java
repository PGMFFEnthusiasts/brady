package me.fireballs.cps.listener.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.common.collect.ImmutableSet;
import me.fireballs.cps.CPSPlugin;
import me.fireballs.cps.manager.ClientDataManager;
import me.fireballs.cps.util.BlockUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;

public class ClickListener implements PacketListener, Listener {
    private final CPSPlugin plugin;
    private final ClientDataManager clientDataManager;

    public ClickListener(CPSPlugin plugin, ClientDataManager clientDataManager) {
        this.plugin = plugin;
        this.clientDataManager = clientDataManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Client)) return;

        User user = event.getUser();
        Player player = event.getPlayer();

        clientDataManager.getData(user).ifPresent(data -> {
            if (PacketCollection.MOVEMENT_PACKETS.contains(type)) {
                data.handleTick();
            }

            else if (type == PacketType.Play.Client.ANIMATION) {
                if (!BlockUtil.hasTargetedBlock(player)) {
                    data.handleClick();
                }
            }

            else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
                var packet = new WrapperPlayClientInteractEntity(event);
                if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && BlockUtil.hasTargetedBlock(player)) {
                    data.handleClick();
                }
            }
        });
    }

    @EventHandler
    public void onUserLogin(UserLoginEvent event) {
        clientDataManager.add(event.getUser());
    }

    @EventHandler
    public void onUserDisconnect(UserDisconnectEvent event) {
        clientDataManager.remove(event.getUser());
    }

    private static class PacketCollection {
        private static final Set<PacketTypeCommon> MOVEMENT_PACKETS = ImmutableSet.of(
                PacketType.Play.Client.PLAYER_FLYING,
                PacketType.Play.Client.PLAYER_POSITION,
                PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                PacketType.Play.Client.PLAYER_ROTATION
        );
    }
}
