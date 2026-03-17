package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.deps.PluginAnnotation
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
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

        // anyone can do this command idc
        command("breakthisintolines") {
            executor {
                val width = capture("Provide a width") { subArgs[0].toInt() }
                val remaining = subArgs.drop(1).joinToString(" ").replace("\\n", "\n")

                var componentized = remaining.cc() as Component
                if (remaining.isEmpty()) componentized =
                    "hello world! ".cc().hover("sup".c()) +
                            "&c&lhow are you today? &7i'm sorry if this example is too complex " +
                            "&9&nbut that's okay!&f ".cc().fill("/helloworld").hover("click to fill".c()) +
                            "anyway, &ahow was your day&r? i hope it &dwent well&r. &6owo"

                for (component in breakIntoLines(componentized, width)) {
                    sender.send(component)
                }
            }
        }
    }

    override fun onDisable() {
        val kickMessage = "&c⚠ Server is restarting ⚠".colorLegacy()
        server.onlinePlayers.forEach { it.kickPlayer(kickMessage) }
        kavyManager.onDisable()
    }

    companion object : KoinComponent {
        val instance by inject<Core>()
        val kavyManager by inject<KavyManager>()

        private val coreAdventure by inject<BukkitAudiences>()
        var injectedAdventure: BukkitAudiences? = null
        var componentRenderFn: (Component) -> Component = { it }

        val adventure: BukkitAudiences
            get() = injectedAdventure ?: coreAdventure
    }
}
