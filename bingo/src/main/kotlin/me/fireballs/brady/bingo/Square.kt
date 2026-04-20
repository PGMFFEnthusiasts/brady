package me.fireballs.brady.bingo

import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.Dispatchers
import me.fireballs.brady.core.*
import me.fireballs.brady.corepgm.component
import me.fireballs.brady.corepgm.currentMatch
import me.fireballs.brady.corepgm.forWhom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.api.match.MatchManager
import tc.oc.pgm.api.player.MatchPlayer
import tc.oc.pgm.teams.TeamMatchModule
import tc.oc.pgm.util.named.NameStyle
import tc.oc.pgm.util.player.PlayerComponent
import java.time.Duration
import java.util.*

open class Square(
    val squareName: String,
    val squareIndex: Int,
) : Listener, KoinComponent {
    protected val bingo by inject<Bingo>()
    protected val matchManager by inject<MatchManager>()

    val squareId = indexToId(squareIndex)
    protected val zeroUUID = UUID(0, 0)

    fun keyPrefix() = "bingo:$squareId"

    var unlockedBy = 0L
        private set

    fun unlockable(): Boolean {
        if (unlockedBy == 0L) return false
        return System.currentTimeMillis() >= unlockedBy
    }

    var discovered: UUID? = null
        private set

    private fun discover(discoverer: UUID) {
        if (discovered != null) return
        discovered = discoverer
        bingo.launch(Dispatchers.IO) {
            bingo.valkeyClient.set("${keyPrefix()}-discovered", discoverer.toString())
        }
    }

    var nameRevealedBy = 0L
        private set

    var totalCompletions = bingo.valkeyClient.llen("${keyPrefix()}-completions")

    fun refreshInfo() {
        unlockedBy = bingo.valkeyClient.get("${keyPrefix()}-unlocked")?.toLongOrNull() ?: 0
        discovered = bingo.valkeyClient.get("${keyPrefix()}-discovered")?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
        nameRevealedBy = bingo.valkeyClient.get("${keyPrefix()}-name")?.toLongOrNull() ?: 0
    }

    fun discoveredComponent(): Component? {
        val d = discovered ?: return null
        if (d == zeroUUID) return "&6Several players".cc()
        return PlayerComponent.player(d, NameStyle.FANCY)
    }

    open fun renderName(forceDeobfuscation: Boolean = false): Component {
        val revealedName = nameRevealedBy != 0L && System.currentTimeMillis() >= nameRevealedBy
        if (forceDeobfuscation || revealedName || totalCompletions >= 5) return "&b$squareName ($squareId)".cc()
        return squareName.obfuscateX().cc() + " &b($squareId)".cc()
    }

    fun renderUnlockTime(): String? {
        if (unlockable()) return "no time at all"
        if (unlockedBy == 0L) return null
        return formatDuration(Duration.ofMillis(unlockedBy - System.currentTimeMillis()))
    }

    init {
        bingo.registerEvents(this)
        refreshInfo()
    }

    protected fun player(player: Player): MatchPlayer? =
        matchManager.getPlayer(player)

//    fun matchesDomainRestriction(): Boolean {
//        val match = matchManager.currentMatch() ?: return false
//        if (!match.isRunning) return false
//        val teams = match.getModule(TeamMatchModule::class.java) ?: return false
//        if (teams.participatingTeams.size != 2) return false
//        return teams.participatingTeams.all { it.party.players.size >= 2 }
//    }

    protected fun playerFloorOf(minInclusive: Int, customReason: String? = null): Pair<Boolean, String?> {
        val reason = customReason ?: "Requires ≥$minInclusive players playing"
        val match = matchManager.currentMatch() ?: return false to reason
        if (!match.isRunning) return false to reason
        return (match.players.count { it.isParticipating } >= minInclusive) to reason
    }

    open fun matchesDomainRestriction(): Pair<Boolean, String?> = true to null

    fun complete(players: List<Player>) {
        val commasPlayers = Component.join(JoinConfiguration.commas(true), players.map { it.component().forWhom() })
        log("bingo", "completion of ".c() + renderName(true) + " by ".c() + commasPlayers)
//        if (!matchesDomainRestriction().first) return
        if (!unlockable()) return
        val ps = players
            .filter { it.bingoData()?.allowed == true }
            .mapNotNull { key -> key.bingoSquare(this)?.let { key to it } }
            .filter { (_, data) -> data.placement == null }
            .toMap()
        if (ps.isEmpty()) return

        val who = if (ps.size == 1) ps.keys.first().component() else "&6Several players".cc()
        matchManager.currentMatch()?.sendMessage(
            ("&6&l⨳ ".cc() + who + "&7 filled " + renderName() + "&7.").command("/bingo")
        )

        var firstPlace = false
        if (discovered == null) {
            firstPlace = true
            matchManager.currentMatch()?.sendMessage(
                "&7⤷ &d${kaomoji.random()} &7This is the first ever fill of this square!".cc()
            )
            discover(if (ps.size == 1) ps.keys.first().uniqueId else zeroUUID)
        }

        for ((_, square) in ps) {
            square.placement = if (firstPlace) "1" else "?"
        }

        bingo.launch(Dispatchers.IO) {
            for ((player, square) in ps) {
                val place = bingo.valkeyClient.lpush("${keyPrefix()}-completions", player.uniqueId.toString())
                if (!firstPlace) square.placement = place.toString()
                bingo.valkeyClient.hset(
                    "bingo:player:${player.uniqueId}-completions",
                    squareId,
                    if (firstPlace) "1" else place.toString()
                )
            }

            totalCompletions = bingo.valkeyClient.llen("${keyPrefix()}-completions")
        }

        bingo.launch {
            for ((player) in ps) {
                rewardFirework(player.location)
            }
        }
    }

    fun complete(player: Player) = complete(listOf(player))
}

open class ProgressSquare(
    squareName: String,
    squareIndex: Int,
    val count: Int,
) : Square(squareName, squareIndex) {
    fun increment(player: Player, progress: Int = 1) {
        log(
            "bingo",
            "progress of ".c() + renderName(true) + " by " + player.component().forWhom() + " +$progress, total: $count"
        )
//        if (!matchesDomainRestriction().first) return
        if (!unlockable()) return
        if (player.bingoData()?.allowed != true) return
        val squareData = player.bingoSquare(this) ?: return
        squareData.progress += progress
        if (squareData.progress >= count) complete(player)
        bingo.launch(Dispatchers.IO) {
            bingo.valkeyClient.hset(
                "bingo:player:${player.uniqueId}-progress",
                squareId,
                squareData.progress.toString()
            )
        }
    }
}
