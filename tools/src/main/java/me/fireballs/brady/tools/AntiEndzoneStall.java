package me.fireballs.brady.tools;

import kotlin.Lazy;
import me.fireballs.brady.core.ComponentKt;
import me.fireballs.brady.corepgm.FeatureFlagBool;
import me.fireballs.brady.corepgm.PGMExtensionsKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchAfterLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.consumable.ConsumableDefinition;
import tc.oc.pgm.consumable.ConsumableMatchModule;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.flag.FlagMatchModule;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.spawns.Spawn;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class AntiEndzoneStall implements Listener {

    private final FeatureFlagBool enabled = new FeatureFlagBool("antiEndzoneStall", false);

    private Match match;
    private CuboidRegion teamOnePortal;
    private CuboidRegion teamTwoPortal;
    private ConsumableDefinition snowballConsumable;

    public AntiEndzoneStall() {
        Lazy<Tools> plugin = KoinJavaComponent.inject(Tools.class);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin.getValue());
    }

    @EventHandler
    public void onMove(PlayerCoarseMoveEvent event) {
        if (!enabled.getState()) return;
        if (teamOnePortal == null) return;
        if (teamTwoPortal == null) return;

        MatchPlayer player = match.getPlayer(event.getPlayer());
        if (player == null) return;

        FlagMatchModule flags = match.getModule(FlagMatchModule.class);
        if (flags == null) return;
        if (!PGMExtensionsKt.isFlagCarrier(player)) return;

        CuboidRegion ownEndzone = getOwnEndzone(player);
        if (ownEndzone != null && ownEndzone.contains(event.getTo())) {
            forceDropFlag(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCycleFinish(MatchAfterLoadEvent event) {
        this.match = event.getMatch();
        this.teamOnePortal = null;
        this.teamTwoPortal = null;
        this.snowballConsumable = null;

        FilterMatchModule filters = match.getModule(FilterMatchModule.class);
        if (filters == null) return;
        if (!(filters.getFilterContext() instanceof FeatureDefinitionContext fdc)) return;

        if (fdc.get("team-one-portal") instanceof CuboidRegion teamOneRegion) {
            this.teamOnePortal = teamOneRegion;
        }

        if (fdc.get("team-two-portal") instanceof CuboidRegion teamTwoRegion) {
            this.teamTwoPortal = teamTwoRegion;
        }

        if (fdc.get("use-snowball") instanceof ConsumableDefinition consumable) {
            this.snowballConsumable = consumable;
        }
    }

    private CuboidRegion getOwnEndzone(MatchPlayer player) {
        SpawnMatchModule spawns = match.getModule(SpawnMatchModule.class);
        if (spawns == null) return null;

        Spawn spawn = spawns.chooseSpawn(player);
        if (spawn == null) return null;

        Location spawnLoc = spawn.getSpawn(player);
        if (spawnLoc == null) return null;

        double teamOnePortalDistance = PGMExtensionsKt.center(teamOnePortal).distanceSquared(spawnLoc.toVector());
        double teamTwoPortalDistance = PGMExtensionsKt.center(teamTwoPortal).distanceSquared(spawnLoc.toVector());

        return teamOnePortalDistance > teamTwoPortalDistance ? teamTwoPortal : teamOnePortal;
    }

    private void forceDropFlag(MatchPlayer player) {
        ConsumableMatchModule consumables = match.getModule(ConsumableMatchModule.class);
        if (consumables == null) return;

        if (snowballConsumable == null) return;

        Inventory inventory = player.getBukkit().getInventory();
        int snowballSlot = inventory.first(Material.SNOW_BALL);
        if (snowballSlot == -1) return;

        ItemStack snowball = inventory.getItem(snowballSlot);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player.getBukkit(), Action.RIGHT_CLICK_AIR, snowball, null, null);
        consumables.runConsumable(interactEvent, snowballConsumable);

        player.sendMessage(ComponentKt.cc("&cThe flag was dropped because you entered your own endzone!", true));
        inventory.remove(snowball);
    }
}
