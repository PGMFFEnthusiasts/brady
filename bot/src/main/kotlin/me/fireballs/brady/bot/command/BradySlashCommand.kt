package me.fireballs.brady.bot.command

import me.fireballs.brady.bot.listener.PlayerCounter
import me.fireballs.brady.core.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.newline
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.ansi.ColorLevel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.util.named.MapNameStyle
import tc.oc.pgm.util.text.TemporalComponent.clock

private val ansiSerializer = ANSIComponentSerializer.builder().colorLevel(ColorLevel.INDEXED_8).build()
private fun ansify(component: Component): String {
    val firstSerialized = LegacyComponentSerializer.legacySection().serialize(component)
    val reserialized = LegacyComponentSerializer.legacySection().deserialize(firstSerialized)
    return ansiSerializer.serialize(reserialized)
}

class BradySlashCommand : SlashCommand(
    "brady",
    "Check how many people are on the server",
    emptyList(),
), KoinComponent {
    private val playerCounter by inject<PlayerCounter>()
    private val matchManager by inject<MatchManager>()

    private fun playerListing(): String {
        val match = matchManager.currentMatch()
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

    private fun matchInformation(): String {
        val match = matchManager.currentMatch()
        var message = "".c() as Component

        val scoreMatchModule = match.getModule(ScoreMatchModule::class.java)
        if (scoreMatchModule != null) message += scoreMatchModule.getStatusMessage(null).forWhom()

        message += "&8 • &7".cc() +
                match.phase.toString().replaceFirstChar { it.titlecaseChar() }.c()
                    .decoration(TextDecoration.BOLD, true) +
                " " + clock(match.duration).color(NamedTextColor.GOLD)

        return ansify(message).replace("\n", "")
    }

    override fun execute(interaction: SlashCommandInteractionEvent) {
        val count = playerCounter.counter

        val currentMatch = matchManager.currentMatch()
        val mapName = ansify(currentMatch.map.getStyledName(MapNameStyle.HIGHLIGHT_WITH_AUTHORS).forWhom())
        val infoBlock = "```ansi\n$mapName\n${matchInformation()}\n${playerListing()}\n```"

        val basePart = "There ${if (count == 1) "is 1 Brady'er" else "are $count Brady'ers"} online"
        interaction.reply("$basePart\n$infoBlock").queue()
    }
}
