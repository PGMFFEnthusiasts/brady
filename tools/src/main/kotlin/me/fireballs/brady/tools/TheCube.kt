package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import net.minecraft.server.v1_8_R3.EnumParticle
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.event.Listener
import org.bukkit.util.Vector
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.absoluteValue

private fun particle(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, multiplier: Float) =
    PacketPlayOutWorldParticles(
        EnumParticle.REDSTONE, true,
        x, y, z,
        r, g, b, multiplier, 0
    )

private val suffixes = listOf(
    1_000_000_000_000.0 to "T",
    1_000_000_000.0 to "B",
    1_000_000.0 to "M",
    1_000.0 to "K"
)

private val suffixFormatter = DecimalFormat("#.###").apply { roundingMode = RoundingMode.HALF_UP }
private val plainFormatter = DecimalFormat("0.###").apply { roundingMode = RoundingMode.HALF_UP }

fun Float.d(): String {
    val absValue = this.absoluteValue
    if (absValue < 1) return this.toString()
    for ((threshold, suffix) in suffixes) {
        if (absValue < threshold) continue
        return suffixFormatter.format(this / threshold) + suffix
    }
    return plainFormatter.format(this)
}

fun diff(a: Float, b: Float): String {
    if (a == b) return a.d()
    return "${a.d()} • ${b.d()}"
}

//private fun Float.d() = String.format("%,.2f", this)
private fun CommandExecution.getFloat(): Pair<Float, Boolean> {
    val a = subArgs.getOrNull(0) ?: throw CommandInterrupt("&cProvide a value or a delta (2.1, +2, +-2.5 ...).".cc())
    val isDelta = a.startsWith("+")
    var value = a.removePrefix("+")
        .replace("k", "", ignoreCase = true)
        .replace("m", "", ignoreCase = true)
        .replace("b", "", ignoreCase = true)
        .replace("t", "", ignoreCase = true)
        .toFloatOrNull() ?: throw CommandInterrupt("&cInvalid value.".cc())
    if (a.endsWith("k", ignoreCase = true)) value *= 1_000
    if (a.endsWith("m", ignoreCase = true)) value *= 1_000_000
    if (a.endsWith("b", ignoreCase = true)) value *= 1_000_000_000
    if (a.endsWith("t", ignoreCase = true)) value *= 1_000_000_000_000
    return value to isDelta
}

private fun CommandExecution.valueSetter(name: String, valueIn: Float, block: (Float) -> Unit) {
    if (subArgs.getOrNull(0) == null) {
        sender.send("&7$name is currently ${valueIn.d()} ($valueIn), provide a value or delta".cc())
        return
    }

    val (v, delta) = getFloat()
    val result = if (delta) valueIn + v else v
    block(result)
    sender.send("&7$name is now ${result.d()} ($result).".cc())
    okay.play(sender)
}

private fun CommandExecution.twoValueSetter(
    name: String,
    valueIn1: Float,
    valueIn2: Float,
    block: (Float, Float) -> Unit
) {
    val (v, delta) = getFloat()
    val result1 = if (delta) valueIn1 + v else v
    val result2 = if (delta) valueIn2 + v else v
    sender.send("&7$name is now ${result1.d()} / ${result2.d()}.".cc())
    block(result1, result2)
    okay.play(sender)
}

class TheCube : Listener, KoinComponent {
    private val tools by inject<Tools>()

    private var spacing = 1f
    private var count = 10
    private var minR = 100_000f
    private var maxR = 1_000_000f
    private var minG = 100_000f
    private var maxG = 1_000_000f
    private var minB = 100_000f
    private var maxB = 1_000_000f
    private var multiplier = 100f

    private var cubeOrigin: Vector? = null

    private fun hash() =
        spacing.toString() + count + minR + maxR + minG + maxG + minB + maxB + multiplier + cubeOrigin.toString()

    private var currentHash = ""
    private var currentParticles = mutableListOf<PacketPlayOutWorldParticles>()
    private var currentComponent = "".cc()

