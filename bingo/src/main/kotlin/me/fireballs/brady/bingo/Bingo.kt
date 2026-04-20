package me.fireballs.brady.bingo

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.bingo.mechanics.Cooking
import me.fireballs.brady.bingo.mechanics.EggCracking
import me.fireballs.brady.bingo.mechanics.PieCooking
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.command
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.core.newValkeyPooledClient
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.send
import me.fireballs.brady.core.serviceOf
import me.fireballs.brady.deps.PluginAnnotation
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.dsl.module

@PluginAnnotation
class Bingo : Listener, SuspendingJavaPlugin() {
    val valkeyClient = newValkeyPooledClient() ?: throw IllegalStateException("Must have VALKEY")

    private fun showNotice(p: CommandSender) {
        p.send()
        p.send("&6&l⨳ &6Bingo event".cc().command("/server 1"))
        p.send()
        p.send("&eThere's a bingo event happening right now.".cc().command("/server 1"))
        p.send("&eIt's on server 1 though, &6&nclick to transfer&e.".cc().command("/server 1"))
        p.send()
        bingoDing.play(p)
    }

    override fun onEnable() {
        if (System.getenv("BRADY_SERVER") != "1") {
            logger.info("Not on server 1; aborting :(")
            registerEvents(this)
            command("bingo") {
                executor {
                    showNotice(sender)
                }
            }
            return
        }

        val bingoModule = module {
            single<Bingo> { this@Bingo }
            serviceOf(::Squares)
            serviceOf(::BingoManager)

            serviceOf(::Cooking)
            serviceOf(::EggCracking)
            serviceOf(::PieCooking)
        }

        loadModule(bingoModule)
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        val p = event.player
        launch(asyncDispatcher) {
            delay(40.ticks)
            showNotice(p)
        }
    }
}
