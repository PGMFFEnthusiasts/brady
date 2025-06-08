package me.fireballs.brady.tools

import me.fireballs.brady.core.*
import org.bukkit.Material
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.cycle.CycleMatchModule
import tc.oc.pgm.util.StringUtils
import tc.oc.pgm.util.named.MapNameStyle

class FlagFootballMaps : KoinComponent {
    private val pgm by inject<PGM>()
    private val mapOrder by inject<MapOrder>()

    private val ffMaps = lazy {
        pgm.mapLibrary.maps.asSequence()
            .filter { it.name.contains("Brady") || it.name.contains("Bowl") }
            .sortedBy { it.name }
            .toList()
    }

    private val menu = menubox(9 * (ffMaps.value.size / 9).coerceAtLeast(1)) {
        title = "Flag Football Maps"
        cancelClicks()
        ffMaps.value.forEachIndexed { index, map ->
            val item = itembox(if (map.name.contains("Bowl")) Material.BOWL else Material.PAPER)
                .name(map.getStyledName(MapNameStyle.COLOR))
                .loreComponentLines(map.authors.map { it.name.forWhom(player) })
                .build()
            addClickItem(index, item) {
                mapOrder.nextMap = map
                pgm.matchManager
                    .currentMatch()
                    .needModule(CycleMatchModule::class.java)
                    .cycleNow()
                close()
            }
        }
    }

    init {
        command("ff", permission = "brady.ff", aliases = arrayOf("ffm")) {
            tabCompleter {
                StringUtils.complete(
                    StringUtils.textToSuggestion(subArgs.joinToString(" ")),
                    ffMaps.value.map { StringUtils.textToSuggestion(it.name) }
                )
            }

            executor {
                if (subArgs.isNotEmpty()) {
                    val query = StringUtils.suggestionToText(subArgs.joinToString(" "))
                    val map = StringUtils.bestFuzzyMatch(query, ffMaps.value) { it.name }
                    if (map == null) err("Map not found!")
                    mapOrder.nextMap = map
                    pgm.matchManager
                        .currentMatch()
                        .needModule(CycleMatchModule::class.java)
                        .cycleNow()
                    return@executor
                }

                menu.open(player())
            }
        }
    }
}