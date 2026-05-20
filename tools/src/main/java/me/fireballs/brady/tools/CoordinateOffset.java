package me.fireballs.brady.tools;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.fireballs.brady.core.PluginExtensionsKt;
import me.fireballs.brady.corepgm.FeatureFlagBool;
import me.fireballs.brady.tools.packet.WrapperPlayServerUpdateSign;
import org.jctools.maps.NonBlockingHashSet;
import org.jetbrains.annotations.NotNull;
import org.koin.java.KoinJavaComponent;

import java.util.Optional;
import java.util.Set;

/**
 * when a player crosses a power of 2, their hitbox changes slightly because
 * of floating point imprecision, which can cause clipping into walls.
 * to mitigate this without moving every map, we can spoof server coordinates
 **/
public class CoordinateOffset extends PacketListenerAbstract {

    // every map must remain within (-n, -n), (n, n)
    private static final int LARGEST_MAP_COORD = 15000;

    // by adding a fixed offset of 2^n+2 - 2^n, map coordinates will never cross a power of 2
    private static final int OFFSET = (Integer.highestOneBit(LARGEST_MAP_COORD - 1) << 1) * 3; // triple the next power of 2
    private static final Vector3i OFFSET_3I = new Vector3i(OFFSET, 0, OFFSET);
    private static final Vector3f OFFSET_3F = new Vector3f(OFFSET, 0, OFFSET);
    private static final Vector3d OFFSET_3D = new Vector3d(OFFSET, 0, OFFSET);

    private static final int CHUNK_OFFSET = OFFSET / 16;
    private static final Vector3i CHUNK_OFFSET_3I = new Vector3i(CHUNK_OFFSET, 0, CHUNK_OFFSET);

    private final FeatureFlagBool enabled = new FeatureFlagBool("coordinateOffset", true);
    private final Set<User> spoofedUsers = new NonBlockingHashSet<>();

    public CoordinateOffset() {
        Tools plugin = KoinJavaComponent.get(Tools.class);
        PluginExtensionsKt.registerPacketEvents(plugin, this);
    }

