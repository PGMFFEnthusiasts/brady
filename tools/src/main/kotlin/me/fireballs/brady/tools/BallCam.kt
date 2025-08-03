package me.fireballs.brady.tools

import me.fireballs.brady.core.cc
import me.fireballs.brady.core.itembox
import me.fireballs.brady.core.specialData
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

val toggleBall = itembox(Material.SNOW_BALL)
    .name("&f&lBall Cam".cc())
    .specialData("tools:toggle-ballcam")

fun isToggleBall(item: ItemStack?) =
    item?.specialData() == "tools:toggle-ballcam"
