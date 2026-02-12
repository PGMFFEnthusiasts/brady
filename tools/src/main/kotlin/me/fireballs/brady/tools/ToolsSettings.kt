package me.fireballs.brady.tools

import me.fireballs.brady.core.*
import me.fireballs.brady.tools.pvpfx.ProjectileSkins
import net.kyori.adventure.text.Component.newline
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent

private val preferencesItem = itembox(Material.REDSTONE_COMPARATOR)
    .name("&b&lPreferences".cc())
    .specialData("tools:preferences")

private fun isPreferencesItem(item: ItemStack?) =
    item?.specialData() == "tools:preferences"

class ToolsSettings : Listener, KoinComponent {
    private val tools by inject<Tools>()
    private val settings by inject<Settings>()

    val splatSetting = BooleanSettingValue(
        "settings.splat",
        true,
        "&aSplat".cc(),
        listOf("&7Makes a thump sound on ball land.".cc()),
        itembox(Material.SLIME_BALL),
    )

    val hideArmor = BooleanSettingValue(
        "settings.hidearmor",
        false,
        "&aHide Armor".cc(),
        listOf("&7Hide the armor on everyone.".cc()),
        itembox(Material.LEATHER_CHESTPLATE),
    )

    val projectileSkin = createEnumSetting<ProjectileSkins>(
        "settings.projectileskin",
        "&bProjectile Skin".cc(),
        listOf("&7Cosmetically change your projectile.".cc()),
        itembox(Material.ENDER_PEARL),
    )

    val ballProjection = BooleanSettingValue(
        "settings.ballprojection",
        true,
        "&dBall Projection".cc(),
        listOf("&7Shows where a projectile may land".cc(), "&7only while observing.".cc()),
        itembox(Material.EYE_OF_ENDER),
    )

    val jumpResetParticles = BooleanSettingValue(
        "settings.jumpreset",
        true,
        "&5Jump Reset Particles".cc(),
        listOf("&7Plays purple jump reset particles".cc(), "&7when a player does a jump reset.".cc()),
        itembox(Material.ENCHANTMENT_TABLE),
    )

    val dayvision = BooleanSettingValue(
        "settings.dayvision",
        false,
        "&bDayvision".cc(),
        listOf("&7Blocks all nightvision.".cc()),
        itembox(Material.DAYLIGHT_DETECTOR),
    )

    init {
        settings.addAll(
            splatSetting,
            hideArmor,
            projectileSkin,
            jumpResetParticles,
            ballProjection,
            dayvision
        )

        tools.registerEvents(this)
    }

    @EventHandler
    private fun giveKit(event: ObserverKitApplyEvent) {
        event.player.bukkit.inventory.setItem(6, preferencesItem.build())
    }

    @EventHandler
    private fun handlePreferencesItem(event: PlayerInteractEvent) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return
        if (!isPreferencesItem(event.getItem())) return
        settings.openSettingsPage(event.player)
    }
}
