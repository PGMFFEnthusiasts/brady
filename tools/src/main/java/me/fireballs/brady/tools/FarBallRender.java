package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.fireballs.brady.corepgm.FeatureFlagBool;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.koin.java.KoinJavaComponent;

import java.util.*;

public class FarBallRender implements Listener {

    private static final double MAX_SNOWBALL_RENDER_DISTANCE = 64*64;
    private static final double MAX_ARMOR_STAND_RENDER_DISTANCE = 256*256;

    private final List<ThrownSnowball> entities = new ArrayList<>();
    private final ToolsSettings settings;
    private final FeatureFlagBool enabled = new FeatureFlagBool("farBallRender", true);

    public FarBallRender() {
        this.settings = KoinJavaComponent.get(ToolsSettings.class);

        Tools plugin = KoinJavaComponent.get(Tools.class);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> entities.removeIf(ThrownSnowball::update), 0L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!enabled.getState()) return;

        if (event.getEntity() instanceof Snowball snowball) {
            ItemType itemType = (snowball.getShooter() instanceof Player player)
                    ? settings.getProjectileSkin().retrieveValue(player.getUniqueId()).getItemType()
                    : ItemTypes.SNOWBALL;

            entities.add(new ThrownSnowball(snowball, itemType));
        }
    }

    private static class ThrownSnowball {

        private record Orientation(Vector3d position, Vector3f armPose) {}

        private final Snowball entity;
        private final int entityId = Bukkit.allocateEntityId();
        private final Set<Player> viewers = new HashSet<>();
        private final Equipment equipment;

        private Location loc;

        public ThrownSnowball(Snowball entity, ItemType itemType) {
            this.entity = entity;
            this.equipment = new Equipment(EquipmentSlot.MAIN_HAND, new ItemStack.Builder()
                    .type(itemType)
                    .build());
        }

        public boolean update() {
            if (!entity.isValid()) {
                for (Player viewer : viewers) {
                    destroyArmorStand(viewer);
                }
                return true;
            }

            this.loc = entity.getLocation();

            Iterator<Player> iterator = viewers.iterator();
            while (iterator.hasNext()) {
                Player viewer = iterator.next();
                if (!viewer.isOnline() || !viewer.getWorld().equals(entity.getWorld())) {
                    destroyArmorStand(viewer);
                    iterator.remove();
                }
            }

            entity.getWorld().getPlayers().forEach(this::updatePlayer);

            return false;
        }

        private void updatePlayer(Player player) {
            double distance = loc.distanceSquared(player.getLocation());

            if (distance > MAX_SNOWBALL_RENDER_DISTANCE && distance < MAX_ARMOR_STAND_RENDER_DISTANCE) {
                if (viewers.add(player)) {
                    spawnArmorStand(player);
                } else {
                    moveArmorStand(player);
                }
            } else if (viewers.remove(player)) {
                destroyArmorStand(player);
            }
        }

        private static final double ARM_LENGTH = 10.0 / 16.0;
        private static final double ARM_PIVOT_HEIGHT = 22.0 / 16.0;
        private static final double SHOULDER_OFFSET_X = -5.0 / 16.0;

        private Orientation getOrientation(Player player) {
            org.bukkit.Location eye = player.getEyeLocation();

            double dx = eye.getX() - loc.getX();
            double dy = eye.getY() - loc.getY();
            double dz = eye.getZ() - loc.getZ();

            double distanceXZ = Math.hypot(dx, dz);

            float pitch = (float) Math.toDegrees(Math.atan2(-distanceXZ, -dy));
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

            Vector3f armPose = new Vector3f(pitch, yaw, 0f);

            double radX = Math.toRadians(pitch);
            double radY = Math.toRadians(yaw);

            double sinX = Math.sin(radX);
            double cosX = Math.cos(radX);
            double sinY = Math.sin(radY);
            double cosY = Math.cos(radY);

            double yPrime = ARM_LENGTH * cosX;
            double zPrime = ARM_LENGTH * sinX;

            double handX = SHOULDER_OFFSET_X + (zPrime * sinY);
            double handY = ARM_PIVOT_HEIGHT - yPrime;
            double handZ = -zPrime * cosY;

            double feetX = loc.getX() - handX;
            double feetY = loc.getY() - handY;
            double feetZ = loc.getZ() - handZ;

            Vector3d position = new Vector3d(feetX, feetY, feetZ);

            return new Orientation(position, armPose);
        }

        private void spawnArmorStand(Player player) {
            Orientation orientation = getOrientation(player);

            var spawn = new WrapperPlayServerSpawnLivingEntity(
                    entityId,
                    UUID.randomUUID(),
                    EntityTypes.ARMOR_STAND,
                    orientation.position(),
                    0,
                    0,
                    0,
                    new Vector3d(),
                    List.of(
                            new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20), // invisible
                            new EntityData<>(10, EntityDataTypes.BYTE, (byte) (0x08 | 0x10)), // no baseplate & marker
                            new EntityData<>(14, EntityDataTypes.ROTATION, orientation.armPose())
                    )
            );

            var equip = new WrapperPlayServerEntityEquipment(entityId, List.of(equipment));

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, equip);
        }

        private void moveArmorStand(Player player) {
            Orientation orientation = getOrientation(player);

            var teleport = new WrapperPlayServerEntityTeleport(entityId, orientation.position(), 0, 0, true);
            var metadata = new WrapperPlayServerEntityMetadata(entityId, List.of(new EntityData<>(14, EntityDataTypes.ROTATION, orientation.armPose())));

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, teleport);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
        }

        private void destroyArmorStand(Player player) {
            var destroy = new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
        }
    }
}
