package me.fireballs.brady.deps

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.settings.PacketEventsSettings
import de.tr7zw.changeme.nbtapi.NBT
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@PluginAnnotation
class Hello : JavaPlugin() {
    override fun onLoad() {
        @Suppress("UnstableApiUsage", "DEPRECATION")
        val settings = PacketEventsSettings()
            .checkForUpdates(false)
            .bStats(false)
            .reEncodeByDefault(true)
        SpigotPacketEventsBuilder.build(this, settings).let {
            PacketEvents.setAPI(it)
            it.load()
        }
    }

    override fun onEnable() {
        PacketEvents.getAPI().let {
            @Suppress("UnstableApiUsage", "DEPRECATION")
            it.settings
                .checkForUpdates(false)
                .bStats(false)
                .reEncodeByDefault(true)
            it.init()
        }

        if (!NBT.preloadApi()) {
            logger.warning("Improper NBT API loading, shutting down")
            Bukkit.shutdown()
            return
        }

        logger.info("Dependencies loaded!")
    }
}
