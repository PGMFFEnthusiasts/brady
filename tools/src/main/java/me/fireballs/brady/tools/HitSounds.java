package me.fireballs.brady.tools;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.koin.java.KoinJavaComponent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class HitSounds implements Listener {

    private final ToolsSettings settings;
    private final ComboTracker comboTracker;
    private final Set<Integer> ignoredHits;

    private static final Field WORLD_ACCESS_LIST_FIELD;

    static {
        try {
            WORLD_ACCESS_LIST_FIELD = World.class.getDeclaredField("u");
            WORLD_ACCESS_LIST_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public HitSounds() {
        Tools plugin = KoinJavaComponent.get(Tools.class);
        this.settings = KoinJavaComponent.get(ToolsSettings.class);
        this.comboTracker = new ComboTracker();
        this.ignoredHits = new HashSet<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(comboTracker, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        WorldServer world = ((CraftWorld) event.getWorld()).getHandle();
        injectInterceptor(world);
    }

    private void injectInterceptor(WorldServer world) {
        try {
            @SuppressWarnings("unchecked")
            List<IWorldAccess> listeners = (List<IWorldAccess>) WORLD_ACCESS_LIST_FIELD.get(world);

            for (int i = 0; i < listeners.size(); i++) {
                IWorldAccess listener = listeners.get(i);
                if (listener instanceof WorldManager) {
                    listeners.set(i, new HitSoundInterceptor(this, listener, world));
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Sound> resolveSounds(Player listener, EntityHuman victim, Sound sound) {
        if (settings.getMineplexSounds().retrieveValue(listener)) {
            Sound mineplexSound = mineplexSound(victim.getBukkitEntity(), sound.volume());
            if (mineplexSound != null) {
                sound = mineplexSound;
            }
        }

        if (settings.getFlagSounds().retrieveValue(listener)) {
            ItemStack helmet = victim.getBukkitEntity().getInventory().getHelmet();
            if (helmet != null && helmet.getType() == Material.BANNER) {
                sound = new Sound("mob.zombie.wood", 0.8f, 2f);
            }
        }

        List<Sound> sounds = new ArrayList<>();
        sounds.add(sound);

        if (settings.getComboSounds().retrieveValue(listener)) {
            int combo = comboTracker.getCombo(listener.getEntityId(), victim.getBukkitEntity().getEntityId());
            int extraSounds = Math.min(4, combo - 1);

            for (int i = 0; i < extraSounds; i++) {
                sounds.add(sounds.getFirst());
            }
        }

        return sounds;
    }

    private Sound mineplexSound(CraftHumanEntity victim, float volume) {
        PlayerInventory inventory = victim.getInventory();
        double r = ThreadLocalRandom.current().nextDouble();

        ItemStack armorSample;

        if (r > 0.5) {
            armorSample = inventory.getChestplate();
        } else if (r > 0.25) {
            armorSample = inventory.getLeggings();
        } else if (r > 0.1) {
            armorSample = inventory.getHelmet();
        } else {
            armorSample = inventory.getBoots();
        }

        if (armorSample == null) return null;

        return switch (armorSample.getType()) {
            case LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS ->
                    new Sound("random.bow", volume, 2f);
            case CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS ->
                    new Sound("random.break", volume, 1.4f);
            case GOLD_HELMET, GOLD_CHESTPLATE, GOLD_LEGGINGS, GOLD_BOOTS ->
                    new Sound("random.break", volume, 1.8f);
            case IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS ->
                    new Sound("mob.blaze.hit", volume, 0.7f);
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS ->
                    new Sound("mob.blaze.hit", volume, 0.9f);
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && event.getDamager() instanceof Snowball) {
            ignoredHits.add(victim.getEntityId());
        } else {
            ignoredHits.remove(victim.getEntityId());
        }
    }

    public record Sound(String name, float volume, float pitch) {}

    private record HitSoundInterceptor(HitSounds parent, IWorldAccess wa, WorldServer world) implements IWorldAccess {

        @Override
        public void a(EntityHuman victim, String name, double x, double y, double z, float volume, float pitch) {
            if (!"game.player.hurt".equals(name)) {
                wa.a(victim, name, x, y, z, volume, pitch);
                return;
            }

            double radius = (volume > 1.0F) ? (16.0 * volume) : 16.0;

            int victimId = victim.getBukkitEntity().getEntityId();

            if (parent.ignoredHits.contains(victimId)) {
                parent.ignoredHits.remove(victimId);
                return;
            }

            Sound sound = new Sound(name, volume, pitch);

            for (EntityHuman listener : world.players) {
                if (!(listener instanceof EntityPlayer nmsListener)) continue;
                if (listener.dimension != world.dimension) continue;
                if (listener == victim) continue;

                if (victim instanceof EntityPlayer sourcePlayer) {
                    if (!nmsListener.getBukkitEntity().canSee(sourcePlayer.getBukkitEntity())) continue;
                }

                double dx = x - listener.locX;
                double dy = y - listener.locY;
                double dz = z - listener.locZ;

                if ((dx * dx + dy * dy + dz * dz) > radius * radius) continue;

                parent.resolveSounds(nmsListener.getBukkitEntity(), victim, sound).forEach(s ->
                        nmsListener.playerConnection.sendPacket(new PacketPlayOutNamedSoundEffect(s.name(), x, y, z, s.volume(), s.pitch())));
            }
        }

        @Override public void a(BlockPosition bp) {wa.a(bp);}
        @Override public void b(BlockPosition bp) {wa.b(bp);}
        @Override public void a(int i, int i1, int i2, int i3, int i4, int i5) {wa.a(i, i1, i2, i3, i4, i5);}
        @Override public void a(String s, double v, double v1, double v2, float v3, float v4) {wa.a(s, v, v1, v2, v3, v4);}
        @Override public void a(int i, boolean b, double v, double v1, double v2, double v3, double v4, double v5, int... ints) {wa.a(i, b, v, v1, v2, v3, v4, v5, ints);}
        @Override public void a(Entity entity) {wa.a(entity);}
        @Override public void b(Entity entity) {wa.b(entity);}
        @Override public void a(String s, BlockPosition bp) {wa.a(s, bp);}
        @Override public void a(int i, BlockPosition bp, int i1) {wa.a(i, bp, i1);}
        @Override public void a(EntityHuman eh, int i, BlockPosition bp, int i1) {wa.a(eh, i, bp, i1);}
        @Override public void b(int i, BlockPosition bp, int i1) {wa.b(i, bp, i1);}
    }

    public static class ComboTracker implements Listener {
        private static final int COMBO_RESET_TICKS = 30;
        private final Table<Integer, Integer, ComboData> combos = HashBasedTable.create();

        private static class ComboData {
            int count = 0;
            long lastHitTick = 0;
        }

        public int getCombo(int attackerId, int victimId) {
            ComboData data = combos.get(attackerId, victimId);
            if (data != null && (MinecraftServer.currentTick - data.lastHitTick) <= COMBO_RESET_TICKS) {
                return data.count;
            }
            return 0;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerDamage(EntityDamageByEntityEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
            if (!(event.getEntity() instanceof Player victim)) return;
            if (!(event.getDamager() instanceof Player attacker)) return;

            int attackerId = attacker.getEntityId();
            int victimId = victim.getEntityId();

            ComboData data = combos.get(attackerId, victimId);
            if (data == null) {
                data = new ComboData();
                combos.put(attackerId, victimId, data);
            }

            ComboData victimData = combos.get(victimId, attackerId);
            if (victimData != null) {
                victimData.count = 0;
            }

            if (MinecraftServer.currentTick - data.lastHitTick > COMBO_RESET_TICKS) {
                data.count = 0;
            }

            data.count++;
            data.lastHitTick = MinecraftServer.currentTick;
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            int entityId = event.getPlayer().getEntityId();
            combos.row(entityId).clear();
            combos.column(entityId).clear();
        }
    }
}
