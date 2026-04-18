package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import net.md_5.bungee.api.ChatColor
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val legacyAmpersand = LegacyComponentSerializer.legacyAmpersand()
private val legacySection = LegacyComponentSerializer.legacySection()
fun String.cc(useAmpersand: Boolean = true) = (if (useAmpersand) legacyAmpersand else legacySection).deserialize(this)
fun String.c() = Component.text(this)

// only color, not anything else
fun String.c(color: Char): TextComponent {
    val namedColor = when(color) {
        '0' -> NamedTextColor.BLACK
        '1' -> NamedTextColor.DARK_BLUE
        '2' -> NamedTextColor.DARK_GREEN
        '3' -> NamedTextColor.DARK_AQUA
        '4' -> NamedTextColor.DARK_RED
        '5' -> NamedTextColor.DARK_PURPLE
        '6' -> NamedTextColor.GOLD
        '7' -> NamedTextColor.GRAY
        '8' -> NamedTextColor.DARK_GRAY
        '9' -> NamedTextColor.BLUE
        'a' -> NamedTextColor.GREEN
        'b' -> NamedTextColor.AQUA
        'c' -> NamedTextColor.RED
        'd' -> NamedTextColor.LIGHT_PURPLE
        'e' -> NamedTextColor.YELLOW
        'f' -> NamedTextColor.WHITE
        else -> return this.c()
    }

    return this.c().color(namedColor)
}

operator fun Component.plus(other: Component) =
    Component.join(JoinConfiguration.noSeparators(), this, other)

operator fun Component.plus(other: String) =
    Component.join(JoinConfiguration.noSeparators(), this, other.cc())

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
