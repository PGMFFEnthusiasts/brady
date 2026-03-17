package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.*
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.text.BreakIterator
import java.util.Locale
import kotlin.collections.ArrayDeque

private val glyphSizes: ByteArray by lazy {
    // note: this is NOT glyph_sizes.bin; this acts slightly differently
    Core::class.java.classLoader.getResourceAsStream("widths.bin")?.readBytes()
        ?: throw IllegalStateException("widths.bin could not be read")
}

// special case: if is section symbol, returns -1
fun charWidth(char: Char): Int {
    if (char == '\u00A7') return -1
    if (char == ' ') return 4

    val value = glyphSizes[char.code].toInt() and 0xFF
    if (value == 0) return 0

    val start = value ushr 4
    val end = value and 0x0F

    return (end + 1 - start) / 2 + 1
}

fun stringWidth(text: CharSequence?): Int {
    text ?: return 0

    var width = 0
    var bold = false
    var i = 0

    while (i < text.length) {
        var charWidth = charWidth(text[i])
        if (charWidth < 0 && i < text.length - 1) {
            i++
            when (text[i]) {
                'l', 'L' -> bold = true
                'r', 'R' -> bold = false
            }
            charWidth = 0
        }

        width += charWidth
        if (bold && charWidth > 0) ++width

        i++
    }

    return width
}

private fun internalCharWidth(char: Char, bold: Boolean): Int {
    if (char == '\u00A7') return 0 // lost cause
    var width = charWidth(char)
    if (bold && width > 0) ++width
    return width
}

private fun internalStringWidth(text: CharSequence, bold: Boolean) =
    text.map { internalCharWidth(it, bold) }.sum()

// todo: make inserting newlines possible
// todo: fix edge case of components not being flattened before broken (will require some hard work to fix)
fun breakIntoLinesWithWidths(
    component: Component,
    width: Int,
    locale: Locale = Locale.ROOT
): Iterable<Pair<Component, Int>> {
    val breakIterator = BreakIterator.getLineInstance(locale)

    val totalLines = mutableListOf<Pair<Component, Int>>()
    val currentLine = mutableListOf<TextComponent>()
    var currentWidth = 0

    fun combineLine() {
        if (currentLine.isEmpty()) return
        var deltaWidth = 0
        val trimmed = currentLine.mapIndexed { index, component ->
            if (index == currentLine.lastIndex) {
                val oc = component.content()
                val trimmedText = oc.trim()
                if (trimmedText != oc) deltaWidth = -internalStringWidth(
                    oc.removePrefix(trimmedText),
                    component.style().hasDecoration(TextDecoration.BOLD)
                )
                text(trimmedText, component.style())
            } else component
        }
        currentWidth += deltaWidth
        totalLines += join(JoinConfiguration.noSeparators(), trimmed) to currentWidth
        currentWidth = 0
        currentLine.clear()
    }

    val listener = object : FlattenerListener {
        val styleStack = ArrayDeque<Style>()

        override fun component(text: String) {
            var currentStyle = Style.empty()
            for (style in styleStack) currentStyle = currentStyle.merge(style)
            val bolded = currentStyle.hasDecoration(TextDecoration.BOLD)

            breakIterator.setText(text)
            var start = breakIterator.first()
            var end = breakIterator.next()
            while (end != BreakIterator.DONE) {
                val chunk = text.substring(start, end)
                val partWidth = internalStringWidth(chunk, bolded)
                if (currentWidth + partWidth > width) combineLine()

                currentWidth += partWidth
                currentLine.add(text(chunk, currentStyle))

                start = end
                end = breakIterator.next()
            }
        }

        override fun pushStyle(style: Style) {
            styleStack.addLast(style)
        }

        override fun popStyle(style: Style) {
            styleStack.removeLast()
        }
    }

    // hail mary call to render to TextComponents
    val converted = Core.componentRenderFn(component)
    ComponentFlattener
        .textOnly()
        .flatten(converted, listener)

    combineLine()

    return totalLines
}

fun breakIntoLines(component: Component, width: Int, locale: Locale = Locale.ROOT): Iterable<Component> =
    breakIntoLinesWithWidths(component, width, locale).map { it.first }

fun breakIntoLines(text: String, width: Int, useAmpersand: Boolean = true, locale: Locale = Locale.ROOT) =
    breakIntoLines(text.cc(useAmpersand), width, locale)

fun computePaddingOfSize(size: Int): Component {
    if (size < 1) return empty()
    val sb = StringBuilder()
    var style = Style.empty()
    var remaining = size
    val leftovers = remaining % 4
    if (leftovers != 0) {
        style = Style.style(NamedTextColor.DARK_GRAY)
        sb.append("⁚".repeat(leftovers))
        remaining -= leftovers
    }
    sb.append(" ".repeat(remaining / 4))
    return text(sb.toString(), style)
    // use to debug
    // return text("⁚".repeat(size), NamedTextColor.DARK_GRAY)
}

fun justifyCenter(component: Component, width: Int, locale: Locale = Locale.ROOT): Iterable<Component> =
    breakIntoLinesWithWidths(component, width / 2, locale)
        .map { (component, computedWidth) -> computePaddingOfSize((width - computedWidth) / 2) + component }

fun justifyRight(component: Component, width: Int, locale: Locale = Locale.ROOT): Iterable<Component> =
    breakIntoLinesWithWidths(component, width, locale)
        .map { (component, computedWidth) -> computePaddingOfSize(width - computedWidth) + component }
