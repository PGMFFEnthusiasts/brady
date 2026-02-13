package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.deps.PluginAnnotation
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

@PluginAnnotation
class Core : SuspendingJavaPlugin() {
    override fun onEnable() {
        val appModule = module {
            single<Core> { this@Core }
            single<BukkitAudiences> { BukkitAudiences.create(this@Core) }
            single<MenuManager> { MenuManager() }
            serviceOf(::KavyManager)
            serviceOf(::Settings)
        }

        startKoin {
            modules(appModule)
        }

        DebuggingSubscriber()
    }

    override fun onDisable() {
        val kickMessage = "&c⚠ Server is restarting ⚠".colorLegacy()
        server.onlinePlayers.forEach { it.kickPlayer(kickMessage) }
        kavyManager.onDisable()
    }

    companion object : KoinComponent {
        val instance by inject<Core>()
        val adventure by inject<BukkitAudiences>()
        val kavyManager by inject<KavyManager>()
    }
}
