package me.fireballs.brady.core.tag;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;

import java.util.List;

public record TagPacketListener(TagTracker tracker) implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) return;

        User viewer = event.getUser();
        int viewerId = viewer.getEntityId();

        if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            tracker.startTracking(viewer, new WrapperPlayServerSpawnPlayer(event).getEntityId());

        } else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            tracker.stopTracking(viewerId, new WrapperPlayServerDestroyEntities(event).getEntityIds());

        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            var wrapper = new WrapperPlayServerEntityRelativeMove(event);
            tracker.getView(viewerId, wrapper.getEntityId()).ifPresent(v ->
                    v.shownStandIds().forEach(s ->
                            viewer.sendPacket(new WrapperPlayServerEntityRelativeMove(s, wrapper.getDeltaX(), wrapper.getDeltaY(), wrapper.getDeltaZ(), wrapper.isOnGround()))));

        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            var wrapper = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            tracker.getView(viewerId, wrapper.getEntityId()).ifPresent(v ->
                    v.shownStandIds().forEach(s ->
                            viewer.sendPacket(new WrapperPlayServerEntityRelativeMove(s, wrapper.getDeltaX(), wrapper.getDeltaY(), wrapper.getDeltaZ(), wrapper.isOnGround()))));

        } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            var wrapper = new WrapperPlayServerEntityTeleport(event);
            tracker.getView(viewerId, wrapper.getEntityId()).ifPresent(v -> {
                List<Integer> standIds = v.shownStandIds();
                for (int index = 0; index < standIds.size(); index++) {
                    Vector3d position = wrapper.getPosition().add(0, TagAPI.HEIGHT_OFFSET + index * TagAPI.LAYER_SPACING, 0);
                    viewer.sendPacket(new WrapperPlayServerEntityTeleport(standIds.get(index), position, 0, 0, false));
                }
            });

        } else if (type == PacketType.Play.Server.ENTITY_METADATA) {
            var wrapper = new WrapperPlayServerEntityMetadata(event);

            tracker.getView(viewerId, wrapper.getEntityId()).ifPresent(v -> {
                for (EntityData<?> meta : wrapper.getEntityMetadata()) {
                    if (meta.getIndex() != 0 && meta.getIndex() != 6) continue;

                    if (meta.getType() == EntityDataTypes.FLOAT) {
                        float health = (float) meta.getValue();
                        if (health <= 0) tracker.stopTracking(viewerId, new int[]{wrapper.getEntityId()});

                    } else if (meta.getType() == EntityDataTypes.BYTE) {
                        boolean sneaking = ((Byte) meta.getValue() & 0x02) != 0;
                        if (v.setSneaking(sneaking)) {
                            v.shownStandIds().forEach(s -> viewer.sendPacket(new WrapperPlayServerEntityMetadata(s, List.of(new EntityData<>(3, EntityDataTypes.BYTE, (byte) (sneaking ? 0 : 1))))));
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        tracker.cleanup(event.getUser().getEntityId());
    }

}
