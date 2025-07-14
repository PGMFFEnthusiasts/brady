package me.fireballs.brady.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.ansi.ColorLevel

private val ansiSerializer = ANSIComponentSerializer.builder().colorLevel(ColorLevel.INDEXED_8).build()
fun ansify(component: Component): String {
    val firstSerialized = LegacyComponentSerializer.legacySection().serialize(component.forWhom())
    val reserialized = LegacyComponentSerializer.legacySection().deserialize(firstSerialized)
    return ansiSerializer.serialize(reserialized)
}
