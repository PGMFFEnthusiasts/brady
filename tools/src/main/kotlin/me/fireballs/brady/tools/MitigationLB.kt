package me.fireballs.brady.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fireballs.brady.core.c
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.command
import me.fireballs.brady.core.event.BradyClickMitigateEvent
import me.fireballs.brady.core.hover
import me.fireballs.brady.core.newValkeyPooledClient
import me.fireballs.brady.core.plus
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.send
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PAGE_SIZE = 15L

class MitigationLB : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val valkey = newValkeyPooledClient()

    init {
        tools.registerEvents(this)

        command("mitigationlb", aliases = arrayOf("mlb"), permission = "brady.admin") {
            executor {
                val valkey = valkey ?: err("Storage backend not available")
                val page = subArgs.getOrNull(0)?.toIntOrNull()
                if (subArgs.isEmpty() || page != null) {
                    val page = page ?: 1
                    if (page < 1) err("Page must be positive")
                    val start = (page - 1) * PAGE_SIZE
                    val results = withContext(Dispatchers.IO) {
                        val top = valkey.zrevrangeWithScores("mitigations", start, start + PAGE_SIZE)
                        if (top.isEmpty()) return@withContext emptyList()
                        val names = valkey.hmget("nameCache", *top.map { it.element.lowercase() }.toTypedArray())
                        top.zip(names).filter { it.second != null }
                    }
                    val total = withContext(Dispatchers.IO) { valkey.zcard("mitigations") }

                    sender.send()
                    sender.send("&6Page $page/${(total / PAGE_SIZE) + 1} ($total total)".cc())
                    sender.send()
                    for ((i, e) in results.withIndex())
                        sender.send("${start + i + 1}. ".c('7') + "${e.second}".c('e').hover("UUID: ${e.first.element}".c('7')) + " - ".c('7') + "${e.first.score.toLong()}".c('6'))
                    var navigator = empty() as Component
                    if (page != 1) navigator += " «".c('6').command("/mlb ${page - 1}")
                    navigator += " $page ".c('e')
                    if (page.toLong() != (total / PAGE_SIZE) + 1) navigator += "» ".c('6').command("/mlb ${page + 1}")
                    sender.send()
                    sender.send(navigator)
                    sender.send()
                    return@executor
                }

                err("Search not implemented yet")
            }
        }
    }

    @EventHandler
    private suspend fun onMitigate(event: BradyClickMitigateEvent) {
        val valkey = valkey ?: return
        withContext(Dispatchers.IO) {
            valkey.zincrby("mitigations", 1.0, event.player.uniqueId.toString())
        }
    }
}