    init {
        tools.registerEvents(this)

        command("cube", "the cube", "tools.cube", aliases = arrayOf("cu")) {
            subcommand("here") {
                executor {
                    cubeOrigin = player().location.toVector()
                    okay.play(sender)
                    sender.send("&7As you wish.".cc())
                }
            }

            subcommand("kill") {
                executor {
                    cubeOrigin = null
                    uhOh.play(sender)
                    sender.send("&7Till we meet again.".cc())
                }
            }

            subcommand("current", aliases = arrayOf("show")) {
                executor {
                    sender.send("&c$minR • $maxR &a$minG • $maxG &9$minB • $maxB &f$multiplier".cc())
                    sender.send("$spacing spacing, $count count".cc())
                    okay.play(sender)
                }
            }

            subcommand("space", aliases = arrayOf("spacing")) {
                executor {
                    valueSetter("Spacing", spacing) { spacing = it }
                }
            }

            subcommand("count") {
                executor {
                    valueSetter("Count", count.toFloat()) { count = it.toInt() }
                }
            }

            subcommand("minr", aliases = arrayOf("rmin")) {
                executor {
                    valueSetter("minR", minR) { minR = it }
                }
            }

            subcommand("ming", aliases = arrayOf("gmin")) {
                executor {
                    valueSetter("minG", minG) { minG = it }
                }
            }

            subcommand("minb", aliases = arrayOf("bmin")) {
                executor {
                    valueSetter("minB", minB) { minB = it }
                }
            }

            subcommand("maxr", aliases = arrayOf("rmax")) {
                executor {
                    valueSetter("maxR", maxR) { maxR = it }
                }
            }

            subcommand("maxg", aliases = arrayOf("gmax")) {
                executor {
                    valueSetter("maxG", maxG) { maxG = it }
                }
            }

            subcommand("maxb", aliases = arrayOf("bmax")) {
                executor {
                    valueSetter("maxB", maxB) { maxB = it }
                }
            }

            subcommand("r") {
                executor {
                    twoValueSetter("Red", minR, maxR) { v1, v2 ->
                        minR = v1
                        maxR = v2
                    }
                }
            }

            subcommand("g") {
                executor {
                    twoValueSetter("Green", minG, maxG) { v1, v2 ->
                        minG = v1
                        maxG = v2
                    }
                }
            }

            subcommand("b") {
                executor {
                    twoValueSetter("Blue", minB, maxB) { v1, v2 ->
                        minB = v1
                        maxB = v2
                    }
                }
            }

            subcommand("multiplier", aliases = arrayOf("mul")) {
                executor {
                    valueSetter("Multiplier", multiplier) { multiplier = it }
                }
            }
        }

        val subdivisions = 4
        tools.launch {
            var nthTick = 0
            while (true) {
                delay(1.ticks)
                val origin = cubeOrigin ?: continue

                ++nthTick
                nthTick %= subdivisions

                if (currentHash != hash()) {
                    currentHash = hash()
                    currentParticles.clear()
                    currentComponent =
                        "&c${diff(minR, maxR)} &a${diff(minG, maxG)} &9${diff(minB, maxB)} &f(${multiplier.d()})".cc()

                    repeat(count) { x ->
                        repeat(count) { y ->
                            repeat(count) { z ->
                                val r = lerp(x.toFloat() / count, minR, maxR)
                                val g = lerp(y.toFloat() / count, minG, maxG)
                                val b = lerp(z.toFloat() / count, minB, maxB)

                                currentParticles.add(
                                    particle(
                                        (origin.x + x * spacing).toFloat(),
                                        (origin.y + y * spacing).toFloat(),
                                        (origin.z + z * spacing).toFloat(),
                                        r, g, b,
                                        multiplier
                                    )
                                )
                            }
                        }
                    }
                }

                for (player in tools.server.onlinePlayers) {
                    player.actionBar(currentComponent)
                    val connection = (player as CraftPlayer).handle.playerConnection
                    currentParticles.forEachIndexed { index, packet ->
                        if (count <= 12 || (index + packet.hashCode()) % subdivisions == nthTick)
                            connection.sendPacket(packet)
                    }
//                    for (currentParticle in currentParticles) connection.sendPacket(currentParticle)
                }
            }
        }
    }
}
