package me.fireballs.brady.core.tag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TagAPI {

    public static final double HEIGHT_OFFSET = 2.065;
    public static final double LAYER_SPACING = 0.3;

    private final List<TagLayer> layers = new ArrayList<>();

    public TagAPI(Plugin plugin) {
        TagTracker tracker = new TagTracker(layers);
        PacketEvents.getAPI().getEventManager().registerListener(new TagPacketListener(tracker), PacketListenerPriority.NORMAL);
        Bukkit.getScheduler().runTaskTimer(plugin, tracker::tick, 0L, 1L);
    }

    public void register(TagLayer layer) {
        layers.add(layer);
        layers.sort(Comparator.comparingInt(TagLayer::priority));
    }
}
