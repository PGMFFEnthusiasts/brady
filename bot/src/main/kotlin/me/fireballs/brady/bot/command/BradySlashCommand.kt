package me.fireballs.brady.bot.command

import me.fireballs.brady.bot.utils.InfoBoard
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BradySlashCommand : SlashCommand(
    "brady",
    "Check how many people are on the server",
    emptyList(),
), KoinComponent {
    private val infoBoard by inject<InfoBoard>()

    override fun execute(interaction: SlashCommandInteractionEvent) {
        val count = Bukkit.getOnlinePlayers().size
        val basePart = "There ${if (count == 1) "is 1 Brady'er" else "are $count Brady'ers"} online"
        interaction.reply("$basePart\n${infoBoard.generateInfoBoard()}").setEphemeral(true).queue()
    }
}
