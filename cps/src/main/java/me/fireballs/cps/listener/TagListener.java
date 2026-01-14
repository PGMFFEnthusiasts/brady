package me.fireballs.cps.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import me.fireballs.cps.profile.Profile;
import me.fireballs.cps.profile.ProfileManager;

import java.util.*;

public record TagListener(ProfileManager profileManager) implements PacketListener {

    private static final double HEIGHT_OFFSET = 2.065;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) return;

        User user = event.getUser();

        if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            var wrapper = new WrapperPlayServerSpawnPlayer(event);

            Profile profile = profileManager.getProfile(wrapper.getEntityId());
            if (profile == null) return;

            Vector3d standPosition = new Vector3d(wrapper.getPosition().x, wrapper.getPosition().y + HEIGHT_OFFSET, wrapper.getPosition().z);

            List<EntityData<?>> entityDataList = List.of(
                    new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20), // invisible
                    new EntityData<>(2, EntityDataTypes.STRING, profile.getTag()), // nametag
                    new EntityData<>(3, EntityDataTypes.BYTE, (byte) 1), // always show nametag
                    new EntityData<>(10, EntityDataTypes.BYTE, (byte) (0x08 | 0x10)) // no baseplate & marker
            );

            user.sendPacket(new WrapperPlayServerSpawnEntity(profile.getTagId(), null, EntityTypes.ARMOR_STAND, standPosition, 0, 0, 0f, 0, Optional.empty()));
            user.sendPacket(new WrapperPlayServerEntityMetadata(profile.getTagId(), entityDataList));

            profile.addViewer(user);

        } else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            var wrapper = new WrapperPlayServerDestroyEntities(event);

            for (int entityId : wrapper.getEntityIds()) {
                Profile profile = profileManager.getProfile(entityId);
                if (profile == null) continue;

                user.sendPacket(new WrapperPlayServerDestroyEntities(profile.getTagId()));
                profile.removeViewer(user);
            }

        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            var wrapper = new WrapperPlayServerEntityRelativeMove(event);

            Profile profile = profileManager.getProfile(wrapper.getEntityId());
            if (profile == null) return;

            var wrapperCopy = new WrapperPlayServerEntityRelativeMove(0, 0, 0, 0, true);
            wrapperCopy.copy(wrapper);
            wrapperCopy.setEntityId(profile.getTagId());
            user.sendPacket(wrapperCopy);

        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            var wrapper = new WrapperPlayServerEntityRelativeMoveAndRotation(event);

            Profile profile = profileManager.getProfile(wrapper.getEntityId());
            if (profile == null) return;

            var wrapperCopy = new WrapperPlayServerEntityRelativeMoveAndRotation(0, 0, 0, 0, 0, 0, true);
            wrapperCopy.copy(wrapper);
            wrapperCopy.setEntityId(profile.getTagId());
            user.sendPacket(wrapperCopy);

        } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            var wrapper = new WrapperPlayServerEntityTeleport(event);

            Profile profile = profileManager.getProfile(wrapper.getEntityId());
            if (profile == null) return;

            var wrapperCopy = new WrapperPlayServerEntityTeleport(profile.getTagId(), wrapper.getPosition().add(0, HEIGHT_OFFSET, 0), 0, 0, false);
            user.sendPacket(wrapperCopy);

        } else if (type == PacketType.Play.Server.ENTITY_METADATA) {
            var wrapper = new WrapperPlayServerEntityMetadata(event);

            Profile profile = profileManager.getProfile(wrapper.getEntityId());
            if (profile == null) return;

            for (EntityData<?> meta : wrapper.getEntityMetadata()) {
                if (meta.getIndex() != 0 && meta.getIndex() != 6) continue;

                if (meta.getType() == EntityDataTypes.FLOAT) {
                    float health = (float) meta.getValue();
                    if (health <= 0) {
                        user.sendPacket(new WrapperPlayServerDestroyEntities(profile.getTagId()));
                        profile.removeViewer(user);
                    }
                } else if (meta.getType() == EntityDataTypes.BYTE) {
                    Byte item = (Byte) meta.getValue();
                    boolean crouching = (item & 0x02) != 0;
                    byte showNametag = (byte) (crouching ? 0 : 1);

                    user.sendPacket(new WrapperPlayServerEntityMetadata(profile.getTagId(), List.of(new EntityData<>(3, EntityDataTypes.BYTE, showNametag))));
                }
            }
        }
    }
}
