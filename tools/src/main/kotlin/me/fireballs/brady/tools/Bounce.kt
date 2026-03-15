package me.fireballs.brady.tools

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.google.common.collect.MapMaker
import me.fireballs.brady.core.log
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.sendPacket
import me.fireballs.brady.corepgm.currentMatch
import net.minecraft.server.v1_8_R3.*
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import kotlin.math.sign
import kotlin.math.sqrt

class Bouncer(
    val entityPredicate: (Entity) -> Boolean,
    val entityPlayerMapper: (Entity) -> Iterable<Player> = { e -> e.world.players },
    val bounceCoefficient: Double = 0.8,
    val kineticFriction: Double = 0.6,
    val rollingTicks: Int = 5,
) : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val matchManager by inject<MatchManager>()

    init {
        tools.registerEvents(this)
    }

    private val rollingTickMap = MapMaker()
        .weakKeys()
        .makeMap<Entity, Int>()

    private fun test(world: World, c: Vec3D, x: Double, y: Double, z: Double): Pair<MovingObjectPosition, Block>? {
        val mop = (world as CraftWorld).handle.rayTrace(c, Vec3D(x, y, z), false, true, false) ?: return null
        if (mop.type != MovingObjectPosition.EnumMovingObjectType.BLOCK) return null
        val bp = mop.a()
        return mop to world.getBlockAt(bp.x, bp.y, bp.z)
    }

    private fun playBounce(
        entity: Entity,
        type: Block,
        top: Boolean,
        v: Double,
        x: Double,
        y: Double,
        z: Double
    ) {
        val block = CraftMagicNumbers.getBlock(type as CraftBlock)
        val blockData = (type.chunk as CraftChunk).handle.getBlockData(BlockPosition(type.x, type.y, type.z))
        val sound = block.stepSound
        var pi = sound.volume2 * 0.8f + (Math.random() * 0.05).toFloat() + v.toFloat() / 8f
        var vo = (sound.volume1 + 1f) * v.toFloat() * 1.5f
        val isBarrier = type.type == Material.BARRIER
        if (isBarrier) {
            pi = 0.7f
            vo *= 0.3f
        }
        if (v > 0.03) {
            val sound =
                PacketPlayOutNamedSoundEffect(if (isBarrier) "random.anvil_land" else sound.breakSound, x, y, z, vo, pi)
            entityPlayerMapper(entity).forEach { it.sendPacket(sound) }
        }
        val s = (0.15 + 0.15 * v).toFloat()
        val pc = (v * 8).toInt()
        val playGrass = type.type == Material.GRASS && top
        if (pc != 0 && type.type != Material.BARRIER) {
            val particles = PacketPlayOutWorldParticles(
                EnumParticle.BLOCK_CRACK,
                true,
                x.toFloat(),
                y.toFloat(),
                z.toFloat(),
                s,
                s,
                s,
                1f,
                pc,
                net.minecraft.server.v1_8_R3.Block.getCombinedId(
                    if (playGrass) Blocks.TALLGRASS.fromLegacyData(BlockLongGrass.EnumTallGrassType.GRASS.a()) else blockData
                )
            )
            entityPlayerMapper(entity).forEach { it.sendPacket(particles) }
        }
    }

    private fun tickEntity(entity: Entity) {
        val ce = entity as CraftEntity
        val han = ce.handle
        val x = han.locX
        val y = han.locY
        val z = han.locZ
        var vx = han.motX
        var vy = han.motY
        var vz = han.motZ
        var nx = x + vx
        var ny = y + vy
        var nz = z + vz

        val currentVec = Vec3D(x, y, z)

        fun genericBounce(tx: Boolean, ty: Boolean, tz: Boolean): Boolean {
            val tested = test(entity.world, currentVec, if (tx) nx else x, if (ty) ny else y, if (tz) nz else z)
            if (tested == null) return true
            val xs = !tx || tested.first.direction.adjacentX.toDouble().sign != vx.sign
            val ys = !ty || tested.first.direction.adjacentY.toDouble().sign != vy.sign
            val zs = !tz || tested.first.direction.adjacentZ.toDouble().sign != vz.sign
            if (!xs || !ys || !zs) return true

            if (tx) {
                vx = -vx * bounceCoefficient
                nx = x
            }

            if (ty) {
                vy = -vy * bounceCoefficient
                ny = y

                if (vy * vy < 0.03) {
                    vx *= kineticFriction
                    vy *= kineticFriction
                    vz *= kineticFriction
                    rollingTickMap[entity] = (rollingTickMap[entity] ?: 0) + 1
                } else rollingTickMap[entity] = 0
            }

            if (tz) {
                vz = -vz * bounceCoefficient
                nz = z
            }

            if (vx * vx + vy * vy + vz * vz < 0.03 && (rollingTickMap[entity] ?: 0) > rollingTicks) {
                log("bouncy", "at rest")
                entity.remove()
                return false
            }

            playBounce(
                entity,
                tested.second,
                tested.first.direction == EnumDirection.UP,
                sqrt(vx * vx + vy * vy + vz * vz),
                nx,
                ny,
                nz
            )
            return true
        }

        if (!genericBounce(tx = true, ty = false, tz = false)
            || !genericBounce(tx = false, ty = true, tz = false)
            || !genericBounce(tx = false, ty = false, tz = true)
            || !genericBounce(tx = true, ty = true, tz = false)
            || !genericBounce(tx = true, ty = false, tz = true)
            || !genericBounce(tx = false, ty = true, tz = true)
            || !genericBounce(tx = true, ty = true, tz = true)
        ) return

        han.motX = vx
        han.motY = vy
        han.motZ = vz
        han.ai = true

        val pos = PacketPlayOutEntityTeleport(han)
        val vel = PacketPlayOutEntityVelocity(han.id, vx, vy, vz)
        (han.world as WorldServer).tracker.trackedEntities.get(han.id).trackedPlayers.forEach { p ->
            p.playerConnection.sendPacket(pos)
            p.playerConnection.sendPacket(vel)
        }
    }

    fun tick() {
        matchManager.currentMatch()?.world?.entities?.forEach {
            if (entityPredicate(it)) tickEntity(it)
        }
    }

    @EventHandler
    private fun onEntityDeath(event: EntityRemoveFromWorldEvent) {
        rollingTickMap.remove(event.entity)
    }
}
