package me.fireballs.share;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.stream.JsonReader;
import me.fireballs.brady.core.event.BradyShareEvent;
import me.fireballs.share.command.FootballDebugCommand;
import me.fireballs.share.football.FootballListenerImpl;
import me.fireballs.share.listener.packet.ChatListener;
import me.fireballs.share.listener.packet.ClickListener;
import me.fireballs.share.listener.packet.ShadowListener;
import me.fireballs.share.listener.pgm.ActionNodeTriggerListener;
import me.fireballs.share.listener.pgm.MatchCycleListener;
import me.fireballs.share.listener.pgm.MatchJoinListener;
import me.fireballs.share.listener.pgm.MatchStatsListener;
import me.fireballs.share.manager.ClientDataManager;
import me.fireballs.share.manager.ShadowManager;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.FootballDebugChannel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;

public class SharePlugin extends JavaPlugin {
    private static final String PASTES_DEV_URL = "https://pastes.dev/";

    private final ClientDataManager clientDataManager = new ClientDataManager();
    private final ShadowManager shadowManager = new ShadowManager(this);
    private ActionNodeTriggerListener actionNodeTriggerListener;
    private Database database;
    private FootballListenerImpl footballListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        StatManager statManager = new StatManager();
        new ChatListener(statManager);

        final String serverName = System.getenv().getOrDefault("BRADY_SERVER", "unknown");
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
        // hardcoding action IDs for now hopefully they don't change! why would they!
        this.actionNodeTriggerListener = new ActionNodeTriggerListener(
            "flag-pickup-event",
            "flag-receive-event",
            "snowball-thrown",
            "flag-steal-event",
            "carrier-died-event",
            "increment-round",
            "reset-flag"
        );
        Bukkit.getPluginManager().registerEvents(this.actionNodeTriggerListener, this);
        this.footballListener = new FootballListenerImpl(this, statManager, database);
        Bukkit.getPluginManager().registerEvents(this.footballListener, this);
        this.actionNodeTriggerListener.addObserver(this.footballListener);

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
        FootballDebugChannel.init(this);
        this.getCommand("tbdebug").setExecutor(new FootballDebugCommand());
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            this.database.close();
        }
        FootballDebugChannel.unload();
        HandlerList.unregisterAll(this.actionNodeTriggerListener);
        if (this.footballListener != null) {
            HandlerList.unregisterAll(this.footballListener);
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

    public void sendStatsPaste(String response) {
        try (JsonReader reader = new JsonReader(new StringReader(response))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("key")) {
                    String key = reader.nextString();
                    sendStats("Match Stats", PASTES_DEV_URL + key);
                    break;
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    public void sendStats(String prefix, String link) {
        Bukkit.getPluginManager().callEvent(new BradyShareEvent(prefix, link));
        Bukkit.broadcast(
            new ComponentBuilder("\n» ")
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to view the match stats!")
                    .color(ChatColor.AQUA)
                    .create()))
                .append(prefix + ": ").color(ChatColor.GOLD).bold(true)
                .append(link).color(ChatColor.BLUE).bold(false)
                .create()
        );
    }
}
