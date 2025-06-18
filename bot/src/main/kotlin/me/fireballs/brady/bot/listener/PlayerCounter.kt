package me.fireballs.brady.bot.listener

import me.fireballs.brady.bot.Bot
import me.fireballs.brady.core.registerEvents
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerCounter : Listener, KoinComponent {
    private val plugin by inject<Bot>()
    private val jda by inject<JDA>()

    init {
        plugin.registerEvents(this)
        onPlayerCountChange(Bukkit.getOnlinePlayers().size)
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        onPlayerCountChange(Bukkit.getOnlinePlayers().size)
    }

    @EventHandler
    private fun onLeave(event: PlayerQuitEvent) {
        onPlayerCountChange(Bukkit.getOnlinePlayers().size - 1)
    }

    private fun onPlayerCountChange(n: Int) {
        // that moment when NO abstraction
        jda.presence.activity = Activity.of(
            Activity.ActivityType.PLAYING,
            "TB | ${if (n > 0) "$n online" else "Nobody :("}",
        )
    }
}