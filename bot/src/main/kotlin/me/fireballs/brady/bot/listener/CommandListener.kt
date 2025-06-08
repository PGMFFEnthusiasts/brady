package me.fireballs.brady.bot.listener

import me.fireballs.brady.bot.command.BradySlashCommand
import me.fireballs.brady.bot.command.SlashCommand
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands

private val testGuilds = listOf(
    416767292320514048L, // test
    1365177238320058409L, // pgm ff
//    777989916809232406L, // kunet test
)

class CommandListener : ListenerAdapter() {
    private val slashCommands = listOf<SlashCommand>(
        BradySlashCommand()
    )

    private val slashCommandMap = mutableMapOf<String, SlashCommand>().apply {
        slashCommands.forEach { put(it.name, it) }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val correspondingCommand = slashCommandMap[event.name]
        if (correspondingCommand == null) {
            event.reply("I don't think I can help you with that").queue()
            return
        }

        correspondingCommand.execute(event);
    }

    override fun onReady(event: ReadyEvent) {
        val slashCommandData = slashCommands.map {
            Commands.slash(it.name, it.description).setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        }

        event.jda.guilds.forEach {
            if (!testGuilds.contains(it.idLong)) return@forEach
            it.updateCommands().addCommands(slashCommandData).queue()
        }
    }
}