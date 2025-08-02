package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import kotlin.Lazy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

import java.util.*;

import static me.fireballs.brady.core.PluginExtensionsKt.registerEvents;
import static me.fireballs.brady.core.PluginExtensionsKt.registerPacketEvents;
import static net.kyori.adventure.text.Component.text;

public class BallCam extends PacketListenerAbstract implements Listener {
    private static final Component PREFIX = text("» ", NamedTextColor.AQUA).append(text("BallCam toggled ", NamedTextColor.GRAY));
    private static final Component ON = PREFIX.append(text("ON", NamedTextColor.GREEN, TextDecoration.BOLD));
    private static final Component OFF = PREFIX.append(text("OFF", NamedTextColor.RED, TextDecoration.BOLD));

    private static final int SLOT = 7;
    private static final double RIDING_OFFSET = -1.184425;
    private static final short TID = Short.MAX_VALUE * 5 / 6;

    private final ThreadLocal<Map<User, Rider>> riders = ThreadLocal.withInitial(HashMap::new);

    public BallCam() {
        Lazy<Tools> plugin = KoinJavaComponent.inject(Tools.class);
        registerEvents(plugin.getValue(), this);
        registerPacketEvents(plugin.getValue(), this);
    }

    private enum Status {
        DISMOUNTED, RIDING, DISMOUNTING
    }

    private static class Rider {
        private boolean enabled;
        private Status status = Status.DISMOUNTED;
        private int snowballId;
        private int heldSlot;
        private Location location;

        private void toggle(User user) {
            enabled = !enabled;

            if (enabled) {
                user.sendMessage(ON);
            } else {
                user.sendMessage(OFF);
                if (status == Status.RIDING) {
                    this.beginDismount(user);
                }
            }
        }

        private boolean startRide(int snowballId) {
            if (snowballId != this.snowballId) {
                this.snowballId = snowballId;
                return true;
            }
            return false;
        }

        private void finishRide() {
            this.status = Status.RIDING;
        }

        private void beginDismount(User user) {
            this.status = Status.DISMOUNTING;

            var ping = new WrapperPlayServerWindowConfirmation((byte) 0, TID, false);
            user.sendPacket(ping);

            var riding = new WrapperPlayServerAttachEntity(user.getEntityId(), -1, false);
            user.sendPacket(riding);

            var pos = new WrapperPlayServerPlayerPositionAndLook(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    (byte) 0,
                    0,
                    true
            );
            user.sendPacket(pos);
        }

        private void finishDismount() {
            this.status = Status.DISMOUNTED;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Server)) return;

        Player player = event.getPlayer();
        User user = event.getUser();
        Rider rider = riders.get().get(user);
        if (rider == null) return;

        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            if (!rider.enabled) return;
            if (rider.status != Status.DISMOUNTED) return;

            MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
            if (matchPlayer == null) return;
            if (!matchPlayer.isObserving()) return;

            var spawn = new WrapperPlayServerSpawnEntity(event);
            if (spawn.getEntityType() != EntityTypes.SNOWBALL) return;

            Vector3d newPos = spawn.getPosition().add(0, RIDING_OFFSET, 0);
            spawn.setPosition(newPos);
            event.markForReEncode(true);

            int snowballId = spawn.getEntityId();
            var riding = new WrapperPlayServerAttachEntity(user.getEntityId(), snowballId, false);
            Location location = player.getLocation();

            var pos = new WrapperPlayServerPlayerPositionAndLook(
                    0,
                    0,
                    0,
                    spawn.getYaw(),
                    spawn.getPitch(),
                    (byte) ((1 << 0) | (1 << 1) | (1 << 2)),
                    0,
                    true
            );

            event.getTasksAfterSend().add(() -> {
                if (rider.startRide(snowballId)) {
                    user.sendPacket(riding);
                    user.sendPacket(pos);
                    user.sendMessage(text(""), ChatTypes.GAME_INFO);
                    rider.location = location;
                    rider.finishRide();
                }
            });
        }

        else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            if (rider.status != Status.RIDING) return;

            var destroy = new WrapperPlayServerDestroyEntities(event);
            for (int entityId : destroy.getEntityIds()) {
                if (entityId == rider.snowballId) {
                    rider.beginDismount(user);
                    break;
                }
            }
        }

        else if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            if (rider.status == Status.RIDING) {
                rider.beginDismount(user);
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Client)) return;

        Player player = event.getPlayer();
        User user = event.getUser();
        Rider rider = riders.get().get(user);
        if (rider == null) return;

        if (type == PacketType.Play.Client.STEER_VEHICLE) {
            if (rider.status != Status.RIDING) return;

            var steer = new WrapperPlayClientSteerVehicle(event);
            if (steer.isUnmount()) {
                rider.beginDismount(user);
            }
        }

        else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            if (rider.status != Status.DISMOUNTED) {
                event.setCancelled(true);
            }
        }

        else if (type == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            if (rider.status != Status.DISMOUNTING) return;

            var ping = new WrapperPlayClientWindowConfirmation(event);
            if (ping.getActionId() == TID) {
                rider.finishDismount();
            }
        }

        else if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            var slot = new WrapperPlayClientHeldItemChange(event);
            rider.heldSlot = slot.getSlot();
        }

        else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            var place = new WrapperPlayClientPlayerBlockPlacement(event);
            if (place.getFace() != BlockFace.OTHER) return;
            if (rider.heldSlot != SLOT) return;

            MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
            if (matchPlayer != null && matchPlayer.isObserving()) {
                rider.toggle(user);
            }
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        riders.get().put(event.getUser(), new Rider());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        riders.get().remove(event.getUser());
    }

    @EventHandler
    public void giveKit(ObserverKitApplyEvent event) {
        ItemStack item = new ItemStack(Material.SNOW_BALL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + ChatColor.BOLD.toString() + "Ball Cam");
        item.setItemMeta(meta);

        Player player = event.getPlayer().getBukkit();
        player.getInventory().setItem(SLOT, item);
    }
}
