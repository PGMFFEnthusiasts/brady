package me.fireballs.brady.corepgm

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import me.fireballs.brady.core.Core
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.deps.PluginAnnotation
import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.util.Audience

@PluginAnnotation
class CorePGM : SuspendingJavaPlugin() {
    override fun onEnable() {
        val coreModule = module {
            single<PGM> { PGM.get() }
            single<MatchManager> { PGM.get().matchManager }
            single<MapOrder> { PGM.get().mapOrder }
        }

        loadModule(coreModule)

        // this makes me deeply sad
        launch {
            while (!Bukkit.getPluginManager().isPluginEnabled("PGM"))
                delay(1.ticks)
            Core.injectedAdventure = Audience.PROVIDER
            Core.componentRenderFn = { it.forWhom() }
            Core.adventure.console().sendMessage("&a[CorePGM] Successfully hooked PGM's BukkitAudiences".cc())
        }

        FeatureFlagsSubscriber()
    }

    companion object : KoinComponent {
        internal val matchManager by inject<MatchManager>()
    }
}
