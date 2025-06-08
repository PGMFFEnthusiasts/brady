package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.delay
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.CraftSound
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SoundPair(val offset: Int, val sound: Sound)

fun soundbox() = Soundbox()

class Soundbox(
    val soundList: MutableList<SoundPair> = mutableListOf(),
) : KoinComponent {
    private val app by inject<Core>()

    fun add(offset: Int, sound: org.bukkit.Sound, pitch: Float = 1f, volume: Float = 1000f): Soundbox {
        soundList += SoundPair(offset, Sound.sound(Key.key(CraftSound.getSound(sound)), Sound.Source.MASTER, volume, pitch))
        return this
    }

    fun add(sound: org.bukkit.Sound, pitch: Float = 1f, volume: Float = 1000f) = add(0, sound, pitch, volume)

    fun play(sender: CommandSender) {
        if (sender !is Player) return

        app.launch {
            for (soundPair in soundList) {
                if (soundPair.offset.ticks > 0) delay(soundPair.offset.ticks)
                if (!sender.isOnline) return@launch

                Core.adventure.sender(sender).playSound(soundPair.sound)
            }
        }
    }

    fun broadcast(world: World) {
        world.players.forEach { play(it) }
    }
}
