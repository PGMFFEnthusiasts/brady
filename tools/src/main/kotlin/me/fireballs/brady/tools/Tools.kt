package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.core.registerEvents
import me.fireballs.brady.core.registerPacketEvents
import me.fireballs.brady.deps.PluginAnnotation
import org.koin.dsl.module

@PluginAnnotation
class Tools : SuspendingJavaPlugin() {
    override fun onEnable() {
        val toolModule = module {
            single<Tools> { this@Tools }
            single<Drafting>(createdAtStart = true) { Drafting() }
            single<FlagFootballMaps>(createdAtStart = true) { FlagFootballMaps() }
            single<Splat>(createdAtStart = true) { Splat() }
        }

        loadModule(toolModule)

        registerPacketEvents(TabListResize())
    }
}
