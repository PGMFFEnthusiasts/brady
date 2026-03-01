package me.fireballs.brady.bingo

import me.fireballs.brady.core.soundbox
import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.inventory.meta.FireworkMeta
import tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS
import java.time.Duration


val kaomoji = listOf("(˶˃ᵕ˂˶)", "( ⸝⸝´꒳`⸝⸝)", "(˶❛⩊❛˵)", "(˶ᵔ ᵕ ᵔ˶)", "(≧ᗜ≦)", "(˶˃ᆺ˂˶)", "₍^. .^₎⟆")

fun String.obfuscateX(): String {
    val sb = StringBuilder()
    var prev = false
    for (c in this) {
        if (c == ' ') {
            if (prev) {
                prev = false
                sb.append("&r")
            }
            sb.append(" ")
        } else {
            if (!prev) {
                prev = true
                sb.append("&b&k")
            }
            sb.append("x")
        }
    }

    return sb.toString()
}

fun top(position: Int, total: Int): Double {
    return (((position - 1) / (total - 1).toDouble()) * 1000.0).toInt() / 10.0
}

const val boardSize = 5
const val boardCount = boardSize * boardSize

fun indexToId(index: Int): String {
    val col = 'A' + (index / boardSize)
    val row = (index % boardSize) + 1
    return "$col$row"
}

val rewardFirework = firework {
    addEffect(
        FireworkEffect.builder()
            .with(FireworkEffect.Type.BURST)
            .withFlicker()
            .withColor(Color.LIME)
            .withFade(Color.BLACK)
            .build()
    )
}

val bingoDing = soundbox()
    .add(Sound.LEVEL_UP, 1.5f)
    .add(Sound.CHICKEN_EGG_POP, 0.75f)

fun formatDuration(duration: Duration): String {
    if (duration.isNegative) return "an eternity"

    val seconds = duration.seconds
    val days = duration.toDays()
    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()

    return when {
        days >= 1 -> "$days day${if (days > 1) "s" else ""}"
        days < 1 && hours >= 1 -> "$hours hour${if (hours > 1) "s" else ""}, $minutes minute${if (minutes > 1) "s" else ""}"
        seconds in 61..3600 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        else -> "just a bit"
    }
}

fun firework(builder: FireworkMeta.() -> Unit): (Location) -> Unit {
    val meta = Bukkit.getItemFactory().getItemMeta(Material.FIREWORK) as FireworkMeta
    builder(meta)

    return { spawnLocation ->
        val firework = spawnLocation.world.spawn(spawnLocation, Firework::class.java)
        firework.fireworkMeta = meta
        NMS_HACKS.skipFireworksLaunch(firework)
    }
}
