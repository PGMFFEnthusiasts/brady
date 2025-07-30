package me.fireballs.cps.listener.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.fireballs.cps.CPSPlugin;
import me.fireballs.cps.manager.ShadowManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;

import java.util.*;

public class ShadowListener implements PacketListener, Listener {
    private static final double HEIGHT_OFFSET = 2.065;

    private final CPSPlugin plugin;
    private final ShadowManager shadowManager;

    public ShadowListener(CPSPlugin plugin, ShadowManager shadowManager) {
        this.plugin = plugin;
        this.shadowManager = shadowManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Server)) return;

        if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            var packet = new WrapperPlayServerSpawnPlayer(event);

            int playerId = packet.getEntityId();
            Entity player = SpigotConversionUtil.getEntityById(((Player) event.getPlayer()).getWorld(), playerId);
            if (!(player instanceof Player)) return;

            Location standLocation = player.getLocation();
            standLocation.setY(standLocation.getY() + HEIGHT_OFFSET);

            shadowManager.getData(packet.getEntityId()).ifPresent(data -> {
                ChatColor color = ChatColor.AQUA;
                MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
                if (matchPlayer != null && matchPlayer.getCompetitor() instanceof Team team) {
                    color = team.getInfo().getDefaultColor();
                }

                List<EntityData<?>> entityDataList = List.of(
                        new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20), // invisible
                        new EntityData<>(2, EntityDataTypes.STRING, color + "0§7 CPS"),
                        new EntityData<>(3, EntityDataTypes.BYTE, (byte) 1), // always show nametag
                        new EntityData<>(10, EntityDataTypes.BYTE, (byte) (0x08 | 0x10)) // nobaseplate & marker
                );

                event.getUser().sendPacket(new WrapperPlayServerSpawnEntity(data.getId(), null, EntityTypes.ARMOR_STAND, SpigotConversionUtil.fromBukkitLocation(standLocation), 0f, 0, null));
                event.getUser().sendPacket(new WrapperPlayServerEntityMetadata(data.getId(), entityDataList));

                data.addViewer(event.getPlayer());
            });
        }

        else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            var packet = new WrapperPlayServerDestroyEntities(event);

            Arrays.stream(packet.getEntityIds())
                    .mapToObj(shadowManager::getData)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(data -> {
                        event.getUser().sendPacket(new WrapperPlayServerDestroyEntities(data.getId()));
                        data.removeViewer(event.getPlayer());
                    });
        }

        else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            var packet = new WrapperPlayServerEntityRelativeMove(event);
            shadowManager.getData(packet.getEntityId()).ifPresent(data -> {
                var packetCopy = new WrapperPlayServerEntityRelativeMove(0, 0, 0, 0, true);
                packetCopy.copy(packet);
                packetCopy.setEntityId(data.getId());
                event.getUser().sendPacket(packetCopy);
            });
        }

        else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            var packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            shadowManager.getData(packet.getEntityId()).ifPresent(data -> {
                var packetCopy = new WrapperPlayServerEntityRelativeMoveAndRotation(0, 0, 0, 0, 0, 0, true);
                packetCopy.copy(packet);
                packetCopy.setEntityId(data.getId());
                event.getUser().sendPacket(packetCopy);
            });
        }

        else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            var packet = new WrapperPlayServerEntityTeleport(event);
            shadowManager.getData(packet.getEntityId()).ifPresent(data -> {
                var packetCopy = new WrapperPlayServerEntityTeleport(data.getId(), packet.getPosition().add(0, HEIGHT_OFFSET, 0), 0, 0, false);
                event.getUser().sendPacket(packetCopy);
            });
        }

        else if (type == PacketType.Play.Server.ENTITY_METADATA) {
            var packet = new WrapperPlayServerEntityMetadata(event);
            shadowManager.getData(packet.getEntityId()).ifPresent(data -> {
                for (EntityData<?> meta : packet.getEntityMetadata()) {
                    if (meta.getIndex() != 0 && meta.getIndex() != 6) continue;

                    if (meta.getType() == EntityDataTypes.FLOAT) {
                        float health = (float) meta.getValue();
                        if (health <= 0) {
                            event.getUser().sendPacket(new WrapperPlayServerDestroyEntities(data.getId()));
                            data.removeViewer(event.getPlayer());
                        }
                    }

                    else if (meta.getType() == EntityDataTypes.BYTE) {
                        Byte item = (Byte) meta.getValue();
                        boolean crouching = (item & 0x02) != 0;
                        byte showNametag = (byte) (crouching ? 0 : 1);

                        event.getUser().sendPacket(
                                new WrapperPlayServerEntityMetadata(data.getId(), List.of(new EntityData<>(3, EntityDataTypes.BYTE, showNametag)))
                        );
                    }
                }
            });
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        shadowManager.add(event.getPlayer().getEntityId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                shadowManager.remove(event.getPlayer().getEntityId()), 1L);
    }
}