package me.fireballs.brady.bot.utils

import me.fireballs.brady.core.c
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.currentMatch
import me.fireballs.brady.core.forWhom
import me.fireballs.brady.core.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.util.named.MapNameStyle
import tc.oc.pgm.util.text.TemporalComponent.clock
import kotlin.collections.forEach
import kotlin.getValue

class InfoBoard : KoinComponent {
    private val matchManager by inject<MatchManager>()

    private fun playerListing(): String? {
        val match = matchManager.currentMatch() ?: return null
        var message = "".c() as Component

        val teamModule = match.getModule(TeamMatchModule::class.java)
        if (teamModule != null) teamModule.teams.forEach {
            message += teamBlock(
                it.name,
                it.textColor,
                it.players,
                it.maxPlayers
            )
        }
        else message += teamBlock("&7Participants:".cc(), NamedTextColor.WHITE, match.participants, -1)

        message += teamBlock(match.defaultParty.name, match.defaultParty.textColor, match.defaultParty.players, -1)
        return ansify(message).removeSuffix("\n")
    }

    private fun teamBlock(
        teamName: Component,
        teamColor: NamedTextColor,
        players: Collection<MatchPlayer>,
        maxSize: Int
    ): Component {
        if (players.isEmpty()) return "".c()
        var line = teamName + " " + players.size.toString()
        if (maxSize >= 0) line += "&7/${maxSize}"
        line += "&7:".cc() + newline()
        players.forEach { p -> line += "• ".c().color(teamColor) + p.name.forWhom() + newline() }
        return line
    }

    private fun matchInformation(): String? {
        val match = matchManager.currentMatch() ?: return null
        var message = "".c() as Component

        val scoreMatchModule = match.getModule(ScoreMatchModule::class.java)
        if (scoreMatchModule != null) message += scoreMatchModule.getStatusMessage(null).forWhom()

        message += "&8 • &7".cc() +
                match.phase.toString().replaceFirstChar { it.titlecaseChar() }.c()
                    .decoration(TextDecoration.BOLD, true) +
                " " + clock(match.duration).color(NamedTextColor.GOLD)

        return ansify(message).replace("\n", "")
    }

    fun generateInfoBoard(): String? {
        val currentMatch = matchManager.currentMatch() ?: return null
        val mapName = ansify(currentMatch.map.getStyledName(MapNameStyle.HIGHLIGHT_WITH_AUTHORS).forWhom())
        return "```ansi\n$mapName\n${matchInformation()}\n${playerListing()}\n```"
    }
}