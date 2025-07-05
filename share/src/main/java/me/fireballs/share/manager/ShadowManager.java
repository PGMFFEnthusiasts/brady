package me.fireballs.share.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.google.common.collect.MapMaker;
import me.fireballs.share.SharePlugin;
import me.fireballs.share.data.ShadowData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShadowManager {
    private final SharePlugin plugin;
    private final Map<Integer, ShadowData> dataMap = new MapMaker().makeMap();

    public ShadowManager(SharePlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<ShadowData> getData(int id) {
        return Optional.ofNullable(dataMap.get(id));
    }

    public void add(int id) {
        Bukkit.getScheduler().runTask(plugin, () ->
                dataMap.put(id, new ShadowData()));
    }

    public void remove(int id) {
        Bukkit.getScheduler().runTask(plugin, () ->
                dataMap.remove(id));
    }

    public void updateCPS(ShadowData data, int cps, Player player) {
        data.getViewers().forEach(viewer -> {
            ChatColor color = ChatColor.AQUA;

            MatchManager matchManager = PGM.get().getMatchManager();
            if (matchManager != null) {
                MatchPlayer matchPlayer = matchManager.getPlayer(player);
                if (matchPlayer != null && matchPlayer.getCompetitor() instanceof Team team) {
                    color = team.getInfo().getDefaultColor();
                }

                String tag = color.toString() + cps + "§7 CPS";
                var packet = new WrapperPlayServerEntityMetadata(data.getId(), List.of(new EntityData<>(2, EntityDataTypes.STRING, tag)));
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        });
    }
}
