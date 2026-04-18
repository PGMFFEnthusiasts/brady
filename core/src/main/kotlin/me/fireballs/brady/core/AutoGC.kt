package me.fireballs.brady.core

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AutoGC : KoinComponent, Listener {
    private val core by inject<Core>()

    init {
        core.registerEvents(this)
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        if (Bukkit.getServer().onlinePlayers.size != 1) return
        System.gc() // ...no one yell at me... please?
    }
}
