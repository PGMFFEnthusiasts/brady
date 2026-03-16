package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.data.SoundKeys
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.soundbox
import me.fireballs.brady.corepgm.FeatureFlagBool
import me.fireballs.brady.corepgm.currentMatch
import net.kyori.adventure.text.Component.empty
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.event.ActionNodeTriggerEvent
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.match.event.MatchStartEvent

class BouncyBall : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val matchManager by inject<MatchManager>()
    val enabled = FeatureFlagBool("bouncy")
    private val bouncer = Bouncer({ e -> e is Projectile })

    init {
        tools.registerEvents(this)
        tools.launch {
            while (tools.isEnabled) {
                delay(1.ticks)
                if (enabled.state) bouncer.tick()
            }
        }

        enabled.changeHandlers += { enabled ->
            if (enabled) matchManager.currentMatch()?.let { playBouncy(it) }
        }
    }

    @EventHandler
    @Suppress("unused")
    private fun onFlagReset(event: ActionNodeTriggerEvent) {
        matchManager.currentMatch()?.world?.entities
            ?.filterIsInstance<Snowball>()
            ?.forEach { it.remove() }
    }

    @EventHandler
    private fun onLaunch(event: ProjectileLaunchEvent) {
        if (!enabled.state) return
        val snowball = event.entity
        if (snowball !is Snowball) return
        snowball.velocity = snowball.velocity.multiply(0.85)
    }

    private val bounceAnnounceSound = soundbox()
        .add(SoundKeys.NOTE_BASS, 0.8f)
        .add(2, SoundKeys.NOTE_BASS, 0.8f)

    @EventHandler
    private suspend fun onMatchStart(event: MatchStartEvent) {
        if (!enabled.state) return
        delay(10.ticks)
        playBouncy(event.match)
    }

    private fun playBouncy(match: Match) {
        match.sendMessage(empty())
        match.sendMessage("&e&l ⎲ &6＊ &6&lＢＯＵＮＣＹ&6 ＊".cc())
        match.sendMessage("&e&l ⎳ &f&oThe balls have elastic properties".cc())
        match.sendMessage(empty())
        bounceAnnounceSound.broadcast(match.world)
    }
}
