package me.fireballs.brady.tools

import me.fireballs.brady.core.BooleanSettingValue
import me.fireballs.brady.core.Settings
import me.fireballs.brady.core.cc
import me.fireballs.brady.core.createEnumSetting
import me.fireballs.brady.core.itembox
import me.fireballs.brady.core.plus
import me.fireballs.brady.tools.pvpfx.ProjectileSkins
import net.kyori.adventure.text.Component.newline
import org.bukkit.Material
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToolsSettings : KoinComponent {
    private val settings by inject<Settings>()

    val splatSetting = BooleanSettingValue(
        "settings.splat",
        true,
        "&aSplat".cc(),
        "&7Makes a thump sound on ball land.".cc(),
        itembox(Material.SLIME_BALL),
    )

    val hideArmor = BooleanSettingValue(
        "settings.hidearmor",
        false,
        "&aHide Armor".cc(),
        "&7Hide the armor on everyone.".cc(),
        itembox(Material.LEATHER_CHESTPLATE),
    )

    val projectileSkin = createEnumSetting<ProjectileSkins>(
        "settings.projectileskin",
        "&bProjectile Skin".cc(),
        "&7Cosmetically change your projectile.".cc(),
        itembox(Material.SNOW_BALL),
    )

    val jumpResetParticles = BooleanSettingValue(
        "settings.jumpreset",
        true,
        "&5Jump Reset Particles".cc(),
        "&7Plays purple jump reset particles".cc() + newline() + "&7when a player does a jump reset.",
        itembox(Material.ENCHANTMENT_TABLE),
    )

    init {
        settings.addAll(
            splatSetting,
            hideArmor,
            projectileSkin,
            jumpResetParticles,
        )
    }
}