package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.match.MatchManager
import kotlin.math.max

class CommandInterrupt(val reason: Component?) : Exception(reason?.plainText())

class CommandExecution(
    val sender: CommandSender,

    val args: Array<String>,
    val subArgs: Array<String>,
) : KoinComponent {
    private val matchManager by inject<MatchManager>()

    fun error(reason: Component): Nothing = throw CommandInterrupt(reason)
    fun err(reason: Component): Nothing = error("&c⚠ ".cc() + reason)
    fun err(reason: String): Nothing = error("&c⚠ $reason".cc())

    fun player(): Player {
        if (sender !is Player) err("You must be a player to use this!")
        return sender
    }

    fun match(): Match = matchManager.getMatch(player().world) ?: err("Match not found")
}

typealias CommandCompleter = CommandExecution.() -> Collection<String>
typealias CommandExecutor = suspend CommandExecution.() -> Unit
typealias CommandBuilderBlock = CommandBuilder.() -> Unit

class CommandBuilder(
    val name: String,
    val description: String?,
    val permission: String?,
    val usageMessage: String?,
    val aliases: Set<String>,
) {
    internal val subcommands = mutableListOf<CommandBuilder>()

    val playerCompleter: CommandCompleter = {
        Bukkit.getOnlinePlayers()
            .filter { it.name.startsWith(subArgs.last(), true) }
            .map { it.name }
            .sortedWith(String.Companion.CASE_INSENSITIVE_ORDER)
    }

    var executor: CommandExecutor = { error("&c⚠ This command doesn't do anything".cc()) }
    var tabCompleter: CommandCompleter = { listOf() }

    fun executor(block: CommandExecutor) {
        executor = block
    }

    fun tabCompleter(block: CommandCompleter) {
        tabCompleter = block
    }

    fun subcommand(
        name: String,
        permission: String? = null,
        vararg aliases: String = arrayOf(),
        builder: CommandBuilderBlock = {},
    ) {
        val subCommand = CommandBuilder(name, null, permission, usageMessage, aliases.toSet())
        builder(subCommand)
        subcommands.add(subCommand)
    }
}

fun command(
    name: String,
    description: String = "This command has no description",
    permission: String? = null,
    usageMessage: String = "/$name",
    vararg aliases: String = arrayOf(),
    builder: CommandBuilder.() -> Unit = {},
) {
    val command = CommandBuilder(name, description, permission, usageMessage, aliases.toSet())
    builder(command)

    val simpleCommandMap = Bukkit.getServer().commandMap as SimpleCommandMap
    simpleCommandMap.register("brady", CommandHandler(Core.instance, command))
}

private class CommandHandler(val javaPlugin: SuspendingJavaPlugin, val commandBuilder: CommandBuilder) : Command(
    commandBuilder.name,
    commandBuilder.description,
    commandBuilder.usageMessage ?: "/${commandBuilder.name}",
    commandBuilder.aliases.toList()
) {
    override fun getPermission() = commandBuilder.permission

    init {
        permission = commandBuilder.permission
    }

    private fun getSubcommand(command: CommandBuilder, arg: String): CommandBuilder? =
        command.subcommands.find {
            it.name.equals(arg, true) ||
                    (it.aliases.isNotEmpty() && it.aliases.any { alias -> alias.equals(arg, false) })
        }

    override fun execute(sender: CommandSender, commandLabel: String?, args: Array<String>): Boolean {
        if (!testPerm(sender, commandBuilder.permission)) return true

        var depth = 0
        var head = commandBuilder

        for (arg in args) {
            val subcommand = getSubcommand(head, arg) ?: break
            head = subcommand
            ++depth
            if (!testPerm(sender, head.permission)) return true
        }

        val subArgs = args.copyOfRange(max(depth, 0), args.size)
        val commandContext = CommandExecution(sender, args, subArgs)
        javaPlugin.launch {
            try {
                head.executor(commandContext)
            } catch (interrupt: CommandInterrupt) {
                if (interrupt.reason != null) {
                    sender.send(interrupt.reason)
                    uhOh.play(sender)
                    return@launch
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                sender.send("&c⚠ A fatal error has occurred in the course of executing this command".cc())
                soundbox()
                    .add(0, Sound.EXPLODE)
                    .play(sender)
            }
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String?, args: Array<String>): MutableList<String> {
        if (!testPermSilent(sender, commandBuilder.permission)) return mutableListOf()

        var depth = 0
        var head = commandBuilder
        var flag = false

        while (depth < args.size && !flag) {
            val subcommand = getSubcommand(head, args[depth])
            if (subcommand != null) head = subcommand
            else flag = true
            ++depth
        }

        val currentArg = args[depth - 1]
        val candidates = mutableListOf<String>()

        for (subcommand in head.subcommands) {
            if (!testPermSilent(sender, subcommand.permission)) continue
            candidates.add(subcommand.name)
            candidates.addAll(subcommand.aliases)
        }

        val subcommandCandidates = candidates
            .filter { it.startsWith(currentArg, true) }
            .toList()

        val subArgs = args.copyOfRange(depth - 1, args.size)

        val completions = mutableListOf<String>()
        completions.addAll(subcommandCandidates)
        completions.addAll(head.tabCompleter(CommandExecution(sender, args, subArgs)))

        completions.removeIf { !it.startsWith(currentArg, true) }

        return completions
    }
}
