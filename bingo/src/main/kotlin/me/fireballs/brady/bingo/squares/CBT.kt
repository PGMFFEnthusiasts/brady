package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.ProgressSquare
import me.fireballs.brady.bingo.firework
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.entity.Chicken
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent

class CBT(squareIndex: Int) : ProgressSquare("C.B.T.", squareIndex, 4) {
    val bleedFirework = firework {
        addEffect(
            FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.RED)
                .build()
        )
    }

    @EventHandler
    private fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Snowball) return
        val chicken = event.entity
        if (chicken !is Chicken) return
        val thrower = damager.shooter
        if (thrower !is Player) return
        bleedFirework(chicken.location)
        increment(thrower)
    }
}
