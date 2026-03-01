package me.fireballs.brady.bingo

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import com.google.common.collect.MapMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.fireballs.brady.core.*
import me.fireballs.brady.corepgm.event.BradyFootballLifecycleEvent
import me.fireballs.brady.corepgm.forWhom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration.Companion.seconds

private val openSound = soundbox()
    .add(Sound.DOOR_OPEN, 1.25f, 0.6f)

private val closeSound = soundbox()
    .add(Sound.DOOR_CLOSE, 0.75f, 0.2f)

class BingoManager : Listener, KoinComponent {
    private val bingo by inject<Bingo>()
    private val squares by inject<Squares>()

    private val residentSlots = (0..<9 * 5).filter { (2..6).contains(it % 9) }
    private fun bingoMenu(debug: Boolean = false) = menubox(9 * 5) {
        title = "Bingo ⨳"

        squares.grid.forEachIndexed { index, square ->
            if (square == null) return@forEachIndexed

            if (!square.unlockable()) {
                val unlockTime = square.renderUnlockTime()
                addItem(
                    residentSlots[index],
                    itembox(Material.STAINED_GLASS)
                        .name(square.renderName(debug))
                        .loreComponentLines(
                            if (unlockTime != null) listOf(
                                "&bSquare not yet released.".cc(),
                                "&bUnlocks in ${square.renderUnlockTime()}".cc()
                            ) else listOf("&bSquare not yet released.".cc())
                        )
                        .setDamage(8)
                        .build(),
                )
                return@forEachIndexed
            }

            val lines = mutableListOf<Component>()

            val data = player.bingoSquare(square)
            val position = data?.placement
            if (position != null) {
                lines.add("&6&l⨳ &eSquare Filled &6&l⨳".cc())
                lines.add(empty())
            }

            val progress = data?.progress ?: 0
            val discovered = square.discoveredComponent()
            if (square is ProgressSquare && position == null && progress > 0) {
                lines.add("&7Progress: &6≈${((progress / square.count.toDouble()) * 100).toInt()}%".cc())
                if (discovered != null) lines.add(empty())
            }

            if (position != null) {
                lines.add(
                    "&7Your position: &6#$position&7/${square.totalCompletions} (Top ${
                        top(
                            position.toIntOrNull() ?: 1,
                            square.totalCompletions.toInt()
                        )
                    }%)".cc()
                )
                lines.add(empty())
            }

            if (discovered != null) {
                val finderText =
                    if (square.discovered == player.uniqueId) "&6ꕛ &7First Finder: ".cc() else "&7First Finder: ".cc()
                lines.add(finderText + discovered.forWhom(player))
                if (square.totalCompletions > 1)
                    lines.add("&8⤷ &oamong ${square.totalCompletions} total".cc())
            }

            val (matches, domainRestriction) = square.matchesDomainRestriction()
            if (domainRestriction != null) {
                val line = if (matches) "&a&l✔ &a$domainRestriction".cc() else "&cⓘ $domainRestriction".cc()
                lines.add(empty())
                lines.add(line)
            }

            addItem(
                residentSlots[index],
                itembox(Material.WOOL)
                    .name(square.renderName(debug))
                    .loreComponentLines(lines)
                    .setDamage(if (position != null) 5 else 7)
                    .build(),
            )
        }

        cancelClicks()
        addCloseHandler { closeSound.play(viewer) }
    }

    init {
        bingo.registerEvents(this)
        command("bingo") {
            executor = {
                val player = player()
                bingoMenu().open(player)
                openSound.play(player)
            }

            subcommand("debug", permission = "brady.bingo") {
                executor = {
                    val player = player()
                    if (player.name != "KuNet") err("???")
                    bingoMenu(true).open(player)
                    openSound.play(player)
                }
            }
        }
    }

    val dataMap: ConcurrentMap<UUID, BingoData> = MapMaker()
        .concurrencyLevel(4)
        .makeMap()

    @EventHandler(ignoreCancelled = true)
    private fun preJoin(event: AsyncPlayerPreLoginEvent) {
        dataMap[event.uniqueId] = BingoData(event.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    private fun postJoin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        dataMap.remove(event.uniqueId)
    }

    @EventHandler
    private fun onQuit(event: PlayerQuitEvent) {
        dataMap.remove(event.player.uniqueId)
    }

    @EventHandler
    private fun onJoin(event: PlayerJoinEvent) {
        val p = event.player
        bingo.launch(bingo.asyncDispatcher) {
            delay(40.ticks)
            p.send()
            p.send("&6&l⨳ &6Bingo event".cc().command("/bingo"))
            p.send()
            p.send("&eThere's a bingo event happening right now.".cc().command("/bingo"))
            p.send("&eCheck out your progress with &6&n/bingo&e.".cc().command("/bingo"))
            p.send()
            bingoDing.play(p)
        }
    }

    @EventHandler
    private fun bradyLifecycleEvent(event: BradyFootballLifecycleEvent) {
        log(
            "lifecycle",
            event.action.name.c() +
                    " actor: " + (event.actor?.name?.forWhom() ?: "none".c()) +
                    " thrower: " + (event.thrower?.name?.forWhom() ?: "none".c()) +
                    " catcher: " + (event.catcher?.name?.forWhom() ?: "none".c())
        )
    }
}

private object BingoManagerAccessor : KoinComponent {
    val manager by inject<BingoManager>()
}

fun Player.bingoData() =
    BingoManagerAccessor.manager.dataMap[uniqueId]

fun Player.bingoSquare(square: Square) =
    bingoData()?.squareData[square]

class SquareData(
    var placement: String? = null,
    var progress: Int = 0,
)

class BingoData(
    val uuid: UUID,
) : KoinComponent {
    private val bingo by inject<Bingo>()
    private val squares by inject<Squares>()

    val squareData = mutableMapOf<Square, SquareData>()
    val allowed = !bingo.valkeyClient.sismember("bingo:barred", "$uuid")

    fun keyPrefix() = "bingo:player:${uuid}"

    init {
        val squareCompletions = bingo.valkeyClient.hgetAll("${keyPrefix()}-completions")
        val squareProgresses = bingo.valkeyClient.hgetAll("${keyPrefix()}-progress")

        for (square in squares.grid) {
            squareData[square ?: continue] = SquareData(
                squareCompletions[square.squareId],
                squareProgresses[square.squareId]?.toIntOrNull() ?: 0
            )
        }

        bingo.launch(Dispatchers.IO) {
            while (bingo.isEnabled) {
                delay(10.seconds)
                for (square in squares.grid) {
                    square ?: continue
                    square.refreshInfo()
                }
            }
        }
    }
}
