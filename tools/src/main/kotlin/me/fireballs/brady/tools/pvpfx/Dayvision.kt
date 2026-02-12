package me.fireballs.brady.tools.pvpfx

import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.tools.Tools
import me.fireballs.brady.tools.ToolsSettings
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PotionEffectAddEvent
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Dayvision : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val settings by inject<ToolsSettings>()

    init {
        tools.registerEvents(this)
    }

    @EventHandler
    private fun onEffect(event: PotionEffectAddEvent) {
        if (event.effect.type != PotionEffectType.NIGHT_VISION) return
        val p = event.entity
        if (p !is Player) return
        if (!settings.dayvision.retrieveValue(p)) return
        event.isCancelled = true
    }
}
