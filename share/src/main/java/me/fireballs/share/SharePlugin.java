package me.fireballs.share;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.stream.JsonReader;

import me.fireballs.brady.core.event.BradyShareEvent;
import me.fireballs.share.command.FootballDebugCommand;
import me.fireballs.share.listener.pgm.ActionNodeTriggerListener;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.FootballDebugChannel;
import me.fireballs.share.util.FootballStatistic;
import net.kyori.adventure.text.Component;
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
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;

public class SharePlugin extends JavaPlugin {
    private static final String URL = "https://pastes.dev/";

    private final ClientDataManager clientDataManager = new ClientDataManager();
    private final ShadowManager shadowManager = new ShadowManager(this);
    private ActionNodeTriggerListener actionNodeTriggerListener;
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
        this.actionNodeTriggerListener.addObserver((completedThrow) -> {
            if (!completedThrow.catcher().getParty().equals(completedThrow.thrower().getParty())) {
                FootballDebugChannel.sendMessage(Component.text("team mismatch ignore"));
                return;
            }
            final SpawnMatchModule spawnMatchModule =
                completedThrow.thrower().getMatch().getModule(SpawnMatchModule.class);
            if (spawnMatchModule == null) {
                FootballDebugChannel.sendMessage(Component.text("SpawnMatchModule was null for some reason"));
                return;
            }
            // horrendous code incoming!
            if (spawnMatchModule.getSpawns().size() < 2) {
                FootballDebugChannel.sendMessage(Component.text("Less than 2 spawns?"));
                return;
            }
            final Spawn spawn1 = spawnMatchModule.getSpawns().get(0);
            final Spawn spawn2 = spawnMatchModule.getSpawns().get(1);
            // really this shouldn't be needed if spawns aligned on the cross axis but who knows!
            final double epsilon = 1.1;
            final Location referencePoint1 = spawn1.getSpawn(completedThrow.thrower());
            final Location referencePoint2 = spawn2.getSpawn(completedThrow.thrower());
            final Location theDiff = referencePoint2.clone().subtract(referencePoint1);
            final boolean crossAxisIsZ = Math.abs(theDiff.getX()) > epsilon;
            final Function<Location, Location> postProcessLocation = (location) -> {
                Location newLocation = location.clone();
                newLocation.setY(0);
                if (crossAxisIsZ) {
                    newLocation.setZ(0);
                } else {
                    newLocation.setX(0);
                }
                return newLocation;
            };

            // calculate +/-
            final Optional<Spawn> validSpawn =
                spawnMatchModule.getSpawns().stream().filter((spawn) -> spawn.allows(completedThrow.thrower()))
                    .findFirst();
            if (validSpawn.isEmpty()) {
                FootballDebugChannel.sendMessage(Component.text("Spawn was empty for some reason"));
                return;
            }
            final Location spawnLocation = validSpawn.get().getSpawn(completedThrow.thrower());
            int magnitude = 1;
            // if the spot the catcher lost the ball at is closer to spawn, that's an overall negative yardage
            if (completedThrow.lossOfControlLocation().distanceSquared(spawnLocation)
                < completedThrow.throwLocation().distanceSquared(spawnLocation)) {
                magnitude = -1;
            }

            final DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(1);
            double distance =
                Math.abs(
                    postProcessLocation.apply(completedThrow.lossOfControlLocation()).distance(
                        postProcessLocation.apply(completedThrow.throwLocation())
                    )) * magnitude;
            FootballDebugChannel.sendMessage(
                Component.text(
                    "(" + df.format(distance) + " blocks) " +
                        completedThrow.thrower() + " to " +
                        completedThrow.catcher()
                )
            );
            statManager.mergeStat(
                completedThrow.thrower().getBukkit().getUniqueId(), FootballStatistic.TOTAL_PASSING_BLOCKS,
                (int) distance, Integer::sum
            );
            statManager.mergeStat(
                completedThrow.catcher().getBukkit().getUniqueId(), FootballStatistic.TOTAL_RECEIVING_BLOCKS,
                (int) distance, Integer::sum
            );
        });

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
