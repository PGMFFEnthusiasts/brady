package me.fireballs.brady.broxy

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.google.inject.Inject
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.minn.jda.ktx.jdabuilder.default
import me.fireballs.brady.broxy.listeners.ApolloChatLength
import me.fireballs.brady.broxy.listeners.BetterMOTD
import me.fireballs.brady.broxy.listeners.Router
import me.fireballs.brady.broxy.listeners.Status
import me.fireballs.brady.broxy.listeners.StatusPull
import me.fireballs.brady.broxy.loggy.Loggy
import me.fireballs.brady.broxy.tournament.TournamentStateManager
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
        server.eventManager.register(this, ApolloChatLength)

        val status = StatusPull(this)
        server.eventManager.register(this, status)
        val tournamentStateManager = TournamentStateManager(this)
        server.eventManager.register(this, tournamentStateManager)

        val botToken = System.getenv("BRADY_BOT_TOKEN")
        if (botToken.isNullOrEmpty()) {
            logger.error("No bot token... :( sad!")
            return
        }

        JDALogger.setFallbackLoggerEnabled(false)
        val jda = default(botToken, enableCoroutines = true)
        Loggy(this, jda)
        server.eventManager.register(this, Status(this, jda))

        val commandManager = server.commandManager
        for (registeredServer in server.allServers) {
            commandManager.register(
                commandManager.metaBuilder(registeredServer.serverInfo.name)
                    .plugin(this)
                    .build(),
                InstantTransferCommand(server, registeredServer)
            )
        }
    }

    class InstantTransferCommand(val proxyServer: ProxyServer, val registeredServer: RegisteredServer) : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            if (source !is Player) return
            source.createConnectionRequest(registeredServer).fireAndForget()
        }
    }
}
