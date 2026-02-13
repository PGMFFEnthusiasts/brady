package me.fireballs.brady.corepgm

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import tc.oc.pgm.util.player.PlayerComponent.player

fun CommandSender.component(): Component = player(this)
