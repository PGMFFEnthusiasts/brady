package me.fireballs.brady.core

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.fireballs.brady.core.data.SoundKeys
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.CraftSound
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// backported from boxes

enum class SoundMode {
    PERVASIVELY,
    AT_AUDIENCE,
}

data class TimedSound(
    val sound: Sound,
    val delayBefore: Int = 0,
)

private const val k = 1_000_000f
private const val r = 16f
private const val maxVolume = 0.99999f

// yields the correct volume should the source be distance blocks away
private fun computeVolume(desiredVolume: Float, dist: Float = k) =
    dist / (r * (1f - desiredVolume.coerceIn(0f, maxVolume)))

class Soundbox(
    val soundMode: SoundMode = SoundMode.PERVASIVELY,
    val defaultVolume: Float = 1f,
    val soundList: MutableList<TimedSound> = mutableListOf(),
) : KoinComponent {
    private val core by inject<Core>()

    fun add(sound: SoundKeys, pitch: Float = 1f, volume: Float = defaultVolume) = add(0, sound, pitch, volume)

    fun add(offset: Int, sound: SoundKeys, pitch: Float = 1f, volume: Float = defaultVolume) =
        add(offset, sound.toSound(pitch, volume))

    fun add(sound: org.bukkit.Sound, pitch: Float = 1f, volume: Float = defaultVolume) = add(0, sound, pitch, volume)

    fun add(offset: Int, sound: org.bukkit.Sound, pitch: Float = 1f, volume: Float = defaultVolume) = add(
        offset,
        Sound.sound(Key.key(CraftSound.getSound(sound)), Sound.Source.MASTER, volume, pitch),
    )

    fun add(sound: Sound) = add(0, sound)

    fun add(offset: Int, sound: Sound): Soundbox {
        soundList += TimedSound(sound, offset)
        return this
    }

    fun play(sender: CommandSender, position: Vector? = null): Job? {
        if (sender !is Player) return null
        val adventure = Core.adventure.sender(sender)
        val sl = soundList.toList()
        if (sl.isEmpty()) return null

        return core.launch {
            for (info in sl) {
                if (info.delayBefore > 0) delay(info.delayBefore.ticks)
                if (!sender.isOnline) return@launch

                if (position != null) {
                    adventure.playSound(info.sound, position.x, position.y, position.z)
                    continue
                }

                val sl = sender.eyeLocation
                when (soundMode) {
                    SoundMode.PERVASIVELY -> {
                        val modifiedSound = Sound.sound(
                            info.sound.name(),
                            info.sound.source(),
                            computeVolume(info.sound.volume()),
                            info.sound.pitch(),
                        )
                        adventure.playSound(modifiedSound, sl.x, sl.y + k.toDouble(), sl.z)
                    }
                    SoundMode.AT_AUDIENCE -> adventure.playSound(info.sound, sl.x, sl.y, sl.z)
                }
            }
        }
    }

    fun play(senders: Iterable<CommandSender>, position: Vector? = null) =
        senders.map { play(it, position) }

    fun broadcast(world: World) =
        play(world.players)
}

fun soundbox(soundMode: SoundMode = SoundMode.PERVASIVELY, defaultVolume: Float = 1f) =
    Soundbox(soundMode, defaultVolume)
