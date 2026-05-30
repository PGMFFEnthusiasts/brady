package me.fireballs.brady.tools;

import com.destroystokyo.paper.PaperConfig;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.fireballs.brady.core.*;
import me.fireballs.brady.corepgm.FeatureFlagBool;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.ObsTools;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PracticeBall implements Listener {

    private static final ItemBox BALL = ItemBoxKt.itembox(Material.SNOW_BALL)
            .name(ComponentKt.cc("&b&lHolo-Ball", true))
            .lore(ComponentKt.cc("&7Latest and greatest from BradyCorp", true))
            .specialData("tools:practiceBall");

    private final Tools plugin;
    private final ToolsSettings settings;
    private final FeatureFlagBool enabled = new FeatureFlagBool("practiceBall", true);

    public PracticeBall() {
        this.plugin = KoinJavaComponent.get(Tools.class);
        this.settings = KoinJavaComponent.get(ToolsSettings.class);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onKitApply(ObserverKitApplyEvent event) {
        if (enabled.getState()) {
            giveBall(event.getPlayer().getBukkit());
        }
    }

    private static void giveBall(Player player) {
        MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
        if (matchPlayer == null) return;
        if (!matchPlayer.isObserving()) return;

        player.getInventory().setItem(1, BALL.build());

        // we replaced the edit wand slot
        if (ObsTools.canUseEditWand(player)) {
            player.getInventory().setItem(3, ObsTools.getEditWand(player));
        }
    }

    private static void takeBall(Player player) {
        player.getInventory().removeItem(BALL.build());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!"tools:practiceBall".equals(ItemBoxKt.specialData(event.getItem()))) return;
        if (!enabled.getState()) return;

        Player shooter = event.getPlayer();
        MatchManager matchManager = PGM.get().getMatchManager();

        MatchPlayer matchShooter = matchManager.getPlayer(shooter);
        if (matchShooter == null) return;
        if (!matchShooter.isObserving()) return;

        Set<Player> viewers = StreamSupport.stream(PlayerExtensionsKt.trackedPlayers(shooter).spliterator(), false)
                .map(ep -> matchManager.getPlayer(ep.getBukkitEntity().getPlayer()))
                .filter(mp -> mp != null && mp.isObserving())
                .map(MatchPlayer::getBukkit)
                .collect(Collectors.toSet());

        viewers.add(shooter);

        EntityType entityType = settings.getProjectileSkin().retrieveValue(shooter.getUniqueId()).getEntityType();
        boolean splat = settings.getSplatSetting().retrieveValue(shooter.getUniqueId());

        VirtualSnowball snowball = new VirtualSnowball(shooter, new ArrayList<>(viewers), entityType, splat);
        takeBall(shooter);
        snowball.shoot(plugin);

        event.setCancelled(true);
    }

    private static class VirtualSnowball {

        private static final Soundbox THROW_SOUNDS = new Soundbox()
                .add(Sound.SHOOT_ARROW, 0.5f, 1f)
                .add(Sound.SHOOT_ARROW, 0.6f, 1f)
                .add(Sound.SHOOT_ARROW, 0.7f, 1f)
                .add(Sound.FIREWORK_LAUNCH, 0.5f, 1f);

        private static final Soundbox PICKUP_SOUNDS = new Soundbox()
                .add(Sound.FIREWORK_LAUNCH, 1f, 1f)
                .add(Sound.FIREWORK_LAUNCH, 0.9f, 1f)
                .add(Sound.FIREWORK_LAUNCH, 0.8f, 1f);

        private static final Soundbox CATCH_SOUNDS = new Soundbox()
                .add(Sound.SLIME_WALK2, 1f, 1f)
                .add(Sound.SLIME_WALK2, 0.9f, 1f)
                .add(Sound.SLIME_WALK2, 0.8f, 1f)
                .add(Sound.NOTE_SNARE_DRUM, 1.2f, 0.5f)
                .add(Sound.ORB_PICKUP, 1.5f, 0.7f)
                .add(Sound.ORB_PICKUP, 1f, 0.6f)
                .add(Sound.PORTAL_TRAVEL, 1.5f, 0.1f);

        private final Player shooter;
        private final List<Player> viewers;
        private final EntityType entityType;
        private final boolean splat;
        private final int entityId;
        private final WorldServer nmsWorld;

        public double x, y, z;
        public double vx, vy, vz;

        private int ticksLived;

        private VirtualSnowball(Player shooter, List<Player> viewers, EntityType entityType, boolean splat) {
            this.shooter = shooter;
            this.viewers = viewers;
            this.entityType = entityType;
            this.splat = splat;
            this.entityId = Bukkit.allocateEntityId();
            this.nmsWorld = ((CraftWorld) shooter.getWorld()).getHandle();

            Location shooterLoc = shooter.getLocation();

            this.x = shooterLoc.getX();
            this.y = shooterLoc.getY() + shooter.getEyeHeight() - 0.1;
            this.z = shooterLoc.getZ();

            Vector dir = shooter.getLocation().getDirection().normalize().multiply(1.5);
            this.vx = dir.getX();
            this.vy = dir.getY();
            this.vz = dir.getZ();
        }

        public void shoot(Plugin plugin) {
            float volume = ThreadLocalRandom.current().nextFloat() * 0.4f + 0.8f;
            Soundbox sound = new Soundbox().add(Sound.SHOOT_ARROW, 0.4f, volume);
            playSound(shooter, sound, shooter.getLocation().toVector());
            playSound(shooter, THROW_SOUNDS, shooter.getLocation().toVector());

            spawn();

            double compensationTicks = Math.min(shooter.spigot().getPing() / 100.0, 3.0);
            boolean dead = false;

            while (compensationTicks > 0 && !dead) {
                double motion = Math.min(compensationTicks, 1);
                dead = !tick(motion);
                compensationTicks -= 1;
            }

            if (dead) {
                destroy();
                giveBall(shooter);
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (y < 0 || !tick(1)) {
                        destroy();
                        cancel();
                        giveBall(shooter);
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }

        private boolean tick(double motionFactor) {
            ticksLived++;

            Iterator<Player> it = viewers.iterator();
            while (it.hasNext()) {
                Player viewer = it.next();
                MatchPlayer mp = PGM.get().getMatchManager().getPlayer(viewer);

                if (mp != null && !mp.isObserving()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, new WrapperPlayServerDestroyEntities(entityId));
                    it.remove();
                }
            }

            Vec3D currentPos = new Vec3D(x, y, z);
            Vec3D nextPos = new Vec3D(x + vx * motionFactor, y + vy * motionFactor, z + vz * motionFactor);

            MovingObjectPosition hitResult = nmsWorld.rayTrace(currentPos, nextPos);

            if (hitResult != null) {
                nextPos = new Vec3D(hitResult.pos.a, hitResult.pos.b, hitResult.pos.c);
            }

            Player hit = null;
            double closestHit = Double.MAX_VALUE;

            Player inside = null;
            double closestInside = Double.MAX_VALUE;

            for (Player target : viewers) {
                if (target == shooter && ticksLived < 5) continue;

                EntityPlayer nmsPlayer = ((CraftPlayer) target).getHandle();
                AxisAlignedBB targetBox = nmsPlayer.getBoundingBox().grow(0.3, 0.3, 0.3);
                MovingObjectPosition intersect = targetBox.a(currentPos, nextPos);

                if (intersect != null) {
                    double distance = currentPos.distanceSquared(intersect.pos);
                    if (distance < closestHit) {
                        closestHit = distance;
                        hit = target;
                    }
                } else if (targetBox.a(currentPos)) { // inside
                    Vec3D targetCenter = new Vec3D(nmsPlayer.locX, nmsPlayer.locY + nmsPlayer.getHeadHeight() / 2.0, nmsPlayer.locZ);
                    double distance = currentPos.distanceSquared(targetCenter);
                    if (distance < closestInside) {
                        closestInside = distance;
                        inside = target;
                    }
                }
            }

            Player finalHit = inside != null ? inside : hit;

            if (finalHit != null) {
                damage(finalHit, shooter);

                playSound(finalHit, PICKUP_SOUNDS, finalHit.getLocation().toVector());
                playSound(shooter, PICKUP_SOUNDS, shooter.getLocation().toVector());

                playSound(finalHit, CATCH_SOUNDS, finalHit.getLocation().toVector());
                playSound(shooter, CATCH_SOUNDS, shooter.getLocation().toVector());

                return false;
            }

            if (hitResult != null) {
                if (splat) {
                    playSound(shooter, CommonSoundsKt.getThud(), new Vector(x, y, z));
                }
                return false;
            }

            x += vx * motionFactor;
            y += vy * motionFactor;
            z += vz * motionFactor;

            double dragFactor = Math.pow(0.99, motionFactor);
            vx *= dragFactor;
            vy *= dragFactor;
            vz *= dragFactor;
            vy -= 0.03 * motionFactor;

            return true;
        }

        private void spawn() {
            sendPacket(new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.empty(),
                    entityType,
                    new Vector3d(x, y, z),
                    0f, 0f, 0f, 0,
                    Optional.empty()
            ));

            sendPacket(new WrapperPlayServerEntityVelocity(entityId, new Vector3d(vx, vy, vz)));
        }

        private void destroy() {
            sendPacket(new WrapperPlayServerDestroyEntities(entityId));
        }

        private void sendPacket(PacketWrapper<?> packet) {
            viewers.forEach(v -> PacketEvents.getAPI().getPlayerManager().sendPacket(v, packet));
        }

        private void playSound(Player player, Soundbox sound, Vector vector) {
            MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
            if (matchPlayer == null) return;
            if (!matchPlayer.isObserving()) return;

            sound.play(player, vector);
        }

        private void damage(Player target, Player shooter) {
            if (target.getNoDamageTicks() > target.getMaximumNoDamageTicks() / 2) return;

            Location targetLoc = target.getLocation();
            Location shooterLoc = shooter.getLocation();

            double dx = shooterLoc.getX() - targetLoc.getX();
            double dz = shooterLoc.getZ() - targetLoc.getZ();

            while (dx * dx + dz * dz < 0.0001) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                dx = (r.nextDouble() - r.nextDouble()) * 0.01;
                dz = (r.nextDouble() - r.nextDouble()) * 0.01;
            }

            double magnitude = Math.hypot(dx, dz);

            Vector velocity = target.getVelocity();
            velocity.setX(velocity.getX() / PaperConfig.knockbackFriction);
            velocity.setY(velocity.getY() / PaperConfig.knockbackFriction);
            velocity.setZ(velocity.getZ() / PaperConfig.knockbackFriction);

            velocity.setX(velocity.getX() - (dx / magnitude * PaperConfig.knockbackHorizontal));
            velocity.setY(velocity.getY() + PaperConfig.knockbackVertical);
            velocity.setZ(velocity.getZ() - (dz / magnitude * PaperConfig.knockbackHorizontal));

            if (velocity.getY() > PaperConfig.knockbackVerticalLimit) {
                velocity.setY(PaperConfig.knockbackVerticalLimit);
            }

            target.setVelocity(velocity);
            target.setNoDamageTicks(target.getMaximumNoDamageTicks());
            target.playEffect(EntityEffect.HURT);

            float pitch = (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.2f + 1f;
            Soundbox sound = new Soundbox().add(Sound.HURT_FLESH, 1f, pitch);

            playSound(target, sound, target.getLocation().toVector());
            playSound(shooter, sound, target.getLocation().toVector());
        }
    }
}
