package me.fireballs.brady.bot.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData

abstract class SlashCommand(
    val name: String,
    val description: String,
    val options: List<OptionData>,
) {
    abstract fun execute(interaction: SlashCommandInteractionEvent)
}
