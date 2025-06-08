package me.fireballs.share.util;

import net.minecraft.server.v1_8_R3.MovingObjectPosition;
import net.minecraft.server.v1_8_R3.Vec3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockUtil {
    // https://github.com/PGMDev/PGM/blob/dev/platform/platform-sportpaper/src/main/java/tc/oc/pgm/platform/sportpaper/impl/SpPlayerUtils.java#L132
    public static boolean hasTargetedBlock(Player player) {
        Location start = player.getEyeLocation();
        World world = player.getWorld();
        Vector startVector = start.toVector();
        Vector end = start
                .toVector()
                .add(start.getDirection().multiply(player.getGameMode() == GameMode.CREATIVE ? 6 : 4.5));
        MovingObjectPosition hit = ((CraftWorld) world)
                .getHandle()
                .rayTrace(
                        new Vec3D(startVector.getX(), startVector.getY(), startVector.getZ()),
                        new Vec3D(end.getX(), end.getY(), end.getZ()),
                        false,
                        false,
                        false);
        return hit != null && hit.type == MovingObjectPosition.EnumMovingObjectType.BLOCK;
    }

}
