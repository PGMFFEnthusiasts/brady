package me.fireballs.brady.core.tag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TagAPI {

    public static final double LAYER_SPACING = quantize(9 * 1.15 * 0.02666667); // from mcp RenderPlayer & RendererLivingEntity
    public static final double HEIGHT_OFFSET = quantize(1.8 + LAYER_SPACING);

    private static double quantize(double value) {
        return Math.round(value * 32) / 32.0;
    }

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
