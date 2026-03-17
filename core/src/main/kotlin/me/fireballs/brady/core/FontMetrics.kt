package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.join
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.text.BreakIterator
import java.util.Locale
import kotlin.collections.ArrayDeque
import kotlin.collections.Iterable
import kotlin.collections.contains
import kotlin.collections.lastIndex
import kotlin.collections.mapIndexed
import kotlin.collections.mutableListOf
import kotlin.collections.plusAssign
import kotlin.collections.sum

private val glyphSizes: ByteArray by lazy {
    Core::class.java.classLoader.getResourceAsStream("glyph_sizes.bin")?.readBytes()
        ?: throw IllegalStateException("glyph_sizes.bin could not be read")
}

// special case: if is section symbol, returns -1
fun charWidth(char: Char): Int {
    if (char == '\u00A7') return -1
    if (char == ' ') return 4

    val value = glyphSizes[char.code].toInt() and 0xFF
    if (value == 0) return 0

    var start = value ushr 4
    var end = value and 0x0F

    if (end > 7) {
        end = 15
        start = 0
    }

    end += 1
    return (end - start) / 2 + 1
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
fun breakIntoLines(component: Component, width: Int, locale: Locale = Locale.ROOT): Iterable<Component> {
    val breakIterator = BreakIterator.getLineInstance(locale)

    val totalLines = mutableListOf<Component>()
    val currentLine = mutableListOf<TextComponent>()
    var currentWidth = 0

    fun combineLine() {
        currentWidth = 0
        if (currentLine.isEmpty()) return
        val trimmed = currentLine.mapIndexed { index, component ->
            if (index == currentLine.lastIndex) text(component.content().trim(), component.style())
            else component
        }
        totalLines += join(JoinConfiguration.noSeparators(), trimmed)
        currentLine.clear()
    }

    val listener = object : FlattenerListener {
        val styleStack = ArrayDeque<Style>()

        override fun component(text: String) {
            val currentStyle = styleStack.lastOrNull() ?: Style.empty()
            val bolded = currentStyle.decorations().contains(TextDecoration.BOLD)

            breakIterator.setText(text)
            var start = breakIterator.first()
            var end = breakIterator.next()
            while (end != BreakIterator.DONE) {
                val chunk = text.substring(start, end)
                if (chunk.isNotBlank()) {
                    val partWidth = internalStringWidth(chunk, bolded)
                    if (currentWidth + partWidth > width) combineLine()

                    currentWidth += partWidth
                    currentLine.add(text(chunk, currentStyle))
                }

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

fun breakIntoLines(text: String, width: Int, useAmpersand: Boolean = true, locale: Locale = Locale.ROOT) =
    breakIntoLines(text.cc(useAmpersand), width, locale)
