package me.fireballs.share;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.stream.JsonReader;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import me.fireballs.share.listener.packet.ChatListener;
import me.fireballs.share.listener.pgm.MatchCycleListener;
import me.fireballs.share.listener.pgm.MatchJoinListener;
import me.fireballs.share.listener.pgm.MatchStatsListener;
import me.fireballs.share.listener.packet.ClickListener;
import me.fireballs.share.listener.packet.ShadowListener;
import me.fireballs.share.manager.ClientDataManager;
import me.fireballs.share.manager.ShadowManager;
import me.fireballs.share.manager.StatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;

public class SharePlugin extends JavaPlugin {
    private static final String URL = "https://pastes.dev/";

    private final ClientDataManager clientDataManager = new ClientDataManager();
    private final ShadowManager shadowManager = new ShadowManager(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        StatManager statManager = new StatManager();

        ChatListener chatListener = new ChatListener(statManager);
        PacketEvents.getAPI().getEventManager().registerListener(chatListener, PacketListenerPriority.MONITOR);

        Bukkit.getPluginManager().registerEvents(chatListener, this);
        Bukkit.getPluginManager().registerEvents(new MatchStatsListener(this, statManager), this);
        Bukkit.getPluginManager().registerEvents(new MatchCycleListener(statManager), this);

        if (getConfig().getBoolean("cps-tags")) {
            ClickListener clickListener = new ClickListener(clientDataManager);
            ShadowListener shadowListener = new ShadowListener(this, shadowManager);

            PacketEvents.getAPI().getEventManager().registerListener(clickListener, PacketListenerPriority.MONITOR);
            PacketEvents.getAPI().getEventManager().registerListener(shadowListener, PacketListenerPriority.MONITOR);

            Bukkit.getPluginManager().registerEvents(clickListener, this);
            Bukkit.getPluginManager().registerEvents(shadowListener, this);
            Bukkit.getPluginManager().registerEvents(new MatchJoinListener(this), this);

            Bukkit.getScheduler().runTaskTimer(this, () ->
                    Bukkit.getOnlinePlayers()
                            .forEach(this::refreshCPS), 0L, 1L);
        }
    }

    public void refreshCPS(Player player) {
        clientDataManager.getData(player.getUniqueId()).ifPresent(clientData ->
                shadowManager.getData(player.getEntityId()).ifPresent(shadowData -> {
                    int cps = clientData.getCPS();
                    if (cps != clientData.getLastDispatch()) {
                        clientData.setLastDispatch(cps);
                        shadowManager.updateCPS(shadowData, cps, player);
                    }
                }));
    }

    public void sendStats(String response) {
        try (JsonReader reader = new JsonReader(new StringReader(response))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("key")) {
                    String key = reader.nextString();

                    Bukkit.broadcast(
                            new ComponentBuilder("\n» ")
                                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, URL + key))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to view the match stats!")
                                            .color(ChatColor.AQUA)
                                            .create()))
                                    .append("Match Stats: ").color(ChatColor.GOLD).bold(true)
                                    .append(URL + key).color(ChatColor.BLUE).bold(false)
                                    .create()
                    );
                    break;
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, ex.getMessage(), ex);
        }
    }
}