    @Override
    public void onUserLogin(@NotNull UserLoginEvent event) {
        if (enabled.getState()) {
            spoofedUsers.add(event.getUser());
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        spoofedUsers.remove(event.getUser());
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Client type)) return;
        if (!spoofedUsers.contains(event.getUser())) return;

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            var wrapper = new WrapperPlayClientInteractEntity(event);
            wrapper.getTarget().ifPresent(target -> {
                wrapper.setTarget(Optional.of(target.subtract(OFFSET_3F)));
                event.markForReEncode(true);
            });
        }

        else if (type == PacketType.Play.Client.PLAYER_POSITION) {
            var wrapper = new WrapperPlayClientPlayerPosition(event);
            wrapper.setPosition(wrapper.getPosition().subtract(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            var wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            wrapper.setPosition(wrapper.getPosition().subtract(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            var wrapper = new WrapperPlayClientPlayerDigging(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().subtract(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            var wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().subtract(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Client.UPDATE_SIGN) {
            var wrapper = new WrapperPlayClientUpdateSign(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().subtract(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Client.TAB_COMPLETE) {
            var wrapper = new WrapperPlayClientTabComplete(event);
            wrapper.getBlockPosition().ifPresent(pos -> {
                wrapper.setBlockPosition(pos.subtract(OFFSET_3I));
                event.markForReEncode(true);
            });
        }
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) return;
        if (!spoofedUsers.contains(event.getUser())) return;

        if (type == PacketType.Play.Server.SPAWN_POSITION) {
            var wrapper = new WrapperPlayServerSpawnPosition(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            var wrapper = new WrapperPlayServerPlayerPositionAndLook(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.USE_BED) {
            var wrapper = new WrapperPlayServerUseBed(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            var wrapper = new WrapperPlayServerSpawnPlayer(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            var wrapper = new WrapperPlayServerSpawnEntity(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            var wrapper = new WrapperPlayServerSpawnLivingEntity(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_PAINTING) {
            var wrapper = new WrapperPlayServerSpawnPainting(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_EXPERIENCE_ORB) {
            var wrapper = new WrapperPlayServerSpawnExperienceOrb(event);
            wrapper.setX(wrapper.getX() + OFFSET);
            wrapper.setZ(wrapper.getZ() + OFFSET);
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            var wrapper = new WrapperPlayServerEntityTeleport(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.CHUNK_DATA) {
            var wrapper = new WrapperPlayServerChunkData(event);
            Column column = wrapper.getColumn();

            TileEntity[] tileEntities = column.getTileEntities();
            for (TileEntity entity : column.getTileEntities()) {
                NBTCompound nbt = entity.getNBT();
                Number xTag = nbt.getNumberTagValueOrNull("x");
                Number zTag = nbt.getNumberTagValueOrNull("z");

                if (xTag != null && zTag != null) {
                    nbt.setTag("x", new NBTInt(xTag.intValue() + OFFSET));
                    nbt.setTag("z", new NBTInt(zTag.intValue() + OFFSET));
                }
            }

            int chunkX = column.getX() + CHUNK_OFFSET;
            int chunkZ = column.getZ() + CHUNK_OFFSET;

            column = wrapper.getColumn().hasBiomeData()
                    ? new Column(chunkX, chunkZ, column.isFullChunk(), column.getChunks(), tileEntities, column.getBiomeDataBytes())
                    : new Column(chunkX, chunkZ, column.isFullChunk(), column.getChunks(), tileEntities);

            wrapper.setColumn(column);
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            var wrapper = new WrapperPlayServerMultiBlockChange(event);
            wrapper.setChunkPosition(wrapper.getChunkPosition().add(CHUNK_OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.BLOCK_CHANGE) {
            var wrapper = new WrapperPlayServerBlockChange(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.BLOCK_ACTION) {
            var wrapper = new WrapperPlayServerBlockAction(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.BLOCK_BREAK_ANIMATION) {
            var wrapper = new WrapperPlayServerBlockBreakAnimation(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.MAP_CHUNK_BULK) {
            var wrapper = new WrapperPlayServerChunkDataBulk(event);
            int[] chunkX = wrapper.getX();
            int[] chunkZ = wrapper.getZ();

            for (int i = 0; i < chunkX.length; i++) {
                chunkX[i] += CHUNK_OFFSET;
                chunkZ[i] += CHUNK_OFFSET;
            }

            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.EXPLOSION) {
            var wrapper = new WrapperPlayServerExplosion(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.EFFECT) {
            var wrapper = new WrapperPlayServerEffect(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SOUND_EFFECT) {
            var wrapper = new WrapperPlayServerSoundEffect(event);
            wrapper.setEffectPosition(wrapper.getEffectPosition().add(OFFSET_3I.multiply(8)));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.PARTICLE) {
            var wrapper = new WrapperPlayServerParticle(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3D));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.SPAWN_WEATHER_ENTITY) {
            var wrapper = new WrapperPlayServerSpawnWeatherEntity(event);
            wrapper.setX(wrapper.getX() + OFFSET);
            wrapper.setZ(wrapper.getZ() + OFFSET);
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.UPDATE_SIGN) {
            var wrapper = new WrapperPlayServerUpdateSign(event);
            wrapper.setBlockPosition(wrapper.getBlockPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.BLOCK_ENTITY_DATA) {
            var wrapper = new WrapperPlayServerBlockEntityData(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));

            NBTCompound nbt = wrapper.getNBT();
            Number xTag = nbt.getNumberTagValueOrNull("x");
            Number zTag = nbt.getNumberTagValueOrNull("z");

            if (xTag != null && zTag != null) {
                nbt.setTag("x", new NBTInt(xTag.intValue() + OFFSET));
                nbt.setTag("z", new NBTInt(zTag.intValue() + OFFSET));
            }

            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.OPEN_SIGN_EDITOR) {
            var wrapper = new WrapperPlayServerOpenSignEditor(event);
            wrapper.setPosition(wrapper.getPosition().add(OFFSET_3I));
            event.markForReEncode(true);
        }

        else if (type == PacketType.Play.Server.WORLD_BORDER) {
            var wrapper = new WrapperPlayServerWorldBorder(event);
            if (wrapper.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.INITIALIZE
                    || wrapper.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.SET_CENTER) {
                wrapper.setCenterX(wrapper.getCenterX() + OFFSET);
                wrapper.setCenterZ(wrapper.getCenterZ() + OFFSET);
                event.markForReEncode(true);
            }
        }
    }
}