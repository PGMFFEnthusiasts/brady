package me.fireballs.brady.core

import de.tr7zw.changeme.nbtapi.NBT
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT
import net.kyori.adventure.text.Component
import net.minecraft.server.v1_8_R3.Block
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.material.MaterialData

fun itembox(itemType: Material = Material.DIRT): ItemBox {
    return ItemBox(ItemStack(itemType))
}

class ItemBox(
    val stack: ItemStack,
) {
    fun itemMeta(builder: ItemMeta.() -> Unit): ItemBox {
        val itemMeta = stack.itemMeta
        builder(itemMeta)
        stack.itemMeta = itemMeta
        return this
    }

    fun materialData(builder: MaterialData.() -> Unit): ItemBox {
        val data = stack.data
        builder(data)
        stack.data = data
        return this
    }

    fun name(name: String) = itemMeta { displayName = name }
    fun name(name: Component) = itemMeta { displayName = name.coloredText() }

    fun lore(vararg lines: String) = itemMeta { lore = lines.toList() }
    fun loreLines(lines: Iterable<String>) = itemMeta { lore = lines.toList() }
    fun lore(vararg components: Component) = itemMeta { lore = components.map { it.coloredText() }.toList() }
    fun loreComponentLines(components: Iterable<Component>) = itemMeta { lore = components.map { it.coloredText() }.toList() }

    fun appendLore(vararg lines: String) = itemMeta { lore = lore + lines.toList() }

    fun shiny() = itemMeta {
        addEnchant(Enchantment.ARROW_INFINITE, 0, true)
        addItemFlags(ItemFlag.HIDE_ENCHANTS)
    }

    fun unbreakable() = itemMeta {
        addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        spigot().isUnbreakable = true
    }

    fun hideAttributes() = itemMeta { addItemFlags(ItemFlag.HIDE_ATTRIBUTES) }

    fun count(n: Int): ItemBox {
        stack.amount = n
        return this
    }

    fun nbt(builder: ReadWriteItemNBT.() -> Unit): ItemBox {
        NBT.modify(stack) { builder(it) }
        return this
    }

    fun specialData(data: String): ItemBox {
        return nbt { setString("special", data) }
    }

    fun breaksEverything(): ItemBox {
        nbt {
            val canBreak = getStringList("CanDestroy")
            for ((index, value) in Block.REGISTRY.withIndex()) {
                if (value == null) continue
                canBreak.add(index.toString())
            }
        }
        itemMeta { addItemFlags(ItemFlag.HIDE_DESTROYS) }
        return this
    }

    fun build() = stack
}

fun ItemStack.specialData(): String? {
    if (type == Material.AIR) return null
    val readNbt = NBT.readNbt(this)
    if (!readNbt.keys.contains("special")) return null
    return readNbt.getString("special")
}

fun ItemStack.boxed() = ItemBox(this.clone())
