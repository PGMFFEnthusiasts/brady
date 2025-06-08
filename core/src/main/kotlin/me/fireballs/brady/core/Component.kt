package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import tc.oc.pgm.util.text.ComponentRenderer
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val legacyAmpersand = LegacyComponentSerializer.legacyAmpersand()
private val legacySection = LegacyComponentSerializer.legacySection()
fun String.cc(useAmpersand: Boolean = true) = (if (useAmpersand) legacyAmpersand else legacySection).deserialize(this)
fun String.c() = Component.text(this)

operator fun Component.plus(other: Component) =
    Component.join(JoinConfiguration.noSeparators(), this, other)

operator fun Component.plus(other: String) =
    Component.join(JoinConfiguration.noSeparators(), this, other.cc())

fun Component.forWhom(sender: CommandSender = Bukkit.getConsoleSender()) =
    ComponentRenderer.RENDERER.render(this, Core.adventure.sender(sender))

fun Component.url(url: String) = this.clickEvent(ClickEvent.openUrl(url))
fun Component.command(command: String) = this.clickEvent(ClickEvent.runCommand(command))
fun Component.fill(command: String) = this.clickEvent(ClickEvent.suggestCommand(command))
fun Component.hover(component: Component) = this.hoverEvent(component)

private val plainTextComponentSerializer = PlainTextComponentSerializer.plainText()
fun Component.plainText() = plainTextComponentSerializer.serialize(this)
fun Component.coloredText() = legacySection.serialize(this)

fun String.colorLegacy(char: Char = '&'): String = ChatColor.translateAlternateColorCodes(char, this)

fun titleTimes(fadeIn: Duration, stay: Duration, fadeOut: Duration) =
    Title.Times.times(
        fadeIn.toJavaDuration(),
        stay.toJavaDuration(),
        fadeOut.toJavaDuration()
    )
