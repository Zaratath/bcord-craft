package io.github.zaratath.enchantments;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;


public class EnchantmentListener implements Listener {
    public static final Enchantment[] AXE_ENCHANTS = {
            Enchantment.LOOT_BONUS_MOBS,
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT
    };

    public static final Set<Material> AXES = new HashSet<>(Arrays.asList(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    ));

    public static final Map<Enchantment, Integer> ENCHANT_MULTI = Map.of(
            Enchantment.LOOT_BONUS_MOBS, 2,
            Enchantment.KNOCKBACK, 1,
            Enchantment.FIRE_ASPECT, 2
    );

    @EventHandler
    public void AxeLooting(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack leftItem = inv.getFirstItem();
        ItemStack rightItem = inv.getSecondItem();

        //needs 2 items
        if((leftItem == null) || (rightItem == null)) {
            return;
        }
        //must be an axe
        if(!AXES.contains(leftItem.getType()))
            return;
        //must use a book to slam an axe
        if(!rightItem.getType().equals(Material.ENCHANTED_BOOK))
            return;

        ItemMeta leftData = leftItem.getItemMeta();
        EnchantmentStorageMeta rightData = (EnchantmentStorageMeta) rightItem.getItemMeta();

        ItemStack result = event.getResult();
        if(result == null) {
            result = leftItem.clone();
        }

        ItemMeta resultMeta = result.getItemMeta();

        int addedCost = 0;

        for(Enchantment e:AXE_ENCHANTS) {
            int axeLvl = leftData.getEnchantLevel(e);
            int bookLvl = rightData.getStoredEnchantLevel(e);

            //book has the enchant
            if(bookLvl > 0) {
                            //level is the same, add one but not over max. if not, max between the two items.
                int newLvl = (bookLvl == axeLvl ? Math.max(e.getMaxLevel(), bookLvl+1) : Math.max(axeLvl, bookLvl));
                resultMeta.addEnchant(e, newLvl, false);

                addedCost += newLvl*ENCHANT_MULTI.get(e);
            }
        }

        result.setItemMeta(resultMeta);

        inv.setRepairCost(inv.getRepairCost() + addedCost);
        event.setResult(result);
    }
}
