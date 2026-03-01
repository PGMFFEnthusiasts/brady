package me.fireballs.brady.bingo

import me.fireballs.brady.bingo.squares.*
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.plus
import net.kyori.adventure.text.Component
import org.koin.core.component.KoinComponent

class Squares : KoinComponent {
    val grid: Array<Square?> = Array(boardCount) { null }

    init {
        grid[0] = DummySquare("Consejero", 0)
        grid[1] = ABABAB(1)
        grid[2] = Explosion(2)
        grid[3] = Forbidden(3)
        grid[4] = DummySquare("True Faith", 4)

        grid[5] = DummySquare("Hall of Famer", 5)
        grid[6] = UnderPressure(6)
        grid[7] = WWCD(7)
        grid[8] = TakeBacksies(8)
        grid[9] = DummySquare("Streaking", 9)

        grid[10] = DynamicDuo(10)
        grid[11] = Ax2bxc(11)
        grid[12] = CBT(12)
        grid[13] = Effigy(13)
        grid[14] = HotPotato(14)

        grid[15] = DummySquare("True Contributor", 15)
        grid[16] = Cheerleading(16)
        grid[17] = InvisibleSuffering(17)
        grid[18] = Mogged(18)
        grid[19] = DummySquare("Streaking", 19)

        grid[20] = DummySquare("To Whom?", 20)
        grid[21] = ThatWorked(21)
        grid[22] = DyingForPie(22)
        grid[23] = LetMeDownGently(23)
        grid[24] = DummySquare("Crowd goes wild", 24)
    }

    class DummySquare(name: String, squareIndex: Int) : Square(name, squareIndex) {
        override fun renderName(forceDeobfuscation: Boolean): Component {
            if (forceDeobfuscation) return super.renderName(true) + "&c - DUMMY".cc()
            return super.renderName(false)
        }
    }
}
