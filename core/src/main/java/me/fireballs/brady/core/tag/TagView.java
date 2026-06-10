package me.fireballs.brady.core.tag;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class TagView {

    private final User user;

    private final Map<String, Integer> standIds = new HashMap<>();
    private final Map<String, String> lastText = new HashMap<>();

    private volatile List<Integer> shownStandIds = List.of();
    private volatile boolean sneaking;

    public TagView(User user) {
        this.user = user;
    }

    public void update(Player target, Player viewer, List<TagLayer> layers, Map<String, String> texts) {
        List<TagLayer> visible = new ArrayList<>(layers.size());
        List<Integer> ids = new ArrayList<>(layers.size());
        for (TagLayer layer : layers) {
            if (layer.visibleTo().test(target, viewer)) {
                visible.add(layer);
                ids.add(standId(layer));
            }
        }

        boolean rebuild = !ids.equals(shownStandIds);

        if (rebuild) {
            destroy();
            shownStandIds = List.copyOf(ids);
        }

        Location location = target.getLocation();

        for (int index = 0; index < visible.size(); index++) {
            TagLayer layer = visible.get(index);
            String text = texts.get(layer.id());

            if (rebuild) {
                Vector3d position = new Vector3d(location.getX(), location.getY() + TagAPI.HEIGHT_OFFSET + index * TagAPI.LAYER_SPACING, location.getZ());
                user.sendPacket(new WrapperPlayServerSpawnLivingEntity(ids.get(index), UUID.randomUUID(), EntityTypes.ARMOR_STAND, position, 0, 0, 0, new Vector3d(), List.of(
                        new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20), // invisible
                        new EntityData<>(10, EntityDataTypes.BYTE, (byte) (0x08 | 0x10)), // no baseplate & marker
                        new EntityData<>(2, EntityDataTypes.STRING, text),
                        new EntityData<>(3, EntityDataTypes.BYTE, (byte) (sneaking ? 0 : 1))
                )));
            } else if (!text.equals(lastText.get(layer.id()))) {
                user.sendPacket(new WrapperPlayServerEntityMetadata(ids.get(index), List.of(new EntityData<>(2, EntityDataTypes.STRING, text))));
            }

            lastText.put(layer.id(), text);
        }
    }

    public void destroy() {
        List<Integer> ids = shownStandIds;
        if (ids.isEmpty()) return;
        shownStandIds = List.of();
        user.sendPacket(new WrapperPlayServerDestroyEntities(ids.stream().mapToInt(Integer::intValue).toArray()));
    }

    public List<Integer> shownStandIds() {
        return shownStandIds;
    }

    public boolean setSneaking(boolean sneaking) {
        if (this.sneaking == sneaking) return false;
        this.sneaking = sneaking;
        return true;
    }

    private int standId(TagLayer layer) {
        return standIds.computeIfAbsent(layer.id(), _ -> Bukkit.allocateEntityId());
    }
}
