package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.core.registerPacketEvents
import me.fireballs.brady.core.serviceOf
import me.fireballs.brady.deps.PluginAnnotation
import me.fireballs.brady.tools.pvpfx.Dayvision
import me.fireballs.brady.tools.pvpfx.HideArmor
import me.fireballs.brady.tools.pvpfx.ProjectileSkin
import org.koin.dsl.module

@PluginAnnotation
class Tools : SuspendingJavaPlugin() {
    override fun onEnable() {
        val toolModule = module {
            single<Tools> { this@Tools }
            // serviceOf(::Drafting)
            serviceOf(::FlagFootballMaps)
            serviceOf(::Splat)
            serviceOf(::Ready)
            serviceOf(::BallCam)
            serviceOf(::BallProjection)
            serviceOf(::TheCube)
            serviceOf(::VoidKill)
            serviceOf(::Pause)
            serviceOf(::HT)
            serviceOf(::Tournaments)
            serviceOf(::JumpResetParticles)
            serviceOf(::ACR)
            serviceOf(::Benched)
            serviceOf(::Colorizer)
            serviceOf(::Ping)
            serviceOf(::RespawnBugFix)
            serviceOf(::ToolsSettings)
            serviceOf(::HideArmor)
            serviceOf(::ProjectileSkin)
            serviceOf(::Dayvision)
        }

        loadModule(toolModule)

        registerPacketEvents(TabListResize())
    }
}
