package me.fireballs.cps.data;

import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.NMSHacks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShadowData {
    private final int id = NMSHacks.NMS_HACKS.allocateEntityId();
    private final List<Player> viewers = new CopyOnWriteArrayList<>();

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
