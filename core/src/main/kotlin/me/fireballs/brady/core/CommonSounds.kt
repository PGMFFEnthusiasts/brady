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
