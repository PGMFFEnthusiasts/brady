package me.fireballs.brady.tools

import me.fireballs.brady.core.FeatureFlagBool
import me.fireballs.brady.core.registerEvents
import net.minecraft.server.v1_8_R3.DamageSource
import org.bukkit.GameMode
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VoidKill : Listener, KoinComponent {
    val enabled = FeatureFlagBool("voidSetback", true)
    private val tools: Tools by inject<Tools>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler
    private fun onVoidKill(event: PlayerTeleportEvent) {
        if (!enabled.state) return
        val p = (event.player as CraftPlayer)
        if (p.gameMode != GameMode.SURVIVAL && p.gameMode != GameMode.ADVENTURE) return
        if (event.to.y > 0) return
        event.isCancelled = true
        p.handle.damageEntity(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE)
    }

    @EventHandler
    private fun onVoidFalling(event: PlayerMoveEvent) {
        if (!enabled.state) return
        val p = (event.player as CraftPlayer)
        if (p.gameMode != GameMode.SURVIVAL && p.gameMode != GameMode.ADVENTURE) return
        if (event.to.y > -10) return
        p.handle.damageEntity(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE)
    }
}
