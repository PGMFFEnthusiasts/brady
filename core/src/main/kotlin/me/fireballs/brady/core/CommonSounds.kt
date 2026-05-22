package me.fireballs.brady.core

import org.bukkit.Sound

val uhOh = soundbox()
    .add(Sound.NOTE_PLING, 0.65f)
    .add(Sound.NOTE_PLING, 0.8f)
    .add(Sound.NOTE_PLING, 0.9f)
    .add(2, Sound.NOTE_PLING, 0.65f)
    .add(Sound.NOTE_PLING, 0.8f)
    .add(Sound.NOTE_PLING, 0.9f)

val okay = soundbox()
    .add(Sound.SUCCESSFUL_HIT, 1.5f)
    .add(2, Sound.SUCCESSFUL_HIT, 1.2f)

val thud = soundbox()
    .add(Sound.DIG_GRASS, 1.75f, 10f)
    .add(Sound.DIG_SNOW, 1.75f, 10f)
    .add(Sound.NOTE_BASS, 0.5f, 10f)
