package me.fireballs.share.football;

import me.fireballs.share.manager.StatManager;
import me.fireballs.share.storage.Database;
import me.fireballs.share.util.FootballDebugChannel;
import me.fireballs.share.util.FootballStatistic;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.tracker.TrackerMatchModule;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Function;

public class FootballListenerImpl implements FootballListener, Listener {
    private final JavaPlugin plugin;
    private final StatManager statManager;
    private final Database database;
    private Match match;
    private Player carrier;

    public FootballListenerImpl(JavaPlugin plugin, StatManager statManager, Database database) {
        this.plugin = plugin;
        this.statManager = statManager;
        this.database = database;
    }

    @Override
    public void onPassPossessionCompletion(CompletedFootballThrow completedThrow) {
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
        if (distance < 0.0) {
            distance = 0.0;
        }
        statManager.mergeStat(
            completedThrow.thrower().getBukkit().getUniqueId(), FootballStatistic.TOTAL_PASSING_BLOCKS,
            distance, Double::sum
        );
        statManager.mergeStat(
            completedThrow.catcher().getBukkit().getUniqueId(), FootballStatistic.TOTAL_RECEIVING_BLOCKS,
            distance, Double::sum
        );
    }

    @Override
    public void onPass(MatchPlayer thrower, MatchPlayer catcher) {
        if (thrower.getParty().equals(catcher.getParty())) return;
        statManager.incrementStat(thrower.getBukkit().getUniqueId(), FootballStatistic.PASS_INTERCEPTIONS);
        statManager.incrementStat(catcher.getBukkit().getUniqueId(), FootballStatistic.DEFENSE_INTERCEPTIONS);
    }

    @Override
    public void onCarrierChange(MatchPlayer newCarrier) {
        carrier = newCarrier.getBukkit();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchLoad(MatchLoadEvent event) {
        match = event.getMatch();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (match == null) return;
        ParticipantState damager =
            match.needModule(TrackerMatchModule.class).getOwner(event.getDamager());
        ParticipantState damaged = match.getParticipantState(event.getEntity());

        // Prevent tracking damage to entities or self
        if (damaged == null || (damager != null && damaged.getId() == damager.getId())) return;
        double absHearts = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
        double realFinalDamage =
            Math.min(event.getFinalDamage(), ((Player) event.getEntity()).getHealth()) + absHearts;
        if (realFinalDamage <= 0) return;
        if (event.getEntity().equals(carrier)) {
            final MatchPlayer participant = match.getParticipant(event.getDamager());
            if (participant != null) {
                statManager.incrementStat(
                    participant.getBukkit().getUniqueId(), FootballStatistic.DMG_CARRIER, realFinalDamage
                );
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (database == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.updatePlayerIdentity(event.getPlayer()));
    }
}
