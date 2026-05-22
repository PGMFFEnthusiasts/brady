package me.fireballs.brady.tools;

import me.fireballs.brady.corepgm.PGMExtensionsKt;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.koin.java.KoinJavaComponent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

import java.util.Set;

public class DontDropSword implements Listener {

    private static final Set<Material> WEAPONS = Set.of(
            Material.WOOD_SWORD, Material.GOLD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD,
            Material.WOOD_AXE, Material.GOLD_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE,
            Material.WOOD_PICKAXE, Material.GOLD_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.WOOD_SPADE, Material.GOLD_SPADE, Material.STONE_SPADE, Material.IRON_SPADE, Material.DIAMOND_SPADE,
            Material.WOOD_HOE, Material.GOLD_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.DIAMOND_HOE
    );
    
    public DontDropSword() {
        Tools plugin = KoinJavaComponent.get(Tools.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Match match = PGMExtensionsKt.currentMatch(PGM.get().getMatchManager());
        if (match == null) return;
        if (!PGMExtensionsKt.isTouchdown(match.getMap())) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (!isWeapon(item)) return;

        event.setCancelled(true);
    }

    private static boolean isWeapon(ItemStack itemStack) {
        if (itemStack == null) return false;

        boolean isWeapon = WEAPONS.contains(itemStack.getType());
        boolean hasSharpness = itemStack.containsEnchantment(Enchantment.DAMAGE_ALL);

        return isWeapon || hasSharpness;
    }
}