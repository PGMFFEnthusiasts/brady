package me.fireballs.brady.core

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.platform.facet.FacetAudienceProvider
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.command.CraftConsoleCommandSender
import java.util.function.Consumer

class DummyConsoleSender(
    val dispatchHandlers: MutableList<Consumer<String>> = mutableListOf(),
) : CraftConsoleCommandSender() {
    override fun sendRawMessage(message: String?) {
        super.sendRawMessage(message)
        if (message != null) dispatchHandlers.forEach { it.accept(message) }
    }
}

private val alreadyInjectedAudiences = mutableMapOf<BukkitAudiences, DummyConsoleSender>()

@Suppress("UnstableApiUsage")
fun addConsoleForwarding(provider: BukkitAudiences, onMessage: Consumer<String>) {
    val dummy = alreadyInjectedAudiences.getOrPut(provider) {
        @Suppress("UNCHECKED_CAST", "UNCHECKED_CAST")
        val fap = provider as FacetAudienceProvider<CommandSender, *>
        val console = DummyConsoleSender()

        fap.removeViewer(Bukkit.getServer().consoleSender)
        fap.addViewer(console)

        console
    }

    dummy.dispatchHandlers.add(onMessage)
}
