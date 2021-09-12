package io.github.zaratath.qol;

import com.google.common.primitives.Ints;
import io.github.zaratath.BcordCraft;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BundleListener implements Listener {

    private final NamespacedKey KEY = new NamespacedKey(BcordCraft.getInstance(), "bundlesettings");
    private final Material[] matVals = Material.values();
    private final Map<Inventory, ItemStack> openSettings = new HashMap();
    private final int SETTINGS_SIZE = 27;
    private final int ITEM_LIMIT = 64; //default
    private Inventory inv;
    private final Random SOUNDRANDOM = new Random();

    /**
     *
     * @param meta
     * @return Set of materials in the bundle's settings. Null if none.
     */
    public Set<Material> getBundleSettings(ItemMeta meta) {
        int[] data = meta.getPersistentDataContainer().get(KEY, PersistentDataType.INTEGER_ARRAY);
        if(data == null || data.length == 0) return null;
        HashSet<Material> set = new HashSet();
        for(int i:data){
            set.add(matVals[i]);
        }
        return set;
    }

    public void settingsClick(Inventory settings, ItemStack current, ItemStack cursor) {
        //top inventory click.
        if(openSettings.containsKey(settings)) {
            if(current != null) {
                settings.remove(current.getType());
            }
            if(cursor != null && !settings.contains(cursor.getType())) {
                settings.addItem(new ItemStack(cursor.getType()));
            }
        }
    }

    @EventHandler
    public void bundleSettingsClick(InventoryClickEvent e) {
        InventoryView invView = e.getView();
        Inventory topInventory = invView.getTopInventory();
        if(!openSettings.containsKey(topInventory)) return;
        //bundle settings is top inventory.

        Inventory clickedInventory = invView.getInventory(e.getRawSlot());
        boolean clickedIsSettings;
        if(clickedInventory == topInventory) {
            //all clicks on the bundle inventory are cancelled.
            clickedIsSettings = true;
            e.setCancelled(true);
        }
        else {
            clickedIsSettings = false;
        }

        //no collects with bundle inv open.
        if(e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        ItemStack current = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        ClickType click = e.getClick();
        if(click.isShiftClick()) {
            e.setCancelled(true);
            if(clickedIsSettings) {
                //shift clicked on bundle inv, just remove.
                settingsClick(topInventory, current, null);
            }
            else {
                //shift clicked on player inv, just add.
                settingsClick(topInventory, null, current);
            }
        }

        //no keyboard clicks from bundle inv.
        if(click.isKeyboardClick()) {
            if(e.getRawSlot() < SETTINGS_SIZE) {
                e.setCancelled(true);
                return;
            }
        }

        if(clickedIsSettings) {
            settingsClick(topInventory, current, cursor);
        }
    }

    @EventHandler
    public void bundleSettingsDrag(InventoryDragEvent e) {
        InventoryView invView = e.getView();
        Inventory topInventory = invView.getTopInventory();
        if(!openSettings.containsKey(topInventory)) return;
        //bundle settings is top inventory.

        //cancel if it's a multi slot drag involving the settings inv.
        Set<Integer> slots = e.getRawSlots();
        if(slots.size() > 1) {
            for(int slot:slots) {
                if(slot < SETTINGS_SIZE) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        else {
            //single slot dragged on, handles it as a click normally.
            int slot = slots.stream().findFirst().get();
            Inventory clickedInventory = invView.getInventory(slot);
            //any clicks on the bundle inventory are cancelled.
            if(clickedInventory == topInventory) {
                settingsClick(topInventory, invView.getItem(slot), e.getOldCursor());
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void bundleSettingsOpen(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if(item == null || item.getAmount() == 0) return;
        if(item.getType() != Material.BUNDLE) return;

        if(openSettings.containsKey(e.getInventory())) {
            //cannot touch bundles in bundle inventory.
            e.setCancelled(true);
            return;
        }

        if(!e.isRightClick() || !e.isShiftClick()) return;

        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv.getType() != InventoryType.PLAYER) {
            //can only open settings from player inventory.
            return;
        }

        //opening settings.
        e.setCancelled(true);
        Inventory inv = Bukkit.createInventory(null, SETTINGS_SIZE, Component.text("Bundle Settings"));
        openSettings.put(inv, item);
        Set<Material> settings = getBundleSettings(item.getItemMeta());
        if(settings != null) {
            for(Material mat:settings) {
                inv.addItem(new ItemStack(mat));
            }
        }
        Bukkit.getScheduler().runTask(BcordCraft.getInstance(), () -> {
            e.getWhoClicked().openInventory(inv);
        });
    }

    @EventHandler
    public void bundleSettingsClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        ItemStack bundle = openSettings.remove(inv);
        if(bundle == null) return;

        Set<Integer> settings = new HashSet();
        for(ItemStack item:inv.getContents()) {
            if(item == null || item.getType() == Material.AIR) continue;
            settings.add(item.getType().ordinal());
        }

        BundleMeta meta = (BundleMeta) bundle.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY, PersistentDataType.INTEGER_ARRAY, Ints.toArray(settings));
        bundle.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void pickupEvent(PlayerAttemptPickupItemEvent e) {
        Player player = e.getPlayer();

        Item itemOnGround = e.getItem();
        //dunno when the player can't pickup but w/e
        if (!itemOnGround.canPlayerPickup()) {
            return;
        }

        if(itemOnGround.getPickupDelay() > 0) {
            return;
        }

        ItemStack item = e.getItem().getItemStack();
        Inventory inv = player.getInventory();

        Set<ItemStack> bundles = new HashSet();
        for (ItemStack i : inv.getContents()) {
            if (i == null) continue;
            if (i.getType() == Material.BUNDLE) {
                bundles.add(i);
            }
        }


        //player has no bundles.
        if(bundles.size() == 0) return;

        //how many items there are in the stack.
        int itemScale = getItemScale(item);

        for (ItemStack bundle : bundles) {
            Set<Material> bundleSettings = getBundleSettings(bundle.getItemMeta());
            if (bundleSettings != null && bundleSettings.contains(item.getType())) {
                //how many items are in the stack.
                int itemCount = item.getAmount();
                BundleMeta meta = (BundleMeta) bundle.getItemMeta();
                //counting how far the bundle is away from max storage.
                int bundleCount = 0;
                for(ItemStack i:meta.getItems()) {
                    bundleCount += (i.getAmount() * getItemScale(i));
                }

                //getting how many could possibly fit into the bundle.
                int bundleFit = (int) Math.floor((ITEM_LIMIT - bundleCount) / itemScale);
                int itemFit = Math.min(itemCount - (itemCount - bundleFit), itemCount);
                int totalToPickup = itemFit;

                //adding item to be picked up into bundle instead.
                //for some godforsaken reason, bundlemeta.addItem() doesn't stack with
                //existing items of the same type. so we do it manually here.
                List<ItemStack> metaItems = meta.getItems();
                for(ItemStack itemStack:metaItems) {
                    //closes for loop if we've exhausted the pickup.
                    if(itemFit <= 0) {
                        break;
                    }
                    if(itemStack.isSimilar(item)) {
                        int stackToItemAmt = (itemStack.getMaxStackSize() - itemStack.getAmount());
                        int stackFit = Math.min(itemFit - (itemFit - stackToItemAmt), itemFit);
                        itemStack.add(stackFit);
                        itemFit -= stackFit;
                    }
                }
                meta.setItems(metaItems);
                if(itemFit > 0) {
                    //if there's still more room left after adding to existing stacks.
                    //added after modifying existing items cuz the getItems list is immutable
                    //:vomit:
                    ItemStack newItem = item.clone();
                    newItem.setAmount(itemFit);
                    meta.addItem(newItem);
                }
                bundle.setItemMeta(meta);

                //altering item to be picked up. either deleting or reducing amount.
                item.subtract(totalToPickup);
                if(item.getAmount() <= 0) {
                    Location loc = itemOnGround.getLocation();
                    Sound PICKUP_SOUND = Sound.sound(
                            Key.key("entity.item.pickup"), Sound.Source.MASTER, 0.3f, SOUNDRANDOM.nextFloat()+1.0f
                    );
                    player.playSound(PICKUP_SOUND, loc.getX(), loc.getY(), loc.getZ());

                    itemOnGround.remove();
                    e.setCancelled(true);
                    return;
                }
                else {
                    itemOnGround.setItemStack(item);
                }
            }
        }
    }

    public int getItemScale(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return 0;
        }
        return (ITEM_LIMIT/item.getMaxStackSize());
    }
}
