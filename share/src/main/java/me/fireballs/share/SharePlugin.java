package me.fireballs.share;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.stream.JsonReader;

import me.fireballs.brady.core.event.BradyShareEvent;
import me.fireballs.share.storage.Database;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;

public class SharePlugin extends JavaPlugin {
    private static final String URL = "https://pastes.dev/";

    private final ClientDataManager clientDataManager = new ClientDataManager();
    private final ShadowManager shadowManager = new ShadowManager(this);
    private Database database;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        StatManager statManager = new StatManager();
        new ChatListener(statManager);

        final String serverName = getConfig().getString("server-name", "unknown");
        final ConfigurationSection databaseConfig = getConfig().getConfigurationSection("database");
        if (databaseConfig != null) {
            final String path = databaseConfig.getString("path");
            this.database = new Database(getLogger());
            this.database.init(path);
        }

        Bukkit.getPluginManager().registerEvents(
            new MatchStatsListener(this, statManager, serverName, database),
            this
        );
        Bukkit.getPluginManager().registerEvents(new MatchCycleListener(statManager), this);

        if (getConfig().getBoolean("cps-tags")) {
            ClickListener clickListener = new ClickListener(clientDataManager);
            ShadowListener shadowListener = new ShadowListener(this, shadowManager);

            PacketEvents.getAPI().getEventManager().registerListener(clickListener, PacketListenerPriority.NORMAL);
            PacketEvents.getAPI().getEventManager().registerListener(shadowListener, PacketListenerPriority.MONITOR);

            Bukkit.getPluginManager().registerEvents(clickListener, this);
            Bukkit.getPluginManager().registerEvents(shadowListener, this);
            Bukkit.getPluginManager().registerEvents(new MatchJoinListener(this), this);

            Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    Bukkit.getOnlinePlayers().forEach(this::refreshCPS);
                } catch (NullPointerException ex) {
                    getLogger().log(Level.WARNING, ex.getMessage(), ex);
                }
            }, 0L, 1L);
        }
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            this.database.close();
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

                    Bukkit.getPluginManager().callEvent(new BradyShareEvent(URL + key));
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
