package me.fireballs.brady.bot.command

import me.fireballs.brady.bot.listener.PlayerCounter
import me.fireballs.brady.core.*
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.ansi.ColorLevel
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.util.named.MapNameStyle

private val ansiSerializer = ANSIComponentSerializer.builder().colorLevel(ColorLevel.INDEXED_8).build()
fun doTheDansi(component: Component): String {
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

    override fun execute(interaction: SlashCommandInteractionEvent) {
        val count = playerCounter.counter

        val lines = Bukkit.getOnlinePlayers().map { "• ".cc() + it.component().forWhom() }
        val joined = Component.join(JoinConfiguration.newlines(), lines)
        val playerList = doTheDansi(joined)
        val currentMatch = matchManager.currentMatch()
        val mapName = doTheDansi(currentMatch.map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS).forWhom())
        val phase = doTheDansi("&8 • &7".cc() + currentMatch.phase.toString().uppercase())

        val infoBlock = "```ansi\n$mapName$phase\n$playerList\n```"

        interaction.reply(
                "There ${if (count == 1) "is 1 Brady'er" else "are $count Brady'ers"} online\n$infoBlock"
        ).queue()
    }
}
