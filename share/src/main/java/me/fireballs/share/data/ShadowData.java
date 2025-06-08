package me.fireballs.share.data;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.NMSHacks;

import java.util.ArrayList;
import java.util.List;

public class ShadowData {
    private final int id = NMSHacks.NMS_HACKS.allocateEntityId();
    private final List<Player> viewers = new ArrayList<>();

    public int getId() {
        return id;
    }

    public List<Player> getViewers() {
        return viewers;
    }

    public void addViewer(Player player) {
        viewers.add(player);
    }

    public void removeViewer(Player player) {
        viewers.remove(player);
    }
}
