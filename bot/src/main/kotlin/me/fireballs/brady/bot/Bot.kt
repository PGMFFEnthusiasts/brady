package me.fireballs.brady.bot

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import dev.minn.jda.ktx.jdabuilder.default
import me.fireballs.brady.bot.listener.Loggy
import me.fireballs.brady.bot.utils.InfoBoard
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.deps.PluginAnnotation
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.internal.utils.JDALogger
import org.bukkit.Bukkit
import org.koin.dsl.module

@PluginAnnotation
class Bot : SuspendingJavaPlugin() {
    private var jda: JDA? = null

    override fun onEnable() {
        val botToken = System.getenv("BRADY_BOT_TOKEN")
        if (botToken.isNullOrEmpty()) {
            logger.severe("no bot token, shutting down")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        JDALogger.setFallbackLoggerEnabled(false)
        val jda = default(botToken, enableCoroutines = true)

        val botModule = module {
            single<Bot> { this@Bot }
//            single<PlayerCounter>(createdAtStart = true) { PlayerCounter() }
            single<InfoBoard> { InfoBoard() }
            single<Billboard>(createdAtStart = true) { Billboard() }
            single<JDA> { jda }
            single<Loggy>(createdAtStart = true) { Loggy() }
        }

        this.jda = jda

        loadModule(botModule)
    }

    override fun onDisable() {
        jda?.shutdown()
    }
}
