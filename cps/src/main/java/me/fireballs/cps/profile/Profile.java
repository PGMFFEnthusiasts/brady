package me.fireballs.cps.profile;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jctools.maps.NonBlockingHashSet;

import java.util.List;
import java.util.Set;

public class Profile {

    private final int[] clickBuffer = new int[20];
    private int index;
    private int cps;
    private int lastSentCps;

    private final int tagId = Bukkit.allocateEntityId();
    private final Set<User> viewers = new NonBlockingHashSet<>();

    private volatile ChatColor teamColor = ChatColor.AQUA;
    private ChatColor lastSentColor;

    public void click() {
        clickBuffer[index]++;
        cps++;
    }

    public void tick() {
        index = (index + 1) % 20;
        cps -= clickBuffer[index];
        clickBuffer[index] = 0;

        updateViewers();
    }

    public void setTeamColor(ChatColor teamColor) {
        this.teamColor = teamColor;
    }

    public void updateViewers() {
        if (cps == lastSentCps && teamColor == lastSentColor) return;

        lastSentCps = cps;
        lastSentColor = teamColor;

        var wrapper = new WrapperPlayServerEntityMetadata(tagId, List.of(new EntityData<>(2, EntityDataTypes.STRING, getTag())));

        for (User user : viewers) {
            user.sendPacket(wrapper);
        }
    }

    public String getTag() {
        return teamColor.toString() + lastSentCps + ChatColor.GRAY + " CPS";
    }

    public int getTagId() {
        return tagId;
    }

    public void addViewer(User user) {
        viewers.add(user);
    }

    public void removeViewer(User user) {
        viewers.remove(user);
    }

    public void destroy() {
        if (viewers.isEmpty()) return;

        var wrapper = new WrapperPlayServerDestroyEntities(tagId);
        for (User user : viewers) {
            user.sendPacket(wrapper);
        }
        viewers.clear();
    }
}
