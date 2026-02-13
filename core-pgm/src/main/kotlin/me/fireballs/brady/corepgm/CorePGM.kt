package me.fireballs.brady.corepgm

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.deps.PluginAnnotation
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.MatchManager

@PluginAnnotation
class CorePGM : SuspendingJavaPlugin() {
    override fun onEnable() {
        val coreModule = module {
            single<PGM> { PGM.get() }
            single<MatchManager> { PGM.get().matchManager }
            single<MapOrder> { PGM.get().mapOrder }
        }

        loadModule(coreModule)

        FeatureFlagsSubscriber()
    }

    companion object : KoinComponent {
        internal val matchManager by inject<MatchManager>()
    }
}
