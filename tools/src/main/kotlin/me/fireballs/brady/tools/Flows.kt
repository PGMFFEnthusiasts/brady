package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.CommandBuilder
import me.fireballs.brady.core.CommandExecution
import me.fireballs.brady.core.CommandExecutor
import me.fireballs.brady.core.CommandInterrupt
import me.fireballs.brady.core.c
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.command
import me.fireballs.brady.core.plus
import me.fireballs.brady.core.send
import me.fireballs.brady.corepgm.match
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.party.Competitor
import tc.oc.pgm.teams.TeamMatchModule
import kotlin.system.measureTimeMillis

class Flows : KoinComponent {
    private val tools by inject<Tools>()
    private val bouncy by inject<BouncyBall>()

    private val prefix = "&d⧗&7 ".cc()

    private fun log(component: Component) {
        tools.server.onlinePlayers
            .filter { it.hasPermission("brady.admin") }
            .forEach { it.send(prefix + component) }
    }

    private fun CommandBuilder.defineFlow(name: String, block: CommandExecutor) {
        subcommand(name) {
            executor {
                log("executing flow $name".c())
                try {
                    val time = measureTimeMillis { block() }
                    log("finished flow $name in ${time}ms".c())
                } catch (err: CommandInterrupt) {
                    log("flow $name stopped: ".c() + (err.reason ?: "reason not provided".c()))
                } catch (e: Exception) {
                    log("flow $name failed (see trace): ".c() + (e.message ?: "reason not provided").c())
                    e.printStackTrace()
                }
            }
        }
    }

    private fun CommandExecution.joinPlayer(p: Player = player()) {
        val match = match()
        val teams = match.getModule(TeamMatchModule::class.java) ?: err("not teams")
        teams.join(match.getPlayer(p), null as Competitor?)
    }

    init {
        command("flow", permission = "brady.admin") {
            defineFlow("bouncyarrows") {
                bouncy.enabled.set("true")
                val match = match()
                joinPlayer()
                match.start()
                delay(5.ticks)
                for (player in match.players) {
                    player.bukkit.inventory.addItem(ItemStack(Material.BOW))
                    player.bukkit.inventory.addItem(ItemStack(Material.ARROW, 64))
                }
            }
        }
    }
}
