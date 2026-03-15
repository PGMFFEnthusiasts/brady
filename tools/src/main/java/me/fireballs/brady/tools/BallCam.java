package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.google.common.collect.MapMaker;
import kotlin.Lazy;
import kotlin.Unit;
import me.fireballs.brady.core.BooleanSettingValue;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

import java.util.Map;

import static me.fireballs.brady.core.PluginExtensionsKt.registerEvents;
import static me.fireballs.brady.core.PluginExtensionsKt.registerPacketEvents;
import static net.kyori.adventure.text.Component.text;

public class BallCam extends PacketListenerAbstract implements Listener {
    private static final short TID = Short.MAX_VALUE * 5 / 6;
    private static final float MIN_PITCH = 20;

    private final Map<User, Rider> riders = new MapMaker()
            .concurrencyLevel(4)
            .makeMap();

    private final BooleanSettingValue setting;

    public BallCam() {
        Lazy<Tools> plugin = KoinJavaComponent.inject(Tools.class);
        Lazy<ToolsSettings> settings = KoinJavaComponent.inject(ToolsSettings.class);
        setting = settings.getValue().getBallCam();
        registerEvents(plugin.getValue(), this);
        registerPacketEvents(plugin.getValue(), this);

        setting.onSettingChange((player, newValue) -> {
            handleBallSettingsChange(player, newValue);
            return Unit.INSTANCE;
        });
    }

    private enum Status {
        DISMOUNTED, RIDING, DISMOUNTING
    }

    private static class Rider {
        private Status status = Status.DISMOUNTED;
        private int snowballId;
        private Location location;

        private void setEnabled(User user, boolean enabled) {
            if (enabled) return;
            if (status != Status.RIDING) return;
            this.beginDismount(user);
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
        Rider rider = riders.get(user);
        if (rider == null) return;

        switch (type) {
            case PacketType.Play.Server.SPAWN_ENTITY -> {
                if (!setting.retrieveValue(player)) return;
                if (rider.status != Status.DISMOUNTED) return;

                MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
                if (matchPlayer == null) return;
                if (!matchPlayer.isObserving()) return;

                var spawn = new WrapperPlayServerSpawnEntity(event);
                if (spawn.getEntityType() != EntityTypes.SNOWBALL) return;

                if (spawn.getYaw() < MIN_PITCH) return; // yaw & pitch are sent out of order

                int snowballId = spawn.getEntityId();
                var riding = new WrapperPlayServerAttachEntity(user.getEntityId(), snowballId, false);
                Location location = player.getLocation();

                var pos = new WrapperPlayServerPlayerPositionAndLook(
                        0,
                        0,
                        0,
                        -spawn.getPitch(),
                        -spawn.getYaw(),
                        (byte) (1 | (1 << 1) | (1 << 2)),
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
            case PacketType.Play.Server.DESTROY_ENTITIES -> {
                if (rider.status != Status.RIDING) return;

                var destroy = new WrapperPlayServerDestroyEntities(event);
                for (int entityId : destroy.getEntityIds()) {
                    if (entityId == rider.snowballId) {
                        rider.beginDismount(user);
                        break;
                    }
                }
            }
            case PacketType.Play.Server.PLAYER_POSITION_AND_LOOK -> {
                if (rider.status == Status.RIDING) {
                    rider.beginDismount(user);
                }
            }
            default -> {}
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Client)) return;

        User user = event.getUser();
        Rider rider = riders.get(user);
        if (rider == null) return;

        switch (type) {
            case PacketType.Play.Client.STEER_VEHICLE -> {
                if (rider.status != Status.RIDING) return;

                var steer = new WrapperPlayClientSteerVehicle(event);
                if (steer.isUnmount()) {
                    rider.beginDismount(user);
                }
            }
            case PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION -> {
                if (rider.status != Status.DISMOUNTED) {
                    event.setCancelled(true);
                }
            }
            case PacketType.Play.Client.WINDOW_CONFIRMATION -> {
                if (rider.status != Status.DISMOUNTING) return;

                var ping = new WrapperPlayClientWindowConfirmation(event);
                if (ping.getActionId() == TID) {
                    rider.finishDismount();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        riders.put(event.getUser(), new Rider());
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        riders.remove(event.getUser());
    }

    private void handleBallSettingsChange(Player player, boolean enabled) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        if (matchPlayer == null || !matchPlayer.isObserving()) return;

        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        //noinspection ConstantValue
        if (user == null) return;

        Rider rider = riders.get(user);
        if (rider == null) return;

        var swing = new WrapperPlayServerEntityAnimation(user.getEntityId(), WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM);
        user.sendPacket(swing);

        player.playSound(player.getLocation(), Sound.CLICK, 1f, 1.7f);

        rider.setEnabled(user, enabled);
    }
}
