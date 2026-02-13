package me.fireballs.brady.corepgm

import me.fireballs.brady.core.Core
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import tc.oc.pgm.util.text.ComponentRenderer

fun Component.forWhom(sender: CommandSender = Bukkit.getConsoleSender()) =
    ComponentRenderer.RENDERER.render(this, Core.adventure.sender(sender))
