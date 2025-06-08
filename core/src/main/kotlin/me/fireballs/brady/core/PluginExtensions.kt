package me.fireballs.brady.core

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

fun SuspendingJavaPlugin.registerEvents(listener: Listener) {
    Bukkit.getPluginManager().registerSuspendingEvents(listener, this)
}

fun SuspendingJavaPlugin.registerPacketEvents(listener: PacketListenerCommon) {
    PacketEvents.getAPI().eventManager.registerListener(listener)
}

fun SuspendingJavaPlugin.loadModule(module: Module) {
    KoinPlatformTools.defaultContext().loadKoinModules(module, true)
}
