package me.fireballs.brady.bingo.squares

import me.fireballs.brady.bingo.Square
import me.fireballs.brady.core.log
import org.bukkit.Material
import org.bukkit.event.EventHandler
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent
import tc.oc.pgm.api.tracker.info.FallInfo
import tc.oc.pgm.api.tracker.info.MeleeInfo
import tc.oc.pgm.tracker.info.ItemInfo
import tc.oc.pgm.tracker.info.ProjectileInfo

class LetMeDownGently(squareIndex: Int) : Square("Let me down gently", squareIndex) {
    @EventHandler
    private fun onDeath(event: MatchPlayerDeathEvent) {
        if (!event.player.isParticipating) return
        val damageInfo = event.damageInfo
        if (damageInfo !is FallInfo || damageInfo.to != FallInfo.To.VOID) return
        val cause = damageInfo.cause
        if (cause !is MeleeInfo) return
        val attacker = damageInfo.attacker?.player?.orElse(null) ?: return
        val weapon = cause.weapon
        if (weapon !is ItemInfo) return
        if (weapon.item.type != Material.AIR && weapon.item.type != Material.GOLDEN_APPLE)
        complete(attacker.bukkit)
    }

    override fun matchesDomainRestriction() = playerFloorOf(2)
}
