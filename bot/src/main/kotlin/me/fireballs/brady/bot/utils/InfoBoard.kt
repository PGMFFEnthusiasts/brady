package me.fireballs.brady.bot.utils

import me.fireballs.brady.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.util.named.MapNameStyle
import tc.oc.pgm.util.text.TemporalComponent.clock

class InfoBoard : KoinComponent {
    private val matchManager by inject<MatchManager>()

    private fun playerListing(
        newLines: Boolean = true,
    ): Component {
        val match = matchManager.currentMatch() ?: return "".c()
        var message = "".c() as Component

        val teamModule = match.getModule(TeamMatchModule::class.java)
        if (teamModule != null) teamModule.teams.forEach {
            message += teamBlock(
                it.name,
                it.textColor,
                it.players,
                it.maxPlayers,
                newLines,
            )
        } else message += teamBlock("&7Participants:".cc(), NamedTextColor.WHITE, match.participants, -1, newLines)

        message += teamBlock(
            match.defaultParty.name,
            match.defaultParty.textColor,
            match.defaultParty.players,
            -1,
            newLines
        )

        return message
    }

    private fun teamBlock(
        teamName: Component,
        teamColor: NamedTextColor,
        players: Collection<MatchPlayer>,
        maxSize: Int,
        newLines: Boolean,
    ): Component {
        if (players.isEmpty()) return "".c()
        var line = teamName + " " + players.size.toString()
        if (maxSize >= 0) line += "&7/${maxSize}"
        line += "&7: ".cc()
        if (newLines) line += newline()
        players.forEach { p ->
            line += if (newLines) "• ".c().color(teamColor) + p.name.forWhom() + newline() else p.name.forWhom() + " "
        }

        return line
    }

    private fun matchInformation(): Component {
        val match = matchManager.currentMatch() ?: return "".c()
        var message = "".c() as Component

        val scoreMatchModule = match.getModule(ScoreMatchModule::class.java)
        if (scoreMatchModule != null) message += scoreMatchModule.getStatusMessage(null).forWhom()

        message += "&8 • &7".cc() +
                match.phase.toString().replaceFirstChar { it.titlecaseChar() }.c()
                    .decoration(TextDecoration.BOLD, true) +
                " " + clock(match.duration).color(NamedTextColor.GOLD)
        return message
    }

    fun generateInfoBoard(): String? {
        val currentMatch = matchManager.currentMatch() ?: return null
        val mapName = ansify(currentMatch.map.getStyledName(MapNameStyle.HIGHLIGHT_WITH_AUTHORS).forWhom())
        return "```ansi\n$mapName\n${ansify(matchInformation()).replace("\n", "")}" +
                "\n${ansify(playerListing()).removeSuffix("\n")}\n```"
    }

    fun generateInGameInfoBoard(): String {
        val currentMatch = matchManager.currentMatch() ?: return ""
        return matchInformation().forWhom().coloredText() +
                "\n" + currentMatch.map.getStyledName(MapNameStyle.HIGHLIGHT_WITH_AUTHORS).forWhom().coloredText() +
                "\n\n${playerListing(false).forWhom().coloredText()}"
    }
}
