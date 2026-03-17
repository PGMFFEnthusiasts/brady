package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.*
import net.kyori.adventure.text.JoinConfiguration.noSeparators
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.text.BreakIterator
import java.util.*

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

fun breakIntoLinesWithWidths(
    component: Component,
    width: Int,
    locale: Locale = Locale.ROOT
): Iterable<Pair<Component, Int>> {
    val styleList = mutableListOf<Style>()
    val aggregateText = StringBuilder()

    val listener = object : FlattenerListener {
        var aggregateStyle = Style.empty()

        override fun component(text: String) {
            aggregateText.append(text)
            repeat(text.length) { styleList.add(aggregateStyle) }
        }

        override fun pushStyle(style: Style) {
            aggregateStyle = aggregateStyle.merge(style)
        }

        override fun popStyle(style: Style) {
            aggregateStyle = aggregateStyle.unmerge(style)
        }
    }

    // hail mary call to render to TextComponents
    val converted = Core.componentRenderFn(component)
    ComponentFlattener
        .textOnly()
        .flatten(converted, listener)

    val breakIterator = BreakIterator.getLineInstance(locale)
    val fullText = aggregateText.toString()

    // break "lines" into spans (including hiding newlines)
    val spans = mutableListOf<Pair<Int, Int>>()
    breakIterator.setText(fullText)
    var start = breakIterator.first()
    var end = breakIterator.next()
    while (end != BreakIterator.DONE) {
        var spanStart = start

        for (i in start..<end) {
            if (fullText[i] != '\n') continue
            if (spanStart < i) spans += spanStart to i
            spans += i to i
            spanStart = i + 1
        }

        if (spanStart < end) spans += spanStart to end

        start = end
        end = breakIterator.next()
    }

    val totalLines = mutableListOf<Pair<Component, Int>>()
    val currentLine = mutableListOf<TextComponent>()
    var currentWidth = 0

    fun pushLine() {
        // hacky trim. acceptable though :)
        while (currentLine.isNotEmpty()) {
            val last = currentLine.last()
            if (last.content() != " ") break
            currentWidth -= internalCharWidth(' ', last.hasDecoration(TextDecoration.BOLD))
            currentLine.removeLast()
        }

        totalLines += join(noSeparators(), currentLine).compact() to currentWidth
        currentLine.clear()
        currentWidth = 0
    }

    for ((start, end) in spans) {
        // special case: made newline spans 0 to make this easier
        if (start == end) {
            pushLine()
            continue
        }

        val currentWord = mutableListOf<TextComponent>()
        var wordWidth = 0
        for (i in start..<end) {
            val character = fullText[i]
            val style = styleList[i]
            wordWidth += internalCharWidth(character, style.hasDecoration(TextDecoration.BOLD))
            currentWord += text(character, style)
        }

        if (currentWidth + wordWidth > width) pushLine()

        currentWidth += wordWidth
        currentLine.addAll(currentWord)
    }

    if (currentLine.isNotEmpty()) pushLine()

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
