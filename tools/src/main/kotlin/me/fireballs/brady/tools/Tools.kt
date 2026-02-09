package me.fireballs.brady.tools

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import me.fireballs.brady.core.loadModule
import me.fireballs.brady.core.registerPacketEvents
import me.fireballs.brady.deps.PluginAnnotation
import org.koin.dsl.module

@PluginAnnotation
class Tools : SuspendingJavaPlugin() {
    override fun onEnable() {
        val toolModule = module {
            single<Tools> { this@Tools }
//            single<Drafting>(createdAtStart = true) { Drafting() }
            single<FlagFootballMaps>(createdAtStart = true) { FlagFootballMaps() }
            single<Splat>(createdAtStart = true) { Splat() }
            single<Ready>(createdAtStart = true) { Ready() }
            single<BallCam>(createdAtStart = true) { BallCam() }
            single<BallProjection>(createdAtStart = true) { BallProjection() }
            single<TheCube>(createdAtStart = true) { TheCube() }
            single<VoidKill>(createdAtStart = true) { VoidKill() }
            single<Pause>(createdAtStart = true) { Pause() }
            single<HT>(createdAtStart = true) { HT() }
            single<Tournaments>(createdAtStart = true) { Tournaments() }
            single<JumpResetParticles>(createdAtStart = true) { JumpResetParticles() }
            single<ACR>(createdAtStart = true) { ACR() }
            single<Benched>(createdAtStart = true) { Benched() }
            single<Colorizer>(createdAtStart = true) { Colorizer() }
            single<Ping>(createdAtStart = true) { Ping() }
            single<RespawnBugFix>(createdAtStart = true) { RespawnBugFix() }
        }

        loadModule(toolModule)

        registerPacketEvents(TabListResize())
    }
}
