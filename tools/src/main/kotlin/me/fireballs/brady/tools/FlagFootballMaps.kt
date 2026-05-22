package me.fireballs.brady.tools

import me.fireballs.brady.core.command
import me.fireballs.brady.core.itembox
import me.fireballs.brady.core.menubox
import me.fireballs.brady.corepgm.currentMatch
import me.fireballs.brady.corepgm.forWhom
import me.fireballs.brady.corepgm.isTouchdown
import org.bukkit.Material
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.map.MapOrder
import tc.oc.pgm.cycle.CycleMatchModule
import tc.oc.pgm.util.StringUtils
import tc.oc.pgm.util.named.MapNameStyle
import kotlin.math.ceil

class FlagFootballMaps : KoinComponent {
    private val pgm by inject<PGM>()
    private val mapOrder by inject<MapOrder>()

    private fun ffMaps() = pgm.mapLibrary.maps.asSequence()
        .filter { it.isTouchdown() }
        .sortedBy { it.name }
        .toList()

    private val menu = menubox(9 * ceil(ffMaps().size / 9.toDouble()).toInt()) {
        title = "Touchdown Maps"
        cancelClicks()
        ffMaps().forEachIndexed { index, map ->
            val item = itembox(if (map.name.contains("Bowl")) Material.BOWL else Material.PAPER)
                .name(map.getStyledName(MapNameStyle.COLOR))
                .loreComponentLines(map.authors.map { it.name.forWhom(player) })
                .build()
            addClickItem(index, item) {
                mapOrder.nextMap = map
                pgm.matchManager
                    .currentMatch()
                    ?.needModule(CycleMatchModule::class.java)
                    ?.cycleNow()
                close()
            }
        }
    }

    init {
        command("ff", permission = "brady.ff", aliases = arrayOf("ffm")) {
            tabCompleter {
                StringUtils.complete(
                    StringUtils.textToSuggestion(subArgs.joinToString(" ")),
                    ffMaps().map { StringUtils.textToSuggestion(it.name) }
                )
            }

            executor {
                if (subArgs.isNotEmpty()) {
                    val query = StringUtils.suggestionToText(subArgs.joinToString(" "))
                    val map = StringUtils.bestFuzzyMatch(query, ffMaps()) { it.name }
                    if (map == null) err("Map not found!")
                    mapOrder.nextMap = map
                    pgm.matchManager
                        .currentMatch()
                        ?.needModule(CycleMatchModule::class.java)
                        ?.cycleNow()
                    return@executor
                }

                menu.open(player())
            }
        }
    }
}