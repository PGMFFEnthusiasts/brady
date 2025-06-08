package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.deps.PluginAnnotation
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.api.match.MatchManager

@PluginAnnotation
class Core : SuspendingJavaPlugin() {
    override fun onEnable() {
        val appModule = module {
            single<Core> { this@Core }
            single<PGM> { PGM.get() }
            single<MatchManager> { PGM.get().matchManager }
            single<MapOrder> { PGM.get().mapOrder }
            single<BukkitAudiences> { BukkitAudiences.create(this@Core) }
            single<MenuManager> { MenuManager() }
        }

        startKoin {
            modules(appModule)
        }
    }

    override fun onDisable() {
        val kickMessage = "&c⚠ Server is restarting ⚠".colorLegacy()
        server.onlinePlayers.forEach { it.kickPlayer(kickMessage) }
    }

    companion object : KoinComponent {
        val instance by inject<Core>()
        val adventure by inject<BukkitAudiences>()
    }
}
