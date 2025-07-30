package me.fireballs.cps;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.fireballs.cps.listener.packet.ClickListener;
import me.fireballs.cps.listener.packet.ShadowListener;
import me.fireballs.cps.listener.pgm.MatchJoinListener;
import me.fireballs.cps.manager.ClientDataManager;
import me.fireballs.cps.manager.ShadowManager;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;

import java.util.List;
import java.util.logging.Level;

public class CPSPlugin extends JavaPlugin {
    private final ClientDataManager clientDataManager = new ClientDataManager();
    private final ShadowManager shadowManager = new ShadowManager();

    private long lastError = System.currentTimeMillis();

    @Override
    public void onEnable() {
        ClickListener clickListener = new ClickListener(this, clientDataManager);
        ShadowListener shadowListener = new ShadowListener(this, shadowManager);

        PacketEvents.getAPI().getEventManager().registerListener(clickListener, PacketListenerPriority.NORMAL);
        PacketEvents.getAPI().getEventManager().registerListener(shadowListener, PacketListenerPriority.MONITOR);

        Bukkit.getPluginManager().registerEvents(shadowListener, this);
        Bukkit.getPluginManager().registerEvents(new MatchJoinListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                Bukkit.getOnlinePlayers().forEach(this::refreshShadows);
            } catch (NullPointerException ex) {
                if (lastError + 1000L < System.currentTimeMillis()) {
                    getLogger().log(Level.SEVERE, ExceptionUtils.getStackTrace(ex), ex);
                    lastError = System.currentTimeMillis();
                }
            }
        }, 0L, 1L);
    }

    public void refreshShadows(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        //noinspection ConstantValue
        if (user == null) return;

        clientDataManager.getData(user).ifPresent(clientData ->
                shadowManager.getData(player.getEntityId()).ifPresent(shadowData -> {
                    int cps = clientData.getCPS();
                    if (cps != clientData.getLastSent()) {
                        clientData.setLastSent(cps);

                        ChatColor color = ChatColor.AQUA;
                        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
                        if (matchPlayer != null && matchPlayer.getCompetitor() instanceof Team team) {
                            color = team.getInfo().getDefaultColor();
                        }

                        String tag = color.toString() + cps + "§7 CPS";
                        var packet = new WrapperPlayServerEntityMetadata(shadowData.getId(), List.of(new EntityData<>(2, EntityDataTypes.STRING, tag)));

                        shadowData.getViewers().forEach(viewer ->
                                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet));
                    }
                })
        );
    }
}
