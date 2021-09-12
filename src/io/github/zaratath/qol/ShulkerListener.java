package io.github.zaratath.qol;

import io.github.zaratath.BcordCraft;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ShulkerListener implements Listener {

    private static Set<Material> SHULKERS = Tag.SHULKER_BOXES.getValues();

    private static Map<ItemStack, Inventory> OpenInventories = new HashMap();

    private boolean isInShulkerBox(int rawSlot) {
        return rawSlot >= 0 && rawSlot < 27;
    }

    private int toRawSlot(int slot) {
        return slot >= 0 && slot < 9 ? slot + 54 : slot + 18;
    }

    private int toSlot(int rawSlot) {
        return rawSlot >= 54 ? rawSlot - 54 : rawSlot - 18;
    }

    public void shulkerModify() {

    }

    @EventHandler
    public void openShulker(InventoryClickEvent e) {

        //dunno why it'd be null
        if(e.getCursor() == null || e.getCursor().getType() != Material.AIR) return;
        if(!e.isRightClick()) return;

        ItemStack item = e.getCurrentItem();
        if(item == null) return;
        System.out.println(item == null);
        System.out.println(!SHULKERS.contains(item.getType()));
        if(!SHULKERS.contains(item.getType())) return;
        //right clicked a shulker box in an inventory.
        e.setCancelled(true);

        //WEW BUDDY
        ShulkerBox meta = (ShulkerBox) ((BlockStateMeta) item.getItemMeta()).getBlockState();

        OpenInventories.put(item, meta.getInventory());
        Bukkit.getScheduler().runTask(BcordCraft.getInstance(), () -> {
            e.getWhoClicked().openInventory(meta.getInventory());
        });

    }

    public void closeShulker(InventoryCloseEvent e) {
        HumanEntity player = e.getPlayer();

    }
}
