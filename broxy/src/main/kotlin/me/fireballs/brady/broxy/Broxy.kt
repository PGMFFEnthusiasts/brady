package me.fireballs.brady.broxy

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import me.fireballs.brady.broxy.listeners.BetterMOTD
import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.velocitypowered.api.plugin.PluginContainer
import dev.minn.jda.ktx.jdabuilder.default
import me.fireballs.brady.broxy.listeners.Router
import me.fireballs.brady.broxy.listeners.Status
import me.fireballs.brady.broxy.loggy.Loggy
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger

@Suppress("ConvertSecondaryConstructorToPrimary", "unused")
@Plugin(id = "broxy", name = "Broxy", version = "12.0.0-TB", authors = ["Tom Brady"])
class Broxy {
    val server: ProxyServer
    val logger: Logger
    val pluginContainer: PluginContainer

    @Inject
    constructor(
        suspendingPluginContainer: SuspendingPluginContainer,
        server: ProxyServer,
        logger: Logger,
    ) {
        suspendingPluginContainer.initialize(this)

        this.server = server
        this.logger = logger
        this.pluginContainer = suspendingPluginContainer.pluginContainer
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        server.eventManager.register(this, BetterMOTD(this))
        server.eventManager.register(this, Router(this))

        val botToken = System.getenv("BRADY_BOT_TOKEN")
        if (botToken.isNullOrEmpty()) {
            logger.error("No bot token... :( sad!")
            return
        }

        JDALogger.setFallbackLoggerEnabled(false)
        val jda = default(botToken, enableCoroutines = true)
        Loggy(this, jda)
        server.eventManager.register(this, Status(this, jda))
    }
}
