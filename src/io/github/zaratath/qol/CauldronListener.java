package io.github.zaratath.qol;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CauldronListener implements Listener {
                            //toDrop,    <Block, Block result>
    /**public static final Map<Material, Map<Material, Material>> dispenseMap = Map.of(
        Material.BUCKET, Map.of(Material.LAVA_CAULDRON, Material.CAULDRON),
        Material.GLASS_BOTTLE, Map.of(Material.WATER, Material.WATER)
            );**/ //a lil ambitious but maybe do this later.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void dispenserEvent(BlockDispenseEvent e) {
        //making sure it's a dispenser. (dropper might use this event too i think?)
        Block block = e.getBlock();
        if(block.getType() != Material.DISPENSER) return;
        Dispenser dispenser = (Dispenser) block.getBlockData();

        //not dispensing a bucket.
        ItemStack toDrop = e.getItem();
        if(toDrop.getType() != Material.BUCKET) return;
        //dispensing a bucket into a lava cauldron.
        Block facing = block.getRelative(dispenser.getFacing());
        if(facing.getType() != Material.LAVA_CAULDRON) return;

        //cauldron is for sure getting emptied here, so empty it.
        facing.setType(Material.CAULDRON);

        Container container = (Container) block.getState();
        Inventory inv = container.getInventory();
        //finding space to put lava in dispenser inventory.
        int firstEmpty = inv.firstEmpty();
        if(firstEmpty >= 0) {
            //there's an empty space!
            inv.addItem(new ItemStack(Material.LAVA_BUCKET));
            inv.getItem(inv.first(Material.BUCKET)).subtract();
            e.setCancelled(true);
            return;
        }

        //no space in inv, toss bucket on ground.*/
        e.setItem(new ItemStack(Material.LAVA_BUCKET));
        inv.getItem(inv.first(Material.BUCKET)).subtract();
    }
}
