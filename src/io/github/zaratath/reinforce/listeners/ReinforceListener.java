package io.github.zaratath.reinforce.listeners;

import io.github.zaratath.playerdata.PlayerAPI;
import io.github.zaratath.playerdata.PlayerWrapper;
import io.github.zaratath.playerdata.guilds.Guild;
import io.github.zaratath.reinforce.Reinforcement;
import io.github.zaratath.reinforce.ReinforcementType;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class ReinforceListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void reinforceBlock(final PlayerInteractEvent event) {
        //sneaking punch.
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Guild guild = PlayerAPI.getAPI().getWrapper(player.getUniqueId()).guild;
        //player is in a guild.
        if(guild == null) return;
        //item in hand.
        final ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
        if (itemStack.getAmount() == 0) return;
        //block exists.
        final Block block = event.getClickedBlock();
        assert block != null;
        //held item is a reinforcement item.
        ReinforcementType type = ReinforcementType.getType(itemStack.getType());
        if(type == null) return;
        //no reinforcement on the block already.

        //makes a new reinforcement for the block.
        Reinforcement reinforcement = new Reinforcement(type, guild, type.getDurability());
        if(Reinforcement.newReinforcement(block, reinforcement)) {
            itemStack.setAmount(itemStack.getAmount()-1);
            sendReinforcementBar(player, reinforcement);
        };
    }

     public void sendReinforcementBar(Player player, Reinforcement reinf) {
        player.sendActionBar(Component.join(
                Component.text(" - "),
                Component.text("["+reinf.guild.getName()+"]"),
                Component.text(ChatColor.DARK_AQUA.toString() + reinf.durability + " / " + reinf.type.getDurability())
        ));
    }

    /**
     * Gives a player an action bar for the reinforcement blocks they're viewing.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void moveEvent(final PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Block block = player.getTargetBlockExact(4);
        if(block == null) {
            player.sendActionBar(Component.text(""));
            return;
        }

        Reinforcement reinf = Reinforcement.getReinforcement(block);
        if(reinf == null) {
            player.sendActionBar(Component.text(""));
            return;
        }
        sendReinforcementBar(player, reinf);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockBreakEvent(BlockBreakEvent event) {
        Block block = event.getBlock();
        if(block == null) return;

        Reinforcement reinf = Reinforcement.getReinforcement(event.getBlock());
        if(reinf == null) return;

        Player player = event.getPlayer();

        Guild playerGuild = PlayerAPI.getAPI().getWrapper(player.getUniqueId()).guild;
        if(reinf.guild == playerGuild) {
            Reinforcement.removeReinforcement(block);
            if(reinf.durability >= reinf.type.getDurability()) {
                player.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(reinf.type.getMaterial(), 1));
            }
            return;
        }
        else {
            reinf.durability -= 1;
            Reinforcement.updateReinforcement(block, reinf);
            if (reinf.durability > 0) {
                event.setCancelled(true);
                sendReinforcementBar(player, reinf);
                return;
            }
            player.sendActionBar(Component.text(""));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockExplosionEvent(EntityExplodeEvent e) {
        Iterator<Block> iterator = e.blockList().iterator();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Reinforcement reinf = Reinforcement.getReinforcement(block);
            if (reinf == null) {
                continue;
            }
            reinf.durability -= 20;
            Reinforcement.updateReinforcement(block, reinf);
            if (reinf.durability > 0) {
                iterator.remove();
            }
        }
    }

    // prevent opening reinforced things
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void openContainer(PlayerInteractEvent e) {
        if (!e.hasBlock()) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Reinforcement rein = Reinforcement.getReinforcement(e.getClickedBlock());
        if (rein == null) {
            return;
        }
        PlayerWrapper wrapper = PlayerAPI.getAPI().getWrapper(e.getPlayer().getUniqueId());
        if (e.getClickedBlock().getState() instanceof Container) {
            if (wrapper.guild != rein.guild) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(
                        String.format(
                                "%s is reinforced by %s%s", e.getClickedBlock().getType().name(),
                        ChatColor.RED, rein.guild.getName()));
            }
            return;
        }
        if (e.getClickedBlock().getBlockData() instanceof Openable) {
            if (wrapper.guild != rein.guild) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(
                        String.format(
                                "%s is reinforced by %s%s", e.getClickedBlock().getType().name(),
                                ChatColor.RED, rein.guild.getName()));
            }
        }
    }
}
