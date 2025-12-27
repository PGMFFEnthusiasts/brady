package me.fireballs.share.effects;

import org.apache.commons.collections4.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ActionNodeTriggerEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EffectApplicationListener implements Listener, PersistentEffectRegistrationSubscriber {
    private final JavaPlugin plugin;
    private final String removeCagesActionId;
    private final Map<PotionEffectType, Integer> activeEffects;

    public EffectApplicationListener(
        JavaPlugin plugin, String removeCagesActionId, Map<PotionEffectType, Integer> activeEffects
    ) {
        this.plugin = plugin;
        this.removeCagesActionId = removeCagesActionId;
        this.activeEffects = activeEffects;
    }

    @EventHandler
    public void onActionNodeTrigger(final ActionNodeTriggerEvent event) {
        if (event.nodeId.equalsIgnoreCase(removeCagesActionId)) {
            getEveryone(true).forEach(this::applyEffects);
        }
    }

    @EventHandler
    public void onSpawn(final ParticipantSpawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyEffects(event.getPlayer().getBukkit()), 1L);
    }

    private void applyEffects(final Player player) {
        for (final Map.Entry<PotionEffectType, Integer> effectEntry : activeEffects.entrySet()) {
            player.addPotionEffect(new PotionEffect(
                effectEntry.getKey(), 1000000, effectEntry.getValue(), true, false
            ), true);
        }
    }

    private void removeInactiveEffects(final Player player) {
        final var effectsThatNeedToGoByeBye = CollectionUtils.disjunction(
            player.getActivePotionEffects().stream().map(PotionEffect::getType).collect(Collectors.toSet()),
            activeEffects.keySet()
        );
        effectsThatNeedToGoByeBye.forEach(player::removePotionEffect);
    }

    private List<Player> getEveryone(final boolean adding) {
        if (adding) {
            Iterator<Match> it = PGM.get().getMatchManager().getMatches();
            if (it.hasNext()) {
                final Match match = it.next();
                return match.getParticipants().stream()
                    .filter(mp -> !mp.isObserving())
                    .map(MatchPlayer::getBukkit)
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } else {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }
    }

    @Override
    public void onEffectAdd(PotionEffectApplication potionEffectApplication) {
        getEveryone(true).forEach(this::applyEffects);
    }

    @Override
    public void onEffectRemove(PotionEffectType potionEffectType) {
        getEveryone(false).forEach(this::removeInactiveEffects);
    }

    @EventHandler
    public void onMatchEnd(final MatchFinishEvent event) {
        getEveryone(false).forEach(this::removeInactiveEffects);
    }
}
