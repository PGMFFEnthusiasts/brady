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
            serviceOf(::AutoGC)
        }

        startKoin {
            modules(appModule)
        }

        DebuggingSubscriber()

        val testComponent = "hello world! ".cc().hover("sup".c()) +
                "&chow are you today? &7i'm sorry if this example is too complex " +
                "&9&nbut that's okay!&f ".cc().fill("/helloworld").hover("click to fill".c()) +
                "anyway, &ahow was your day&r? i hope it &dwent well&r. &6owo"

        fun CommandExecution.widthAndComponent(): Pair<Int, Component> {
            val width = capture("Provide a width") { subArgs[0].toInt() }
            val remaining = subArgs.drop(1).joinToString(" ").replace("\\n", "\n")

            var componentized = remaining.cc() as Component
            if (remaining.isEmpty()) componentized = testComponent

            return width to componentized
        }

        // anyone can do these commands idc
        command("textutils") {
            executor {
                sender.send("see subcommands <tab>".c())
            }

            subcommand("wrap") {
                executor {
                    val (width, componentized) = widthAndComponent()
                    for (component in breakIntoLines(componentized, width)) sender.send(component)
                }
            }

            subcommand("center") {
                executor {
                    val (width, componentized) = widthAndComponent()
                    for (component in justifyCenter(componentized, width)) sender.send(component)
                }
            }

            subcommand("right") {
                executor {
                    val (width, componentized) = widthAndComponent()
                    for (component in justifyRight(componentized, width)) sender.send(component)
                }
            }

            subcommand("slideup") {
                executor {
                    repeat(20) {
                        sender.send(computePaddingOfSize(it) + "hey")
                    }
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
