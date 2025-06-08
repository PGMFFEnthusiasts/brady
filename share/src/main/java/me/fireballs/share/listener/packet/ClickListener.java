package me.fireballs.share.listener.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.common.collect.ImmutableSet;
import me.fireballs.share.manager.ClientDataManager;
import me.fireballs.share.util.BlockUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;

public class ClickListener implements PacketListener, Listener {
    private final ClientDataManager clientDataManager;

    public ClickListener(ClientDataManager clientDataManager) {
        this.clientDataManager = clientDataManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPlayer() == null) return;

        Player player = event.getPlayer();

        clientDataManager.getData(player.getUniqueId()).ifPresent(data -> {
            if (PacketCollection.MOVEMENT_PACKETS.contains(event.getPacketType())) {
                data.handleTick();
            }

            else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                if (!BlockUtil.hasTargetedBlock(player)) {
                    data.handleClick();
                }
            }

            else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                var packet = new WrapperPlayClientInteractEntity(event);
                if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && BlockUtil.hasTargetedBlock(player)) {
                    data.handleClick();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        clientDataManager.add(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clientDataManager.remove(event.getPlayer().getUniqueId());
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
