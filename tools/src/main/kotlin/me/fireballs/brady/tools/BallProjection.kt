package me.fireballs.brady.tools

import com.github.retrooper.packetevents.util.Vector3d
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.currentMatch
import me.fireballs.brady.core.lerp
import me.fireballs.brady.core.registerEvents
import net.minecraft.server.v1_8_R3.EnumParticle
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import java.util.concurrent.ThreadLocalRandom

private class BallState(
    var x: Double, var y: Double, var z: Double,
    var vx: Double, var vy: Double, var vz: Double,
) {
    private val drag = 0.99f
    private val gravity = 0.03f

    fun step() {
        x += vx
        y += vy
        z += vz

        vx *= drag
        vy *= drag
        vy -= gravity
        vz *= drag

//        println("$x $y $z : $vx $vy $vz")
    }
}

private fun trace(projectile: Projectile): List<Vector3d> {
    val l = projectile.location
    val v = projectile.velocity
    val state = BallState(l.x, l.y, l.z, v.x, v.y, v.z)
    val positions = mutableListOf<Vector3d>()

    do {
        positions.add(Vector3d(state.x, state.y, state.z))
        state.step()
    } while (positions.size < 20 * 5 && (state.y <= 0 || !Location(
            projectile.world,
            state.x,
            state.y,
            state.z
        ).block.type.isSolid)
    )

    return positions
}

class BallProjection : Listener, KoinComponent {
    private val tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler(ignoreCancelled = true)
    private suspend fun onLaunch(event: ProjectileLaunchEvent) {
        if (event.entity !is Snowball) return

//        delay(1.ticks)
        val ballPositions = trace(event.entity).toMutableList()
        val originalLength = ballPositions.size

        if (originalLength == 0) return

        while (!event.entity.isDead) {
            val t = (originalLength.toFloat() - ballPositions.size) / originalLength
//            Bukkit.broadcastMessage(t.toString())

            for (ballPosition in ballPositions) {
                val packet = PacketPlayOutWorldParticles(
                    EnumParticle.REDSTONE, true,
                    ballPosition.x.toFloat(), ballPosition.y.toFloat(), ballPosition.z.toFloat(),
                    1e7f, 10f, 0.003f,
                    lerp(t, 200f, 2_000_000f), 0
                )

                PGM.get().matchManager.currentMatch()?.observers?.forEach {
                    if (ThreadLocalRandom.current().nextDouble() > 0.77)
                            (it.bukkit as CraftPlayer).handle.playerConnection.sendPacket(packet)
                }

//                val loc = Location(event.world, ballPosition.x, ballPosition.y, ballPosition.z)
//                for (player in event.world.players) {
//                    if (player.gameMode != GameMode.CREATIVE) continue
//                    player.spigot().playEffect(loc, Effect.FLAME, 0, 0, 0f, 0f, 0f, 0f, 1, 1000)
//                }
            }

            if (ballPositions.isNotEmpty()) ballPositions.removeAt(0)

            delay(1.ticks)
        }
    }
}
