package me.fireballs.brady.tools;

import me.fireballs.brady.core.ComponentKt;
import me.fireballs.brady.core.PluginExtensionsKt;
import me.fireballs.brady.corepgm.FeatureFlagEnum;
import me.fireballs.brady.corepgm.PGMExtensionsKt;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.flag.event.FlagPickupEvent;
import tc.oc.pgm.platform.sportpaper.material.SpMaterialUtils;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.material.BlockMaterialData;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class OvertimeEffects implements Listener {

    private static final double HEALTH_BUMP = 1;
    private static final int ENDZONE_STRETCH = 2;

    private final Tools plugin;

    private final FeatureFlagEnum<Effect> effect = new FeatureFlagEnum<>("overtimeEffect", Effect.NONE, Effect.class);
    private final Map<Match, Integer> appliedMinutes = new WeakHashMap<>();

    private enum Effect {
        NONE,
        FLAG_CARRIER_HEALTH,
        STRETCH_ENDZONE,
    }

    public OvertimeEffects() {
        this.plugin = KoinJavaComponent.get(Tools.class);
        PluginExtensionsKt.registerEvents(plugin, this);
        Bukkit.getScheduler().runTaskTimer(plugin, this::pollOvertime, 0L, 1L);
    }

    private void pollOvertime() {
        Iterator<Match> it = PGM.get().getMatchManager().getMatches();
        while (it.hasNext()) {
            Match match = it.next();

            if (isOvertime(match)) {
                int currentMinute = getOvertimeMinutes(match);
                if (currentMinute < 0) continue;

                int appliedMinute = appliedMinutes.getOrDefault(match, -1);

                if (currentMinute > appliedMinute) {
                    switch (effect.getState()) {
                        case NONE -> {}
                        case FLAG_CARRIER_HEALTH -> {
                            DecimalFormat healthFormat = new DecimalFormat("0.#");
                            String hearts = healthFormat.format(HEALTH_BUMP) + "&7 heart" + (HEALTH_BUMP != 1 ? "s" : "");
                            match.sendMessage(ComponentKt.cc(" &e⚠ &b&lOVERTIME!&7 Flag carrier health extended by &a" + hearts + ".", true));
                            match.getPlayers().stream()
                                    .filter(PGMExtensionsKt::isFlagCarrier)
                                    .forEach(p -> applyHealthBump(p, 1));
                        }
                        case STRETCH_ENDZONE -> {
                            match.sendMessage(ComponentKt.cc(" &e⚠ &b&lOVERTIME!&7 Endzones extended by &a" + ENDZONE_STRETCH + "&7 blocks.", true));
                            extendEndzone(match);
                        }
                    }

                    appliedMinutes.put(match, currentMinute);
                }
            }
        }
    }

    private static int getOvertimeMinutes(Match match) {
        TimeLimitMatchModule tl = match.getModule(TimeLimitMatchModule.class);
        if (tl == null) return -1;
        if (tl.getTimeLimit() == null) return -1;

        long overtimeSeconds = match.getDuration().getSeconds() - tl.getTimeLimit().getDuration().getSeconds();
        if (overtimeSeconds < 0) return -1;

        return (int) (overtimeSeconds / 60);
    }

    @EventHandler
    private void onFlagPickup(FlagPickupEvent event) {
        MatchPlayer player = event.getCarrier();
        Match match = player.getMatch();

        if (isOvertime(match) && effect.getState() == Effect.FLAG_CARRIER_HEALTH) {
            int applied = appliedMinutes.getOrDefault(match, -1);

            if (applied >= 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> applyHealthBump(player, applied + 1), 2L);
            }
        }
    }

    private static boolean isOvertime(Match match) {
        FilterMatchModule filters = match.getModule(FilterMatchModule.class);
        if (filters == null) return false;
        if (!(filters.getFilterContext() instanceof FeatureDefinitionContext fdc)) return false;

        Object feature = fdc.get("in-overtime");
        if (feature instanceof Filter inOvertime) {
            return inOvertime.response(match);
        }

        return false;
    }

    private static void applyHealthBump(MatchPlayer matchPlayer, int increments) {
        Player player = matchPlayer.getBukkit();
        double healthIncrease = (HEALTH_BUMP * 2) * increments;

        double newMax = player.getMaxHealth() + healthIncrease;
        player.setMaxHealth(newMax);
        player.setHealth(Math.min(player.getHealth() + healthIncrease, newMax));
    }

    private static void extendEndzone(Match match) {
        FilterMatchModule filters = match.getModule(FilterMatchModule.class);
        if (filters == null) return;
        if (!(filters.getFilterContext() instanceof FeatureDefinitionContext fdc)) return;

        Object teamOneFeature = fdc.get("team-one-portal");
        Object teamTwoFeature = fdc.get("team-two-portal");

        if (!(teamOneFeature instanceof CuboidRegion teamOnePortal)) return;
        if (!(teamTwoFeature instanceof CuboidRegion teamTwoPortal)) return;

        Vector centerOne = PGMExtensionsKt.center(teamOnePortal);
        Vector centerTwo = PGMExtensionsKt.center(teamTwoPortal);

        stretchTowards(match, teamOnePortal, centerTwo);
        stretchTowards(match, teamTwoPortal, centerOne);
    }

    private static void stretchTowards(Match match, CuboidRegion region, Vector targetCenter) {
        Vector min = region.getBounds().getMin().clone();
        Vector max = region.getBounds().getMax().clone();
        Vector center = PGMExtensionsKt.center(region);
        World world = match.getWorld();

        BlockMaterialData sample = null;

        // sample the center column of blocks to use as the new endzone material
        for (int y = max.getBlockY(); y >= 0; y--) {
            Block block = world.getBlockAt(center.getBlockX(), y, center.getBlockZ());
            if (!block.getType().isSolid()) continue;

            sample = SpMaterialUtils.MATERIAL_UTILS.createBlockData(block.getState());
            break;
        }

        if (sample == null) return;

        double dx = targetCenter.getX() - center.getX();
        double dz = targetCenter.getZ() - center.getZ();

        int startX;
        int endX;
        int startZ;
        int endZ;

        // we want to stretch in the direction of the other endzone
        if (Math.abs(dx) > Math.abs(dz)) {
            startZ = min.getBlockZ();
            endZ = max.getBlockZ() - 1;

            if (dx > 0) {
                startX = max.getBlockX();
                endX = max.getBlockX() + ENDZONE_STRETCH - 1;
                max.setX(max.getX() + ENDZONE_STRETCH);
            } else {
                startX = min.getBlockX() - ENDZONE_STRETCH;
                endX = min.getBlockX() - 1;
                min.setX(min.getX() - ENDZONE_STRETCH);
            }
        } else {
            startX = min.getBlockX();
            endX = max.getBlockX() - 1;

            if (dz > 0) {
                startZ = max.getBlockZ();
                endZ = max.getBlockZ() + ENDZONE_STRETCH - 1;
                max.setZ(max.getZ() + ENDZONE_STRETCH);
            } else {
                startZ = min.getBlockZ() - ENDZONE_STRETCH;
                endZ = min.getBlockZ() - 1;
                min.setZ(min.getZ() - ENDZONE_STRETCH);
            }
        }

        // be safe in case the map isn't flag (tron brady)
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = center.getBlockY(); y >= 0; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid()) {
                        sample.applyTo(block, false);
                        break;
                    }
                }
            }
        }

        modifyCuboid(region, min, max);
    }

    public static void modifyCuboid(CuboidRegion region, Vector newMin, Vector newMax) {
        try {
            Object bounds = region.getBounds();

            Field minField = bounds.getClass().getDeclaredField("min");
            minField.setAccessible(true);
            Vector min = (Vector) minField.get(bounds);

            Field maxField = bounds.getClass().getDeclaredField("max");
            maxField.setAccessible(true);
            Vector max = (Vector) maxField.get(bounds);

            min.copy(newMin);
            max.copy(newMax);
        } catch (Exception ignored) {}
    }
}
