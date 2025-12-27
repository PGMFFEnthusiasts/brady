package me.fireballs.share;

import me.fireballs.brady.core.event.BradyShareEvent;
import me.fireballs.share.command.FootballDebugCommand;
import me.fireballs.share.command.PersistentEffectsCommand;
import me.fireballs.share.effects.EffectApplicationListener;
import me.fireballs.share.football.FootballListenerImpl;
import me.fireballs.share.listener.pgm.ActionNodeTriggerListener;
import me.fireballs.share.listener.pgm.MatchCycleListener;
import me.fireballs.share.listener.pgm.MatchStatsListener;
import me.fireballs.share.manager.StatManager;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.FootballDebugChannel;
import me.fireballs.share.util.StatsLink;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static me.fireballs.brady.core.DebuggingKt.log;

public class SharePlugin extends JavaPlugin {
    private ActionNodeTriggerListener actionNodeTriggerListener;
    private Database database;
    private FootballListenerImpl footballListener;
    private EffectApplicationListener effectApplicationListener;
    public boolean uploadPaste;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        StatManager statManager = new StatManager();

        final String serverName = System.getenv().getOrDefault("BRADY_SERVER", "unknown");
        final ConfigurationSection databaseConfig = getConfig().getConfigurationSection("database");
        if (databaseConfig != null) {
            final String path = databaseConfig.getString("path");
            final String username = databaseConfig.getString("username");
            final String password = databaseConfig.getString("password");
            this.database = new Database(getLogger());
            this.database.init(username, password, path);
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

        FootballDebugChannel.init(this);
        this.getCommand("tbdebug").setExecutor(new FootballDebugCommand());
        final var trackedEffects = new HashMap<PotionEffectType, Integer>();
        this.effectApplicationListener = new EffectApplicationListener(
            this, "remove-cages", Collections.unmodifiableMap(trackedEffects)
        );
        Bukkit.getPluginManager().registerEvents(this.effectApplicationListener, this);
        final PersistentEffectsCommand persistentEffectsCommand = new PersistentEffectsCommand(
            trackedEffects,
            List.of(effectApplicationListener)
        );
        this.getCommand("pe").setExecutor(persistentEffectsCommand);
        if (getConfig().getBoolean("upload-paste")) {
            this.uploadPaste = true;
        }
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            this.database.close();
        }
        FootballDebugChannel.unload();
        HandlerList.unregisterAll(this.actionNodeTriggerListener);
        HandlerList.unregisterAll(this.effectApplicationListener);
        if (this.footballListener != null) {
            HandlerList.unregisterAll(this.footballListener);
        }
    }

    public void sendStats(StatsLink statsLink) {
        log("match-stats", "sending stats for " + statsLink.toString());
        Bukkit.getPluginManager().callEvent(new BradyShareEvent(statsLink.source(), statsLink.url()));
    }

    public void broadcastStats(StatsLink statsLink) {
        Bukkit.broadcast(
                new ComponentBuilder("\n» ")
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, statsLink.url()))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click here to view the match stats!")
                                .color(ChatColor.AQUA)
                                .create()))
                        .append(statsLink.source() + ": ").color(ChatColor.GOLD).bold(true)
                        .append(statsLink.url()).color(ChatColor.BLUE).bold(false)
                        .create()
        );
    }
}
